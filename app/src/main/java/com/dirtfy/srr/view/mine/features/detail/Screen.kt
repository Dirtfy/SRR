package com.dirtfy.srr.view.mine.features.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureDetailScreen(
    title: String,
    onBackClick: () -> Unit,
    onItemClick: (Item) -> Unit // New parameter for popup navigation
) {
    // Mock data internal to detail
    val items = listOf(
        Item(1, "Service Quality", android.R.drawable.ic_menu_gallery, isRated = false),
        Item(2, "App Performance", android.R.drawable.ic_menu_manage, isRated = false),
        Item(3, "UI Design", android.R.drawable.ic_menu_compass, isRated = true),
        Item(4, "Feature Request", android.R.drawable.ic_menu_agenda, isRated = true)
    )

    val unratedItems = items.filter { !it.isRated }
    val ratedItems = items.filter { it.isRated }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
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
                SimpleFeedbackCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SectionHeader(title = "Rated Items", count = ratedItems.size)
            }

            items(ratedItems) { item ->
                SimpleFeedbackCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
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
        @OptIn(ExperimentalMaterial3Api::class)
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
fun SimpleFeedbackCard(
    item: Item,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // Clicking the card triggers the popup logic
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
        FeatureDetailScreen(
            title = "UI Performance",
            onBackClick = {},
            onItemClick = {}
        )
    }
}