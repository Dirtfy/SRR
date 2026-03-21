package com.dirtfy.srr.viewmodel.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dirtfy.srr.view.mine.features.Item as RatingItem
import com.dirtfy.srr.view.mine.items.Item as GridItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MineViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MineUiState())
    val uiState: StateFlow<MineUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun toggleView() {
        _uiState.update { it.copy(isFeaturesView = !it.isFeaturesView) }
    }

    // --- Grid Navigation ---
    fun selectItem(item: GridItem) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedItem = null,
                selectedFeature = null
            )
        }
    }

    // --- Feature Popup Navigation ---
    fun selectFeature(feature: RatingItem) {
        _uiState.update { it.copy(selectedFeature = feature) }
    }

    fun clearFeatureSelection() {
        _uiState.update { it.copy(selectedFeature = null) }
    }

    fun loadAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(1000)

            val mockGridItems = listOf(
                GridItem("Premium Plan", android.R.drawable.ic_menu_agenda),
                GridItem("Cloud Storage", android.R.drawable.ic_menu_save)
            )

            val mockRatings = listOf(
                RatingItem(0, "User Interface", 8, 10),
                RatingItem(1, "Performance", 5, 10),
                RatingItem(2, "System Stability", 9, 10)
            )

            _uiState.update { it.copy(
                items = mockGridItems,
                featureRatings = mockRatings,
                isLoading = false
            ) }
        }
    }
}