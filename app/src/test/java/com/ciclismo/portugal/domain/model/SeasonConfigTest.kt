package com.ciclismo.portugal.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SeasonConfig validation and utility functions.
 */
class SeasonConfigTest {

    @Test
    fun `current season is 2026`() {
        assertEquals(2026, SeasonConfig.CURRENT_SEASON)
    }

    @Test
    fun `first season is 2026`() {
        assertEquals(2026, SeasonConfig.FIRST_SEASON)
    }

    @Test
    fun `current season is valid`() {
        assertTrue(SeasonConfig.isValidSeason(SeasonConfig.CURRENT_SEASON))
    }

    @Test
    fun `first season is valid`() {
        assertTrue(SeasonConfig.isValidSeason(SeasonConfig.FIRST_SEASON))
    }

    @Test
    fun `season before first is invalid`() {
        assertFalse(SeasonConfig.isValidSeason(SeasonConfig.FIRST_SEASON - 1))
        assertFalse(SeasonConfig.isValidSeason(2025))
        assertFalse(SeasonConfig.isValidSeason(2020))
    }

    @Test
    fun `season after current is invalid`() {
        assertFalse(SeasonConfig.isValidSeason(SeasonConfig.CURRENT_SEASON + 1))
        assertFalse(SeasonConfig.isValidSeason(2030))
    }

    @Test
    fun `very old season is invalid`() {
        assertFalse(SeasonConfig.isValidSeason(1990))
        assertFalse(SeasonConfig.isValidSeason(2000))
    }

    @Test
    fun `negative season is invalid`() {
        assertFalse(SeasonConfig.isValidSeason(-1))
        assertFalse(SeasonConfig.isValidSeason(-2026))
    }

    @Test
    fun `zero season is invalid`() {
        assertFalse(SeasonConfig.isValidSeason(0))
    }

    @Test
    fun `getAllSeasons returns at least current season`() {
        val seasons = SeasonConfig.getAllSeasons()
        assertTrue(seasons.isNotEmpty())
        assertTrue(seasons.contains(SeasonConfig.CURRENT_SEASON))
    }

    @Test
    fun `getAllSeasons is in descending order`() {
        val seasons = SeasonConfig.getAllSeasons()

        for (i in 1 until seasons.size) {
            assertTrue(
                "Seasons should be in descending order",
                seasons[i] < seasons[i-1]
            )
        }
    }

    @Test
    fun `getAllSeasons includes first and current season`() {
        val seasons = SeasonConfig.getAllSeasons()
        assertTrue(seasons.contains(SeasonConfig.FIRST_SEASON))
        assertTrue(seasons.contains(SeasonConfig.CURRENT_SEASON))
    }

    @Test
    fun `getAllSeasons first element is current season`() {
        val seasons = SeasonConfig.getAllSeasons()
        assertEquals(SeasonConfig.CURRENT_SEASON, seasons.first())
    }

    @Test
    fun `getAllSeasons last element is first season`() {
        val seasons = SeasonConfig.getAllSeasons()
        assertEquals(SeasonConfig.FIRST_SEASON, seasons.last())
    }

    @Test
    fun `season within range is valid`() {
        // If CURRENT_SEASON and FIRST_SEASON are the same (2026),
        // only that season should be valid
        if (SeasonConfig.CURRENT_SEASON == SeasonConfig.FIRST_SEASON) {
            assertTrue(SeasonConfig.isValidSeason(SeasonConfig.CURRENT_SEASON))
        } else {
            // Test middle season if range exists
            val middleSeason = (SeasonConfig.FIRST_SEASON + SeasonConfig.CURRENT_SEASON) / 2
            assertTrue(SeasonConfig.isValidSeason(middleSeason))
        }
    }
}
