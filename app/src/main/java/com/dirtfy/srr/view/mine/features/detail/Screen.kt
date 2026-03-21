package com.dirtfy.srr.view.mine.features.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FeedbackRatingScreen() {
    // Mock data
    val items = listOf(
        Item(1, "Service Quality", android.R.drawable.ic_menu_gallery, isRated = false),
        Item(2, "App Performance", android.R.drawable.ic_menu_manage, isRated = false),
        Item(3, "UI Design", android.R.drawable.ic_menu_compass, isRated = true),
        Item(4, "Feature Request", android.R.drawable.ic_menu_agenda, isRated = true)
    )

    val unratedItems = items.filter { !it.isRated }
    val ratedItems = items.filter { it.isRated }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(title = "Unrated Items", count = unratedItems.size)
            }

            items(unratedItems) { item ->
                SimpleFeedbackCard(item = item)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SectionHeader(title = "Rated Items", count = ratedItems.size)
            }

            items(ratedItems) { item ->
                SimpleFeedbackCard(item = item)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
            Text(text = count.toString(), modifier = Modifier.padding(4.dp))
        }
    }
}

@Composable
fun SimpleFeedbackCard(item: Item) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeedbackPreview() {
    MaterialTheme {
        FeedbackRatingScreen()
    }
}