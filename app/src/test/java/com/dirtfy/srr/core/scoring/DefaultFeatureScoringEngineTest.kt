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
    fun `empty evaluations produce null scores and zero vote counts for every item x feature`() {
        val matrix = engine.computeScores(items, features, emptyMap(), minVoteThreshold = 3)
        for (itemId in items) {
            assertTrue("scores missing item $itemId", matrix.scores.containsKey(itemId))
            assertTrue("voteCounts missing item $itemId", matrix.voteCounts.containsKey(itemId))
            for (featureId in features) {
                assertTrue(
                    "scores[$itemId] missing feature $featureId",
                    matrix.scores[itemId]!!.containsKey(featureId)
                )
                assertNull(matrix.scores[itemId]!![featureId])
                assertTrue(
                    "voteCounts[$itemId] missing feature $featureId",
                    matrix.voteCounts[itemId]!!.containsKey(featureId)
                )
                assertEquals(0, matrix.voteCounts[itemId]!![featureId])
            }
        }
    }

    @Test
    fun `three identical evaluations produce A=10 B=5 C=0 display scores`() {
        val evals = mapOf("f1" to listOf(
            eval("u1", "f1", "A", "B", "C"),
            eval("u2", "f1", "A", "B", "C"),
            eval("u3", "f1", "A", "B", "C")
        ))
        val matrix = engine.computeScores(items, listOf("f1"), evals, minVoteThreshold = 3)
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
        assertNull(matrix.scores["A"]!!["f1"])
        assertNull(matrix.scores["B"]!!["f1"])
        assertNull(matrix.scores["C"]!!["f1"])
        assertEquals(2, matrix.voteCounts["A"]!!["f1"])
        assertEquals(2, matrix.voteCounts["B"]!!["f1"])
        assertEquals(2, matrix.voteCounts["C"]!!["f1"])
    }

    @Test
    fun `two features are scored independently`() {
        val evals = mapOf(
            "f1" to listOf(eval("u1", "f1", "A", "B", "C"), eval("u2", "f1", "A", "B", "C"), eval("u3", "f1", "A", "B", "C")),
            "f2" to listOf(eval("u1", "f2", "C", "B", "A"), eval("u2", "f2", "C", "B", "A"), eval("u3", "f2", "C", "B", "A"))
        )
        val matrix = engine.computeScores(items, listOf("f1", "f2"), evals, minVoteThreshold = 3)
        // f1: A wins → 10, C loses → 0
        assertEquals(10.0, matrix.scores["A"]!!["f1"]!!, 0.001)
        assertEquals(0.0,  matrix.scores["C"]!!["f1"]!!, 0.001)
        // f2: C wins → 10, A loses → 0 (reversed)
        assertEquals(10.0, matrix.scores["C"]!!["f2"]!!, 0.001)
        assertEquals(0.0,  matrix.scores["A"]!!["f2"]!!, 0.001)
    }

    @Test
    fun `voteCount counts only users who included the item in their evaluation`() {
        val evals = mapOf("f1" to listOf(
            eval("u1", "f1", "A", "C"),       // B not evaluated by u1
            eval("u2", "f1", "A", "B", "C"),
            eval("u3", "f1", "A", "B", "C")
        ))
        val matrix = engine.computeScores(items, listOf("f1"), evals, minVoteThreshold = 3)
        assertEquals(3, matrix.voteCounts["A"]!!["f1"])
        assertEquals(2, matrix.voteCounts["B"]!!["f1"])  // u1 did not include B
        assertEquals(3, matrix.voteCounts["C"]!!["f1"])
    }

    @Test
    fun `evaluations for feature not in allFeatureIds are silently ignored`() {
        val evals = mapOf(
            "f1" to listOf(eval("u1", "f1", "A", "B", "C"), eval("u2", "f1", "A", "B", "C"), eval("u3", "f1", "A", "B", "C")),
            "f_unknown" to listOf(eval("u1", "f_unknown", "A"))
        )
        // f_unknown not in allFeatureIds
        val matrix = engine.computeScores(items, listOf("f1"), evals, minVoteThreshold = 3)
        assertFalse("f_unknown should not appear in scores", matrix.scores["A"]!!.containsKey("f_unknown"))
        assertFalse("f_unknown should not appear in voteCounts", matrix.voteCounts["A"]!!.containsKey("f_unknown"))
    }
}
