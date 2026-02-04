package com.ciclismo.portugal.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FinalGcBonusPoints calculations.
 */
class FinalGcBonusPointsTest {

    @Test
    fun `first place GC gets 200 points`() {
        assertEquals(200, FinalGcBonusPoints.getBonus(1))
    }

    @Test
    fun `second place GC gets 150 points`() {
        assertEquals(150, FinalGcBonusPoints.getBonus(2))
    }

    @Test
    fun `third place GC gets 100 points`() {
        assertEquals(100, FinalGcBonusPoints.getBonus(3))
    }

    @Test
    fun `fourth place GC gets 80 points`() {
        assertEquals(80, FinalGcBonusPoints.getBonus(4))
    }

    @Test
    fun `fifth place GC gets 60 points`() {
        assertEquals(60, FinalGcBonusPoints.getBonus(5))
    }

    @Test
    fun `positions 6 to 10 get decreasing bonus`() {
        assertEquals(50, FinalGcBonusPoints.getBonus(6))
        assertEquals(40, FinalGcBonusPoints.getBonus(7))
        assertEquals(35, FinalGcBonusPoints.getBonus(8))
        assertEquals(30, FinalGcBonusPoints.getBonus(9))
        assertEquals(25, FinalGcBonusPoints.getBonus(10))
    }

    @Test
    fun `position outside top 10 gets zero`() {
        assertEquals(0, FinalGcBonusPoints.getBonus(11))
        assertEquals(0, FinalGcBonusPoints.getBonus(20))
        assertEquals(0, FinalGcBonusPoints.getBonus(100))
    }

    @Test
    fun `invalid position gets zero`() {
        assertEquals(0, FinalGcBonusPoints.getBonus(0))
        assertEquals(0, FinalGcBonusPoints.getBonus(-1))
    }

    @Test
    fun `bonus decreases as position increases`() {
        val bonuses = (1..10).map { FinalGcBonusPoints.getBonus(it) }

        for (i in 1 until bonuses.size) {
            assertTrue(
                "Position ${i+1} should have fewer bonus points than position $i",
                bonuses[i] < bonuses[i-1]
            )
        }
    }

    @Test
    fun `top 3 have significant gap from rest`() {
        val third = FinalGcBonusPoints.getBonus(3)
        val fourth = FinalGcBonusPoints.getBonus(4)

        // 100 - 80 = 20 point gap between 3rd and 4th
        assertEquals(20, third - fourth)
    }

    @Test
    fun `winner gets double of fourth place`() {
        val first = FinalGcBonusPoints.getBonus(1)
        val fourth = FinalGcBonusPoints.getBonus(4)

        assertTrue(first > fourth * 2) // 200 > 160
    }
}
