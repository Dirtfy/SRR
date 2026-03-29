package com.dirtfy.srr.ui.performer.compilation

import android.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dirtfy.srr.ui.performer.base.theme.SRRTheme
import com.dirtfy.srr.ui.performer.compilation.features.CompilationFeaturesScreen
import com.dirtfy.srr.ui.performer.compilation.items.CompilationItemsGridScreen
import com.dirtfy.srr.ui.performer.compilation.map.MapScreen
import com.dirtfy.srr.ui.performer.compilation.features.Item as FeatureItem
import com.dirtfy.srr.ui.performer.compilation.items.Item as GridItem
import com.dirtfy.srr.ui.performer.compilation.map.Item as MapItem

/**
 * COORDINATOR: This file manages the navigation logic between sub-views.
 */
@Composable
fun CompilationMainScreen(
    modifier: Modifier = Modifier,
    uiState: CompilationUiState,
    onViewModeChange: (ViewMode) -> Unit,
    onGridItemClick: (GridItem) -> Unit,
    onFeatureItemClick: (FeatureItem) -> Unit,
    onMapItemClick: (MapItem) -> Unit,
    onBackToMain: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            // 1. DETAIL VIEW MODES (Specific to each type)
            uiState.selectedGridItem != null -> {
                BackHandler { onBackToMain() }
                SpecializedDetailContent(
                    title = uiState.selectedGridItem.title,
                    description = uiState.selectedGridItem.description,
                    onBack = onBackToMain
                )
            }
            uiState.selectedFeatureItem != null -> {
                BackHandler { onBackToMain() }
                SpecializedDetailContent(
                    title = uiState.selectedFeatureItem.name,
                    description = "Score: ${uiState.selectedFeatureItem.totalCount}",
                    onBack = onBackToMain
                )
            }
            uiState.selectedMapItem != null -> {
                BackHandler { onBackToMain() }
                SpecializedDetailContent(
                    title = uiState.selectedMapItem.title,
                    description = "Location: ${uiState.selectedMapItem.primaryScore}, ${uiState.selectedMapItem.secondaryScore}",
                    onBack = onBackToMain
                )
            }

            // 2. LOADING STATE
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // 3. MAIN NAVIGATION MODE
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // TOP BAR / VIEW SELECTOR
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View: ${uiState.viewMode.name}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Box {
                            TextButton(onClick = { menuExpanded = true }) {
                                Text("Switch View")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                ViewMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.name) },
                                        onClick = {
                                            onViewModeChange(mode)
                                            menuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // CONTENT AREA: Passing specialized lists and clicks
                    Box(modifier = Modifier.weight(1f)) {
                        when (uiState.viewMode) {
                            ViewMode.ITEMS -> {
                                CompilationItemsGridScreen(
                                    items = uiState.gridItems,
                                    onItemClick = onGridItemClick
                                )
                            }
                            ViewMode.FEATURES -> {
                                CompilationFeaturesScreen(
                                    items = uiState.featureItems,
                                    onItemClick = onFeatureItemClick
                                )
                            }
                            ViewMode.MAP -> {
                                MapScreen(
                                    items = uiState.mapItems,
                                    availableFeatures = uiState.availableFeatureList,
                                    onItemClick = onMapItemClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecializedDetailContent(title: String, description: String, onBack: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = description, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Back to List") }
    }
}

// --- PREVIEW SECTION ---

@Preview(showBackground = true)
@Composable
fun CompilationMainScreenPreview() {
    // Mock Raw Data for conversion
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

    val previewState = CompilationUiState(
        gridItems = mockGridItems,
        featureItems = mockFeatures,
        mapItems = mockMapItems,
        viewMode = ViewMode.ITEMS
    )

    SRRTheme {
        Surface {
            CompilationMainScreen(
                uiState = previewState,
                onViewModeChange = {},
                onGridItemClick = {},
                onFeatureItemClick = {},
                onMapItemClick = {},
                onBackToMain = {}
            )
        }
    }
}