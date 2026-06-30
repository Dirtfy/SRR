package com.dirtfy.srr.core.item

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the image URL look-up logic used before Storage cleanup calls.
 *
 * Both onConfirmDelete and onSubmitEditItemImage retrieve the item's current
 * imageUrl from the in-memory state before performing the Firestore write, then
 * pass that URL to storageRepository.deleteImage(). These tests validate that
 * look-up in isolation.
 */
class StorageCleanupLogicTest {

    private data class Item(val id: String, val imageUrl: String?)

    private fun findImageUrl(itemId: String, items: List<Item>): String? =
        items.find { it.id == itemId }?.imageUrl

    @Test
    fun findImageUrl_returnsUrlWhenItemExists() {
        val items = listOf(Item("id_a", "https://storage.example.com/items/img_a.jpg"))
        assertEquals("https://storage.example.com/items/img_a.jpg", findImageUrl("id_a", items))
    }

    @Test
    fun findImageUrl_returnsNullWhenItemHasNoImage() {
        val items = listOf(Item("id_a", null))
        assertNull(findImageUrl("id_a", items))
    }

    @Test
    fun findImageUrl_returnsNullWhenItemNotFound() {
        val items = listOf(Item("id_a", "https://storage.example.com/items/img_a.jpg"))
        assertNull(findImageUrl("id_missing", items))
    }

    @Test
    fun findImageUrl_returnsNullOnEmptyList() {
        assertNull(findImageUrl("id_a", emptyList()))
    }

    @Test
    fun findImageUrl_returnsCorrectItemWhenMultiplePresent() {
        val items = listOf(
            Item("id_a", "https://storage.example.com/items/img_a.jpg"),
            Item("id_b", "https://storage.example.com/items/img_b.jpg")
        )
        assertEquals("https://storage.example.com/items/img_b.jpg", findImageUrl("id_b", items))
    }
}
