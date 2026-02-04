package com.ciclismo.portugal.domain.bots

import android.util.Log
import com.ciclismo.portugal.data.local.dao.CyclistDao
import com.ciclismo.portugal.data.local.dao.FantasyTeamDao
import com.ciclismo.portugal.data.local.entity.FantasyTeamEntity
import com.ciclismo.portugal.data.local.entity.TeamCyclistEntity
import com.ciclismo.portugal.data.local.entity.toDomain
import com.ciclismo.portugal.data.remote.firebase.CyclistFirestoreService
import com.ciclismo.portugal.data.remote.firebase.FantasyTeamFirestoreService
import com.ciclismo.portugal.data.remote.firebase.LeagueFirestoreService
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.SeasonConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servico para criar e gerir equipas bot.
 * Usa o BotTeamGenerator para criar equipas e guarda-as localmente e no Firestore.
 */
@Singleton
class BotTeamService @Inject constructor(
    private val generator: BotTeamGenerator,
    private val fantasyTeamDao: FantasyTeamDao,
    private val cyclistDao: CyclistDao,
    private val cyclistFirestoreService: CyclistFirestoreService,
    private val fantasyTeamFirestoreService: FantasyTeamFirestoreService,
    private val leagueFirestoreService: LeagueFirestoreService
) {
    companion object {
        private const val TAG = "BotTeamService"
        private const val BOT_USER_PREFIX = "bot_user_"
    }

    /**
     * Gera e guarda as 236 equipas bot.
     *
     * @param onProgress Callback para reportar progresso (0-100)
     * @return Resultado com numero de equipas criadas
     */
    suspend fun generateBotTeams(
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val season = SeasonConfig.CURRENT_SEASON

            // Verificar se ja existem bots
            val existingBots = fantasyTeamDao.getBotTeamCountForSeason(season)
            if (existingBots >= BotTeamGenerator.TARGET_BOT_TEAMS) {
                Log.d(TAG, "Bot teams already exist: $existingBots")
                return@withContext Result.success(existingBots)
            }

            onProgress(0, "A obter ciclistas...")

            // Obter todos os ciclistas validados
            val cyclists = cyclistFirestoreService.getValidatedCyclists(season).first()
            if (cyclists.isEmpty()) {
                Log.e(TAG, "No validated cyclists found")
                return@withContext Result.failure(Exception("Nenhum ciclista validado encontrado"))
            }

            Log.d(TAG, "Found ${cyclists.size} validated cyclists")
            onProgress(5, "Encontrados ${cyclists.size} ciclistas")

            // Reset nomes usados
            generator.resetUsedNames()

            // Garantir liga global existe
            val globalLeague = try {
                leagueFirestoreService.ensureGlobalLeagueExists()
            } catch (e: Exception) {
                Log.w(TAG, "Could not ensure global league: ${e.message}")
                null
            }

            // Gerar equipas
            val teamsToCreate = BotTeamGenerator.TARGET_BOT_TEAMS - existingBots
            var created = 0
            var failed = 0

            for (i in 0 until teamsToCreate) {
                try {
                    val progress = 5 + ((i.toFloat() / teamsToCreate) * 90).toInt()
                    onProgress(progress, "A criar equipa ${i + 1}/$teamsToCreate...")

                    // Gerar dados da equipa
                    val strategy = generator.selectStrategy(i)
                    val teamName = generator.generateTeamName()
                    val selectedCyclists = generator.selectCyclists(cyclists, strategy)

                    if (selectedCyclists.size < BotTeamGenerator.TEAM_SIZE) {
                        Log.w(TAG, "Could not select enough cyclists for team $teamName")
                        failed++
                        continue
                    }

                    // Calcular budget restante
                    val totalCost = selectedCyclists.sumOf { it.price }
                    val remainingBudget = BotTeamGenerator.INITIAL_BUDGET - totalCost

                    // Criar entidade da equipa
                    val teamId = UUID.randomUUID().toString()
                    val botUserId = "$BOT_USER_PREFIX${UUID.randomUUID()}"

                    val teamEntity = FantasyTeamEntity(
                        id = teamId,
                        userId = botUserId,
                        teamName = teamName,
                        season = season,
                        budget = remainingBudget.coerceAtLeast(0.0),
                        totalPoints = generateBotPoints(strategy),
                        freeTransfers = 2,
                        isBot = true
                    )

                    // Guardar equipa localmente
                    fantasyTeamDao.insertTeam(teamEntity)

                    // Selecionar ativos e capitao
                    val activeIds = generator.selectActiveCyclists(selectedCyclists)
                    val captainId = generator.selectCaptain(selectedCyclists)

                    // Adicionar ciclistas a equipa
                    for (cyclist in selectedCyclists) {
                        val teamCyclist = TeamCyclistEntity(
                            teamId = teamId,
                            cyclistId = cyclist.id,
                            isActive = cyclist.id in activeIds,
                            isCaptain = cyclist.id == captainId,
                            purchasePrice = cyclist.price
                        )
                        fantasyTeamDao.addCyclistToTeam(teamCyclist)
                    }

                    // Sync para Firestore (fire and forget)
                    try {
                        fantasyTeamFirestoreService.createTeam(teamEntity.toDomain())
                        val teamCyclists = fantasyTeamDao.getTeamCyclistsSync(teamId).map { it.toDomain() }
                        fantasyTeamFirestoreService.syncTeamCyclists(teamId, teamCyclists)

                        // Adicionar a liga global
                        if (globalLeague != null) {
                            leagueFirestoreService.joinLeague(
                                leagueId = globalLeague.id,
                                userId = botUserId,
                                teamId = teamId,
                                teamName = teamName
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to sync bot team to Firestore: ${e.message}")
                    }

                    created++

                    // Pequeno delay para nao sobrecarregar
                    if (i % 10 == 9) {
                        delay(100)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error creating bot team $i: ${e.message}")
                    failed++
                }
            }

            onProgress(100, "Concluido! $created equipas criadas")
            Log.d(TAG, "Bot teams generation complete: $created created, $failed failed")

            Result.success(created)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating bot teams: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Gera pontos iniciais para a equipa bot baseado na estrategia.
     * Bots comecam com alguns pontos para parecer mais realista.
     */
    private fun generateBotPoints(strategy: BotStrategy): Int {
        val base = when (strategy) {
            BotStrategy.BALANCED -> 50
            BotStrategy.CLIMBER_HEAVY -> 60
            BotStrategy.SPRINTER_HEAVY -> 55
            BotStrategy.GC_FOCUSED -> 70
            BotStrategy.VALUE_PICKS -> 40
        }
        return base + (0..30).random()
    }

    /**
     * Apaga todas as equipas bot da temporada atual.
     */
    suspend fun deleteBotTeams(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val season = SeasonConfig.CURRENT_SEASON
            val count = fantasyTeamDao.getBotTeamCountForSeason(season)

            // Apagar equipas bot da liga
            val botTeams = fantasyTeamDao.getBotTeamsForSeason(season)
            for (team in botTeams) {
                try {
                    // Apagar ciclistas da equipa
                    fantasyTeamDao.clearTeamCyclists(team.id)

                    // Apagar do Firestore
                    fantasyTeamFirestoreService.deleteTeam(team.id)

                    // Remover da liga
                    try {
                        val globalLeague = leagueFirestoreService.ensureGlobalLeagueExists()
                        leagueFirestoreService.leaveLeague(globalLeague.id, team.userId)
                    } catch (e: Exception) {
                        // Ignore
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error deleting bot team ${team.id}: ${e.message}")
                }
            }

            // Apagar localmente
            fantasyTeamDao.deleteBotTeamsForSeason(season)

            Log.d(TAG, "Deleted $count bot teams")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting bot teams: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtem o numero de equipas bot na temporada atual.
     */
    suspend fun getBotTeamCount(): Int = withContext(Dispatchers.IO) {
        fantasyTeamDao.getBotTeamCountForSeason(SeasonConfig.CURRENT_SEASON)
    }

    /**
     * Verifica se as equipas bot ja foram geradas.
     */
    suspend fun areBotTeamsGenerated(): Boolean = withContext(Dispatchers.IO) {
        getBotTeamCount() >= BotTeamGenerator.TARGET_BOT_TEAMS
    }
}
