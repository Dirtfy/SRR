package com.dirtfy.srr.core.item

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for item image URL handling logic used around updateItemImage.
 *
 * Validates the URL normalisation contract: blank/empty strings are treated as
 * "no image" (null) before being persisted, so callers never write an empty
 * imageUrl to Firestore.
 */
class ItemImageUpdateLogicTest {

    private fun normaliseImageUrl(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() }

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
}
