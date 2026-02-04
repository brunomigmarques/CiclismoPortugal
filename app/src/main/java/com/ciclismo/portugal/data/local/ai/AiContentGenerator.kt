package com.ciclismo.portugal.data.local.ai

import android.util.Log
import com.ciclismo.portugal.domain.model.AiContentType
import com.ciclismo.portugal.domain.model.AiGeneratedContent
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceInsight
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.model.TopCandidate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates AI content for the Fantasy hub.
 * Creates top candidate recommendations and race insights.
 */
@Singleton
class AiContentGenerator @Inject constructor(
    private val aiService: AiService
) {
    companion object {
        private const val TAG = "AiContentGenerator"
    }

    private val gson = Gson()

    /**
     * Generate top 3 cyclist candidates for the next race.
     */
    suspend fun generateTopCandidates(
        race: Race,
        stage: Stage? = null,
        availableCyclists: List<Cyclist>
    ): Result<List<TopCandidate>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating top candidates for ${race.name}")

            // Filter and sort cyclists by form and points
            val topCyclists = availableCyclists
                .filter { !it.isDisabled }
                .sortedByDescending { it.totalPoints + (it.form * 10).toInt() }
                .take(20)

            val cyclistList = topCyclists.joinToString("\n") {
                "- ${it.fullName} (${it.teamName}) | ${it.category.name} | ${it.displayPrice} | ${it.totalPoints} pts | Forma: ${it.form}"
            }

            val stageInfo = stage?.let {
                """
                Etapa ${it.stageNumber}: ${it.stageType.displayNamePt}
                Percurso: ${it.startLocation} -> ${it.finishLocation}
                Distancia: ${it.distanceDisplay}
                """
            } ?: "Corrida de um dia"

            val prompt = """
                Es um analista de Fantasy Ciclismo. Analisa a proxima etapa e recomenda os TOP 3 ciclistas COM MAIOR PROBABILIDADE DE VENCER A ETAPA (nao a classificacao geral).

                CORRIDA: ${race.name}
                TIPO: ${race.type.name}
                $stageInfo

                CICLISTAS DISPONIVEIS (ordenados por pontos e forma):
                $cyclistList

                IMPORTANTE: Recomenda ciclistas que podem VENCER esta etapa especifica, NAO lideres da classificacao geral.
                - Etapa de montanha = escaladores puros, nao GC riders
                - Etapa plana = sprinters
                - Etapa de media montanha = puncheurs, atacantes
                - Contra-relogio = especialistas TT

                Responde APENAS com um JSON valido neste formato exato (sem texto adicional):
                [
                  {
                    "rank": 1,
                    "cyclistName": "Nome do Ciclista",
                    "teamName": "Nome da Equipa",
                    "category": "GC/Climber/Sprinter/etc",
                    "reason": "Razao curta (max 50 chars)",
                    "expectedPoints": "25-40"
                  },
                  {
                    "rank": 2,
                    ...
                  },
                  {
                    "rank": 3,
                    ...
                  }
                ]

                Considera:
                - Quem pode VENCER a etapa (nao quem lidera a geral)
                - Tipo de etapa (montanha = escaladores, plana = sprinters)
                - Forma recente do ciclista
                - Historico de vitorias em etapas similares
            """.trimIndent()

            val response = aiService.generateResponse(prompt)

            response.fold(
                onSuccess = { text ->
                    try {
                        // Extract JSON from response
                        val jsonMatch = Regex("\\[.*\\]", RegexOption.DOT_MATCHES_ALL).find(text)
                        val jsonText = jsonMatch?.value ?: text

                        val type = object : TypeToken<List<TopCandidateJson>>() {}.type
                        val candidates: List<TopCandidateJson> = gson.fromJson(jsonText, type)

                        val result = candidates.mapIndexed { index, json ->
                            // Try to find the cyclist in our list to get the ID
                            val cyclist = topCyclists.find {
                                it.fullName.contains(json.cyclistName, ignoreCase = true) ||
                                json.cyclistName.contains(it.lastName, ignoreCase = true)
                            }

                            TopCandidate(
                                rank = json.rank ?: (index + 1),
                                cyclistId = cyclist?.id ?: "",
                                cyclistName = json.cyclistName,
                                teamName = json.teamName ?: cyclist?.teamName ?: "",
                                category = json.category ?: cyclist?.category?.name ?: "",
                                price = cyclist?.price ?: 0.0,
                                reason = json.reason ?: "Boa forma recente",
                                expectedPoints = json.expectedPoints ?: "20-35",
                                photoUrl = cyclist?.photoUrl
                            )
                        }

                        Log.d(TAG, "Generated ${result.size} top candidates")
                        Result.success(result)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse candidates JSON: ${e.message}")
                        // Fallback: generate from top cyclists
                        Result.success(generateFallbackCandidates(topCyclists.take(3), race, stage))
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "AI generation failed: ${error.message}")
                    Log.d(TAG, "Generating fallback candidates from top ${topCyclists.take(3).size} cyclists")
                    val fallbackCandidates = generateFallbackCandidates(topCyclists.take(3), race, stage)
                    Log.d(TAG, "Fallback candidates generated: ${fallbackCandidates.map { it.cyclistName }}")
                    Result.success(fallbackCandidates)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating top candidates: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Generate insights for a race/stage.
     */
    suspend fun generateRaceInsight(
        race: Race,
        stage: Stage? = null
    ): Result<RaceInsight> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating insights for ${race.name}")

            val stageInfo = stage?.let {
                """
                Etapa ${it.stageNumber}: ${it.stageType.displayNamePt}
                Percurso: ${it.startLocation} -> ${it.finishLocation}
                Distancia: ${it.distanceDisplay}
                Elevacao: ${it.elevationGain}m
                """
            } ?: "Corrida classica de um dia"

            val prompt = """
                Es um analista de ciclismo. Analisa esta corrida/etapa e da insights para Fantasy.

                CORRIDA: ${race.name}
                TIPO: ${race.type.name}
                $stageInfo

                Responde APENAS com um JSON valido neste formato exato (sem texto adicional):
                {
                  "keyFactors": ["fator 1", "fator 2", "fator 3"],
                  "recommendation": "Recomendacao curta para Fantasy (max 100 chars)",
                  "difficulty": "Easy/Medium/Hard/Extreme",
                  "bestCategories": ["GC", "Climber", "Sprinter"]
                }

                Considera:
                - Perfil da etapa (montanha, plana, contra-relogio)
                - Que tipos de ciclistas beneficiam
                - Estrategia recomendada
            """.trimIndent()

            val response = aiService.generateResponse(prompt)

            response.fold(
                onSuccess = { text ->
                    try {
                        // Extract JSON from response
                        val jsonMatch = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL).find(text)
                        val jsonText = jsonMatch?.value ?: text

                        val insightJson: RaceInsightJson = gson.fromJson(jsonText, RaceInsightJson::class.java)

                        val insight = RaceInsight(
                            raceId = race.id,
                            raceName = race.name,
                            stageNumber = stage?.stageNumber,
                            stageType = stage?.stageType?.displayNamePt,
                            keyFactors = insightJson.keyFactors ?: listOf("Forma recente importante"),
                            recommendation = insightJson.recommendation ?: "Ativa os ciclistas adequados ao perfil",
                            difficulty = insightJson.difficulty ?: "Medium",
                            bestCategories = insightJson.bestCategories ?: listOf("GC", "Climber")
                        )

                        Log.d(TAG, "Generated race insight")
                        Result.success(insight)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse insight JSON: ${e.message}")
                        Result.success(generateFallbackInsight(race, stage))
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "AI generation failed: ${error.message}")
                    Result.success(generateFallbackInsight(race, stage))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating race insight: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Generate a daily tip.
     */
    suspend fun generateDailyTip(
        nextRace: Race?,
        userTeamSize: Int = 0
    ): Result<AiGeneratedContent> = withContext(Dispatchers.IO) {
        try {
            val context = if (nextRace != null) {
                "Proxima corrida: ${nextRace.name} (${nextRace.type.name})"
            } else {
                "Nenhuma corrida proxima"
            }

            val prompt = """
                Es um assistente de Fantasy Ciclismo. Gera uma dica do dia curta e util.

                CONTEXTO: $context
                UTILIZADOR TEM EQUIPA: ${userTeamSize > 0}

                Gera uma dica pratica em 1-2 frases (max 150 caracteres).
                Foca em estrategia, transferencias, ou preparacao para corridas.

                Responde APENAS com a dica, sem formatacao extra.
            """.trimIndent()

            val response = aiService.generateResponse(prompt)

            response.fold(
                onSuccess = { tip ->
                    val content = AiGeneratedContent(
                        id = UUID.randomUUID().toString(),
                        type = AiContentType.DAILY_TIP,
                        title = "Dica do Dia",
                        content = tip.take(200).trim(),
                        raceId = nextRace?.id,
                        raceName = nextRace?.name
                    )
                    Result.success(content)
                },
                onFailure = { error ->
                    // Fallback tip
                    val content = AiGeneratedContent(
                        id = UUID.randomUUID().toString(),
                        type = AiContentType.DAILY_TIP,
                        title = "Dica do Dia",
                        content = "Verifica a forma dos teus ciclistas antes da proxima corrida!",
                        raceId = nextRace?.id,
                        raceName = nextRace?.name
                    )
                    Result.success(content)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating daily tip: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ========== Fallback Generators ==========

    private fun generateFallbackCandidates(
        cyclists: List<Cyclist>,
        race: Race,
        stage: Stage?
    ): List<TopCandidate> {
        return cyclists.mapIndexed { index, cyclist ->
            val reason = when {
                stage?.stageType?.displayNamePt?.contains("Montanha") == true &&
                cyclist.category.name == "CLIMBER" -> "Escalador em boa forma"
                stage?.stageType?.displayNamePt?.contains("Plana") == true &&
                cyclist.category.name == "SPRINTER" -> "Sprinter para etapa plana"
                cyclist.category.name == "GC" -> "Lider de GC consistente"
                else -> "Boa forma e historico de pontos"
            }

            TopCandidate(
                rank = index + 1,
                cyclistId = cyclist.id,
                cyclistName = cyclist.fullName,
                teamName = cyclist.teamName,
                category = cyclist.category.name,
                price = cyclist.price,
                reason = reason,
                expectedPoints = "${20 + (cyclist.form * 2).toInt()}-${35 + (cyclist.form * 2).toInt()}",
                photoUrl = cyclist.photoUrl
            )
        }
    }

    private fun generateFallbackInsight(race: Race, stage: Stage?): RaceInsight {
        val stageType = stage?.stageType?.displayNamePt ?: "Classica"

        val (difficulty, categories) = when {
            stageType.contains("Montanha") -> "Hard" to listOf("Climber", "GC")
            stageType.contains("Contra") -> "Medium" to listOf("TT", "GC")
            stageType.contains("Plana") -> "Easy" to listOf("Sprinter", "Classics")
            else -> "Medium" to listOf("GC", "Climber", "Classics")
        }

        return RaceInsight(
            raceId = race.id,
            raceName = race.name,
            stageNumber = stage?.stageNumber,
            stageType = stageType,
            keyFactors = listOf(
                "Forma recente dos ciclistas",
                "Adequacao ao perfil da etapa",
                "Historico na corrida"
            ),
            recommendation = "Ativa ciclistas adequados ao perfil: $stageType",
            difficulty = difficulty,
            bestCategories = categories
        )
    }
}

// JSON parsing classes
private data class TopCandidateJson(
    val rank: Int?,
    val cyclistName: String,
    val teamName: String?,
    val category: String?,
    val reason: String?,
    val expectedPoints: String?
)

private data class RaceInsightJson(
    val keyFactors: List<String>?,
    val recommendation: String?,
    val difficulty: String?,
    val bestCategories: List<String>?
)
