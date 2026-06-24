package com.dirtfy.srr.core.scoring

import com.dirtfy.srr.core.model.Evaluation
import com.dirtfy.srr.core.model.ScoreMatrix

class DefaultFeatureScoringEngine : FeatureScoringEngine {
    override fun computeScores(
        allItemIds: List<String>,
        allFeatureIds: List<String>,
        evaluationsByFeature: Map<String, List<Evaluation>>,
        minVoteThreshold: Int
    ): ScoreMatrix {
        val scores     = mutableMapOf<String, MutableMap<String, Double?>>()
        val voteCounts = mutableMapOf<String, MutableMap<String, Int>>()

        for (featureId in allFeatureIds) {
            val evaluations = evaluationsByFeature[featureId] ?: emptyList()
            val rawAverages = ScoreAggregator.aggregate(evaluations, allItemIds, minVoteThreshold)

            for (itemId in allItemIds) {
                val displayScore = ScoreRemapper.remapOrNull(rawAverages[itemId])
                val voteCount    = evaluations.count { itemId in it.orderedItemIds }

                scores    .getOrPut(itemId) { mutableMapOf() }[featureId] = displayScore
                voteCounts.getOrPut(itemId) { mutableMapOf() }[featureId] = voteCount
            }
        }

        return ScoreMatrix(
            scores     = scores.mapValues { it.value.toMap() },
            voteCounts = voteCounts.mapValues { it.value.toMap() }
        )
    }
}
