package com.dirtfy.srr.core.util

/**
 * Pure Kotlin dimension math for image resizing.
 *
 * Determines the sub-sample size for an initial low-memory decode, then the
 * final pixel dimensions needed to fit within [maxDimension] on the longest
 * side. No Android imports — fully unit-testable.
 */
data class ResizePlan(
    val sampleSize: Int,
    val targetWidth: Int,
    val targetHeight: Int
)

fun planResize(origWidth: Int, origHeight: Int, maxDimension: Int): ResizePlan {
    require(origWidth > 0 && origHeight > 0) { "Dimensions must be positive" }
    require(maxDimension > 0) { "maxDimension must be positive" }

    // Halve repeatedly until the pre-sampled size is at most 2× the target.
    var sampleSize = 1
    var w = origWidth
    var h = origHeight
    while (w > maxDimension * 2 && h > maxDimension * 2) {
        sampleSize *= 2
        w /= 2
        h /= 2
    }

    // Scale precisely so the longest side equals maxDimension.
    val longest = maxOf(w, h)
    return if (longest > maxDimension) {
        val scale = maxDimension.toFloat() / longest
        ResizePlan(
            sampleSize  = sampleSize,
            targetWidth  = (w * scale).toInt().coerceAtLeast(1),
            targetHeight = (h * scale).toInt().coerceAtLeast(1)
        )
    } else {
        ResizePlan(sampleSize = sampleSize, targetWidth = w, targetHeight = h)
    }
}
