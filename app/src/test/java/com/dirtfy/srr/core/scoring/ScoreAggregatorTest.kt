package com.dirtfy.srr.core.scoring

import com.dirtfy.srr.core.model.Evaluation
import org.junit.Assert.*
import org.junit.Test

class ScoreAggregatorTest {

    private val allItems = listOf("A", "B", "C")
    private fun eval(userId: String, vararg ids: String) = Evaluation(userId, "f", ids.toList())

    @Test
    fun `zero evaluations returns null for all items`() {
        val result = ScoreAggregator.aggregate(emptyList(), allItems, minVotes = 3)
        assertTrue(result.values.all { it == null })
    }

    @Test
    fun `one below minVotes returns null for all items`() {
        val evals = listOf(eval("u1", "A", "B", "C"), eval("u2", "A", "B", "C"))
        val result = ScoreAggregator.aggregate(evals, allItems, minVotes = 3)
        assertTrue(result.values.all { it == null })
    }

    @Test
    fun `exactly minVotes evaluations meets threshold and returns non-null scores`() {
        val evals = listOf(eval("u1", "A", "B", "C"), eval("u2", "A", "B", "C"), eval("u3", "A", "B", "C"))
        val result = ScoreAggregator.aggregate(evals, allItems, minVotes = 3)
        assertNotNull(result["A"])
        assertNotNull(result["B"])
        assertNotNull(result["C"])
    }

    @Test
    fun `three identical evaluations produce A=+1 B=0 C=-1 averages`() {
        val evals = listOf(eval("u1", "A", "B", "C"), eval("u2", "A", "B", "C"), eval("u3", "A", "B", "C"))
        val result = ScoreAggregator.aggregate(evals, allItems, minVotes = 3)
        assertEquals(+1.0, result["A"]!!, 0.001)
        assertEquals( 0.0, result["B"]!!, 0.001)
        assertEquals(-1.0, result["C"]!!, 0.001)
    }

    @Test
    fun `mixed evaluations average correctly`() {
        // User1=[A,B,C], User2=[C,B,A], User3=[A,B,C]
        val evals = listOf(eval("u1", "A", "B", "C"), eval("u2", "C", "B", "A"), eval("u3", "A", "B", "C"))
        val result = ScoreAggregator.aggregate(evals, allItems, minVotes = 3)
        // A: (1 + -1 + 1) / 3 = 1/3
        assertEquals(1.0 / 3.0,  result["A"]!!, 0.001)
        // B: (0 + 0 + 0) / 3 = 0
        assertEquals(0.0,         result["B"]!!, 0.001)
        // C: (-1 + 1 + -1) / 3 = -1/3
        assertEquals(-1.0 / 3.0, result["C"]!!, 0.001)
    }

    @Test
    fun `item in allItemIds absent from all evaluations returns null`() {
        val evals = listOf(eval("u1", "A", "B"), eval("u2", "A", "B"), eval("u3", "A", "B"))
        val result = ScoreAggregator.aggregate(evals, listOf("A", "B", "C"), minVotes = 3)
        assertNull(result["C"])
    }

    @Test
    fun `user evaluating only subset contributes vote only to evaluated items`() {
        val evals = listOf(eval("u1", "A", "C"), eval("u2", "A", "C"), eval("u3", "A", "C"))
        val result = ScoreAggregator.aggregate(evals, listOf("A", "B", "C"), minVotes = 3)
        assertNotNull(result["A"])
        assertNotNull(result["C"])
        assertNull(result["B"])  // B has 0 votes — below threshold
    }

    @Test
    fun `minVotes zero with no evaluations does not produce NaN`() {
        val result = ScoreAggregator.aggregate(emptyList(), listOf("A"), minVotes = 0)
        val score = result["A"]
        if (score != null) assertFalse("Expected null or finite, got NaN", score.isNaN())
    }
}
