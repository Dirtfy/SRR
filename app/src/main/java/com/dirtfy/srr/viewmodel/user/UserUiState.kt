package com.dirtfy.srr.viewmodel.user

import com.dirtfy.srr.view.mine.items.Item as GridItem
import com.dirtfy.srr.view.mine.features.Item as RatingItem
import com.dirtfy.srr.view.mine.features.detail.Item as DetailSubItem

data class UserUiState(
    val items: List<GridItem> = emptyList(),
    val featureRatings: List<RatingItem> = emptyList(),
    val isFeaturesView: Boolean = false,
    val isLoading: Boolean = false,
    val selectedItem: GridItem? = null,    // Full Screen Grid Detail
    val selectedFeature: RatingItem? = null, // Full Screen Feature Detail
    val selectedSubItem: DetailSubItem? = null, // NEW: For the Popup inside Feature Detail
    val errorMessage: String? = null
)