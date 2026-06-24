package com.dirtfy.srr.ui.performer.compilation

import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.model.ScoreMatrix

sealed class CompilationUiState {
    object Loading : CompilationUiState()
    data class Error(val message: String) : CompilationUiState()
    data class Ready(
        // Item and Feature are core.model.Item / core.model.Feature — not UI models
        val items: List<Item>,
        val features: List<Feature>,
        val scoreMatrix: ScoreMatrix,
        val activeTab: Tab = Tab.ITEMS,
        val selectedItem: Item? = null,
        val selectedFeature: Feature? = null,
        val mapPopupItem: Item? = null,
        // Null means "use the list index default" (features[0]/features[1])
        val mapXFeatureId: String? = null,
        val mapYFeatureId: String? = null
    ) : CompilationUiState()

    enum class Tab { ITEMS, FEATURES, MAP }
}
