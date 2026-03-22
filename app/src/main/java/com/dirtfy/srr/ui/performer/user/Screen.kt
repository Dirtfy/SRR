package com.dirtfy.srr.ui.performer.user

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dirtfy.srr.ui.performer.user.features.FeatureListScreen
import com.dirtfy.srr.ui.performer.user.features.detail.FeatureDetailScreen
import com.dirtfy.srr.ui.performer.user.features.detail.ItemDetailPopup
import com.dirtfy.srr.ui.performer.user.items.ItemGridScreen
import com.dirtfy.srr.ui.performer.user.items.detail.ItemDetailScreen
// Explicitly import Item classes for mock data in Previews
import com.dirtfy.srr.ui.performer.user.items.Item as GridItem
import com.dirtfy.srr.ui.performer.user.features.Item as RatingItem

@Composable
fun MineMainScreen(
    uiState: UserUiState,
    onToggleView: () -> Unit,
    onItemClick: (GridItem) -> Unit,
    onFeatureClick: (RatingItem) -> Unit,
    onSubItemClick: (com.dirtfy.srr.ui.performer.user.features.detail.Item) -> Unit,
    onDismissSubPopup: () -> Unit,
    onBackToGrid: () -> Unit
) {
    // 1. POPUP LOGIC: Shows on top of the FeatureDetailScreen
    if (uiState.selectedSubItem != null) {
        ItemDetailPopup(
            title = uiState.selectedSubItem.title,
            imageRes = uiState.selectedSubItem.imageRes,
            onDismiss = onDismissSubPopup
        )
    }

    // 2. NAVIGATION LOGIC
    when {
        // Full Screen Item Detail
        uiState.selectedItem != null -> {
            BackHandler { onBackToGrid() }
            ItemDetailScreen(
                title = uiState.selectedItem.title,
                onBackClick = onBackToGrid
            )
        }

        // Full Screen Feature Detail
        uiState.selectedFeature != null -> {
            BackHandler { onBackToGrid() }
            FeatureDetailScreen(
                title = uiState.selectedFeature.name,
                onBackClick = onBackToGrid,
                onItemClick = onSubItemClick // When item inside this screen is clicked, trigger popup
            )
        }

        // Main List View
        else -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (uiState.isFeaturesView) "Features View" else "Items Grid",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Switch(
                                checked = uiState.isFeaturesView,
                                onCheckedChange = { onToggleView() }
                            )
                        }

                        HorizontalDivider()

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
}

// --- PREVIEWS ---

@Preview(showBackground = true, name = "Items Grid View")
@Composable
fun MineMainScreenGridPreview() {
    val mockItems = listOf(
        GridItem("Premium Plan", android.R.drawable.ic_menu_agenda),
        GridItem("Cloud Storage", android.R.drawable.ic_menu_save)
    )

    MaterialTheme {
        MineMainScreen(
            uiState = UserUiState(
                items = mockItems,
                isFeaturesView = false,
                isLoading = false
            ),
            onToggleView = {},
            onItemClick = {},
            onFeatureClick = {},
            onSubItemClick = {},
            onDismissSubPopup = {},
            onBackToGrid = {}
        )
    }
}

@Preview(showBackground = true, name = "Features Rating View")
@Composable
fun MineMainScreenFeaturesPreview() {
    val mockRatings = listOf(
        RatingItem(1, "UI Design", 8, 10),
        RatingItem(2, "Performance", 5, 10)
    )

    MaterialTheme {
        MineMainScreen(
            uiState = UserUiState(
                featureRatings = mockRatings,
                isFeaturesView = true,
                isLoading = false
            ),
            onToggleView = {},
            onItemClick = {},
            onFeatureClick = {},
            onSubItemClick = {},
            onDismissSubPopup = {},
            onBackToGrid = {}
        )
    }
}

@Preview(showBackground = true, name = "Detail Screen Active")
@Composable
fun MineMainScreenDetailActivePreview() {
    MaterialTheme {
        MineMainScreen(
            uiState = UserUiState(
                selectedFeature = RatingItem(1, "UI Design", 8, 10),
                isLoading = false
            ),
            onToggleView = {},
            onItemClick = {},
            onFeatureClick = {},
            onSubItemClick = {},
            onDismissSubPopup = {},
            onBackToGrid = {}
        )
    }
}