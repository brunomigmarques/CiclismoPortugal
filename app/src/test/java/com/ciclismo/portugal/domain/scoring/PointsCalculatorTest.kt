package com.ciclismo.portugal.domain.scoring

import com.ciclismo.portugal.domain.model.RaceResult
import com.ciclismo.portugal.domain.model.RaceType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PointsCalculator - the core fantasy points calculation logic.
 */
class PointsCalculatorTest {

    // ==================== ONE-DAY RACE TESTS ====================

    @Test
    fun `one-day race first place gets 100 points`() {
        assertEquals(100, PointsCalculator.calculateOneDayPoints(1))
    }

    @Test
    fun `one-day race second place gets 70 points`() {
        assertEquals(70, PointsCalculator.calculateOneDayPoints(2))
    }

    @Test
    fun `one-day race third place gets 50 points`() {
        assertEquals(50, PointsCalculator.calculateOneDayPoints(3))
    }

    @Test
    fun `one-day race top 10 positions get specific points`() {
        val expectedPoints = mapOf(
            1 to 100, 2 to 70, 3 to 50, 4 to 40, 5 to 35,
            6 to 30, 7 to 25, 8 to 20, 9 to 15, 10 to 10
        )
        expectedPoints.forEach { (position, expected) ->
            assertEquals(
                "Position $position should get $expected points",
                expected,
                PointsCalculator.calculateOneDayPoints(position)
            )
        }
    }

    @Test
    fun `one-day race positions 11-20 get 5 points`() {
        (11..20).forEach { position ->
            assertEquals(
                "Position $position should get 5 points",
                5,
                PointsCalculator.calculateOneDayPoints(position)
            )
        }
    }

    @Test
    fun `one-day race positions 21-30 get 2 points`() {
        (21..30).forEach { position ->
            assertEquals(
                "Position $position should get 2 points",
                2,
                PointsCalculator.calculateOneDayPoints(position)
            )
        }
    }

    @Test
    fun `one-day race positions outside 30 get 0 points`() {
        assertEquals(0, PointsCalculator.calculateOneDayPoints(31))
        assertEquals(0, PointsCalculator.calculateOneDayPoints(50))
        assertEquals(0, PointsCalculator.calculateOneDayPoints(100))
    }

    @Test
    fun `one-day race null position gets 0 points`() {
        assertEquals(0, PointsCalculator.calculateOneDayPoints(null))
    }

    // ==================== STAGE RACE TESTS ====================

    @Test
    fun `stage race first place gets 50 points`() {
        assertEquals(50, PointsCalculator.calculateStagePoints(1))
    }

    @Test
    fun `stage race second place gets 35 points`() {
        assertEquals(35, PointsCalculator.calculateStagePoints(2))
    }

    @Test
    fun `stage race third place gets 25 points`() {
        assertEquals(25, PointsCalculator.calculateStagePoints(3))
    }

    @Test
    fun `stage race top 10 positions get specific points`() {
        val expectedPoints = mapOf(
            1 to 50, 2 to 35, 3 to 25, 4 to 18, 5 to 12,
            6 to 10, 7 to 8, 8 to 6, 9 to 4, 10 to 2
        )
        expectedPoints.forEach { (position, expected) ->
            assertEquals(
                "Position $position should get $expected points",
                expected,
                PointsCalculator.calculateStagePoints(position)
            )
        }
    }

    @Test
    fun `stage race positions 11-20 get 1 point`() {
        (11..20).forEach { position ->
            assertEquals(
                "Position $position should get 1 point",
                1,
                PointsCalculator.calculateStagePoints(position)
            )
        }
    }

    @Test
    fun `stage race positions outside 20 get 0 points`() {
        assertEquals(0, PointsCalculator.calculateStagePoints(21))
        assertEquals(0, PointsCalculator.calculateStagePoints(50))
    }

    @Test
    fun `stage race null position gets 0 points`() {
        assertEquals(0, PointsCalculator.calculateStagePoints(null))
    }

    // ==================== JERSEY CONSTANTS TESTS ====================

    @Test
    fun `GC leader jersey is worth 10 points`() {
        assertEquals(10, PointsCalculator.JERSEY_GC_LEADER)
    }

    @Test
    fun `mountains jersey is worth 5 points`() {
        assertEquals(5, PointsCalculator.JERSEY_MOUNTAINS)
    }

    @Test
    fun `points jersey is worth 5 points`() {
        assertEquals(5, PointsCalculator.JERSEY_POINTS)
    }

    @Test
    fun `young rider jersey is worth 3 points`() {
        assertEquals(3, PointsCalculator.JERSEY_YOUNG)
    }

    // ==================== FINAL GC BONUS TESTS ====================

    @Test
    fun `final GC first place gets 200 bonus`() {
        assertEquals(200, PointsCalculator.calculateGcFinalBonus(1))
    }

    @Test
    fun `final GC second place gets 150 bonus`() {
        assertEquals(150, PointsCalculator.calculateGcFinalBonus(2))
    }

    @Test
    fun `final GC third place gets 100 bonus`() {
        assertEquals(100, PointsCalculator.calculateGcFinalBonus(3))
    }

    @Test
    fun `final GC positions 4-10 get decreasing bonus`() {
        val expected = mapOf(
            4 to 75, 5 to 50, 6 to 40, 7 to 30, 8 to 25, 9 to 20, 10 to 15
        )
        expected.forEach { (position, bonus) ->
            assertEquals(
                "GC position $position should get $bonus bonus",
                bonus,
                PointsCalculator.calculateGcFinalBonus(position)
            )
        }
    }

    @Test
    fun `final GC positions 11-20 get 10 bonus`() {
        (11..20).forEach { position ->
            assertEquals(
                "GC position $position should get 10 bonus",
                10,
                PointsCalculator.calculateGcFinalBonus(position)
            )
        }
    }

    @Test
    fun `final GC positions outside 20 get 0 bonus`() {
        assertEquals(0, PointsCalculator.calculateGcFinalBonus(21))
        assertEquals(0, PointsCalculator.calculateGcFinalBonus(50))
    }

    @Test
    fun `final GC null position gets 0 bonus`() {
        assertEquals(0, PointsCalculator.calculateGcFinalBonus(null))
    }

    // ==================== CAPTAIN MULTIPLIER TESTS ====================

    @Test
    fun `captain doubles points`() {
        assertEquals(100, PointsCalculator.applyCaptain(50))
        assertEquals(200, PointsCalculator.applyCaptain(100))
        assertEquals(0, PointsCalculator.applyCaptain(0))
    }

    @Test
    fun `triple captain triples points`() {
        assertEquals(150, PointsCalculator.applyTripleCaptain(50))
        assertEquals(300, PointsCalculator.applyTripleCaptain(100))
        assertEquals(0, PointsCalculator.applyTripleCaptain(0))
    }

    // ==================== CALCULATE RESULT POINTS TESTS ====================

    @Test
    fun `calculate result points for one-day race winner`() {
        val result = createRaceResult(position = 1)
        val points = PointsCalculator.calculateResultPoints(result, RaceType.ONE_DAY)
        assertEquals(100, points)
    }

    @Test
    fun `calculate result points for stage race winner`() {
        val result = createRaceResult(position = 1)
        val points = PointsCalculator.calculateResultPoints(result, RaceType.GRAND_TOUR)
        assertEquals(50, points)
    }

    @Test
    fun `calculate result points includes GC leader jersey bonus`() {
        val result = createRaceResult(position = 1, isGcLeader = true)
        val points = PointsCalculator.calculateResultPoints(result, RaceType.GRAND_TOUR)
        assertEquals(60, points) // 50 + 10
    }

    @Test
    fun `calculate result points includes all jersey bonuses`() {
        val result = createRaceResult(
            position = 1,
            isGcLeader = true,
            isMountainsLeader = true,
            isPointsLeader = true,
            isYoungLeader = true
        )
        val points = PointsCalculator.calculateResultPoints(result, RaceType.GRAND_TOUR)
        assertEquals(73, points) // 50 + 10 + 5 + 5 + 3
    }

    @Test
    fun `calculate result points includes bonus points`() {
        val result = createRaceResult(position = 1, bonusPoints = 15)
        val points = PointsCalculator.calculateResultPoints(result, RaceType.ONE_DAY)
        assertEquals(115, points) // 100 + 15
    }

    // ==================== CALCULATE CYCLIST POINTS TESTS ====================

    @Test
    fun `calculate cyclist points without captain`() {
        val result = createRaceResult(position = 1)
        val points = PointsCalculator.calculateCyclistPoints(result, RaceType.ONE_DAY, false, false)
        assertEquals(100, points)
    }

    @Test
    fun `calculate cyclist points with captain doubles`() {
        val result = createRaceResult(position = 1)
        val points = PointsCalculator.calculateCyclistPoints(result, RaceType.ONE_DAY, true, false)
        assertEquals(200, points) // 100 * 2
    }

    @Test
    fun `calculate cyclist points with triple captain triples`() {
        val result = createRaceResult(position = 1)
        val points = PointsCalculator.calculateCyclistPoints(result, RaceType.ONE_DAY, true, true)
        assertEquals(300, points) // 100 * 3
    }

    @Test
    fun `calculate cyclist points with captain and jerseys`() {
        val result = createRaceResult(position = 1, isGcLeader = true)
        val points = PointsCalculator.calculateCyclistPoints(result, RaceType.GRAND_TOUR, true, false)
        assertEquals(120, points) // (50 + 10) * 2
    }

    @Test
    fun `calculate cyclist points with triple captain and all jerseys`() {
        val result = createRaceResult(
            position = 1,
            isGcLeader = true,
            isMountainsLeader = true,
            isPointsLeader = true,
            isYoungLeader = true
        )
        val points = PointsCalculator.calculateCyclistPoints(result, RaceType.GRAND_TOUR, true, true)
        assertEquals(219, points) // (50 + 10 + 5 + 5 + 3) * 3
    }

    // ==================== POINTS BREAKDOWN TESTS ====================

    @Test
    fun `points breakdown shows position points`() {
        val result = createRaceResult(position = 1)
        val breakdown = PointsCalculator.getPointsBreakdown(result, RaceType.ONE_DAY)

        assertTrue(breakdown.isNotEmpty())
        val positionItem = breakdown.find { it.label.contains("lugar") }
        assertNotNull(positionItem)
        assertEquals(100, positionItem?.points)
    }

    @Test
    fun `points breakdown shows jersey bonuses`() {
        val result = createRaceResult(
            position = 5,
            isGcLeader = true,
            isMountainsLeader = true
        )
        val breakdown = PointsCalculator.getPointsBreakdown(result, RaceType.GRAND_TOUR)

        assertTrue(breakdown.size >= 3) // Position + 2 jerseys
        assertTrue(breakdown.any { it.label.contains("GC") && it.points == 10 })
        assertTrue(breakdown.any { it.label.contains("Montanha") && it.points == 5 })
    }

    @Test
    fun `points breakdown shows bonus points`() {
        val result = createRaceResult(position = 10, bonusPoints = 20)
        val breakdown = PointsCalculator.getPointsBreakdown(result, RaceType.ONE_DAY)

        assertTrue(breakdown.any { it.label == "Bónus" && it.points == 20 })
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `DNF cyclist with no position gets 0 points`() {
        val result = createRaceResult(position = null)
        val points = PointsCalculator.calculateResultPoints(result, RaceType.ONE_DAY)
        assertEquals(0, points)
    }

    @Test
    fun `cyclist with only jersey bonus gets jersey points only`() {
        val result = createRaceResult(position = null, isGcLeader = true)
        val points = PointsCalculator.calculateResultPoints(result, RaceType.GRAND_TOUR)
        assertEquals(10, points) // Only GC leader bonus
    }

    @Test
    fun `last place finisher gets 0 position points`() {
        val result = createRaceResult(position = 150)
        val points = PointsCalculator.calculateResultPoints(result, RaceType.ONE_DAY)
        assertEquals(0, points)
    }

    // Helper function to create test RaceResult
    private fun createRaceResult(
        position: Int? = null,
        isGcLeader: Boolean = false,
        isMountainsLeader: Boolean = false,
        isPointsLeader: Boolean = false,
        isYoungLeader: Boolean = false,
        bonusPoints: Int = 0
    ) = RaceResult(
        id = "test-result-id",
        raceId = "test-race-id",
        cyclistId = "test-cyclist-id",
        stageNumber = null,
        position = position,
        points = 0,
        bonusPoints = bonusPoints,
        isGcLeader = isGcLeader,
        isMountainsLeader = isMountainsLeader,
        isPointsLeader = isPointsLeader,
        isYoungLeader = isYoungLeader,
        status = ""
    )
}
