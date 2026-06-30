package com.dirtfy.srr.ui.performer.user

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dirtfy.srr.core.model.Evaluation
import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import android.net.Uri
import com.dirtfy.srr.core.repository.EvaluationRepository
import com.dirtfy.srr.core.repository.FeatureRepository
import com.dirtfy.srr.core.repository.ItemRepository
import com.dirtfy.srr.core.repository.StorageRepository
import com.dirtfy.srr.core.repository.UserAccountRepository
import com.dirtfy.srr.core.scoring.DefaultFeatureScoringEngine
import com.dirtfy.srr.core.usecase.LoadFeatureScoresUseCase
import com.dirtfy.srr.remote.repository.RemoteEvaluationRepository
import com.dirtfy.srr.remote.repository.RemoteFeatureRepository
import com.dirtfy.srr.remote.repository.RemoteItemRepository
import com.dirtfy.srr.remote.repository.RemoteStorageRepository
import com.dirtfy.srr.remote.repository.RemoteUserAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val userAccountRepository: UserAccountRepository,
    private val evaluationRepository: EvaluationRepository,
    private val itemRepository: ItemRepository,
    private val featureRepository: FeatureRepository,
    private val storageRepository: StorageRepository,
    private val loadFeatureScoresUseCase: LoadFeatureScoresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private var cachedEvaluations: Map<String, List<Evaluation>> = emptyMap()

    init {
        loadAllData()
    }

    fun loadAllData() {
        val previousTab = (_uiState.value as? UserUiState.Ready)?.activeTab ?: UserUiState.Tab.ITEMS
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            loadFeatureScoresUseCase.execute()
                .onSuccess { output ->
                    cachedEvaluations = output.evaluationsByFeature
                    val userId = userAccountRepository.currentUserId() ?: ""
                    val evaluatedFeatureIds = output.evaluationsByFeature
                        .filterValues { evals -> evals.any { it.userId == userId } }
                        .keys
                    val myEvaluationByFeature = output.evaluationsByFeature
                        .mapValues { (_, evals) ->
                            evals.find { it.userId == userId }?.orderedItemIds ?: emptyList()
                        }
                    val evaluatorCountByFeature = output.evaluationsByFeature
                        .mapValues { (_, evals) -> evals.size }
                    _uiState.value = UserUiState.Ready(
                        items                    = output.items,
                        features                 = output.features,
                        scoreMatrix              = output.scoreMatrix,
                        currentUserId            = userId,
                        evaluatedFeatureIds      = evaluatedFeatureIds,
                        myEvaluationByFeature    = myEvaluationByFeature,
                        evaluatorCountByFeature  = evaluatorCountByFeature,
                        activeTab                = previousTab
                    )
                }
                .onFailure { e -> _uiState.value = UserUiState.Error(e.message ?: "Failed to load") }
        }
    }

    fun onRetryTap() = loadAllData()

    fun onTabSelected(tab: UserUiState.Tab) {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(
                activeTab        = tab,
                selectedItem     = null,
                selectedFeature  = null,
                evaluationEditor = null
            )
        }
    }

    fun onItemSelected(item: Item) {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(selectedItem = item)
        }
    }

    fun onFeatureSelected(feature: Feature) {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(selectedFeature = feature)
        }
    }

    fun onOpenEvaluationEditor(featureId: String) {
        val state = _uiState.value as? UserUiState.Ready ?: return
        val existingOrder = cachedEvaluations[featureId]
            ?.find { it.userId == state.currentUserId }
            ?.orderedItemIds
            ?: state.items.map { it.id }
        _uiState.value = state.copy(
            evaluationEditor = UserUiState.Ready.EvaluationEditorState(featureId, existingOrder)
        )
    }

    fun onEvaluationReorder(newOrder: List<String>) {
        val state  = _uiState.value as? UserUiState.Ready ?: return
        val editor = state.evaluationEditor ?: return
        _uiState.value = state.copy(evaluationEditor = editor.copy(orderedItemIds = newOrder))
    }

    fun onSubmitEvaluation() {
        val state  = _uiState.value as? UserUiState.Ready ?: return
        val editor = state.evaluationEditor ?: return
        _uiState.value = state.copy(evaluationEditor = editor.copy(isSaving = true, saveError = null))
        viewModelScope.launch {
            evaluationRepository.submitEvaluation(
                Evaluation(state.currentUserId, editor.featureId, editor.orderedItemIds)
            )
            .onSuccess { loadAllData() }
            .onFailure { e ->
                val s = _uiState.value as? UserUiState.Ready ?: return@onFailure
                val current = s.evaluationEditor ?: return@onFailure
                _uiState.value = s.copy(evaluationEditor = current.copy(isSaving = false, saveError = e.message))
            }
        }
    }

    fun onOpenEditItemImageDialog(itemId: String) {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(editItemImageDialog = UserUiState.Ready.EditItemImageDialogState(itemId))
        }
    }

    fun onEditItemImagePicked(uri: Uri) {
        val state = _uiState.value as? UserUiState.Ready ?: return
        val dialog = state.editItemImageDialog ?: return
        _uiState.value = state.copy(
            editItemImageDialog = dialog.copy(imageUri = uri, imageUrl = null, isUploadingImage = true, error = null)
        )
        viewModelScope.launch {
            storageRepository.uploadItemImage(uri)
                .onSuccess { url ->
                    val s = _uiState.value as? UserUiState.Ready ?: return@onSuccess
                    val d = s.editItemImageDialog ?: return@onSuccess
                    _uiState.value = s.copy(editItemImageDialog = d.copy(imageUrl = url, isUploadingImage = false))
                }
                .onFailure { e ->
                    val s = _uiState.value as? UserUiState.Ready ?: return@onFailure
                    val d = s.editItemImageDialog ?: return@onFailure
                    _uiState.value = s.copy(
                        editItemImageDialog = d.copy(imageUri = null, imageUrl = null, isUploadingImage = false,
                            error = "Image upload failed: ${e.message}")
                    )
                }
        }
    }

    fun onSubmitEditItemImage() {
        val state = _uiState.value as? UserUiState.Ready ?: return
        val dialog = state.editItemImageDialog ?: return
        if (dialog.isUploadingImage || dialog.imageUrl == null) return
        _uiState.value = state.copy(editItemImageDialog = dialog.copy(isSaving = true, error = null))
        viewModelScope.launch {
            itemRepository.updateItemImage(dialog.itemId, dialog.imageUrl)
                .onSuccess { loadAllData() }
                .onFailure { e ->
                    val s = _uiState.value as? UserUiState.Ready ?: return@onFailure
                    val d = s.editItemImageDialog ?: return@onFailure
                    _uiState.value = s.copy(editItemImageDialog = d.copy(isSaving = false, error = e.message))
                }
        }
    }

    fun onDismissEditItemImageDialog() {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(editItemImageDialog = null)
        }
    }

    fun onOpenAddItemDialog() {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(addItemDialog = UserUiState.Ready.AddItemDialogState())
        }
    }

    fun onOpenAddFeatureDialog() {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(addFeatureDialog = UserUiState.Ready.AddFeatureDialogState())
        }
    }

    fun onAddItemNameChange(name: String) {
        val state = _uiState.value as? UserUiState.Ready ?: return
        val dialog = state.addItemDialog ?: return
        _uiState.value = state.copy(addItemDialog = dialog.copy(name = name, error = null))
    }

    fun onAddItemImagePicked(uri: Uri) {
        val state = _uiState.value as? UserUiState.Ready ?: return
        val dialog = state.addItemDialog ?: return
        _uiState.value = state.copy(
            addItemDialog = dialog.copy(imageUri = uri, imageUrl = null, isUploadingImage = true, error = null)
        )
        viewModelScope.launch {
            storageRepository.uploadItemImage(uri)
                .onSuccess { url ->
                    val s = _uiState.value as? UserUiState.Ready ?: return@onSuccess
                    val d = s.addItemDialog ?: return@onSuccess
                    _uiState.value = s.copy(addItemDialog = d.copy(imageUrl = url, isUploadingImage = false))
                }
                .onFailure { e ->
                    val s = _uiState.value as? UserUiState.Ready ?: return@onFailure
                    val d = s.addItemDialog ?: return@onFailure
                    _uiState.value = s.copy(
                        addItemDialog = d.copy(imageUri = null, imageUrl = null, isUploadingImage = false,
                            error = "Image upload failed: ${e.message}")
                    )
                }
        }
    }

    fun onAddFeatureNameChange(name: String) {
        val state = _uiState.value as? UserUiState.Ready ?: return
        val dialog = state.addFeatureDialog ?: return
        _uiState.value = state.copy(addFeatureDialog = dialog.copy(name = name, error = null))
    }

    fun onDismissAddDialog() {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(addItemDialog = null, addFeatureDialog = null)
        }
    }

    fun onAddItem() {
        val state  = _uiState.value as? UserUiState.Ready ?: return
        val dialog = state.addItemDialog ?: return
        if (dialog.isUploadingImage) return
        _uiState.value = state.copy(addItemDialog = dialog.copy(isSaving = true, error = null))
        viewModelScope.launch {
            itemRepository.createItem(
                name      = dialog.name.trim(),
                createdBy = state.currentUserId,
                imageUrl  = dialog.imageUrl
            )
                .onSuccess { loadAllData() }
                .onFailure { e ->
                    val s = _uiState.value as? UserUiState.Ready ?: return@onFailure
                    val current = s.addItemDialog ?: return@onFailure
                    _uiState.value = s.copy(addItemDialog = current.copy(isSaving = false, error = e.message))
                }
        }
    }

    fun onAddFeature() {
        val state  = _uiState.value as? UserUiState.Ready ?: return
        val dialog = state.addFeatureDialog ?: return
        _uiState.value = state.copy(addFeatureDialog = dialog.copy(isSaving = true, error = null))
        viewModelScope.launch {
            featureRepository.createFeature(dialog.name.trim(), state.currentUserId)
                .onSuccess { loadAllData() }
                .onFailure { e ->
                    val s = _uiState.value as? UserUiState.Ready ?: return@onFailure
                    val current = s.addFeatureDialog ?: return@onFailure
                    _uiState.value = s.copy(addFeatureDialog = current.copy(isSaving = false, error = e.message))
                }
        }
    }

    fun onRequestDeleteItem(id: String, name: String) {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(
                deleteConfirmation = UserUiState.Ready.DeleteConfirmationState(
                    id   = id,
                    name = name,
                    type = UserUiState.Ready.DeleteTargetType.ITEM
                )
            )
        }
    }

    fun onRequestDeleteFeature(id: String, name: String) {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(
                deleteConfirmation = UserUiState.Ready.DeleteConfirmationState(
                    id   = id,
                    name = name,
                    type = UserUiState.Ready.DeleteTargetType.FEATURE
                )
            )
        }
    }

    fun onDismissDeleteConfirmation() {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(deleteConfirmation = null)
        }
    }

    fun onConfirmDelete() {
        val state = _uiState.value as? UserUiState.Ready ?: return
        val target = state.deleteConfirmation ?: return
        _uiState.value = state.copy(deleteConfirmation = null)
        viewModelScope.launch {
            when (target.type) {
                UserUiState.Ready.DeleteTargetType.ITEM -> {
                    itemRepository.deleteItem(target.id).also { result ->
                        if (result.isSuccess) {
                            val featureIds = state.features.map { it.id }
                            evaluationRepository.removeItemFromEvaluations(target.id, featureIds)
                        }
                    }
                }
                UserUiState.Ready.DeleteTargetType.FEATURE -> {
                    // Evaluations are deleted first so the feature document still exists
                    // when the security rule checks features/{featureId}.createdBy.
                    evaluationRepository.deleteEvaluationsForFeature(target.id)
                    featureRepository.deleteFeature(target.id)
                }
            }.onSuccess { loadAllData() }
        }
    }

    fun clearSelection() {
        val state = _uiState.value as? UserUiState.Ready ?: return
        _uiState.value = when {
            state.evaluationEditor != null -> state.copy(evaluationEditor = null)
            state.selectedFeature  != null -> state.copy(selectedFeature = null)
            else                           -> state.copy(selectedItem = null)
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val evaluationRepo = RemoteEvaluationRepository()
                val itemRepo       = RemoteItemRepository()
                val featureRepo    = RemoteFeatureRepository()
                return UserViewModel(
                    userAccountRepository    = RemoteUserAccountRepository(),
                    evaluationRepository     = evaluationRepo,
                    itemRepository           = itemRepo,
                    featureRepository        = featureRepo,
                    storageRepository        = RemoteStorageRepository(application),
                    loadFeatureScoresUseCase = LoadFeatureScoresUseCase(
                        itemRepository       = itemRepo,
                        featureRepository    = featureRepo,
                        evaluationRepository = evaluationRepo,
                        scoringEngine        = DefaultFeatureScoringEngine()
                    )
                ) as T
            }
        }
    }
}
