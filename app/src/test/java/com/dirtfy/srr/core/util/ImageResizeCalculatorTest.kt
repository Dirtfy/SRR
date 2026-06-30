package com.dirtfy.srr.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageResizeCalculatorTest {

    // ── sampleSize ────────────────────────────────────────────────────────────

    @Test
    fun `image smaller than max dimension — sampleSize is 1`() {
        val plan = planResize(800, 600, 1920)
        assertEquals(1, plan.sampleSize)
    }

    @Test
    fun `image exactly at max dimension — sampleSize is 1`() {
        val plan = planResize(1920, 1080, 1920)
        assertEquals(1, plan.sampleSize)
    }

    @Test
    fun `very large image — sampleSize halves until both sides fit within 2x max`() {
        // 8000×6000 → halve to 4000×3000; h=3000 < 3840, so loop stops → sampleSize=2
        val plan = planResize(8000, 6000, 1920)
        assertEquals(2, plan.sampleSize)
    }

    @Test
    fun `both sides must exceed 2x max for halving — narrow tall image stays at sampleSize 1`() {
        // Width 600 < 3840 from the start → loop never runs
        val plan = planResize(600, 7680, 1920)
        assertEquals(1, plan.sampleSize)
    }

    @Test
    fun `truly large portrait — sampleSize halves when both sides exceed 2x max`() {
        // 4000×8000 → halve to 2000×4000; w=2000 < 3840, stop → sampleSize=2
        val plan = planResize(4000, 8000, 1920)
        assertEquals(2, plan.sampleSize)
    }

    // ── targetWidth / targetHeight ────────────────────────────────────────────

    @Test
    fun `landscape image wider than max — longest side scaled to maxDimension`() {
        val plan = planResize(3840, 2160, 1920)
        assertEquals(1920, plan.targetWidth)
        assertEquals(1080, plan.targetHeight)
    }

    @Test
    fun `portrait image taller than max — longest side scaled to maxDimension`() {
        val plan = planResize(1080, 3840, 1920)
        assertEquals(540, plan.targetWidth)
        assertEquals(1920, plan.targetHeight)
    }

    @Test
    fun `square image larger than max — both sides equal maxDimension`() {
        val plan = planResize(4000, 4000, 1920)
        assertEquals(plan.targetWidth, plan.targetHeight)
        assertTrue(plan.targetWidth <= 1920)
    }

    @Test
    fun `image smaller than max — target dimensions unchanged from original`() {
        val plan = planResize(640, 480, 1920)
        assertEquals(640, plan.targetWidth)
        assertEquals(480, plan.targetHeight)
    }

    @Test
    fun `target dimensions always at least 1`() {
        val plan = planResize(1, 1, 1920)
        assertTrue(plan.targetWidth >= 1)
        assertTrue(plan.targetHeight >= 1)
    }

    // ── aspect ratio preservation ─────────────────────────────────────────────

    @Test
    fun `aspect ratio is preserved within 1 pixel for landscape`() {
        val origRatio = 16.0 / 9.0
        val plan = planResize(3840, 2160, 1920)
        val planRatio = plan.targetWidth.toDouble() / plan.targetHeight.toDouble()
        assertEquals(origRatio, planRatio, 0.01)
    }

    @Test
    fun `aspect ratio is preserved within 1 pixel for portrait`() {
        val plan = planResize(1080, 1920, 1280)
        val planRatio = plan.targetWidth.toDouble() / plan.targetHeight.toDouble()
        val origRatio = 1080.0 / 1920.0
        assertEquals(origRatio, planRatio, 0.01)
    }

    // ── guard conditions ──────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `zero width throws`() {
        planResize(0, 100, 1920)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero height throws`() {
        planResize(100, 0, 1920)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero maxDimension throws`() {
        planResize(100, 100, 0)
    }
}
