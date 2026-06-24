package com.dirtfy.srr.core.scoring

import com.dirtfy.srr.core.model.Evaluation

object ScoreAggregator {
    // Aggregates all evaluations for one feature into a per-item average.
    // allItemIds: the full item catalogue — ensures every item has an entry in the result,
    //             even if no one evaluated it.
    // Returns null for an item if fewer than minVotes users evaluated it.
    fun aggregate(
        evaluations: List<Evaluation>,
        allItemIds: List<String>,
        minVotes: Int
    ): Map<String, Double?> {
        val collectedScores: Map<String, MutableList<Double>> =
            allItemIds.associateWith { mutableListOf() }

        for (evaluation in evaluations) {
            val scores = EvaluationConverter.convert(evaluation.orderedItemIds)
            for ((itemId, score) in scores) {
                collectedScores[itemId]?.add(score)
            }
        }

        return collectedScores.mapValues { (_, scores) ->
            if (scores.size < minVotes) null else scores.average()
        }
    }
}
