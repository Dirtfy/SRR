package com.dirtfy.srr.ui.performer.compilation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.ui.graphics.vector.ImageVector

enum class ViewMode(val label: String, val icon: ImageVector) {
    ITEMS("Items Grid", Icons.Default.ViewModule),
    FEATURES("Feature List", Icons.Default.Layers),
    MAP("Map View", Icons.Default.Map)
}