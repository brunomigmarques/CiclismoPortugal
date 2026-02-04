package com.ciclismo.portugal.presentation.fantasy.wizard

import com.ciclismo.portugal.domain.model.CyclistCategory

/**
 * Represents each step in the team creation wizard.
 * Each step guides the user to select a specific number of cyclists from one category.
 */
sealed class WizardStep(
    val stepNumber: Int,
    val totalSteps: Int,
    val category: CyclistCategory?,
    val requiredCount: Int,
    val titlePt: String,
    val descriptionPt: String
) {
    data object SelectGC : WizardStep(
        stepNumber = 1,
        totalSteps = 7,
        category = CyclistCategory.GC,
        requiredCount = 3,
        titlePt = "Escolhe 3 Lideres (GC)",
        descriptionPt = "Ciclistas candidatos a classificacao geral"
    )

    data object SelectClimbers : WizardStep(
        stepNumber = 2,
        totalSteps = 7,
        category = CyclistCategory.CLIMBER,
        requiredCount = 3,
        titlePt = "Escolhe 3 Escaladores",
        descriptionPt = "Especialistas em montanha"
    )

    data object SelectSprinters : WizardStep(
        stepNumber = 3,
        totalSteps = 7,
        category = CyclistCategory.SPRINT,
        requiredCount = 3,
        titlePt = "Escolhe 3 Sprinters",
        descriptionPt = "Velocistas para chegadas ao sprint"
    )

    data object SelectTT : WizardStep(
        stepNumber = 4,
        totalSteps = 7,
        category = CyclistCategory.TT,
        requiredCount = 2,
        titlePt = "Escolhe 2 Contra-Relogistas",
        descriptionPt = "Especialistas em contra-relogio"
    )

    data object SelectPunchers : WizardStep(
        stepNumber = 5,
        totalSteps = 7,
        category = CyclistCategory.HILLS,
        requiredCount = 2,
        titlePt = "Escolhe 2 Punchers",
        descriptionPt = "Atacantes em terreno ondulado"
    )

    data object SelectOneDay : WizardStep(
        stepNumber = 6,
        totalSteps = 7,
        category = CyclistCategory.ONEDAY,
        requiredCount = 2,
        titlePt = "Escolhe 2 Classicomanos",
        descriptionPt = "Especialistas em classicas de um dia"
    )

    data object Review : WizardStep(
        stepNumber = 7,
        totalSteps = 7,
        category = null,
        requiredCount = 0,
        titlePt = "Confirmar Equipa",
        descriptionPt = "Reve a tua selecao de 15 ciclistas"
    )

    companion object {
        val allSteps = listOf(
            SelectGC, SelectClimbers, SelectSprinters,
            SelectTT, SelectPunchers, SelectOneDay, Review
        )

        val selectionSteps = listOf(
            SelectGC, SelectClimbers, SelectSprinters,
            SelectTT, SelectPunchers, SelectOneDay
        )

        fun fromStepNumber(number: Int): WizardStep =
            allSteps.find { it.stepNumber == number } ?: SelectGC
    }
}
