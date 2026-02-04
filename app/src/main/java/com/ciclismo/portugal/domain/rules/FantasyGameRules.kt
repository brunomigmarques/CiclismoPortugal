package com.ciclismo.portugal.domain.rules

import com.ciclismo.portugal.domain.model.CyclistCategory

/**
 * Regras oficiais do Jogo das Apostas - Fantasy Ciclismo Portugal
 */
object FantasyGameRules {

    // ========== EQUIPA ==========

    /** Orçamento inicial em milhões */
    const val INITIAL_BUDGET = 100.0

    /** Tamanho total da equipa */
    const val TEAM_SIZE = 15

    /** Ciclistas ativos (ganham pontos) */
    const val ACTIVE_CYCLISTS = 8

    /** Ciclistas no banco (suplentes) */
    const val BENCH_CYCLISTS = 7

    /** Máximo de ciclistas da mesma equipa profissional */
    const val MAX_FROM_SAME_PRO_TEAM = 3

    // ========== COMPOSIÇÃO OBRIGATÓRIA POR CATEGORIA ==========

    /**
     * Regras de composição da equipa por categoria
     * Total: 3 GC + 3 Climber + 3 Sprint + 2 TT + 2 Hills + 2 Oneday = 15
     */
    val CATEGORY_REQUIREMENTS: Map<CyclistCategory, Int> = mapOf(
        CyclistCategory.GC to 3,          // General Classification
        CyclistCategory.CLIMBER to 3,     // Escaladores
        CyclistCategory.SPRINT to 3,      // Sprinters
        CyclistCategory.TT to 2,          // Contra-Relógio
        CyclistCategory.HILLS to 2,       // Punchers
        CyclistCategory.ONEDAY to 2       // Clássicas
    )

    // ========== TRANSFERÊNCIAS ==========

    /** Transferências gratuitas por mês */
    const val FREE_TRANSFERS_PER_MONTH = 2

    /** Máximo de transferências acumuladas */
    const val MAX_ACCUMULATED_TRANSFERS = 5

    /** Penalização por transferência extra (pontos) */
    const val TRANSFER_PENALTY_POINTS = 4

    // ========== CAPITÃO ==========

    /** Multiplicador de pontos do capitão */
    const val CAPTAIN_MULTIPLIER = 2.0

    /** Multiplicador do Triple Captain (wildcard) */
    const val TRIPLE_CAPTAIN_MULTIPLIER = 3.0

    // ========== PONTUAÇÃO - CORRIDAS DE 1 DIA ==========

    val ONE_DAY_RACE_POINTS = mapOf(
        1 to 100,
        2 to 80,
        3 to 70,
        4 to 60,
        5 to 50,
        6 to 45,
        7 to 40,
        8 to 35,
        9 to 30,
        10 to 25,
        11 to 20,
        12 to 18,
        13 to 16,
        14 to 14,
        15 to 12,
        16 to 10,
        17 to 8,
        18 to 6,
        19 to 4,
        20 to 2
    )

    // ========== PONTUAÇÃO - ETAPAS ==========

    val STAGE_RACE_POINTS = mapOf(
        1 to 50,
        2 to 40,
        3 to 35,
        4 to 30,
        5 to 25,
        6 to 20,
        7 to 18,
        8 to 16,
        9 to 14,
        10 to 12,
        11 to 10,
        12 to 8,
        13 to 6,
        14 to 4,
        15 to 2
    )

    // ========== PONTUAÇÃO - CAMISOLAS (por dia) ==========

    /** Pontos por usar camisola de líder (GC) */
    const val JERSEY_GC_POINTS = 10

    /** Pontos por usar camisola da montanha */
    const val JERSEY_MOUNTAIN_POINTS = 5

    /** Pontos por usar camisola dos pontos */
    const val JERSEY_POINTS_POINTS = 5

    /** Pontos por usar camisola de jovem */
    const val JERSEY_YOUNG_POINTS = 3

    // ========== PONTUAÇÃO - CLASSIFICAÇÃO FINAL GRANDES VOLTAS ==========

    val GRAND_TOUR_FINAL_GC_POINTS = mapOf(
        1 to 200,
        2 to 150,
        3 to 100,
        4 to 80,
        5 to 60,
        6 to 50,
        7 to 40,
        8 to 30,
        9 to 20,
        10 to 10
    )

    // ========== PONTOS ESPECIAIS ==========

    /** Pontos por abandono (DNF) - sem penalização, apenas 0 pontos */
    const val DNF_POINTS = 0

    /** Pontos por não partir (DNS) - sem penalização, apenas 0 pontos */
    const val DNS_POINTS = 0

    /** Pontos perdidos por desclassificação (DSQ) */
    const val DSQ_PENALTY = -20

    // ========== MÉTODOS DE VALIDAÇÃO ==========

    /**
     * Verifica se um ciclista pode ser adicionado à equipa
     */
    fun canAddCyclist(
        cyclistCategory: CyclistCategory,
        cyclistPrice: Double,
        cyclistTeamId: String,
        currentBudget: Double,
        currentTeamSize: Int,
        categoryCount: Map<CyclistCategory, Int>,
        proTeamCount: Map<String, Int>
    ): CyclistEligibility {
        // Verificar tamanho da equipa
        if (currentTeamSize >= TEAM_SIZE) {
            return CyclistEligibility.TeamFull
        }

        // Verificar orçamento
        if (cyclistPrice > currentBudget) {
            return CyclistEligibility.InsufficientBudget(currentBudget, cyclistPrice)
        }

        // Verificar limite por equipa profissional
        val currentFromTeam = proTeamCount[cyclistTeamId] ?: 0
        if (currentFromTeam >= MAX_FROM_SAME_PRO_TEAM) {
            return CyclistEligibility.TooManyFromSameTeam(cyclistTeamId, MAX_FROM_SAME_PRO_TEAM)
        }

        // Verificar limite máximo por categoria
        val maxInCategory = CATEGORY_REQUIREMENTS[cyclistCategory] ?: 0
        val currentInCategory = categoryCount[cyclistCategory] ?: 0
        if (currentInCategory >= maxInCategory) {
            return CyclistEligibility.CategoryFull(cyclistCategory, maxInCategory)
        }

        return CyclistEligibility.Eligible
    }

    /**
     * Verifica se a composição da equipa é válida para competir
     */
    fun validateTeamComposition(categoryCount: Map<CyclistCategory, Int>): TeamValidation {
        val missingCategories = mutableListOf<CategoryDeficit>()

        CATEGORY_REQUIREMENTS.forEach { (category, required) ->
            val count = categoryCount[category] ?: 0
            if (count < required) {
                missingCategories.add(
                    CategoryDeficit(category, required, count)
                )
            }
        }

        return if (missingCategories.isEmpty()) {
            TeamValidation.Valid
        } else {
            TeamValidation.Invalid(missingCategories)
        }
    }

    /**
     * Calcula pontos por posição numa corrida
     */
    fun getPointsForPosition(position: Int, isOneDay: Boolean): Int {
        return if (isOneDay) {
            ONE_DAY_RACE_POINTS[position] ?: 0
        } else {
            STAGE_RACE_POINTS[position] ?: 0
        }
    }
}

/**
 * Resultado da verificação de elegibilidade de um ciclista
 */
sealed class CyclistEligibility {
    object Eligible : CyclistEligibility()
    object TeamFull : CyclistEligibility()
    data class InsufficientBudget(val available: Double, val required: Double) : CyclistEligibility()
    data class TooManyFromSameTeam(val teamId: String, val maxAllowed: Int) : CyclistEligibility()
    data class CategoryFull(val category: CyclistCategory, val maxAllowed: Int) : CyclistEligibility()
    object AlreadyInTeam : CyclistEligibility()
}

/**
 * Resultado da validação da composição da equipa
 */
sealed class TeamValidation {
    object Valid : TeamValidation()
    data class Invalid(val deficits: List<CategoryDeficit>) : TeamValidation()
}

/**
 * Défice numa categoria específica
 */
data class CategoryDeficit(
    val category: CyclistCategory,
    val required: Int,
    val current: Int
) {
    val missing: Int get() = required - current
}
