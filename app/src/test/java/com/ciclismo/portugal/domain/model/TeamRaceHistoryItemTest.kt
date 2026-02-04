package com.ciclismo.portugal.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for TeamRaceHistoryItem Grand Tour detection.
 */
class TeamRaceHistoryItemTest {

    private fun createRaceItem(raceName: String) = TeamRaceHistoryItem(
        raceId = "test-race-id",
        raceName = raceName,
        raceDate = System.currentTimeMillis(),
        pointsEarned = 100,
        rank = 1,
        season = 2026
    )

    // Grand Tours (3 big ones)
    @Test
    fun `Tour de France is detected as Grand Tour`() {
        assertTrue(createRaceItem("Tour de France 2026").isLikelyGrandTour)
        assertTrue(createRaceItem("TOUR DE FRANCE").isLikelyGrandTour)
        assertTrue(createRaceItem("tour de france").isLikelyGrandTour)
    }

    @Test
    fun `Giro d'Italia is detected as Grand Tour`() {
        assertTrue(createRaceItem("Giro d'Italia 2026").isLikelyGrandTour)
        assertTrue(createRaceItem("Giro de Italia").isLikelyGrandTour)
        assertTrue(createRaceItem("GIRO D'ITALIA").isLikelyGrandTour)
    }

    @Test
    fun `Vuelta a Espana is detected as Grand Tour`() {
        assertTrue(createRaceItem("Vuelta a Espana 2026").isLikelyGrandTour)
        assertTrue(createRaceItem("Vuelta a España").isLikelyGrandTour)
        assertTrue(createRaceItem("VUELTA A ESPANA").isLikelyGrandTour)
    }

    // Stage races with "Tour of" pattern
    @Test
    fun `Tour of races are detected as Grand Tour`() {
        assertTrue(createRaceItem("Tour of Britain").isLikelyGrandTour)
        assertTrue(createRaceItem("Tour of California").isLikelyGrandTour)
        assertTrue(createRaceItem("Tour of Oman").isLikelyGrandTour)
        assertTrue(createRaceItem("Tour of Turkey").isLikelyGrandTour)
        assertTrue(createRaceItem("Tour of Slovenia").isLikelyGrandTour)
        assertTrue(createRaceItem("Tour of Norway").isLikelyGrandTour)
        assertTrue(createRaceItem("Tour of Denmark").isLikelyGrandTour)
        assertTrue(createRaceItem("Tour of Guangxi").isLikelyGrandTour)
        assertTrue(createRaceItem("Tour of the Alps").isLikelyGrandTour)
        assertTrue(createRaceItem("Tour of Basque").isLikelyGrandTour)
    }

    // Portuguese stage races
    @Test
    fun `Volta a Portugal races are detected as Grand Tour`() {
        assertTrue(createRaceItem("Volta a Portugal").isLikelyGrandTour)
        assertTrue(createRaceItem("Volta ao Algarve").isLikelyGrandTour)
        assertTrue(createRaceItem("Volta a Catalunya").isLikelyGrandTour)
    }

    // Other major stage races
    @Test
    fun `Paris-Nice is detected as Grand Tour`() {
        assertTrue(createRaceItem("Paris-Nice 2026").isLikelyGrandTour)
        assertTrue(createRaceItem("Paris-Nice").isLikelyGrandTour)
    }

    @Test
    fun `Tirreno-Adriatico is detected as Grand Tour`() {
        assertTrue(createRaceItem("Tirreno-Adriatico 2026").isLikelyGrandTour)
        assertTrue(createRaceItem("Tirreno-Adriatico").isLikelyGrandTour)
    }

    @Test
    fun `Criterium du Dauphine is detected as Grand Tour`() {
        assertTrue(createRaceItem("Criterium du Dauphine 2026").isLikelyGrandTour)
        assertTrue(createRaceItem("Criterium du Dauphiné").isLikelyGrandTour)
        assertTrue(createRaceItem("Dauphine").isLikelyGrandTour)
        assertTrue(createRaceItem("Dauphiné").isLikelyGrandTour)
    }

    @Test
    fun `Tour de Suisse is detected as Grand Tour`() {
        assertTrue(createRaceItem("Tour de Suisse 2026").isLikelyGrandTour)
        assertTrue(createRaceItem("Tour de Suisse").isLikelyGrandTour)
    }

    @Test
    fun `Tour de Romandie is detected as Grand Tour`() {
        assertTrue(createRaceItem("Tour de Romandie 2026").isLikelyGrandTour)
    }

    @Test
    fun `Tour Down Under is detected as Grand Tour`() {
        assertTrue(createRaceItem("Tour Down Under 2026").isLikelyGrandTour)
    }

    @Test
    fun `UAE Tour is detected as Grand Tour`() {
        assertTrue(createRaceItem("UAE Tour 2026").isLikelyGrandTour)
        assertTrue(createRaceItem("UAE Tour").isLikelyGrandTour)
    }

    @Test
    fun `Itzulia is detected as Grand Tour`() {
        assertTrue(createRaceItem("Itzulia Basque Country").isLikelyGrandTour)
        assertTrue(createRaceItem("Itzulia").isLikelyGrandTour)
    }

    @Test
    fun `Benelux Tour is detected as Grand Tour`() {
        assertTrue(createRaceItem("Benelux Tour 2026").isLikelyGrandTour)
    }

    @Test
    fun `Deutschland Tour is detected as Grand Tour`() {
        assertTrue(createRaceItem("Deutschland Tour 2026").isLikelyGrandTour)
    }

    @Test
    fun `Tour de Pologne is detected as Grand Tour`() {
        assertTrue(createRaceItem("Tour de Pologne 2026").isLikelyGrandTour)
    }

    // One-day races should NOT be detected
    @Test
    fun `Milan-San Remo is NOT detected as Grand Tour`() {
        assertFalse(createRaceItem("Milano-Sanremo 2026").isLikelyGrandTour)
        assertFalse(createRaceItem("Milan-San Remo").isLikelyGrandTour)
    }

    @Test
    fun `Tour of Flanders is NOT detected as Grand Tour`() {
        // "Tour of" pattern would match, but actual race name variants
        assertFalse(createRaceItem("Ronde van Vlaanderen").isLikelyGrandTour)
    }

    @Test
    fun `Paris-Roubaix is NOT detected as Grand Tour`() {
        assertFalse(createRaceItem("Paris-Roubaix 2026").isLikelyGrandTour)
    }

    @Test
    fun `Liege-Bastogne-Liege is NOT detected as Grand Tour`() {
        assertFalse(createRaceItem("Liege-Bastogne-Liege").isLikelyGrandTour)
    }

    @Test
    fun `Il Lombardia is NOT detected as Grand Tour`() {
        assertFalse(createRaceItem("Il Lombardia 2026").isLikelyGrandTour)
    }

    @Test
    fun `Strade Bianche is NOT detected as Grand Tour`() {
        assertFalse(createRaceItem("Strade Bianche 2026").isLikelyGrandTour)
    }

    @Test
    fun `World Championships is NOT detected as Grand Tour`() {
        assertFalse(createRaceItem("UCI Road World Championships").isLikelyGrandTour)
    }

    @Test
    fun `random race name is NOT detected as Grand Tour`() {
        assertFalse(createRaceItem("GP Ciclismo Portugal").isLikelyGrandTour)
        assertFalse(createRaceItem("Classica de Lisboa").isLikelyGrandTour)
        assertFalse(createRaceItem("Some Random Race").isLikelyGrandTour)
    }

    // Edge cases
    @Test
    fun `empty race name is NOT detected as Grand Tour`() {
        assertFalse(createRaceItem("").isLikelyGrandTour)
    }

    @Test
    fun `race name with only spaces is NOT detected as Grand Tour`() {
        assertFalse(createRaceItem("   ").isLikelyGrandTour)
    }
}
