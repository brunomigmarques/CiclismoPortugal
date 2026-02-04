package com.ciclismo.portugal.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for StagePointsTable calculations.
 */
class StagePointsTableTest {

    @Test
    fun `first place gets 50 base points`() {
        assertEquals(50, StagePointsTable.getPoints(1, StageType.FLAT))
    }

    @Test
    fun `second place gets 40 base points`() {
        assertEquals(40, StagePointsTable.getPoints(2, StageType.FLAT))
    }

    @Test
    fun `third place gets 35 base points`() {
        assertEquals(35, StagePointsTable.getPoints(3, StageType.FLAT))
    }

    @Test
    fun `positions 1-20 all award points`() {
        val positions = StagePointsTable.getPointsPositions()
        assertEquals(20, positions.size)
        assertEquals(1, positions.first())
        assertEquals(20, positions.last())
    }

    @Test
    fun `position outside top 20 gets zero points`() {
        assertEquals(0, StagePointsTable.getPoints(21, StageType.FLAT))
        assertEquals(0, StagePointsTable.getPoints(50, StageType.FLAT))
        assertEquals(0, StagePointsTable.getPoints(100, StageType.FLAT))
    }

    @Test
    fun `mountain stage applies 1_2x multiplier`() {
        // 1st place: 50 * 1.2 = 60
        assertEquals(60, StagePointsTable.getPoints(1, StageType.MOUNTAIN))
        // 2nd place: 40 * 1.2 = 48
        assertEquals(48, StagePointsTable.getPoints(2, StageType.MOUNTAIN))
        // 10th place: 14 * 1.2 = 16.8 -> 16
        assertEquals(16, StagePointsTable.getPoints(10, StageType.MOUNTAIN))
    }

    @Test
    fun `ITT stage applies 1_2x multiplier`() {
        // 1st place: 50 * 1.2 = 60
        assertEquals(60, StagePointsTable.getPoints(1, StageType.ITT))
        // 5th place: 25 * 1.2 = 30
        assertEquals(30, StagePointsTable.getPoints(5, StageType.ITT))
    }

    @Test
    fun `prologue stage applies 0_5x multiplier`() {
        // 1st place: 50 * 0.5 = 25
        assertEquals(25, StagePointsTable.getPoints(1, StageType.PROLOGUE))
        // 2nd place: 40 * 0.5 = 20
        assertEquals(20, StagePointsTable.getPoints(2, StageType.PROLOGUE))
        // 10th place: 14 * 0.5 = 7
        assertEquals(7, StagePointsTable.getPoints(10, StageType.PROLOGUE))
    }

    @Test
    fun `flat stage uses base points`() {
        assertEquals(50, StagePointsTable.getPoints(1, StageType.FLAT))
        assertEquals(40, StagePointsTable.getPoints(2, StageType.FLAT))
        assertEquals(14, StagePointsTable.getPoints(10, StageType.FLAT))
    }

    @Test
    fun `points decrease as position increases`() {
        val flatPoints = (1..20).map { StagePointsTable.getPoints(it, StageType.FLAT) }

        // Each position should have same or fewer points than the previous
        for (i in 1 until flatPoints.size) {
            assertTrue(
                "Position ${i+1} should have fewer or equal points than position $i",
                flatPoints[i] <= flatPoints[i-1]
            )
        }
    }

    @Test
    fun `top 3 positions have distinct points`() {
        val first = StagePointsTable.getPoints(1, StageType.FLAT)
        val second = StagePointsTable.getPoints(2, StageType.FLAT)
        val third = StagePointsTable.getPoints(3, StageType.FLAT)

        assertTrue(first > second)
        assertTrue(second > third)
    }

    @Test
    fun `position 19 and 20 both get 1 point`() {
        assertEquals(1, StagePointsTable.getPoints(19, StageType.FLAT))
        assertEquals(1, StagePointsTable.getPoints(20, StageType.FLAT))
    }

    @Test
    fun `negative position returns zero`() {
        assertEquals(0, StagePointsTable.getPoints(-1, StageType.FLAT))
        assertEquals(0, StagePointsTable.getPoints(0, StageType.FLAT))
    }
}
