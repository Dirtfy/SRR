package com.dirtfy.srr.ui.performer.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
    onRetryTap: () -> Unit,
    onOpenAddItemDialog: () -> Unit,
    onOpenAddFeatureDialog: () -> Unit,
    onAddItemNameChange: (String) -> Unit,
    onAddFeatureNameChange: (String) -> Unit,
    onDismissAddDialog: () -> Unit,
    onAddItem: () -> Unit,
    onAddFeature: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onDeleteFeature: (String) -> Unit
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
                    state                  = uiState,
                    onTabSelected          = onTabSelected,
                    onItemSelected         = onItemSelected,
                    onFeatureSelected      = onFeatureSelected,
                    onOpenEditor           = onOpenEditor,
                    onEvaluationReorder    = onEvaluationReorder,
                    onSubmitEvaluation     = onSubmitEvaluation,
                    onOpenAddItemDialog    = onOpenAddItemDialog,
                    onOpenAddFeatureDialog = onOpenAddFeatureDialog,
                    onDeleteItem           = onDeleteItem,
                    onDeleteFeature        = onDeleteFeature
                )
                uiState.addItemDialog?.let { dialog ->
                    AddDialog(
                        title        = "Add Item",
                        state        = dialog.name,
                        isSaving     = dialog.isSaving,
                        error        = dialog.error,
                        onNameChange = onAddItemNameChange,
                        onConfirm    = onAddItem,
                        onDismiss    = onDismissAddDialog
                    )
                }
                uiState.addFeatureDialog?.let { dialog ->
                    AddDialog(
                        title        = "Add Feature",
                        state        = dialog.name,
                        isSaving     = dialog.isSaving,
                        error        = dialog.error,
                        onNameChange = onAddFeatureNameChange,
                        onConfirm    = onAddFeature,
                        onDismiss    = onDismissAddDialog
                    )
                }
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
    onSubmitEvaluation: () -> Unit,
    onOpenAddItemDialog: () -> Unit,
    onOpenAddFeatureDialog: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onDeleteFeature: (String) -> Unit
) {
    when {
        state.evaluationEditor != null -> {
            EvaluationEditorSheet(
                editor    = state.evaluationEditor,
                items     = state.items,
                onReorder = onEvaluationReorder,
                onSubmit  = onSubmitEvaluation
            )
        }
        state.selectedFeature != null -> {
            UserFeatureDetailContent(
                feature      = state.selectedFeature,
                items        = state.items,
                matrix       = state.scoreMatrix,
                hasEvaluated = state.selectedFeature.id in state.evaluatedFeatureIds,
                onEvaluate   = onOpenEditor
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
                state                  = state,
                onTabSelected          = onTabSelected,
                onItemSelected         = onItemSelected,
                onFeatureSelected      = onFeatureSelected,
                onOpenAddItemDialog    = onOpenAddItemDialog,
                onOpenAddFeatureDialog = onOpenAddFeatureDialog,
                onDeleteItem           = onDeleteItem,
                onDeleteFeature        = onDeleteFeature
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
    onFeatureSelected: (Feature) -> Unit,
    onOpenAddItemDialog: () -> Unit,
    onOpenAddFeatureDialog: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onDeleteFeature: (String) -> Unit
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
                ItemsTabContent(
                    items         = state.items,
                    currentUserId = state.currentUserId,
                    onItemSelected = onItemSelected,
                    onAddClick    = onOpenAddItemDialog,
                    onDeleteItem  = onDeleteItem
                )
            }
            UserUiState.Tab.FEATURES -> {
                FeaturesTabContent(
                    features          = state.features,
                    items             = state.items,
                    matrix            = state.scoreMatrix,
                    currentUserId     = state.currentUserId,
                    onFeatureSelected = onFeatureSelected,
                    onAddClick        = onOpenAddFeatureDialog,
                    onDeleteFeature   = onDeleteFeature
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Items tab
// ---------------------------------------------------------------------------

@Composable
private fun ItemsTabContent(
    items: List<Item>,
    currentUserId: String,
    onItemSelected: (Item) -> Unit,
    onAddClick: () -> Unit,
    onDeleteItem: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val isOwner = item.createdBy == currentUserId && currentUserId.isNotEmpty()
                ListItem(
                    headlineContent    = { Text(item.name) },
                    supportingContent  = if (isOwner) { { Text("Added by you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) } } else null,
                    trailingContent    = if (isOwner) { {
                        IconButton(onClick = { onDeleteItem(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    } } else null,
                    modifier = Modifier.clickableItem { onItemSelected(item) }
                )
                HorizontalDivider()
            }
        }
        FloatingActionButton(
            onClick   = onAddClick,
            modifier  = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add item")
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
    currentUserId: String,
    onFeatureSelected: (Feature) -> Unit,
    onAddClick: () -> Unit,
    onDeleteFeature: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(features, key = { it.id }) { feature ->
                val ratedCount = items.count { item ->
                    matrix.scores[item.id]?.get(feature.id) != null
                }
                val isOwner = feature.createdBy == currentUserId && currentUserId.isNotEmpty()
                ListItem(
                    headlineContent   = { Text(feature.name) },
                    supportingContent = if (isOwner) { { Text("Added by you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) } } else null,
                    trailingContent   = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$ratedCount/${items.size}", style = MaterialTheme.typography.bodyMedium)
                            if (isOwner) {
                                IconButton(onClick = { onDeleteFeature(feature.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    },
                    modifier = Modifier.clickableItem { onFeatureSelected(feature) }
                )
                HorizontalDivider()
            }
        }
        FloatingActionButton(
            onClick   = onAddClick,
            modifier  = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add feature")
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
    hasEvaluated: Boolean,
    onEvaluate: (featureId: String) -> Unit
) {
    val sortedItems = items.sortedWith(
        compareByDescending { matrix.scores[it.id]?.get(feature.id) }
    )

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(feature.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            if (hasEvaluated) {
                Surface(
                    color  = MaterialTheme.colorScheme.primaryContainer,
                    shape  = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text     = "Your evaluation is saved. Scores appear once 3 users have evaluated.",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick  = { onEvaluate(feature.id) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (hasEvaluated) "Re-evaluate" else "Evaluate") }
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
// Add item / Add feature dialog (shared layout)
// ---------------------------------------------------------------------------

@Composable
private fun AddDialog(
    title: String,
    state: String,
    isSaving: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(title) },
        text             = {
            Column {
                OutlinedTextField(
                    value         = state,
                    onValueChange = onNameChange,
                    label         = { Text("Name") },
                    singleLine    = true,
                    isError       = error != null,
                    modifier      = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = state.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Helper extension
// ---------------------------------------------------------------------------

private fun Modifier.clickableItem(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
