package com.dirtfy.srr.view.mine

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dirtfy.srr.view.mine.items.ItemGridScreen
import com.dirtfy.srr.view.mine.features.FeatureListScreen
import com.dirtfy.srr.view.mine.items.detail.ItemDetailScreen
import com.dirtfy.srr.view.mine.features.detail.FeatureDetailScreen
import com.dirtfy.srr.viewmodel.user.MineUiState
// Explicitly import Item classes for mock data in Previews
import com.dirtfy.srr.view.mine.items.Item as GridItem
import com.dirtfy.srr.view.mine.features.Item as RatingItem

@Composable
fun MineMainScreen(
    uiState: MineUiState,
    onToggleView: () -> Unit,
    onItemClick: (com.dirtfy.srr.view.mine.items.Item) -> Unit,
    onFeatureClick: (com.dirtfy.srr.view.mine.features.Item) -> Unit,
    onBackToGrid: () -> Unit
) {
    // Logic: Determine which screen to show
    when {
        // 1. If an Item from the Grid is selected
        uiState.selectedItem != null -> {
            ItemDetailScreen(
                title = uiState.selectedItem.title,
                onBackClick = onBackToGrid
            )
        }

        // 2. If a Feature from the Rating List is selected
        uiState.selectedFeature != null -> {
            FeatureDetailScreen(
                title = uiState.selectedFeature.name,
                onBackClick = onBackToGrid
            )
        }

        // 3. Show the main lists (Default State)
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
            uiState = MineUiState(
                items = mockItems,
                isFeaturesView = false,
                isLoading = false
            ),
            onToggleView = {},
            onItemClick = {},
            onFeatureClick = {},
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
            uiState = MineUiState(
                featureRatings = mockRatings,
                isFeaturesView = true,
                isLoading = false
            ),
            onToggleView = {},
            onItemClick = {},
            onFeatureClick = {},
            onBackToGrid = {}
        )
    }
}

@Preview(showBackground = true, name = "Detail Screen Active")
@Composable
fun MineMainScreenDetailActivePreview() {
    MaterialTheme {
        MineMainScreen(
            uiState = MineUiState(
                selectedFeature = RatingItem(1, "UI Design", 8, 10),
                isLoading = false
            ),
            onToggleView = {},
            onItemClick = {},
            onFeatureClick = {},
            onBackToGrid = {}
        )
    }
}