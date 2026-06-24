package com.dirtfy.srr.core.scoring

import com.dirtfy.srr.core.model.Evaluation
import com.dirtfy.srr.core.model.ScoreMatrix

interface FeatureScoringEngine {
    // allItemIds and allFeatureIds define the full set of known items/features.
    // evaluationsByFeature may be missing entries for features with no evaluations yet.
    // minVoteThreshold: item-feature pairs with fewer votes produce null scores.
    fun computeScores(
        allItemIds: List<String>,
        allFeatureIds: List<String>,
        evaluationsByFeature: Map<String, List<Evaluation>>,
        minVoteThreshold: Int = 3
    ): ScoreMatrix
}
