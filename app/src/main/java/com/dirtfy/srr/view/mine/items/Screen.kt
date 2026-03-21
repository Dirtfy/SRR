package com.dirtfy.srr.view.mine.items

import android.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun ItemGridScreen(items: List<Item>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // Sets a 2-column grid
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { item ->
            GridTile(item)
        }
    }
}

@Composable
fun GridTile(item: Item) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ItemGridScreenPreview() {
    val sampleItems = listOf(
        Item("Item 1", R.drawable.ic_menu_gallery),
        Item("Item 2", R.drawable.ic_menu_camera),
        Item("Item 3", R.drawable.ic_menu_mapmode),
        Item("Item 4", R.drawable.ic_menu_agenda),
        Item("Item 5", R.drawable.ic_menu_directions),
        Item("Item 6", R.drawable.ic_menu_compass)
    )
    MaterialTheme {
        ItemGridScreen(items = sampleItems)
    }
}
