package com.dirtfy.srr.core.model

// One user's ordering of items for one feature.
// orderedItemIds[0] = item with the strongest feature presence.
// Items absent from this list were not evaluated by this user.
data class Evaluation(
    val userId: String,
    val featureId: String,
    val orderedItemIds: List<String>
)
