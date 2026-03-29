package com.dirtfy.srr.ui.performer.compilation.items

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dirtfy.srr.ui.performer.compilation.items.detail.CompilationDetailScreen
// Importing the Detail Screen and its Item type
import com.dirtfy.srr.ui.performer.compilation.items.detail.CompilationDetailScreen
import com.dirtfy.srr.ui.performer.compilation.items.detail.Item as DetailFeatureItem



/**
 * Main entry point for this package that handles navigation logic.
 */
@Composable
fun CompilationItemsNavigation() {
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    if (selectedItem != null) {
        // Handle system back button to return to grid
        BackHandler { selectedItem = null }

        // Navigate to the Detail Screen
        CompilationDetailScreen(
            imageRes = selectedItem!!.imageRes,
            features = listOf(
                DetailFeatureItem("Design Quality", "9.5"),
                DetailFeatureItem("Performance", "8.8"),
                DetailFeatureItem("User Experience", "9.2")
            ),
        )
    } else {
        // Show the Grid Screen
        CompilationItemsGridScreen(
            items = listOf(
                Item(1, "Modern Architecture", "Explore clean lines and functional design.", android.R.drawable.ic_menu_gallery),
                Item(2, "Nature Escape", "Quiet moments captured in the deep wild forest.", android.R.drawable.ic_menu_camera),
                Item(3, "Urban Life", "The hustle and bustle of the city center.", android.R.drawable.ic_menu_compass)
            ),
            onItemClick = { item -> selectedItem = item }
        )
    }
}

@Composable
fun CompilationItemsGridScreen(
    items: List<Item>,
    onItemClick: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            CompilationGridItem(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun CompilationGridItem(
    item: Item,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(MaterialTheme.shapes.medium)
            )

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompilationItemsGridPreview() {
    val mockItems = listOf(
        Item(1, "Modern Architecture", "Explore clean lines and functional design.", android.R.drawable.ic_menu_gallery),
        Item(2, "Nature Escape", "Quiet moments captured in the deep wild forest.", android.R.drawable.ic_menu_camera),
        Item(3, "Urban Life", "The hustle and bustle of the city center.", android.R.drawable.ic_menu_compass),
        Item(4, "Tech Trends", "Latest innovations in mobile and web development.", android.R.drawable.ic_menu_manage)
    )

    MaterialTheme {
        Surface {
            CompilationItemsGridScreen(
                items = mockItems,
                onItemClick = {}
            )
        }
    }
}