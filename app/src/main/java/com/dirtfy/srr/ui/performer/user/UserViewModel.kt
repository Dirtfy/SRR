package com.dirtfy.srr.ui.performer.user

import android.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dirtfy.srr.ui.performer.user.features.Item as RatingItem
import com.dirtfy.srr.ui.performer.user.items.Item as GridItem
import com.dirtfy.srr.ui.performer.user.features.detail.Item as DetailSubItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

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

    // Inside MineViewModel.kt
    fun selectSubItem(subItem: DetailSubItem) {
        _uiState.update { it.copy(selectedSubItem = subItem) }
    }

    fun dismissSubItemPopup() {
        _uiState.update { it.copy(selectedSubItem = null) }
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
                GridItem("Premium Plan", R.drawable.ic_menu_agenda),
                GridItem("Cloud Storage", R.drawable.ic_menu_save)
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