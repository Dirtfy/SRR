package com.dirtfy.srr.ui.performer.compilation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.scoring.DefaultFeatureScoringEngine
import com.dirtfy.srr.core.usecase.LoadFeatureScoresUseCase
import com.dirtfy.srr.remote.repository.RemoteEvaluationRepository
import com.dirtfy.srr.remote.repository.RemoteFeatureRepository
import com.dirtfy.srr.remote.repository.RemoteItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CompilationViewModel(
    private val loadFeatureScoresUseCase: LoadFeatureScoresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CompilationUiState>(CompilationUiState.Loading)
    val uiState: StateFlow<CompilationUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = CompilationUiState.Loading
            loadFeatureScoresUseCase.execute()
                .onSuccess { output ->
                    _uiState.value = CompilationUiState.Ready(
                        items                   = output.items,
                        features                = output.features,
                        scoreMatrix             = output.scoreMatrix,
                        evaluatorCountByFeature = output.evaluationsByFeature
                            .mapValues { (_, evals) -> evals.size }
                    )
                }
                .onFailure { e -> _uiState.value = CompilationUiState.Error(e.message ?: "Failed to load") }
        }
    }

    fun onRetryTap() = loadAllData()

    fun onTabSelected(tab: CompilationUiState.Tab) {
        (_uiState.value as? CompilationUiState.Ready)?.let {
            _uiState.value = it.copy(
                activeTab       = tab,
                selectedItem    = null,
                selectedFeature = null
            )
        }
    }

    fun onItemSelected(item: Item) {
        (_uiState.value as? CompilationUiState.Ready)?.let {
            _uiState.value = it.copy(selectedItem = item)
        }
    }

    fun onFeatureSelected(feature: Feature) {
        (_uiState.value as? CompilationUiState.Ready)?.let {
            _uiState.value = it.copy(selectedFeature = feature)
        }
    }

    fun onMapXFeatureSelected(featureId: String) {
        (_uiState.value as? CompilationUiState.Ready)?.let {
            _uiState.value = it.copy(mapXFeatureId = featureId)
        }
    }

    fun onMapYFeatureSelected(featureId: String) {
        (_uiState.value as? CompilationUiState.Ready)?.let {
            _uiState.value = it.copy(mapYFeatureId = featureId)
        }
    }

    fun clearSelection() {
        val state = _uiState.value as? CompilationUiState.Ready ?: return
        _uiState.value = when {
            state.selectedItem  != null -> state.copy(selectedItem = null)
            else                        -> state.copy(selectedFeature = null)
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CompilationViewModel(
                    loadFeatureScoresUseCase = LoadFeatureScoresUseCase(
                        itemRepository       = RemoteItemRepository(),
                        featureRepository    = RemoteFeatureRepository(),
                        evaluationRepository = RemoteEvaluationRepository(),
                        scoringEngine        = DefaultFeatureScoringEngine()
                    )
                ) as T
        }
    }
}
