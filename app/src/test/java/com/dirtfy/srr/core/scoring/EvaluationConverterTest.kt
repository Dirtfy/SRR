package com.dirtfy.srr.core.scoring

import org.junit.Assert.*
import org.junit.Test

class EvaluationConverterTest {

    @Test
    fun `empty list returns empty map`() {
        val result = EvaluationConverter.convert(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single item gets neutral score 0`() {
        val result = EvaluationConverter.convert(listOf("A"))
        assertEquals(1, result.size)
        assertEquals(0.0, result["A"]!!, 0.0)
    }

    @Test
    fun `two items get plus one and minus one`() {
        val result = EvaluationConverter.convert(listOf("A", "B"))
        assertEquals(+1.0, result["A"]!!, 0.0)
        assertEquals(-1.0, result["B"]!!, 0.0)
    }

    @Test
    fun `three items are uniformly spaced`() {
        val result = EvaluationConverter.convert(listOf("A", "B", "C"))
        assertEquals(+1.0, result["A"]!!, 0.0)
        assertEquals( 0.0, result["B"]!!, 0.0)
        assertEquals(-1.0, result["C"]!!, 0.0)
    }

    @Test
    fun `four items are uniformly spaced within delta`() {
        val result = EvaluationConverter.convert(listOf("A", "B", "C", "D"))
        assertEquals(+1.0,          result["A"]!!, 0.001)
        assertEquals(+1.0 / 3.0,   result["B"]!!, 0.001)
        assertEquals(-1.0 / 3.0,   result["C"]!!, 0.001)
        assertEquals(-1.0,          result["D"]!!, 0.001)
    }

    @Test
    fun `items not in the list are absent from the result`() {
        val result = EvaluationConverter.convert(listOf("A", "B"))
        assertFalse(result.containsKey("Z"))
    }
}
