package com.dirtfy.srr.ui.performer.compilation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.model.ScoreMatrix
import com.dirtfy.srr.ui.performer.compilation.map.Item as MapItem
import com.dirtfy.srr.ui.performer.compilation.map.MapScreen

// ---------------------------------------------------------------------------
// Top-level router
// ---------------------------------------------------------------------------

@Composable
fun CompilationScreen(
    modifier: Modifier = Modifier,
    uiState: CompilationUiState,
    onTabSelected: (CompilationUiState.Tab) -> Unit,
    onItemSelected: (Item) -> Unit,
    onFeatureSelected: (Feature) -> Unit,
    onMapItemTap: (Item) -> Unit,
    onMapXFeatureSelected: (String) -> Unit,
    onMapYFeatureSelected: (String) -> Unit,
    onRetryTap: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is CompilationUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is CompilationUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetryTap) { Text("Retry") }
                }
            }
            is CompilationUiState.Ready -> {
                CompilationReadyContent(
                    state                 = uiState,
                    onTabSelected         = onTabSelected,
                    onItemSelected        = onItemSelected,
                    onFeatureSelected     = onFeatureSelected,
                    onMapItemTap          = onMapItemTap,
                    onMapXFeatureSelected = onMapXFeatureSelected,
                    onMapYFeatureSelected = onMapYFeatureSelected
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ready state router
// ---------------------------------------------------------------------------

@Composable
private fun CompilationReadyContent(
    state: CompilationUiState.Ready,
    onTabSelected: (CompilationUiState.Tab) -> Unit,
    onItemSelected: (Item) -> Unit,
    onFeatureSelected: (Feature) -> Unit,
    onMapItemTap: (Item) -> Unit,
    onMapXFeatureSelected: (String) -> Unit,
    onMapYFeatureSelected: (String) -> Unit
) {
    when {
        state.selectedItem != null -> {
            CompilationItemDetailContent(
                item     = state.selectedItem,
                features = state.features,
                matrix   = state.scoreMatrix
            )
        }
        state.selectedFeature != null -> {
            CompilationFeatureDetailContent(
                feature = state.selectedFeature,
                items   = state.items,
                matrix  = state.scoreMatrix
            )
        }
        else -> {
            CompilationTabContent(
                state                 = state,
                onTabSelected         = onTabSelected,
                onItemSelected        = onItemSelected,
                onFeatureSelected     = onFeatureSelected,
                onMapItemTap          = onMapItemTap,
                onMapXFeatureSelected = onMapXFeatureSelected,
                onMapYFeatureSelected = onMapYFeatureSelected
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tab selector + tab content
// ---------------------------------------------------------------------------

@Composable
private fun CompilationTabContent(
    state: CompilationUiState.Ready,
    onTabSelected: (CompilationUiState.Tab) -> Unit,
    onItemSelected: (Item) -> Unit,
    onFeatureSelected: (Feature) -> Unit,
    onMapItemTap: (Item) -> Unit,
    onMapXFeatureSelected: (String) -> Unit,
    onMapYFeatureSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.activeTab.ordinal) {
            CompilationUiState.Tab.entries.forEach { tab ->
                Tab(
                    selected = state.activeTab == tab,
                    onClick  = { onTabSelected(tab) },
                    text     = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        when (state.activeTab) {
            CompilationUiState.Tab.ITEMS    -> CompilationItemsTab(state.items, onItemSelected)
            CompilationUiState.Tab.FEATURES -> CompilationFeaturesTab(state.features, state.items, state.scoreMatrix, onFeatureSelected)
            CompilationUiState.Tab.MAP      -> CompilationMapTab(state, onMapXFeatureSelected, onMapYFeatureSelected)
        }
    }
}

// ---------------------------------------------------------------------------
// Items tab
// ---------------------------------------------------------------------------

@Composable
private fun CompilationItemsTab(items: List<Item>, onItemSelected: (Item) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { item ->
            ListItem(
                headlineContent = { Text(item.name) },
                modifier = Modifier.clickable { onItemSelected(item) }
            )
            HorizontalDivider()
        }
    }
}

// ---------------------------------------------------------------------------
// Features tab
// ---------------------------------------------------------------------------

@Composable
private fun CompilationFeaturesTab(
    features: List<Feature>,
    items: List<Item>,
    matrix: ScoreMatrix,
    onFeatureSelected: (Feature) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(features, key = { it.id }) { feature ->
            val scoredCount = items.count { matrix.scores[it.id]?.get(feature.id) != null }
            ListItem(
                headlineContent = { Text(feature.name) },
                trailingContent = { Text("$scoredCount/${items.size} scored") },
                modifier = Modifier.clickable { onFeatureSelected(feature) }
            )
            HorizontalDivider()
        }
    }
}

// ---------------------------------------------------------------------------
// Item detail (read-only scores)
// ---------------------------------------------------------------------------

@Composable
private fun CompilationItemDetailContent(
    item: Item,
    features: List<Feature>,
    matrix: ScoreMatrix
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(item.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text("Feature Scores", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        items(features, key = { it.id }) { feature ->
            val raw = matrix.scores[item.id]?.get(feature.id)
            val display = if (raw != null) "%.1f".format(raw) else "—"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(feature.name, modifier = Modifier.weight(1f))
                Text(display)
            }
            HorizontalDivider()
        }
    }
}

// ---------------------------------------------------------------------------
// Feature detail (items sorted by score, read-only)
// ---------------------------------------------------------------------------

@Composable
private fun CompilationFeatureDetailContent(
    feature: Feature,
    items: List<Item>,
    matrix: ScoreMatrix
) {
    val sorted = items.sortedWith(compareByDescending { matrix.scores[it.id]?.get(feature.id) })

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(feature.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text("Item Rankings", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        items(sorted, key = { it.id }) { item ->
            val raw = matrix.scores[item.id]?.get(feature.id)
            val display = if (raw != null) "%.1f".format(raw) else "—"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.name, modifier = Modifier.weight(1f))
                Text(display)
            }
            HorizontalDivider()
        }
    }
}

// ---------------------------------------------------------------------------
// Map tab — converts core.model types to map.Item for the existing MapScreen
// ---------------------------------------------------------------------------

@Composable
private fun CompilationMapTab(
    state: CompilationUiState.Ready,
    onMapXFeatureSelected: (String) -> Unit,
    onMapYFeatureSelected: (String) -> Unit
) {
    val featureNames = state.features.map { it.name }

    val xFeature = state.features.find { it.id == state.mapXFeatureId }
        ?: state.features.getOrNull(0)
    val yFeature = state.features.find { it.id == state.mapYFeatureId }
        ?: state.features.getOrNull(1)
        ?: state.features.getOrNull(0)

    // Convert to the map screen's local Item type, normalising [0,10] → [-1,+1] for the canvas
    val mapItems = state.items.mapNotNull { item ->
        val xScore = xFeature?.let { state.scoreMatrix.scores[item.id]?.get(it.id) } ?: 5.0
        val yScore = yFeature?.let { state.scoreMatrix.scores[item.id]?.get(it.id) } ?: 5.0
        // MapItem expects strings in [-1, +1] raw range
        val xRaw = (xScore / 10.0) * 2.0 - 1.0
        val yRaw = (yScore / 10.0) * 2.0 - 1.0
        MapItem(
            title          = item.name,
            imageRes       = android.R.drawable.ic_menu_agenda,
            primaryScore   = "%.2f".format(xRaw),
            secondaryScore = "%.2f".format(yRaw)
        )
    }

    MapScreen(items = mapItems, availableFeatures = featureNames)
}
