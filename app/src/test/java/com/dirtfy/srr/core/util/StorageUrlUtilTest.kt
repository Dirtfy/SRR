package com.dirtfy.srr.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StorageUrlUtilTest {

    private val bucket = "shared-relative-rank.firebasestorage.app"
    private val uuid   = "3fa4e7b2-1c08-4d12-9a6e-0000deadbeef"

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    fun `production download URL — extracts items path`() {
        val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o/items%2F$uuid.jpg" +
                  "?alt=media&token=abc123"
        assertEquals("items/$uuid.jpg", extractStoragePath(url))
    }

    @Test
    fun `URL without query string — extracts path`() {
        val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o/items%2F$uuid.jpg"
        assertEquals("items/$uuid.jpg", extractStoragePath(url))
    }

    @Test
    fun `emulator URL format — extracts path`() {
        val url = "http://10.0.2.2:9199/v0/b/$bucket/o/items%2F$uuid.jpg?alt=media&token=fake"
        assertEquals("items/$uuid.jpg", extractStoragePath(url))
    }

    @Test
    fun `nested path with multiple segments — decodes correctly`() {
        val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o/a%2Fb%2Fc.jpg?alt=media"
        assertEquals("a/b/c.jpg", extractStoragePath(url))
    }

    // ── edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `URL without slash-o-slash — returns null`() {
        val url = "https://example.com/v0/b/$bucket/items%2F$uuid.jpg"
        assertNull(extractStoragePath(url))
    }

    @Test
    fun `empty string — returns null`() {
        assertNull(extractStoragePath(""))
    }

    @Test
    fun `malformed URL — returns null`() {
        assertNull(extractStoragePath("not a url at all"))
    }
}
