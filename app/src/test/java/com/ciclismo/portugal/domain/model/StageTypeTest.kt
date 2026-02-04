package com.ciclismo.portugal.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for StageType enum and related functionality.
 */
class StageTypeTest {

    @Test
    fun `all stage types have valid multipliers`() {
        StageType.entries.forEach { stageType ->
            assertTrue(
                "Stage type ${stageType.name} should have positive multiplier",
                stageType.pointsMultiplier > 0
            )
        }
    }

    @Test
    fun `prologue has reduced multiplier`() {
        assertEquals(0.5, StageType.PROLOGUE.pointsMultiplier, 0.01)
    }

    @Test
    fun `mountain and ITT have bonus multiplier`() {
        assertEquals(1.2, StageType.MOUNTAIN.pointsMultiplier, 0.01)
        assertEquals(1.2, StageType.ITT.pointsMultiplier, 0.01)
    }

    @Test
    fun `flat hilly and TTT have standard multiplier`() {
        assertEquals(1.0, StageType.FLAT.pointsMultiplier, 0.01)
        assertEquals(1.0, StageType.HILLY.pointsMultiplier, 0.01)
        assertEquals(1.0, StageType.TTT.pointsMultiplier, 0.01)
    }

    @Test
    fun `fromString parses enum names correctly`() {
        assertEquals(StageType.PROLOGUE, StageType.fromString("PROLOGUE"))
        assertEquals(StageType.FLAT, StageType.fromString("FLAT"))
        assertEquals(StageType.HILLY, StageType.fromString("HILLY"))
        assertEquals(StageType.MOUNTAIN, StageType.fromString("MOUNTAIN"))
        assertEquals(StageType.ITT, StageType.fromString("ITT"))
        assertEquals(StageType.TTT, StageType.fromString("TTT"))
    }

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(StageType.MOUNTAIN, StageType.fromString("mountain"))
        assertEquals(StageType.MOUNTAIN, StageType.fromString("Mountain"))
        assertEquals(StageType.MOUNTAIN, StageType.fromString("MOUNTAIN"))
    }

    @Test
    fun `fromString parses Portuguese names`() {
        assertEquals(StageType.PROLOGUE, StageType.fromString("Prologo"))
        assertEquals(StageType.FLAT, StageType.fromString("Plana"))
        assertEquals(StageType.HILLY, StageType.fromString("Ondulada"))
        assertEquals(StageType.MOUNTAIN, StageType.fromString("Montanha"))
        assertEquals(StageType.ITT, StageType.fromString("Contra-Relogio Individual"))
        assertEquals(StageType.TTT, StageType.fromString("Contra-Relogio por Equipas"))
    }

    @Test
    fun `fromString returns FLAT for unknown values`() {
        assertEquals(StageType.FLAT, StageType.fromString("unknown"))
        assertEquals(StageType.FLAT, StageType.fromString(""))
        assertEquals(StageType.FLAT, StageType.fromString("invalid_type"))
    }

    @Test
    fun `all stage types have emoji`() {
        StageType.entries.forEach { stageType ->
            assertTrue(
                "Stage type ${stageType.name} should have non-empty emoji",
                stageType.emoji.isNotBlank()
            )
        }
    }

    @Test
    fun `all stage types have Portuguese display name`() {
        StageType.entries.forEach { stageType ->
            assertTrue(
                "Stage type ${stageType.name} should have non-empty Portuguese name",
                stageType.displayNamePt.isNotBlank()
            )
        }
    }
}
