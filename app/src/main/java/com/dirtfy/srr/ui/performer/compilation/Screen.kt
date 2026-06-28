package com.dirtfy.srr.ui.performer.compilation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    onMapXFeatureSelected: (String) -> Unit,
    onMapYFeatureSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = state.activeTab.ordinal) {
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
            CompilationUiState.Tab.FEATURES -> CompilationFeaturesTab(state.features, state.evaluatorCountByFeature, onFeatureSelected)
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

private const val SCORE_THRESHOLD = 3  // must match LoadFeatureScoresUseCase default

@Composable
private fun CompilationFeaturesTab(
    features: List<Feature>,
    evaluatorCountByFeature: Map<String, Int>,
    onFeatureSelected: (Feature) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(features, key = { it.id }) { feature ->
            val evaluatorCount = evaluatorCountByFeature[feature.id] ?: 0
            val pct = (evaluatorCount.coerceAtMost(SCORE_THRESHOLD) * 100 / SCORE_THRESHOLD)
            val isScored = evaluatorCount >= SCORE_THRESHOLD
            ListItem(
                headlineContent   = { Text(feature.name) },
                supportingContent = if (isScored) { {
                    Text(
                        text  = "Scores available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } } else null,
                trailingContent   = {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text  = "$pct%",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isScored) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text  = "$evaluatorCount / $SCORE_THRESHOLD users",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
            val url = item.imageUrl?.takeIf { it.isNotBlank() }
            if (url != null) {
                AsyncImage(
                    model              = url,
                    contentDescription = item.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(16.dp))
            }
            Text(item.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text("Feature Scores", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        items(features, key = { it.id }) { feature ->
            val raw       = matrix.scores[item.id]?.get(feature.id)
            val voteCount = matrix.voteCounts[item.id]?.get(feature.id) ?: 0
            val needed    = (SCORE_THRESHOLD - voteCount).coerceAtLeast(0)
            val display   = if (raw != null) "%.1f".format(raw) else "Needs $needed more"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(feature.name, modifier = Modifier.weight(1f))
                Text(
                    text  = display,
                    color = if (raw != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
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
            val raw       = matrix.scores[item.id]?.get(feature.id)
            val voteCount = matrix.voteCounts[item.id]?.get(feature.id) ?: 0
            val needed    = (SCORE_THRESHOLD - voteCount).coerceAtLeast(0)
            val display   = if (raw != null) "%.1f".format(raw) else "Needs $needed more"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.name, modifier = Modifier.weight(1f))
                Text(
                    text  = display,
                    color = if (raw != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
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

    // Only show items that have enough evaluations on both selected axes
    val mapItems = state.items.mapNotNull { item ->
        val xScore = xFeature?.let { state.scoreMatrix.scores[item.id]?.get(it.id) }
        val yScore = yFeature?.let { state.scoreMatrix.scores[item.id]?.get(it.id) }
        if (xScore == null || yScore == null) null
        else MapItem(
            title          = item.name,
            imageUrl       = item.imageUrl,
            primaryScore   = "%.1f".format(xScore),
            secondaryScore = "%.1f".format(yScore)
        )
    }

    val hint = "Only items with $SCORE_THRESHOLD+ evaluations on both axes are shown " +
               "(${mapItems.size} of ${state.items.size})"

    MapScreen(
        items              = mapItems,
        availableFeatures  = featureNames,
        featureX           = xFeature?.name ?: "",
        featureY           = yFeature?.name ?: "",
        hint               = hint,
        onFeatureXSelected = { name ->
            state.features.find { it.name == name }?.let { onMapXFeatureSelected(it.id) }
        },
        onFeatureYSelected = { name ->
            state.features.find { it.name == name }?.let { onMapYFeatureSelected(it.id) }
        }
    )
}
