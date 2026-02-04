package com.ciclismo.portugal.domain.bots

import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Gerador de equipas bot para criar competicao inicial.
 * Gera 236 equipas ficticias com nomes portugueses e selecao de ciclistas variada.
 */
@Singleton
class BotTeamGenerator @Inject constructor() {

    companion object {
        const val TARGET_BOT_TEAMS = 236
        const val TEAM_SIZE = 15
        const val INITIAL_BUDGET = 100.0
        const val MAX_FROM_SAME_TEAM = 3

        // Distribuicao de estrategias (soma = 100%)
        const val BALANCED_PERCENT = 40
        const val CLIMBER_HEAVY_PERCENT = 20
        const val SPRINTER_HEAVY_PERCENT = 15
        const val GC_FOCUSED_PERCENT = 15
        const val VALUE_PICKS_PERCENT = 10
    }

    // Nomes de equipas portuguesas ficticias
    private val teamPrefixes = listOf(
        "FC", "SC", "Ciclismo", "Dragoes", "Aguias", "Leoes",
        "Unidos", "Sporting", "Racing", "Cycling", "Team", "Elite",
        "Pro", "Velo", "Pedal", "MTB", "Road", "Sprint", "Climb",
        "Victory", "Furia", "Poder", "Velocidade", "Montanha"
    )

    private val teamSuffixes = listOf(
        "Portugal", "Lisboa", "Porto", "Braga", "Coimbra", "Faro",
        "Aveiro", "Setubal", "Leiria", "Viseu", "Evora", "Santarem",
        "Cascais", "Sintra", "Almada", "Amadora", "Funchal", "Ponta Delgada",
        "Algarve", "Minho", "Douro", "Alentejo", "Beira", "Tras-os-Montes",
        "Racing", "Cycling", "Team", "Pro", "Elite", "Club",
        "Norte", "Sul", "Centro", "Litoral", "Serra"
    )

    private val funnyNames = listOf(
        "Ciclismo Portugal FC", "Dragoes do Asfalto", "Aguias da Estrada",
        "Leoes da Serra", "Unidos pelo Pedal", "Furia das Duas Rodas",
        "Raios de Sol", "Vento nas Costas", "Subida Infinita",
        "Descida Louca", "Pelotao Fantasma", "Grupetto Glorioso",
        "Sprint Final", "Contra-Relogio Masters", "Montanha Sagrada",
        "Vale dos Ciclistas", "Serra da Estrela Cycling", "Algarve Racers",
        "Douro Valley Team", "Minho Power", "Lisboa Night Riders",
        "Porto Cycling Elite", "Cascais Beach Riders", "Sintra Hills",
        "Peneda-Geres MTB", "Arrabida Climbers", "Comporta Sprinters",
        "Nazare Wave Riders", "Berlengas Island Team", "Madeira Altitude",
        "Acores Atlantic", "Fatima Faithful", "Coimbra Students",
        "Tejo River Riders", "Mondego Cycling", "Guadiana Racers"
    )

    private val usedNames = mutableSetOf<String>()

    /**
     * Gera um nome unico para a equipa bot.
     */
    fun generateTeamName(): String {
        // Primeiro, usar nomes engracados pre-definidos
        for (name in funnyNames.shuffled()) {
            if (name !in usedNames) {
                usedNames.add(name)
                return name
            }
        }

        // Depois, gerar nomes aleatorios
        repeat(100) {
            val prefix = teamPrefixes.random()
            val suffix = teamSuffixes.random()
            val name = "$prefix $suffix"
            if (name !in usedNames) {
                usedNames.add(name)
                return name
            }
        }

        // Fallback com numero aleatorio
        val fallback = "Bot Team ${Random.nextInt(10000)}"
        usedNames.add(fallback)
        return fallback
    }

    /**
     * Seleciona a estrategia para a equipa bot baseada na distribuicao.
     */
    fun selectStrategy(teamIndex: Int): BotStrategy {
        val position = teamIndex % 100
        return when {
            position < BALANCED_PERCENT -> BotStrategy.BALANCED
            position < BALANCED_PERCENT + CLIMBER_HEAVY_PERCENT -> BotStrategy.CLIMBER_HEAVY
            position < BALANCED_PERCENT + CLIMBER_HEAVY_PERCENT + SPRINTER_HEAVY_PERCENT -> BotStrategy.SPRINTER_HEAVY
            position < BALANCED_PERCENT + CLIMBER_HEAVY_PERCENT + SPRINTER_HEAVY_PERCENT + GC_FOCUSED_PERCENT -> BotStrategy.GC_FOCUSED
            else -> BotStrategy.VALUE_PICKS
        }
    }

    /**
     * Seleciona 15 ciclistas para a equipa bot baseado na estrategia.
     * Respeita o budget (100M) e max 3 ciclistas da mesma equipa profissional.
     *
     * @param allCyclists Lista de todos os ciclistas disponiveis
     * @param strategy Estrategia de selecao
     * @return Lista de 15 ciclistas selecionados
     */
    fun selectCyclists(allCyclists: List<Cyclist>, strategy: BotStrategy): List<Cyclist> {
        if (allCyclists.size < TEAM_SIZE) {
            return emptyList()
        }

        val selected = mutableListOf<Cyclist>()
        var remainingBudget = INITIAL_BUDGET
        val teamCounts = mutableMapOf<String, Int>() // teamId -> count

        // Ordenar ciclistas por prioridade baseada na estrategia
        val prioritized = prioritizeCyclists(allCyclists, strategy)

        // Selecionar ciclistas
        for (cyclist in prioritized) {
            if (selected.size >= TEAM_SIZE) break

            // Verificar budget
            if (cyclist.price > remainingBudget) continue

            // Verificar max da mesma equipa
            val currentTeamCount = teamCounts.getOrDefault(cyclist.teamId, 0)
            if (currentTeamCount >= MAX_FROM_SAME_TEAM) continue

            // Adicionar ciclista
            selected.add(cyclist)
            remainingBudget -= cyclist.price
            teamCounts[cyclist.teamId] = currentTeamCount + 1
        }

        // Se nao conseguiu 15, tentar preencher com os mais baratos
        if (selected.size < TEAM_SIZE) {
            val remaining = allCyclists
                .filter { it !in selected }
                .sortedBy { it.price }

            for (cyclist in remaining) {
                if (selected.size >= TEAM_SIZE) break
                if (cyclist.price > remainingBudget) continue

                val currentTeamCount = teamCounts.getOrDefault(cyclist.teamId, 0)
                if (currentTeamCount >= MAX_FROM_SAME_TEAM) continue

                selected.add(cyclist)
                remainingBudget -= cyclist.price
                teamCounts[cyclist.teamId] = currentTeamCount + 1
            }
        }

        return selected
    }

    /**
     * Prioriza ciclistas baseado na estrategia.
     */
    private fun prioritizeCyclists(cyclists: List<Cyclist>, strategy: BotStrategy): List<Cyclist> {
        return when (strategy) {
            BotStrategy.BALANCED -> {
                // Mix equilibrado de todas as categorias
                val shuffled = cyclists.shuffled()
                val byCategory = shuffled.groupBy { it.category }
                val result = mutableListOf<Cyclist>()

                // Pegar 2-3 de cada categoria
                for (category in CyclistCategory.entries) {
                    val categoryCyclists = byCategory[category] ?: emptyList()
                    result.addAll(categoryCyclists.take(3))
                }

                // Preencher o resto
                result.addAll(shuffled.filter { it !in result })
                result
            }

            BotStrategy.CLIMBER_HEAVY -> {
                // Priorizar escaladores e GC
                cyclists.sortedByDescending { cyclist ->
                    when (cyclist.category) {
                        CyclistCategory.CLIMBER -> 100 + cyclist.totalPoints
                        CyclistCategory.GC -> 80 + cyclist.totalPoints
                        CyclistCategory.HILLS -> 60 + cyclist.totalPoints
                        else -> cyclist.totalPoints
                    }
                }.shuffled().take(30) + cyclists.shuffled()
            }

            BotStrategy.SPRINTER_HEAVY -> {
                // Priorizar sprinters e TT
                cyclists.sortedByDescending { cyclist ->
                    when (cyclist.category) {
                        CyclistCategory.SPRINT -> 100 + cyclist.totalPoints
                        CyclistCategory.TT -> 80 + cyclist.totalPoints
                        CyclistCategory.ONEDAY -> 60 + cyclist.totalPoints
                        else -> cyclist.totalPoints
                    }
                }.shuffled().take(30) + cyclists.shuffled()
            }

            BotStrategy.GC_FOCUSED -> {
                // Priorizar lideres de GC e escaladores de elite
                cyclists.sortedByDescending { cyclist ->
                    when (cyclist.category) {
                        CyclistCategory.GC -> 100 + cyclist.totalPoints
                        CyclistCategory.CLIMBER -> 80 + cyclist.totalPoints
                        CyclistCategory.TT -> 60 + cyclist.totalPoints
                        else -> cyclist.totalPoints
                    }
                }.shuffled().take(30) + cyclists.shuffled()
            }

            BotStrategy.VALUE_PICKS -> {
                // Priorizar ciclistas baratos com bons pontos (value)
                cyclists.sortedByDescending { cyclist ->
                    if (cyclist.price > 0) {
                        (cyclist.totalPoints.toDouble() / cyclist.price) * 10
                    } else {
                        cyclist.totalPoints.toDouble()
                    }
                }.filter { it.price <= 8.0 }.shuffled().take(50) +
                cyclists.filter { it.price <= 5.0 }.shuffled()
            }
        }
    }

    /**
     * Seleciona 8 ciclistas ativos dos 15 (random mas com criterio).
     * Geralmente prefere os de maior preco/pontos.
     */
    fun selectActiveCyclists(cyclists: List<Cyclist>): List<String> {
        if (cyclists.size < 8) return cyclists.map { it.id }

        // Ordenar por pontos/preco e selecionar os top 6-7
        val sorted = cyclists.sortedByDescending { it.totalPoints + (it.price * 10).toInt() }
        val top = sorted.take(6).map { it.id }
        val remaining = sorted.drop(6).shuffled().take(2).map { it.id }

        return top + remaining
    }

    /**
     * Seleciona o capitao (geralmente o de mais pontos).
     */
    fun selectCaptain(cyclists: List<Cyclist>): String? {
        return cyclists.maxByOrNull { it.totalPoints + (it.price * 5).toInt() }?.id
    }

    /**
     * Reseta os nomes usados (para nova geracao completa).
     */
    fun resetUsedNames() {
        usedNames.clear()
    }
}

/**
 * Estrategias de selecao de ciclistas para equipas bot.
 */
enum class BotStrategy {
    BALANCED,       // Mix equilibrado de todas as categorias
    CLIMBER_HEAVY,  // Foco em escaladores
    SPRINTER_HEAVY, // Foco em sprinters
    GC_FOCUSED,     // Foco em lideres de GC
    VALUE_PICKS     // Foco em ciclistas baratos com boa relacao pontos/preco
}
