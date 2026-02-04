package com.ciclismo.portugal.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LeagueMember ranking calculations.
 */
class LeagueMemberTest {

    // ==================== RANK CHANGE TESTS ====================

    @Test
    fun `rank change is positive when climbing rankings`() {
        val member = createMember(rank = 5, previousRank = 10)
        assertEquals(5, member.rankChange) // 10 - 5 = 5
    }

    @Test
    fun `rank change is negative when falling rankings`() {
        val member = createMember(rank = 10, previousRank = 5)
        assertEquals(-5, member.rankChange) // 5 - 10 = -5
    }

    @Test
    fun `rank change is zero when position unchanged`() {
        val member = createMember(rank = 5, previousRank = 5)
        assertEquals(0, member.rankChange)
    }

    @Test
    fun `rank change from 1 to 1 is zero`() {
        val member = createMember(rank = 1, previousRank = 1)
        assertEquals(0, member.rankChange)
    }

    // ==================== IS RISING TESTS ====================

    @Test
    fun `isRising is true when rank improved`() {
        val member = createMember(rank = 3, previousRank = 7)
        assertTrue(member.isRising)
    }

    @Test
    fun `isRising is false when rank dropped`() {
        val member = createMember(rank = 7, previousRank = 3)
        assertFalse(member.isRising)
    }

    @Test
    fun `isRising is false when rank unchanged`() {
        val member = createMember(rank = 5, previousRank = 5)
        assertFalse(member.isRising)
    }

    // ==================== IS FALLING TESTS ====================

    @Test
    fun `isFalling is true when rank dropped`() {
        val member = createMember(rank = 10, previousRank = 3)
        assertTrue(member.isFalling)
    }

    @Test
    fun `isFalling is false when rank improved`() {
        val member = createMember(rank = 3, previousRank = 10)
        assertFalse(member.isFalling)
    }

    @Test
    fun `isFalling is false when rank unchanged`() {
        val member = createMember(rank = 5, previousRank = 5)
        assertFalse(member.isFalling)
    }

    // ==================== RANK CHANGE DISPLAY TESTS ====================

    @Test
    fun `rankChangeDisplay shows positive change with plus sign`() {
        val member = createMember(rank = 3, previousRank = 8)
        assertEquals("+5", member.rankChangeDisplay)
    }

    @Test
    fun `rankChangeDisplay shows negative change with minus sign`() {
        val member = createMember(rank = 8, previousRank = 3)
        assertEquals("-5", member.rankChangeDisplay)
    }

    @Test
    fun `rankChangeDisplay shows dash when unchanged`() {
        val member = createMember(rank = 5, previousRank = 5)
        assertEquals("-", member.rankChangeDisplay)
    }

    @Test
    fun `rankChangeDisplay for large positive change`() {
        val member = createMember(rank = 1, previousRank = 100)
        assertEquals("+99", member.rankChangeDisplay)
    }

    @Test
    fun `rankChangeDisplay for small positive change`() {
        val member = createMember(rank = 4, previousRank = 5)
        assertEquals("+1", member.rankChangeDisplay)
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `new member at last place with no previous rank`() {
        val member = createMember(rank = 100, previousRank = 100)
        assertEquals(0, member.rankChange)
        assertFalse(member.isRising)
        assertFalse(member.isFalling)
        assertEquals("-", member.rankChangeDisplay)
    }

    @Test
    fun `member jumping from last to first`() {
        val member = createMember(rank = 1, previousRank = 50)
        assertEquals(49, member.rankChange)
        assertTrue(member.isRising)
        assertFalse(member.isFalling)
        assertEquals("+49", member.rankChangeDisplay)
    }

    @Test
    fun `member dropping from first to last`() {
        val member = createMember(rank = 50, previousRank = 1)
        assertEquals(-49, member.rankChange)
        assertFalse(member.isRising)
        assertTrue(member.isFalling)
        assertEquals("-49", member.rankChangeDisplay)
    }

    // ==================== DEFAULT VALUES TESTS ====================

    @Test
    fun `default rank is 0`() {
        val member = LeagueMember(
            leagueId = "league-1",
            userId = "user-1",
            teamId = "team-1",
            teamName = "Test Team"
        )
        assertEquals(0, member.rank)
    }

    @Test
    fun `default points is 0`() {
        val member = LeagueMember(
            leagueId = "league-1",
            userId = "user-1",
            teamId = "team-1",
            teamName = "Test Team"
        )
        assertEquals(0, member.points)
    }

    @Test
    fun `default previousRank is 0`() {
        val member = LeagueMember(
            leagueId = "league-1",
            userId = "user-1",
            teamId = "team-1",
            teamName = "Test Team"
        )
        assertEquals(0, member.previousRank)
    }

    @Test
    fun `default season is current season`() {
        val member = LeagueMember(
            leagueId = "league-1",
            userId = "user-1",
            teamId = "team-1",
            teamName = "Test Team"
        )
        assertEquals(SeasonConfig.CURRENT_SEASON, member.season)
    }

    // Helper function to create test LeagueMember
    private fun createMember(
        rank: Int = 1,
        previousRank: Int = 1,
        points: Int = 0
    ) = LeagueMember(
        leagueId = "test-league-id",
        userId = "test-user-id",
        teamId = "test-team-id",
        teamName = "Test Team",
        rank = rank,
        points = points,
        previousRank = previousRank,
        joinedAt = System.currentTimeMillis(),
        season = SeasonConfig.CURRENT_SEASON
    )
}
