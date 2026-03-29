package com.dirtfy.srr.ui.performer.compilation

import com.dirtfy.srr.ui.performer.compilation.features.Item as FeatureItem
import com.dirtfy.srr.ui.performer.compilation.features.detail.Item as FeatureDetailItem
import com.dirtfy.srr.ui.performer.compilation.items.Item as GridItem
import com.dirtfy.srr.ui.performer.compilation.items.detail.Item as GridDetailItem
import com.dirtfy.srr.ui.performer.compilation.map.Item as MapItem

/**
 * UI State for the Compilation screen.
 * Contains specialized lists for each sub-view.
 */
data class CompilationUiState(
    // Specialized lists for the sub-screens
    val gridItems: List<GridItem> = emptyList(),
    val selectedGridItem: GridItem? = null,
    val gridDetailItems: List<GridDetailItem> = emptyList(),
    val featureItems: List<FeatureItem> = emptyList(),
    val selectedFeatureItem: FeatureItem? = null,
    val featureDetailItems: List<FeatureDetailItem> = emptyList(),
    val mapItems: List<MapItem> = emptyList(),
    val selectedMapItem: MapItem? = null,
    val availableFeatureList: List<String> = emptyList(),

    val viewMode: ViewMode = ViewMode.ITEMS,
    val isLoading: Boolean = false
)