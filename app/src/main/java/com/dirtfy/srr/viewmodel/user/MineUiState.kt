package com.dirtfy.srr.viewmodel.user

import com.dirtfy.srr.view.mine.items.Item

data class MineUiState(
    val items: List<Item> = emptyList()
)