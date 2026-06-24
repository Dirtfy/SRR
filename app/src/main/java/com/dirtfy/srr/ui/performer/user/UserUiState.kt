package com.dirtfy.srr.ui.performer.user

import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.model.ScoreMatrix

sealed class UserUiState {
    object Loading : UserUiState()
    data class Error(val message: String) : UserUiState()
    data class Ready(
        // Item and Feature are core.model.Item / core.model.Feature — not the UI models
        // in user/items/Item.kt or user/features/Item.kt
        val items: List<Item>,
        val features: List<Feature>,
        val scoreMatrix: ScoreMatrix,
        val currentUserId: String,
        val activeTab: Tab = Tab.ITEMS,
        val selectedItem: Item? = null,
        val selectedFeature: Feature? = null,
        val evaluationEditor: EvaluationEditorState? = null
    ) : UserUiState() {
        // Debator fix #6: nested inside Ready so it cannot be accidentally assigned to StateFlow<UserUiState>
        data class EvaluationEditorState(
            val featureId: String,
            val orderedItemIds: List<String>,
            val isSaving: Boolean = false,
            val saveError: String? = null
        )
    }

    enum class Tab { ITEMS, FEATURES }
}
