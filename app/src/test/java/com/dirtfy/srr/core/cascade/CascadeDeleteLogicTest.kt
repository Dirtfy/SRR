package com.dirtfy.srr.core.cascade

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the cascade-delete filtering logic used in RemoteEvaluationRepository.
 *
 * removeItemFromEvaluations filters a deleted item ID out of each orderedItemIds list.
 * These tests validate that filtering invariant in isolation, without Firestore.
 */
class CascadeDeleteLogicTest {

    private fun removeItem(orderedItemIds: List<String>, itemId: String): List<String> =
        orderedItemIds.filter { it != itemId }

    @Test
    fun removeItem_fromMiddleOfList_correctOrder() {
        val result = removeItem(listOf("a", "b", "c"), "b")
        assertEquals(listOf("a", "c"), result)
    }

    @Test
    fun removeItem_fromHead_correctOrder() {
        val result = removeItem(listOf("a", "b", "c"), "a")
        assertEquals(listOf("b", "c"), result)
    }

    @Test
    fun removeItem_fromTail_correctOrder() {
        val result = removeItem(listOf("a", "b", "c"), "c")
        assertEquals(listOf("a", "b"), result)
    }

    @Test
    fun removeItem_notPresent_listUnchanged() {
        val original = listOf("a", "b", "c")
        val result = removeItem(original, "x")
        assertEquals(original, result)
    }

    @Test
    fun removeItem_fromSingletonList_returnsEmpty() {
        val result = removeItem(listOf("a"), "a")
        assertTrue(result.isEmpty())
    }

    @Test
    fun removeItem_fromEmptyList_returnsEmpty() {
        val result = removeItem(emptyList(), "a")
        assertTrue(result.isEmpty())
    }

    @Test
    fun removeItem_doesNotRemoveOtherItems() {
        val original = listOf("a", "b", "c", "d")
        val result = removeItem(original, "b")
        assertTrue("a must remain", "a" in result)
        assertTrue("c must remain", "c" in result)
        assertTrue("d must remain", "d" in result)
        assertFalse("b must be gone", "b" in result)
    }
}
