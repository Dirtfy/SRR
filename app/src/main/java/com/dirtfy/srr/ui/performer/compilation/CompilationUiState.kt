package com.dirtfy.srr.ui.performer.compilation

import com.dirtfy.srr.ui.performer.compilation.features.Item as FeatureItem
import com.dirtfy.srr.ui.performer.compilation.items.Item as GridItem
import com.dirtfy.srr.ui.performer.compilation.map.Item as MapItem

/**
 * UI State for the Compilation screen.
 * Contains specialized lists for each sub-view.
 */
data class CompilationUiState(
    // Specialized lists for the sub-screens
    val gridItems: List<GridItem> = emptyList(),
    val selectedGridItem: GridItem? = null,
    val featureItems: List<FeatureItem> = emptyList(),
    val selectedFeatureItem: FeatureItem? = null,
    val mapItems: List<MapItem> = emptyList(),
    val selectedMapItem: MapItem? = null,
    val availableFeatureList: List<String> = emptyList(),

    val viewMode: ViewMode = ViewMode.ITEMS,
    val isLoading: Boolean = false
)