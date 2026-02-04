package com.ciclismo.portugal.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for League model and LeagueType.
 */
class LeagueTest {

    // ==================== LEAGUE TYPE TESTS ====================

    @Test
    fun `LeagueType has all expected values`() {
        val types = LeagueType.entries
        assertEquals(4, types.size)
        assertTrue(types.contains(LeagueType.GLOBAL))
        assertTrue(types.contains(LeagueType.PRIVATE))
        assertTrue(types.contains(LeagueType.REGIONAL))
        assertTrue(types.contains(LeagueType.MONTHLY))
    }

    // ==================== IS PRIVATE TESTS ====================

    @Test
    fun `isPrivate is true for PRIVATE type`() {
        val league = createLeague(type = LeagueType.PRIVATE)
        assertTrue(league.isPrivate)
    }

    @Test
    fun `isPrivate is false for GLOBAL type`() {
        val league = createLeague(type = LeagueType.GLOBAL)
        assertFalse(league.isPrivate)
    }

    @Test
    fun `isPrivate is false for REGIONAL type`() {
        val league = createLeague(type = LeagueType.REGIONAL)
        assertFalse(league.isPrivate)
    }

    @Test
    fun `isPrivate is false for MONTHLY type`() {
        val league = createLeague(type = LeagueType.MONTHLY)
        assertFalse(league.isPrivate)
    }

    // ==================== IS GLOBAL TESTS ====================

    @Test
    fun `isGlobal is true for GLOBAL type`() {
        val league = createLeague(type = LeagueType.GLOBAL)
        assertTrue(league.isGlobal)
    }

    @Test
    fun `isGlobal is false for PRIVATE type`() {
        val league = createLeague(type = LeagueType.PRIVATE)
        assertFalse(league.isGlobal)
    }

    @Test
    fun `isGlobal is false for REGIONAL type`() {
        val league = createLeague(type = LeagueType.REGIONAL)
        assertFalse(league.isGlobal)
    }

    @Test
    fun `isGlobal is false for MONTHLY type`() {
        val league = createLeague(type = LeagueType.MONTHLY)
        assertFalse(league.isGlobal)
    }

    // ==================== GLOBAL LEAGUE PROPERTIES ====================

    @Test
    fun `global league has no code`() {
        val league = createGlobalLeague()
        assertNull(league.code)
    }

    @Test
    fun `global league has no owner`() {
        val league = createGlobalLeague()
        assertNull(league.ownerId)
    }

    @Test
    fun `global league name includes season`() {
        val league = createGlobalLeague(season = 2026)
        assertTrue(league.name.contains("2026"))
    }

    // ==================== PRIVATE LEAGUE PROPERTIES ====================

    @Test
    fun `private league has code`() {
        val league = createLeague(
            type = LeagueType.PRIVATE,
            code = "ABC123"
        )
        assertNotNull(league.code)
        assertEquals("ABC123", league.code)
    }

    @Test
    fun `private league has owner`() {
        val league = createLeague(
            type = LeagueType.PRIVATE,
            ownerId = "owner-123"
        )
        assertNotNull(league.ownerId)
        assertEquals("owner-123", league.ownerId)
    }

    // ==================== REGIONAL LEAGUE PROPERTIES ====================

    @Test
    fun `regional league has region`() {
        val league = createLeague(
            type = LeagueType.REGIONAL,
            region = "Lisboa"
        )
        assertNotNull(league.region)
        assertEquals("Lisboa", league.region)
    }

    // ==================== DEFAULT VALUES TESTS ====================

    @Test
    fun `default member count is 0`() {
        val league = League(
            id = "test-id",
            name = "Test League",
            type = LeagueType.PRIVATE,
            code = "ABC123",
            ownerId = "owner-1",
            region = null
        )
        assertEquals(0, league.memberCount)
    }

    @Test
    fun `default season is current season`() {
        val league = League(
            id = "test-id",
            name = "Test League",
            type = LeagueType.PRIVATE,
            code = "ABC123",
            ownerId = "owner-1",
            region = null
        )
        assertEquals(SeasonConfig.CURRENT_SEASON, league.season)
    }

    // ==================== MEMBER COUNT TESTS ====================

    @Test
    fun `league tracks member count`() {
        val league = createLeague(memberCount = 100)
        assertEquals(100, league.memberCount)
    }

    @Test
    fun `league can have many members`() {
        val league = createLeague(memberCount = 10000)
        assertEquals(10000, league.memberCount)
    }

    // ==================== SEASON TESTS ====================

    @Test
    fun `league is associated with a season`() {
        val league = createLeague(season = 2026)
        assertEquals(2026, league.season)
    }

    @Test
    fun `league can be for different seasons`() {
        val league2026 = createLeague(season = 2026)
        val league2027 = createLeague(season = 2027)

        assertEquals(2026, league2026.season)
        assertEquals(2027, league2027.season)
    }

    // ==================== COPY TESTS ====================

    @Test
    fun `league can be copied with modified member count`() {
        val original = createLeague(memberCount = 10)
        val updated = original.copy(memberCount = 11)

        assertEquals(10, original.memberCount)
        assertEquals(11, updated.memberCount)
    }

    @Test
    fun `league can be copied with modified name`() {
        val original = createLeague(name = "Original Name")
        val updated = original.copy(name = "New Name")

        assertEquals("Original Name", original.name)
        assertEquals("New Name", updated.name)
    }

    // Helper functions
    private fun createLeague(
        id: String = "test-league-id",
        name: String = "Test League",
        type: LeagueType = LeagueType.PRIVATE,
        code: String? = null,
        ownerId: String? = null,
        region: String? = null,
        memberCount: Int = 0,
        season: Int = SeasonConfig.CURRENT_SEASON
    ) = League(
        id = id,
        name = name,
        type = type,
        code = code,
        ownerId = ownerId,
        region = region,
        memberCount = memberCount,
        createdAt = System.currentTimeMillis(),
        season = season
    )

    private fun createGlobalLeague(season: Int = SeasonConfig.CURRENT_SEASON) = League(
        id = "liga-portugal-global-$season",
        name = "Liga Portugal $season",
        type = LeagueType.GLOBAL,
        code = null,
        ownerId = null,
        region = null,
        memberCount = 0,
        createdAt = System.currentTimeMillis(),
        season = season
    )
}
