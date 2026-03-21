package com.dirtfy.srr.view.compilation.features

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// Importing the detail screen from the other package
import com.dirtfy.srr.view.compilation.features.detail.CompilationDetailScreen
import com.dirtfy.srr.view.compilation.features.detail.Item as DetailItem

/**
 * COORDINATOR: This handles the navigation logic between the list and the detail.
 */
@Composable
fun CompilationFeaturesCoordinator() {
    // Local state to track which item is selected.
    // In a real app, this might come from a ViewModel.
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    if (selectedItem != null) {
        // Handle physical back button to return to list
        BackHandler { selectedItem = null }

        // 2. NAVIGATE TO DETAIL SCREEN
        CompilationDetailScreen(
            title = selectedItem!!.name,
            items = listOf(
                // Mocking data for the detail screen
                DetailItem(1, "Sample Data A", android.R.drawable.ic_menu_compass, "9.0"),
                DetailItem(2, "Sample Data B", android.R.drawable.ic_menu_gallery, "8.2")
            ),
            onBackClick = { selectedItem = null },
            onItemClick = { /* Handle detail item click if needed */ }
        )
    } else {
        // 3. SHOW MAIN LIST
        CompilationFeaturesScreen(
            items = listOf(
                Item(1, "Performance Logs", 12),
                Item(2, "User Feedback", 45),
                Item(3, "Bug Reports", 8)
            ),
            onItemClick = { item -> selectedItem = item }
        )
    }
}

@Composable
fun CompilationFeaturesScreen(
    items: List<Item>,
    onItemClick: (Item) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            FeatureRow(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun FeatureRow(
    item: Item,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "${item.totalCount} Items",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompilationFeaturesScreenPreview() {
    val mockItems = listOf(
        Item(1, "Performance Logs", 12),
        Item(2, "User Feedback", 45),
        Item(3, "Bug Reports", 8),
        Item(4, "Feature Requests", 21)
    )

    MaterialTheme {
        Surface {
            CompilationFeaturesScreen(
                items = mockItems,
                onItemClick = {}
            )
        }
    }
}