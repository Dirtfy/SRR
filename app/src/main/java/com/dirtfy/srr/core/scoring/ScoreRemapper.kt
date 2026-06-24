package com.dirtfy.srr.core.scoring

object ScoreRemapper {
    // [-1.0, +1.0] → [0.0, 10.0]
    // -1.0 → 0.0,  0.0 → 5.0,  +1.0 → 10.0
    fun remap(raw: Double): Double = (raw + 1.0) / 2.0 * 10.0
    fun remapOrNull(raw: Double?): Double? = raw?.let { remap(it) }
}
