package com.dirtfy.srr.core.item

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for item image URL handling logic used around updateItemImage,
 * and for the post-update item lookup used to restore navigation state.
 */
class ItemImageUpdateLogicTest {

    private fun normaliseImageUrl(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() }

    /** Mirrors the lookup inside UserViewModel.loadAllData(selectedItemId). */
    private data class Item(val id: String, val name: String)
    private fun restoreSelectedItem(id: String?, items: List<Item>): Item? =
        id?.let { items.find { item -> item.id == it } }

    @Test
    fun normalise_nonEmptyUrl_returnsUrl() {
        val url = "https://example.com/image.jpg"
        assertEquals(url, normaliseImageUrl(url))
    }

    @Test
    fun normalise_nullUrl_returnsNull() {
        assertNull(normaliseImageUrl(null))
    }

    @Test
    fun normalise_emptyString_returnsNull() {
        assertNull(normaliseImageUrl(""))
    }

    @Test
    fun normalise_blankString_returnsNull() {
        assertNull(normaliseImageUrl("   "))
    }

    @Test
    fun normalise_urlWithLeadingTrailingSpaces_returnsTrimmed() {
        val result = normaliseImageUrl("  https://example.com/img.png  ")
        assertEquals("https://example.com/img.png", result)
    }

    // -----------------------------------------------------------------------
    // Navigation restore: item lookup after reload
    // -----------------------------------------------------------------------

    @Test
    fun restoreSelectedItem_findsItemById() {
        val items = listOf(Item("id_a", "Alpha"), Item("id_b", "Beta"))
        val restored = restoreSelectedItem("id_b", items)
        assertEquals(Item("id_b", "Beta"), restored)
    }

    @Test
    fun restoreSelectedItem_returnsNullWhenIdNotFound() {
        val items = listOf(Item("id_a", "Alpha"))
        val restored = restoreSelectedItem("id_missing", items)
        assertNull(restored)
    }

    @Test
    fun restoreSelectedItem_returnsNullWhenIdIsNull() {
        val items = listOf(Item("id_a", "Alpha"))
        val restored = restoreSelectedItem(null, items)
        assertNull(restored)
    }

    @Test
    fun restoreSelectedItem_returnsNullWhenListIsEmpty() {
        val restored = restoreSelectedItem("id_a", emptyList())
        assertNull(restored)
    }
}
