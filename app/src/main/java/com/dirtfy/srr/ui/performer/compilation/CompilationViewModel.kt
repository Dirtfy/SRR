package com.dirtfy.srr.ui.performer.compilation

import android.R
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
// Import conversion functions/models from sub-packages
import com.dirtfy.srr.ui.performer.compilation.features.Item as FeatureItem
import com.dirtfy.srr.ui.performer.compilation.items.Item as GridItem
import com.dirtfy.srr.ui.performer.compilation.map.Item as MapItem

class CompilationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CompilationUiState())
    val uiState: StateFlow<CompilationUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _uiState.update { it.copy(isLoading = true) }

        val mockGridItems = listOf(
            GridItem(0, "Premium Plan", "Unlimited access to all features", R.drawable.ic_menu_agenda),
            GridItem(1, "Cloud Storage", "Secure backup for your data", R.drawable.ic_menu_save)
        )

        val mockFeatures = listOf(
            FeatureItem(0, "User Interface", 10),
            FeatureItem(1, "Performance", 8),
            FeatureItem(2, "System Stability", 7)
        )

        val mockMapItems = listOf(
            MapItem("User Interface", R.drawable.ic_menu_agenda, "-0.5", "1"),
            MapItem("Performance", R.drawable.ic_menu_agenda, "-0.3", "-0.4"),
            MapItem("System Stability", R.drawable.ic_menu_agenda, "0.5", "0.5")
        )

        val mockAvailableFeatureList = listOf(
            "Performance",
            "Stability",
            "UI Design"
        )

        _uiState.update { state ->
            state.copy(
                gridItems = mockGridItems,
                featureItems = mockFeatures,
                mapItems = mockMapItems,
                availableFeatureList = mockAvailableFeatureList,
                isLoading = false
            )
        }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    // --- Grid Item Selection ---
    fun selectGridItem(item: GridItem) {
        _uiState.update { it.copy(selectedGridItem = item) }
    }

    fun clearGridItem() {
        _uiState.update { it.copy(selectedGridItem = null) }
    }

    // --- Feature Item Selection ---
    fun selectFeatureItem(item: FeatureItem) {
        _uiState.update { it.copy(selectedFeatureItem = item) }
    }

    fun clearFeatureItem() {
        _uiState.update { it.copy(selectedFeatureItem = null) }
    }

    // --- Map Item Selection ---
    fun selectMapItem(item: MapItem) {
        _uiState.update { it.copy(selectedMapItem = item) }
    }

    fun clearMapItem() {
        _uiState.update { it.copy(selectedMapItem = null) }
    }
}