package com.dirtfy.srr.viewmodel.user

import androidx.lifecycle.ViewModel
import com.dirtfy.srr.view.mine.items.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MineViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MineUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Load some sample data into the grid
        _uiState.value = MineUiState(
            items = listOf(
                Item("Gallery", android.R.drawable.ic_menu_gallery),
                Item("Camera", android.R.drawable.ic_menu_camera),
                Item("Map", android.R.drawable.ic_menu_mapmode)
            )
        )
    }
}