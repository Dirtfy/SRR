package com.dirtfy.srr.view.mine.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FeatureListScreen(
    ratings: List<Item>,
    onFeatureClick: (Item) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(ratings) { rating ->
            RatingRow(
                item = rating,
                modifier = Modifier.clickable { onFeatureClick(rating) }
            )
        }
    }
}

@Composable
fun RatingRow(item: Item, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${item.ratedCount}/${item.totalCount}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { item.ratedCount.toFloat() / item.totalCount.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FeedbackRatingScreenPreview() {
    // Mock data for preview
    val mockRatings = listOf(
        Item(1, "User Interface", 8, 10),
        Item(2, "App Performance", 5, 10),
        Item(3, "Feature Completeness", 3, 10),
        Item(4, "Stability", 9, 10),
        Item(5, "Customer Support", 1, 10)
    )

    MaterialTheme {
        FeatureListScreen(
            ratings = mockRatings,
            onFeatureClick = {}
        )
    }
}