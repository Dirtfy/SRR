package com.dirtfy.srr.viewmodel.user

import com.dirtfy.srr.view.mine.items.Item as GridItem
import com.dirtfy.srr.view.mine.features.Item as RatingItem

data class MineUiState(
    val items: List<GridItem> = emptyList(),
    val featureRatings: List<RatingItem> = emptyList(),
    val isFeaturesView: Boolean = false,
    val isLoading: Boolean = false,
    val selectedItem: GridItem? = null,    // For Full Screen Detail
    val selectedFeature: RatingItem? = null, // For Popup Dialog
    val errorMessage: String? = null
)