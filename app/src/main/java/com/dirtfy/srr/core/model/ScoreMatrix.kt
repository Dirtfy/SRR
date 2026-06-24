package com.dirtfy.srr.core.model

data class ScoreMatrix(
    // scores[itemId][featureId] = display score in [0.0, 10.0],
    // or null if the item-feature pair has fewer than N evaluations
    val scores: Map<String, Map<String, Double?>>,
    // voteCounts[itemId][featureId] = how many users evaluated this pair
    val voteCounts: Map<String, Map<String, Int>>
)
