package com.dirtfy.srr.ui.performer.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.model.ScoreMatrix
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

// ---------------------------------------------------------------------------
// Top-level router
// ---------------------------------------------------------------------------

@Composable
fun UserScreen(
    modifier: Modifier = Modifier,
    uiState: UserUiState,
    onTabSelected: (UserUiState.Tab) -> Unit,
    onItemSelected: (Item) -> Unit,
    onFeatureSelected: (Feature) -> Unit,
    onOpenEditor: (featureId: String) -> Unit,
    onEvaluationReorder: (List<String>) -> Unit,
    onSubmitEvaluation: () -> Unit,
    onRetryTap: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UserUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UserUiState.Error -> {
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
            is UserUiState.Ready -> {
                UserReadyContent(
                    state               = uiState,
                    onTabSelected       = onTabSelected,
                    onItemSelected      = onItemSelected,
                    onFeatureSelected   = onFeatureSelected,
                    onOpenEditor        = onOpenEditor,
                    onEvaluationReorder = onEvaluationReorder,
                    onSubmitEvaluation  = onSubmitEvaluation
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ready state router
// ---------------------------------------------------------------------------

@Composable
private fun UserReadyContent(
    state: UserUiState.Ready,
    onTabSelected: (UserUiState.Tab) -> Unit,
    onItemSelected: (Item) -> Unit,
    onFeatureSelected: (Feature) -> Unit,
    onOpenEditor: (featureId: String) -> Unit,
    onEvaluationReorder: (List<String>) -> Unit,
    onSubmitEvaluation: () -> Unit
) {
    when {
        state.evaluationEditor != null -> {
            EvaluationEditorSheet(
                editor       = state.evaluationEditor,
                items        = state.items,
                onReorder    = onEvaluationReorder,
                onSubmit     = onSubmitEvaluation
            )
        }
        state.selectedFeature != null -> {
            UserFeatureDetailContent(
                feature   = state.selectedFeature,
                items     = state.items,
                matrix    = state.scoreMatrix,
                onEvaluate = onOpenEditor
            )
        }
        state.selectedItem != null -> {
            UserItemDetailContent(
                item     = state.selectedItem,
                features = state.features,
                matrix   = state.scoreMatrix
            )
        }
        else -> {
            UserTabContent(
                state            = state,
                onTabSelected    = onTabSelected,
                onItemSelected   = onItemSelected,
                onFeatureSelected = onFeatureSelected
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tab selector + tab content
// ---------------------------------------------------------------------------

@Composable
private fun UserTabContent(
    state: UserUiState.Ready,
    onTabSelected: (UserUiState.Tab) -> Unit,
    onItemSelected: (Item) -> Unit,
    onFeatureSelected: (Feature) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = state.activeTab.ordinal) {
            UserUiState.Tab.entries.forEach { tab ->
                Tab(
                    selected = state.activeTab == tab,
                    onClick  = { onTabSelected(tab) },
                    text     = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        when (state.activeTab) {
            UserUiState.Tab.ITEMS -> {
                ItemsTabContent(items = state.items, onItemSelected = onItemSelected)
            }
            UserUiState.Tab.FEATURES -> {
                FeaturesTabContent(
                    features        = state.features,
                    items           = state.items,
                    matrix          = state.scoreMatrix,
                    onFeatureSelected = onFeatureSelected
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Items tab
// ---------------------------------------------------------------------------

@Composable
private fun ItemsTabContent(items: List<Item>, onItemSelected: (Item) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { item ->
            ListItem(
                headlineContent = { Text(item.name) },
                modifier = Modifier.clickableItem { onItemSelected(item) }
            )
            HorizontalDivider()
        }
    }
}

// ---------------------------------------------------------------------------
// Features tab
// ---------------------------------------------------------------------------

@Composable
private fun FeaturesTabContent(
    features: List<Feature>,
    items: List<Item>,
    matrix: ScoreMatrix,
    onFeatureSelected: (Feature) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(features, key = { it.id }) { feature ->
            val ratedCount = items.count { item ->
                matrix.scores[item.id]?.get(feature.id) != null
            }
            ListItem(
                headlineContent = { Text(feature.name) },
                trailingContent = { Text("$ratedCount/${items.size}") },
                modifier = Modifier.clickableItem { onFeatureSelected(feature) }
            )
            HorizontalDivider()
        }
    }
}

// ---------------------------------------------------------------------------
// Item detail (scores per feature)
// ---------------------------------------------------------------------------

@Composable
private fun UserItemDetailContent(
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
            val rawScore = matrix.scores[item.id]?.get(feature.id)
            val displayScore = if (rawScore != null) "%.1f".format(rawScore) else "—"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(feature.name, modifier = Modifier.weight(1f))
                Text(displayScore, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
        }
    }
}

// ---------------------------------------------------------------------------
// Feature detail (items ordered by score + Evaluate button)
// ---------------------------------------------------------------------------

@Composable
private fun UserFeatureDetailContent(
    feature: Feature,
    items: List<Item>,
    matrix: ScoreMatrix,
    onEvaluate: (featureId: String) -> Unit
) {
    val sortedItems = items.sortedWith(
        compareByDescending { matrix.scores[it.id]?.get(feature.id) }
    )

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(feature.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { onEvaluate(feature.id) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Evaluate") }
            Spacer(Modifier.height(16.dp))
            Text("Item Rankings", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        items(sortedItems, key = { it.id }) { item ->
            val rawScore = matrix.scores[item.id]?.get(feature.id)
            val displayScore = if (rawScore != null) "%.1f".format(rawScore) else "—"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.name, modifier = Modifier.weight(1f))
                Text(displayScore, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
        }
    }
}

// ---------------------------------------------------------------------------
// Evaluation editor (drag-to-reorder via sh.calvin.reorderable)
// ---------------------------------------------------------------------------

@Composable
private fun EvaluationEditorSheet(
    editor: UserUiState.Ready.EvaluationEditorState,
    items: List<Item>,
    onReorder: (List<String>) -> Unit,
    onSubmit: () -> Unit
) {
    val nameById = remember(items) { items.associate { it.id to it.name } }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyColumnState(lazyListState) { from, to ->
        val newOrder = editor.orderedItemIds.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onReorder(newOrder)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Rank items strongest → weakest", style = MaterialTheme.typography.titleMedium)
        Text(
            text  = "Drag the handle to reorder",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            state    = lazyListState
        ) {
            items(editor.orderedItemIds, key = { it }) { itemId ->
                ReorderableItem(reorderState, key = itemId) {
                    val index = editor.orderedItemIds.indexOf(itemId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = "${index + 1}.",
                            modifier = Modifier.width(32.dp),
                            style    = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text     = nameById[itemId] ?: itemId,
                            modifier = Modifier.weight(1f),
                            style    = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(
                            modifier = Modifier.draggableHandle(),
                            onClick  = {}
                        ) {
                            Icon(Icons.Default.DragHandle, contentDescription = "Drag to reorder")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        editor.saveError?.let { err ->
            Text(
                text  = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick  = onSubmit,
            enabled  = !editor.isSaving,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            if (editor.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Submit")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helper extension
// ---------------------------------------------------------------------------

private fun Modifier.clickableItem(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
