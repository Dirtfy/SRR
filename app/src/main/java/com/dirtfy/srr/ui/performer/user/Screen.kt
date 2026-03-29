package com.dirtfy.srr.ui.performer.user

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
import com.dirtfy.srr.ui.performer.user.features.FeatureListScreen
import com.dirtfy.srr.ui.performer.user.features.detail.FeatureDetailScreen
import com.dirtfy.srr.ui.performer.user.features.detail.ItemDetailPopup
import com.dirtfy.srr.ui.performer.user.items.ItemGridScreen
import com.dirtfy.srr.ui.performer.user.items.detail.ItemDetailScreen
import com.dirtfy.srr.ui.performer.base.theme.SRRTheme
// Explicitly import Item classes for mock data in Previews
import com.dirtfy.srr.ui.performer.user.items.Item as GridItem
import com.dirtfy.srr.ui.performer.user.features.Item as RatingItem

@Composable
fun MineMainScreen(
    modifier: Modifier = Modifier, // Injected by BaseFragment (Scaffold innerPadding)
    uiState: UserUiState,
    onToggleView: () -> Unit,
    onItemClick: (GridItem) -> Unit,
    onFeatureClick: (RatingItem) -> Unit,
    onSubItemClick: (com.dirtfy.srr.ui.performer.user.features.detail.Item) -> Unit,
    onDismissSubPopup: () -> Unit,
    onBackToGrid: () -> Unit
) {
    // State for the Dropdown Menu
    var menuExpanded by remember { mutableStateOf(false) }

    // 1. POPUP LOGIC: Shows on top of current view
    if (uiState.selectedSubItem != null) {
        ItemDetailPopup(
            title = uiState.selectedSubItem.title,
            imageRes = uiState.selectedSubItem.imageRes,
            onDismiss = onDismissSubPopup
        )
    }

    // 2. MAIN LAYOUT
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.selectedItem != null -> {
                BackHandler { onBackToGrid() }
                ItemDetailScreen(
                    title = uiState.selectedItem.title,
                    onBackClick = onBackToGrid
                )
            }

            uiState.selectedFeature != null -> {
                BackHandler { onBackToGrid() }
                FeatureDetailScreen(
                    title = uiState.selectedFeature.name,
                    onBackClick = onBackToGrid,
                    onItemClick = onSubItemClick
                )
            }

            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (uiState.isFeaturesView) "Feature Ratings" else "Available Items",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // DROP DOWN MENU IMPLEMENTATION
                        Box {
                            TextButton(onClick = { menuExpanded = true }) {
                                Text(
                                    text = "View Mode",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Available Items") },
                                    onClick = {
                                        if (uiState.isFeaturesView) onToggleView()
                                        menuExpanded = false
                                    },
                                    leadingIcon = {
                                        RadioButton(
                                            selected = !uiState.isFeaturesView,
                                            onClick = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Feature Ratings") },
                                    onClick = {
                                        if (!uiState.isFeaturesView) onToggleView()
                                        menuExpanded = false
                                    },
                                    leadingIcon = {
                                        RadioButton(
                                            selected = uiState.isFeaturesView,
                                            onClick = null
                                        )
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (uiState.isFeaturesView) {
                        FeatureListScreen(
                            ratings = uiState.featureRatings,
                            onFeatureClick = onFeatureClick
                        )
                    } else {
                        ItemGridScreen(
                            items = uiState.items,
                            onItemClick = onItemClick
                        )
                    }
                }
            }
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, name = "Dashboard Grid View")
@Composable
fun MineMainScreenGridPreview() {
    SRRTheme {
        // Use a Surface to provide background color in preview
        Surface(color = MaterialTheme.colorScheme.background) {
            MineMainScreen(
                uiState = UserUiState(
                    items = listOf(GridItem("Premium Plan", android.R.drawable.ic_menu_agenda)),
                    isFeaturesView = false,
                    isLoading = false
                ),
                onToggleView = {}, onItemClick = {}, onFeatureClick = {},
                onSubItemClick = {}, onDismissSubPopup = {}, onBackToGrid = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dashboard Features View")
@Composable
fun MineMainScreenFeaturesPreview() {
    SRRTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MineMainScreen(
                uiState = UserUiState(
                    featureRatings = listOf(RatingItem(1, "UI Design", 8, 10)),
                    isFeaturesView = true,
                    isLoading = false
                ),
                onToggleView = {}, onItemClick = {}, onFeatureClick = {},
                onSubItemClick = {}, onDismissSubPopup = {}, onBackToGrid = {}
            )
        }
    }
}