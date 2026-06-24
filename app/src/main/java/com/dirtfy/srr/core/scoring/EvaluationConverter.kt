package com.dirtfy.srr.core.scoring

object EvaluationConverter {
    // orderedItemIds[0] has the strongest feature presence → score +1.0
    // orderedItemIds[last] has the weakest → score -1.0
    // Items not in the list are excluded (return no entry in the result map).
    fun convert(orderedItemIds: List<String>): Map<String, Double> {
        val n = orderedItemIds.size
        if (n == 0) return emptyMap()
        if (n == 1) return mapOf(orderedItemIds[0] to 0.0)
        val step = 2.0 / (n - 1)
        return orderedItemIds.mapIndexed { i, id ->
            id to (1.0 - i * step)
        }.toMap()
    }
}
