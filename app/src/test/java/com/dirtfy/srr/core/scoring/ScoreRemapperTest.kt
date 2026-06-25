package com.dirtfy.srr.core.scoring

import org.junit.Assert.*
import org.junit.Test

class ScoreRemapperTest {

    @Test
    fun `minus one maps to 0`() = assertEquals(0.0,  ScoreRemapper.remap(-1.0), 0.0)

    @Test
    fun `zero maps to 5`() = assertEquals(5.0,  ScoreRemapper.remap(0.0),  0.0)

    @Test
    fun `plus one maps to 10`() = assertEquals(10.0, ScoreRemapper.remap(+1.0), 0.0)

    @Test
    fun `minus 0 5 maps to 2 5`() = assertEquals(2.5,  ScoreRemapper.remap(-0.5), 0.0)

    @Test
    fun `plus 0 5 maps to 7 5`() = assertEquals(7.5,  ScoreRemapper.remap(+0.5), 0.0)

    @Test
    fun `remapOrNull of null returns null`() = assertNull(ScoreRemapper.remapOrNull(null))

    @Test
    fun `remapOrNull of minus one returns 0`() = assertEquals(0.0, ScoreRemapper.remapOrNull(-1.0)!!, 0.0)
}
