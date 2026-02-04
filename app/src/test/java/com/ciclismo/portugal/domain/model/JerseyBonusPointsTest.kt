package com.ciclismo.portugal.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for JerseyBonusPoints calculations.
 */
class JerseyBonusPointsTest {

    @Test
    fun `GC leader gets 10 points`() {
        assertEquals(JerseyBonusPoints.GC_LEADER, 10)
    }

    @Test
    fun `points leader gets 5 points`() {
        assertEquals(JerseyBonusPoints.POINTS_LEADER, 5)
    }

    @Test
    fun `mountains leader gets 5 points`() {
        assertEquals(JerseyBonusPoints.MOUNTAINS_LEADER, 5)
    }

    @Test
    fun `young leader gets 3 points`() {
        assertEquals(JerseyBonusPoints.YOUNG_LEADER, 3)
    }

    @Test
    fun `no jerseys returns zero`() {
        val bonus = JerseyBonusPoints.calculate(
            isGcLeader = false,
            isPointsLeader = false,
            isMountainsLeader = false,
            isYoungLeader = false
        )
        assertEquals(0, bonus)
    }

    @Test
    fun `only GC leader returns 10`() {
        val bonus = JerseyBonusPoints.calculate(
            isGcLeader = true,
            isPointsLeader = false,
            isMountainsLeader = false,
            isYoungLeader = false
        )
        assertEquals(10, bonus)
    }

    @Test
    fun `only points leader returns 5`() {
        val bonus = JerseyBonusPoints.calculate(
            isGcLeader = false,
            isPointsLeader = true,
            isMountainsLeader = false,
            isYoungLeader = false
        )
        assertEquals(5, bonus)
    }

    @Test
    fun `only mountains leader returns 5`() {
        val bonus = JerseyBonusPoints.calculate(
            isGcLeader = false,
            isPointsLeader = false,
            isMountainsLeader = true,
            isYoungLeader = false
        )
        assertEquals(5, bonus)
    }

    @Test
    fun `only young leader returns 3`() {
        val bonus = JerseyBonusPoints.calculate(
            isGcLeader = false,
            isPointsLeader = false,
            isMountainsLeader = false,
            isYoungLeader = true
        )
        assertEquals(3, bonus)
    }

    @Test
    fun `GC and young leader returns 13`() {
        val bonus = JerseyBonusPoints.calculate(
            isGcLeader = true,
            isPointsLeader = false,
            isMountainsLeader = false,
            isYoungLeader = true
        )
        assertEquals(13, bonus) // 10 + 3
    }

    @Test
    fun `all four jerseys returns 23`() {
        val bonus = JerseyBonusPoints.calculate(
            isGcLeader = true,
            isPointsLeader = true,
            isMountainsLeader = true,
            isYoungLeader = true
        )
        assertEquals(23, bonus) // 10 + 5 + 5 + 3
    }

    @Test
    fun `GC plus points plus mountains returns 20`() {
        val bonus = JerseyBonusPoints.calculate(
            isGcLeader = true,
            isPointsLeader = true,
            isMountainsLeader = true,
            isYoungLeader = false
        )
        assertEquals(20, bonus) // 10 + 5 + 5
    }

    @Test
    fun `points and mountains combination returns 10`() {
        val bonus = JerseyBonusPoints.calculate(
            isGcLeader = false,
            isPointsLeader = true,
            isMountainsLeader = true,
            isYoungLeader = false
        )
        assertEquals(10, bonus) // 5 + 5
    }
}
