package com.dirtfy.srr.ui.performer.user

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
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
    onAddItemImagePicked: (Uri) -> Unit,
    onAddFeatureNameChange: (String) -> Unit,
    onDismissAddDialog: () -> Unit,
    onAddItem: () -> Unit,
    onAddFeature: () -> Unit,
    onRequestDeleteItem: (id: String, name: String) -> Unit,
    onRequestDeleteFeature: (id: String, name: String) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onOpenEditItemImageDialog: (itemId: String) -> Unit,
    onEditItemImagePicked: (Uri) -> Unit,
    onSubmitEditItemImage: () -> Unit,
    onDismissEditItemImageDialog: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { onAddItemImagePicked(it) } }

    val editImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { onEditItemImagePicked(it) } }

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
                    state                       = uiState,
                    onTabSelected               = onTabSelected,
                    onItemSelected              = onItemSelected,
                    onFeatureSelected           = onFeatureSelected,
                    onOpenEditor                = onOpenEditor,
                    onEvaluationReorder         = onEvaluationReorder,
                    onSubmitEvaluation          = onSubmitEvaluation,
                    onOpenAddItemDialog         = onOpenAddItemDialog,
                    onOpenAddFeatureDialog      = onOpenAddFeatureDialog,
                    onRequestDeleteItem         = onRequestDeleteItem,
                    onRequestDeleteFeature      = onRequestDeleteFeature,
                    onOpenEditItemImageDialog   = onOpenEditItemImageDialog
                )
                uiState.addItemDialog?.let { dialog ->
                    AddItemDialog(
                        dialog        = dialog,
                        onNameChange  = onAddItemNameChange,
                        onChooseImage = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onConfirm     = onAddItem,
                        onDismiss     = onDismissAddDialog
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
                uiState.deleteConfirmation?.let { pending ->
                    AlertDialog(
                        onDismissRequest = onDismissDeleteConfirmation,
                        title            = { Text("Delete ${pending.type.name.lowercase().replaceFirstChar { it.uppercase() }}?") },
                        text             = { Text("\"${pending.name}\" will be permanently removed.") },
                        confirmButton    = {
                            TextButton(onClick = onConfirmDelete) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton    = {
                            TextButton(onClick = onDismissDeleteConfirmation) { Text("Cancel") }
                        }
                    )
                }
                uiState.editItemImageDialog?.let { dialog ->
                    EditItemImageDialog(
                        dialog        = dialog,
                        onChooseImage = {
                            editImagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onConfirm     = onSubmitEditItemImage,
                        onDismiss     = onDismissEditItemImageDialog
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
    onRequestDeleteItem: (id: String, name: String) -> Unit,
    onRequestDeleteFeature: (id: String, name: String) -> Unit,
    onOpenEditItemImageDialog: (itemId: String) -> Unit
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
                feature          = state.selectedFeature,
                items            = state.items,
                myOrderedItemIds = state.myEvaluationByFeature[state.selectedFeature.id] ?: emptyList(),
                evaluatorCount   = state.evaluatorCountByFeature[state.selectedFeature.id] ?: 0,
                hasEvaluated     = state.selectedFeature.id in state.evaluatedFeatureIds,
                onEvaluate       = onOpenEditor
            )
        }
        state.selectedItem != null -> {
            UserItemDetailContent(
                item                      = state.selectedItem,
                features                  = state.features,
                currentUserId             = state.currentUserId,
                myEvaluationByFeature     = state.myEvaluationByFeature,
                onEditImage               = onOpenEditItemImageDialog
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
                onRequestDeleteItem    = onRequestDeleteItem,
                onRequestDeleteFeature = onRequestDeleteFeature
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
    onRequestDeleteItem: (id: String, name: String) -> Unit,
    onRequestDeleteFeature: (id: String, name: String) -> Unit
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
                    items                = state.items,
                    currentUserId        = state.currentUserId,
                    onItemSelected       = onItemSelected,
                    onAddClick           = onOpenAddItemDialog,
                    onRequestDeleteItem  = onRequestDeleteItem
                )
            }
            UserUiState.Tab.FEATURES -> {
                FeaturesTabContent(
                    features                = state.features,
                    currentUserId           = state.currentUserId,
                    evaluatedFeatureIds     = state.evaluatedFeatureIds,
                    evaluatorCountByFeature = state.evaluatorCountByFeature,
                    onFeatureSelected       = onFeatureSelected,
                    onAddClick              = onOpenAddFeatureDialog,
                    onRequestDeleteFeature  = onRequestDeleteFeature
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
    onRequestDeleteItem: (id: String, name: String) -> Unit
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
                        IconButton(onClick = { onRequestDeleteItem(item.id, item.name) }) {
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

private const val MIN_EVALUATORS = 3  // must match LoadFeatureScoresUseCase.minVoteThreshold

// ---------------------------------------------------------------------------
// Features tab
// ---------------------------------------------------------------------------

@Composable
private fun FeaturesTabContent(
    features: List<Feature>,
    currentUserId: String,
    evaluatedFeatureIds: Set<String>,
    evaluatorCountByFeature: Map<String, Int>,
    onFeatureSelected: (Feature) -> Unit,
    onAddClick: () -> Unit,
    onRequestDeleteFeature: (id: String, name: String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(features, key = { it.id }) { feature ->
                val evaluatorCount = evaluatorCountByFeature[feature.id] ?: 0
                val hasEvaluated   = feature.id in evaluatedFeatureIds
                val isOwner = feature.createdBy == currentUserId && currentUserId.isNotEmpty()
                val pct = (evaluatorCount.coerceAtMost(MIN_EVALUATORS) * 100 / MIN_EVALUATORS)
                ListItem(
                    headlineContent   = { Text(feature.name) },
                    supportingContent = when {
                        isOwner && !hasEvaluated -> { {
                            Column {
                                Text("Added by you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text("Tap to evaluate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        } }
                        isOwner       -> { { Text("Added by you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) } }
                        !hasEvaluated -> { { Text("Tap to evaluate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) } }
                        else          -> null
                    },
                    trailingContent   = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text  = "$pct%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (evaluatorCount >= MIN_EVALUATORS) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text  = "$evaluatorCount / $MIN_EVALUATORS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (hasEvaluated) {
                                    Text(
                                        text  = "You evaluated",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (isOwner) {
                                IconButton(onClick = { onRequestDeleteFeature(feature.id, feature.name) }) {
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
    currentUserId: String,
    myEvaluationByFeature: Map<String, List<String>>,
    onEditImage: (itemId: String) -> Unit
) {
    val isOwner = item.createdBy == currentUserId && currentUserId.isNotEmpty()
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            val url = item.imageUrl?.takeIf { it.isNotBlank() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (url != null) {
                    AsyncImage(
                        model              = url,
                        contentDescription = item.name,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                }
                if (isOwner) {
                    IconButton(
                        onClick  = { onEditImage(item.id) },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = "Edit image",
                            tint               = MaterialTheme.colorScheme.onPrimary,
                            modifier           = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                )
                                .padding(6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(item.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text("My Ranking per Feature", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        items(features, key = { it.id }) { feature ->
            val myOrder = myEvaluationByFeature[feature.id]
            val rank = myOrder?.indexOf(item.id)?.takeIf { it >= 0 }?.let { it + 1 }
            val display = if (rank != null) "Rank $rank / ${myOrder!!.size}" else "—"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(feature.name, modifier = Modifier.weight(1f))
                Text(display, style = MaterialTheme.typography.bodyMedium)
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
    myOrderedItemIds: List<String>,  // user's personal ranking; empty if not yet evaluated
    evaluatorCount: Int,
    hasEvaluated: Boolean,
    onEvaluate: (featureId: String) -> Unit
) {
    val itemById = remember(items) { items.associateBy { it.id } }
    // Sort by user's own rank; items missing from evaluation go to the bottom
    val sortedItems = if (myOrderedItemIds.isNotEmpty()) {
        val inEval = myOrderedItemIds.mapNotNull { itemById[it] }
        val extra  = items.filter { it.id !in myOrderedItemIds.toSet() }
        inEval + extra
    } else {
        items
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(feature.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "$evaluatorCount user${if (evaluatorCount == 1) "" else "s"} evaluated",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { onEvaluate(feature.id) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (hasEvaluated) "Re-evaluate" else "Evaluate") }
            Spacer(Modifier.height(16.dp))
            Text(
                text  = if (hasEvaluated) "Your Ranking" else "Items (evaluate to rank them)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
        }
        items(sortedItems, key = { it.id }) { item ->
            val rank = myOrderedItemIds.indexOf(item.id).takeIf { it >= 0 }?.let { it + 1 }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.name, modifier = Modifier.weight(1f))
                Text(
                    text  = if (rank != null) "#$rank" else "—",
                    style = MaterialTheme.typography.bodyMedium
                )
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
private fun AddItemDialog(
    dialog: UserUiState.Ready.AddItemDialogState,
    onNameChange: (String) -> Unit,
    onChooseImage: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Add Item") },
        text             = {
            Column {
                OutlinedTextField(
                    value         = dialog.name,
                    onValueChange = onNameChange,
                    label         = { Text("Name") },
                    singleLine    = true,
                    isError       = dialog.error != null,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !dialog.isSaving) { onChooseImage() },
                    contentAlignment = Alignment.Center
                ) {
                    if (dialog.imageUri != null) {
                        AsyncImage(
                            model              = dialog.imageUri,
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize()
                        )
                        if (dialog.isUploadingImage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector        = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier           = Modifier.size(36.dp),
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = "Tap to add image (optional)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (dialog.error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = dialog.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = dialog.name.isNotBlank() && !dialog.isSaving && !dialog.isUploadingImage
            ) {
                if (dialog.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !dialog.isSaving && !dialog.isUploadingImage) {
                Text("Cancel")
            }
        }
    )
}

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
// Edit item image dialog
// ---------------------------------------------------------------------------

@Composable
private fun EditItemImageDialog(
    dialog: UserUiState.Ready.EditItemImageDialogState,
    onChooseImage: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Update Image") },
        text             = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !dialog.isSaving) { onChooseImage() },
                    contentAlignment = Alignment.Center
                ) {
                    if (dialog.imageUri != null) {
                        AsyncImage(
                            model              = dialog.imageUri,
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize()
                        )
                        if (dialog.isUploadingImage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector        = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier           = Modifier.size(36.dp),
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = "Tap to choose a new image",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (dialog.error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = dialog.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = dialog.imageUrl != null && !dialog.isSaving && !dialog.isUploadingImage
            ) {
                if (dialog.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !dialog.isSaving && !dialog.isUploadingImage) {
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
