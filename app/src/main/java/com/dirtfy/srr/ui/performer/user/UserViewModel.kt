package com.dirtfy.srr.ui.performer.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dirtfy.srr.core.model.Evaluation
import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.repository.EvaluationRepository
import com.dirtfy.srr.core.repository.FeatureRepository
import com.dirtfy.srr.core.repository.ItemRepository
import com.dirtfy.srr.core.repository.UserAccountRepository
import com.dirtfy.srr.core.scoring.DefaultFeatureScoringEngine
import com.dirtfy.srr.core.usecase.LoadFeatureScoresUseCase
import com.dirtfy.srr.remote.repository.RemoteEvaluationRepository
import com.dirtfy.srr.remote.repository.RemoteFeatureRepository
import com.dirtfy.srr.remote.repository.RemoteItemRepository
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
    private val loadFeatureScoresUseCase: LoadFeatureScoresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    // Raw evaluations cached so the editor can pre-populate without an extra Firestore call
    private var cachedEvaluations: Map<String, List<Evaluation>> = emptyMap()

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            loadFeatureScoresUseCase.execute()
                .onSuccess { output ->
                    cachedEvaluations = output.evaluationsByFeature
                    val userId = userAccountRepository.currentUserId() ?: ""
                    val evaluatedFeatureIds = output.evaluationsByFeature
                        .filterValues { evals -> evals.any { it.userId == userId } }
                        .keys
                    _uiState.value = UserUiState.Ready(
                        items                = output.items,
                        features             = output.features,
                        scoreMatrix          = output.scoreMatrix,
                        currentUserId        = userId,
                        evaluatedFeatureIds  = evaluatedFeatureIds
                    )
                }
                .onFailure { e -> _uiState.value = UserUiState.Error(e.message ?: "Failed to load") }
        }
    }

    fun onRetryTap() = loadAllData()

    // Debator fix #5: clear selections when switching tabs
    fun onTabSelected(tab: UserUiState.Tab) {
        (_uiState.value as? UserUiState.Ready)?.let {
            _uiState.value = it.copy(
                activeTab       = tab,
                selectedItem    = null,
                selectedFeature = null,
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
        _uiState.value = state.copy(addItemDialog = dialog.copy(isSaving = true, error = null))
        viewModelScope.launch {
            itemRepository.createItem(dialog.name.trim())
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
            featureRepository.createFeature(dialog.name.trim())
                .onSuccess { loadAllData() }
                .onFailure { e ->
                    val s = _uiState.value as? UserUiState.Ready ?: return@onFailure
                    val current = s.addFeatureDialog ?: return@onFailure
                    _uiState.value = s.copy(addFeatureDialog = current.copy(isSaving = false, error = e.message))
                }
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
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
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
