package com.dirtfy.srr.ui.performer.user

import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.model.ScoreMatrix

sealed class UserUiState {
    object Loading : UserUiState()
    data class Error(val message: String) : UserUiState()
    data class Ready(
        val items: List<Item>,
        val features: List<Feature>,
        val scoreMatrix: ScoreMatrix,
        val currentUserId: String,
        val evaluatedFeatureIds: Set<String> = emptySet(),
        // featureId → current user's personally ranked item-id list (empty if not evaluated)
        val myEvaluationByFeature: Map<String, List<String>> = emptyMap(),
        // featureId → total number of users who submitted an evaluation
        val evaluatorCountByFeature: Map<String, Int> = emptyMap(),
        val activeTab: Tab = Tab.ITEMS,
        val selectedItem: Item? = null,
        val selectedFeature: Feature? = null,
        val evaluationEditor: EvaluationEditorState? = null,
        val addItemDialog: AddItemDialogState? = null,
        val addFeatureDialog: AddFeatureDialogState? = null,
        val deleteConfirmation: DeleteConfirmationState? = null,
        val editItemImageDialog: EditItemImageDialogState? = null,
        val imagePreviewUrl: String? = null
    ) : UserUiState() {

        data class EvaluationEditorState(
            val featureId: String,
            val orderedItemIds: List<String>,
            val isSaving: Boolean = false,
            val saveError: String? = null
        )

        data class AddItemDialogState(
            val name: String = "",
            val imageUri: android.net.Uri? = null,
            val imageUrl: String? = null,
            val isUploadingImage: Boolean = false,
            val isSaving: Boolean = false,
            val error: String? = null
        )

        data class AddFeatureDialogState(
            val name: String = "",
            val isSaving: Boolean = false,
            val error: String? = null
        )

        enum class DeleteTargetType { ITEM, FEATURE }
        data class DeleteConfirmationState(
            val id: String,
            val name: String,
            val type: DeleteTargetType
        )

        data class EditItemImageDialogState(
            val itemId: String,
            val imageUri: android.net.Uri? = null,
            val imageUrl: String? = null,
            val isUploadingImage: Boolean = false,
            val isSaving: Boolean = false,
            val error: String? = null
        )
    }

    enum class Tab { ITEMS, FEATURES }
}
