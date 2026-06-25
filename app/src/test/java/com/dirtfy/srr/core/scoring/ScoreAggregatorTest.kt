package com.dirtfy.srr.core.scoring

import com.dirtfy.srr.core.model.Evaluation
import org.junit.Assert.*
import org.junit.Test

class ScoreAggregatorTest {

    private val allItems = listOf("A", "B", "C")
    private fun eval(vararg ids: String) = Evaluation("u", "f", ids.toList())

    @Test
    fun `zero evaluations returns null for all items`() {
        val result = ScoreAggregator.aggregate(emptyList(), allItems, minVotes = 3)
        assertTrue(result.values.all { it == null })
    }

    @Test
    fun `two evaluations below threshold returns null for all items`() {
        val evals = listOf(eval("A", "B", "C"), eval("A", "B", "C"))
        val result = ScoreAggregator.aggregate(evals, allItems, minVotes = 3)
        assertTrue(result.values.all { it == null })
    }

    @Test
    fun `three identical evaluations produce correct averages`() {
        val evals = List(3) { eval("A", "B", "C") }
        val result = ScoreAggregator.aggregate(evals, allItems, minVotes = 3)
        assertEquals(+1.0, result["A"]!!, 0.001)
        assertEquals( 0.0, result["B"]!!, 0.001)
        assertEquals(-1.0, result["C"]!!, 0.001)
    }

    @Test
    fun `mixed evaluations average correctly`() {
        // User1=[A,B,C], User2=[C,B,A], User3=[A,B,C]
        val evals = listOf(eval("A", "B", "C"), eval("C", "B", "A"), eval("A", "B", "C"))
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
        val evals = List(3) { eval("A", "B") }   // "C" never appears
        val result = ScoreAggregator.aggregate(evals, listOf("A", "B", "C"), minVotes = 3)
        assertNull(result["C"])
    }

    @Test
    fun `user evaluating only subset contributes vote only to evaluated items`() {
        // Only A and C evaluated — B should accumulate zero votes
        val evals = List(3) { eval("A", "C") }
        val result = ScoreAggregator.aggregate(evals, listOf("A", "B", "C"), minVotes = 3)
        // A and C have 3 votes → should have scores
        assertNotNull(result["A"])
        assertNotNull(result["C"])
        // B has 0 votes → null (below threshold 3)
        assertNull(result["B"])
    }
}
