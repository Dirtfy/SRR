package com.dirtfy.srr.core.scoring

import com.dirtfy.srr.core.model.Evaluation
import org.junit.Assert.*
import org.junit.Test

class DefaultFeatureScoringEngineTest {

    private val engine = DefaultFeatureScoringEngine()
    private val items = listOf("A", "B", "C")
    private val features = listOf("f1", "f2")

    private fun eval(userId: String, featureId: String, vararg ids: String) =
        Evaluation(userId, featureId, ids.toList())

    @Test
    fun `empty evaluations produce null scores and zero vote counts`() {
        val matrix = engine.computeScores(items, features, emptyMap(), minVoteThreshold = 3)
        for (itemId in items) {
            for (featureId in features) {
                assertNull(matrix.scores[itemId]?.get(featureId))
                assertEquals(0, matrix.voteCounts[itemId]?.get(featureId))
            }
        }
    }

    @Test
    fun `three identical evaluations produce correct display scores`() {
        val evals = mapOf("f1" to List(3) { eval("u$it", "f1", "A", "B", "C") })
        val matrix = engine.computeScores(items, listOf("f1"), evals, minVoteThreshold = 3)
        // A=+1 → remap → 10.0, B=0 → 5.0, C=-1 → 0.0
        assertEquals(10.0, matrix.scores["A"]!!["f1"]!!, 0.001)
        assertEquals(5.0,  matrix.scores["B"]!!["f1"]!!, 0.001)
        assertEquals(0.0,  matrix.scores["C"]!!["f1"]!!, 0.001)
    }

    @Test
    fun `feature with only two evaluations has null scores but non-zero vote counts`() {
        val evals = mapOf("f1" to listOf(
            eval("u1", "f1", "A", "B", "C"),
            eval("u2", "f1", "A", "B", "C")
        ))
        val matrix = engine.computeScores(items, listOf("f1"), evals, minVoteThreshold = 3)
        // Scores null (below threshold)
        assertNull(matrix.scores["A"]!!["f1"])
        assertNull(matrix.scores["B"]!!["f1"])
        assertNull(matrix.scores["C"]!!["f1"])
        // But vote counts reflect 2 evaluations
        assertEquals(2, matrix.voteCounts["A"]!!["f1"])
        assertEquals(2, matrix.voteCounts["B"]!!["f1"])
        assertEquals(2, matrix.voteCounts["C"]!!["f1"])
    }

    @Test
    fun `two features are computed independently`() {
        val evals = mapOf(
            "f1" to List(3) { eval("u$it", "f1", "A", "B", "C") },
            "f2" to List(3) { eval("u$it", "f2", "C", "B", "A") }
        )
        val matrix = engine.computeScores(items, listOf("f1", "f2"), evals, minVoteThreshold = 3)
        // f1: A=10, C=0
        assertEquals(10.0, matrix.scores["A"]!!["f1"]!!, 0.001)
        assertEquals(0.0,  matrix.scores["C"]!!["f1"]!!, 0.001)
        // f2: C=10, A=0 (reversed)
        assertEquals(10.0, matrix.scores["C"]!!["f2"]!!, 0.001)
        assertEquals(0.0,  matrix.scores["A"]!!["f2"]!!, 0.001)
    }

    @Test
    fun `voteCount counts only users who included the item in their evaluation`() {
        // u1 evaluates [A, C] for f1; u2 evaluates [A, B, C]; u3 evaluates [A, B, C]
        val evals = mapOf("f1" to listOf(
            eval("u1", "f1", "A", "C"),
            eval("u2", "f1", "A", "B", "C"),
            eval("u3", "f1", "A", "B", "C")
        ))
        val matrix = engine.computeScores(items, listOf("f1"), evals, minVoteThreshold = 3)
        // A: all 3 users evaluated it
        assertEquals(3, matrix.voteCounts["A"]!!["f1"])
        // B: only u2 and u3 evaluated it
        assertEquals(2, matrix.voteCounts["B"]!!["f1"])
        // C: all 3 users evaluated it
        assertEquals(3, matrix.voteCounts["C"]!!["f1"])
    }
}
