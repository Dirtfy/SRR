package com.dirtfy.srr.view.mine

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dirtfy.srr.view.mine.items.ItemGridScreen
import com.dirtfy.srr.view.mine.features.FeedbackRatingScreen
// Import the specific Item class to fix the type mismatch
import com.dirtfy.srr.view.mine.items.Item

@Composable
fun MineMainScreen(
    // Fixed: Changed from List<Any> to List<Item> to match ItemGridScreen's expected type
    items: List<Item> = emptyList()
) {
    // State to control which view is shown
    // false = Item Grid (Items), true = Feedback/Features
    var isFeaturesView by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Simple Switch Controller
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isFeaturesView) "Features View" else "Items Grid",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Switch View",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = isFeaturesView,
                        onCheckedChange = { isFeaturesView = it }
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (isFeaturesView) {
                    FeedbackRatingScreen()
                } else {
                    // This now receives the correctly typed List<Item>
                    ItemGridScreen(items = items)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MineMainScreenPreview() {
    MaterialTheme {
        MineMainScreen(items = emptyList())
    }
}