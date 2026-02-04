package com.ciclismo.portugal.domain.usecase

import android.util.Log
import com.ciclismo.portugal.data.local.dao.FantasyTeamDao
import com.ciclismo.portugal.data.local.dao.StageResultDao
import com.ciclismo.portugal.data.local.dao.TeamRaceResultDao
import com.ciclismo.portugal.data.local.entity.TeamRaceResultEntity
import com.ciclismo.portugal.data.remote.firebase.CyclistFirestoreService
import com.ciclismo.portugal.data.remote.firebase.FantasyTeamFirestoreService
import com.ciclismo.portugal.data.remote.firebase.LeagueFirestoreService
import com.ciclismo.portugal.data.remote.firebase.StageFirestoreService
import com.ciclismo.portugal.data.remote.firebase.TeamResultFirestoreService
import com.ciclismo.portugal.data.remote.firebase.TeamStageResultData
import com.ciclismo.portugal.domain.model.FinalGcBonusPoints
import com.ciclismo.portugal.domain.model.JerseyBonusPoints
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.model.StagePointsTable
import com.ciclismo.portugal.domain.model.StageResult
import com.ciclismo.portugal.domain.repository.RaceRepository
import com.ciclismo.portugal.domain.repository.StageRepository
import com.ciclismo.portugal.notifications.NotificationHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to calculate and apply fantasy points for stage races (Grand Tours).
 * Points are calculated per stage and accumulated over the race duration.
 */
@Singleton
class CalculateStageFantasyPointsUseCase @Inject constructor(
    private val stageRepository: StageRepository,
    private val stageFirestoreService: StageFirestoreService,
    private val raceRepository: RaceRepository,
    private val fantasyTeamDao: FantasyTeamDao,
    private val stageResultDao: StageResultDao,
    private val teamRaceResultDao: TeamRaceResultDao,
    private val teamResultFirestoreService: TeamResultFirestoreService,
    private val fantasyTeamFirestoreService: FantasyTeamFirestoreService,
    private val leagueFirestoreService: LeagueFirestoreService,
    private val cyclistFirestoreService: CyclistFirestoreService,
    private val notificationHelper: NotificationHelper
) {

    /**
     * Calculate and apply points for a specific stage.
     *
     * @param raceId The race ID
     * @param stageNumber The stage number to process
     * @param stageResults The stage results to process
     * @param currentUserId Optional user ID to send notification for
     * @return Result with stage processing summary
     */
    suspend fun processStage(
        raceId: String,
        stageNumber: Int,
        stageResults: List<StageResult>,
        currentUserId: String? = null
    ): Result<StagePointsResult> {
        return try {
            Log.d(TAG, "Processing stage $stageNumber for race $raceId with ${stageResults.size} results")

            // Get stage info
            val stage = stageFirestoreService.getStage(raceId, stageNumber)
            val stageType = stage?.stageType ?: stageResults.firstOrNull()?.stageType
                ?: return Result.failure(Exception("Stage type not found"))

            // 1. Save stage results to Firestore (cyclist results)
            val saveResult = stageFirestoreService.saveStageResults(stageResults)
            saveResult.fold(
                onSuccess = { count -> Log.d(TAG, "Saved $count stage results to Firestore") },
                onFailure = { e -> Log.e(TAG, "Failed to save stage results to Firestore: ${e.message}") }
            )

            // 2. Save stage results to local DB for offline access
            stageRepository.saveStageResults(stageResults)

            // Get all fantasy teams
            val allTeams = fantasyTeamDao.getAllTeams()
            if (allTeams.isEmpty()) {
                Log.w(TAG, "No fantasy teams found")
                return Result.success(StagePointsResult(
                    stageNumber = stageNumber,
                    teamsProcessed = 0,
                    totalPointsAwarded = 0,
                    teamPoints = emptyMap()
                ))
            }

            // Fetch all cyclists for lookup
            val allCyclists = cyclistFirestoreService.getValidatedCyclistsOnce().getOrNull() ?: emptyList()
            val cyclistMap = allCyclists.associateBy { it.id }

            val teamPoints = mutableMapOf<String, Int>()
            var totalPointsAwarded = 0

            // For each team, calculate points based on their cyclists' stage results
            allTeams.forEach { team ->
                val teamCyclists = fantasyTeamDao.getTeamCyclistsSync(team.id)
                var stagePointsForTeam = 0

                // Check wildcards
                val isBenchBoostActive = team.benchBoostActive
                val isTripleCaptainActive = team.tripleCaptainActive

                teamCyclists.forEach { teamCyclist ->
                    val shouldCount = teamCyclist.isActive || isBenchBoostActive

                    if (shouldCount) {
                        // Find this cyclist's results in the stage
                        val cyclistStageResult = stageResults.find { it.cyclistId == teamCyclist.cyclistId }

                        if (cyclistStageResult != null) {
                            // Calculate base points from position
                            var points = StagePointsTable.getPoints(
                                position = cyclistStageResult.position ?: 0,
                                stageType = stageType
                            )

                            // Add jersey bonuses
                            points += JerseyBonusPoints.calculate(
                                isGcLeader = cyclistStageResult.isGcLeader,
                                isPointsLeader = cyclistStageResult.isPointsLeader,
                                isMountainsLeader = cyclistStageResult.isMountainsLeader,
                                isYoungLeader = cyclistStageResult.isYoungLeader
                            )

                            // Apply captain multiplier
                            val finalPoints = when {
                                teamCyclist.isCaptain && isTripleCaptainActive -> points * 3
                                teamCyclist.isCaptain -> points * 2
                                else -> points
                            }

                            stagePointsForTeam += finalPoints

                            Log.d(TAG, "Cyclist ${teamCyclist.cyclistId}: $finalPoints pts " +
                                "(pos=${cyclistStageResult.position}, captain=${teamCyclist.isCaptain})")
                        }
                    }
                }

                // Update team points (local DB)
                if (stagePointsForTeam > 0) {
                    fantasyTeamDao.addPoints(team.id, stagePointsForTeam)
                    teamPoints[team.id] = stagePointsForTeam
                    totalPointsAwarded += stagePointsForTeam

                    val newTotal = team.totalPoints + stagePointsForTeam
                    Log.d(TAG, "Team ${team.teamName}: +$stagePointsForTeam pts (total: $newTotal)")

                    // Update team points in Firestore
                    try {
                        fantasyTeamFirestoreService.updateTeamPoints(team.id, stagePointsForTeam)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync team points to Firestore: ${e.message}")
                    }

                    // Update league member points
                    try {
                        updateLeagueMemberPoints(team.userId, newTotal)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update league member points: ${e.message}")
                    }

                    // Send notification if current user
                    if (currentUserId != null && team.userId == currentUserId) {
                        notificationHelper.showFantasyPointsNotification(
                            raceName = "Etapa $stageNumber",
                            pointsEarned = stagePointsForTeam,
                            totalPoints = newTotal
                        )
                    }
                }
            }

            // 3. Save team stage results to Firestore
            val teamStageResults = allTeams.mapNotNull { team ->
                val points = teamPoints[team.id] ?: return@mapNotNull null
                if (points <= 0) return@mapNotNull null

                TeamStageResultData(
                    teamId = team.id,
                    raceId = raceId,
                    raceName = stage?.name ?: "Etapa $stageNumber",
                    stageNumber = stageNumber,
                    pointsEarned = points,
                    wasTripleCaptainActive = team.tripleCaptainActive,
                    wasBenchBoostActive = team.benchBoostActive
                )
            }

            if (teamStageResults.isNotEmpty()) {
                try {
                    teamResultFirestoreService.uploadTeamStageResults(teamStageResults)
                    Log.d(TAG, "Uploaded ${teamStageResults.size} team stage results to Firestore")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload team stage results: ${e.message}")
                }
            }

            // Mark stage as processed
            stageFirestoreService.markStageProcessed(raceId, stageNumber)

            Log.d(TAG, "Stage $stageNumber processed: ${teamPoints.size} teams, $totalPointsAwarded total pts")

            Result.success(StagePointsResult(
                stageNumber = stageNumber,
                teamsProcessed = teamPoints.size,
                totalPointsAwarded = totalPointsAwarded,
                teamPoints = teamPoints
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing stage $stageNumber", e)
            Result.failure(e)
        }
    }

    /**
     * Apply final GC bonus points at the end of a Grand Tour.
     *
     * @param raceId The race ID
     * @param currentUserId Optional user ID for notification
     * @return Result with final bonus summary
     */
    suspend fun applyFinalGcBonus(
        raceId: String,
        currentUserId: String? = null
    ): Result<FinalGcBonusResult> {
        return try {
            Log.d(TAG, "Applying final GC bonus for race $raceId")

            // Get final GC standings
            val gcStandings = stageRepository.getGcStandingsOnce(raceId)
            if (gcStandings.isEmpty()) {
                return Result.failure(Exception("No GC standings found"))
            }

            // Get all fantasy teams
            val allTeams = fantasyTeamDao.getAllTeams()

            val teamBonuses = mutableMapOf<String, Int>()
            var totalBonusAwarded = 0

            // For each team, calculate GC bonus for their cyclists
            allTeams.forEach { team ->
                val teamCyclists = fantasyTeamDao.getTeamCyclistsSync(team.id)
                var gcBonusForTeam = 0

                teamCyclists.forEach { teamCyclist ->
                    if (teamCyclist.isActive) {
                        val gcStanding = gcStandings.find { it.cyclistId == teamCyclist.cyclistId }
                        if (gcStanding != null) {
                            val bonus = FinalGcBonusPoints.getBonus(gcStanding.gcPosition)
                            if (bonus > 0) {
                                // Apply captain multiplier to bonus
                                val finalBonus = when {
                                    teamCyclist.isCaptain && team.tripleCaptainActive -> bonus * 3
                                    teamCyclist.isCaptain -> bonus * 2
                                    else -> bonus
                                }
                                gcBonusForTeam += finalBonus
                                Log.d(TAG, "Cyclist ${teamCyclist.cyclistId} GC ${gcStanding.gcPosition}: +$finalBonus bonus")
                            }
                        }
                    }
                }

                if (gcBonusForTeam > 0) {
                    fantasyTeamDao.addPoints(team.id, gcBonusForTeam)
                    teamBonuses[team.id] = gcBonusForTeam
                    totalBonusAwarded += gcBonusForTeam

                    val newTotal = team.totalPoints + gcBonusForTeam
                    Log.d(TAG, "Team ${team.teamName}: +$gcBonusForTeam GC bonus (total: $newTotal)")

                    // Update Firestore
                    try {
                        fantasyTeamFirestoreService.updateTeamPoints(team.id, gcBonusForTeam)
                        updateLeagueMemberPoints(team.userId, newTotal)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync GC bonus: ${e.message}")
                    }

                    // Notification
                    if (currentUserId != null && team.userId == currentUserId) {
                        notificationHelper.showFantasyPointsNotification(
                            raceName = "Bónus Final GC",
                            pointsEarned = gcBonusForTeam,
                            totalPoints = newTotal
                        )
                    }
                }
            }

            // Mark race as completed
            stageRepository.markRaceCompleted(raceId)

            // Recalculate league rankings
            recalculateAllLeagueRankings()

            Log.d(TAG, "Final GC bonus applied: ${teamBonuses.size} teams, $totalBonusAwarded total bonus")

            Result.success(FinalGcBonusResult(
                teamsProcessed = teamBonuses.size,
                totalBonusAwarded = totalBonusAwarded,
                teamBonuses = teamBonuses
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error applying final GC bonus", e)
            Result.failure(e)
        }
    }

    /**
     * Get cumulative points for a team across all stages of a race.
     */
    suspend fun getTeamRacePoints(teamId: String, raceId: String): Int {
        return try {
            val teamCyclists = fantasyTeamDao.getTeamCyclistsSync(teamId)
            var totalPoints = 0

            teamCyclists.forEach { teamCyclist ->
                if (teamCyclist.isActive) {
                    val cyclistPoints = stageResultDao.getCyclistTotalPoints(raceId, teamCyclist.cyclistId)
                    totalPoints += cyclistPoints
                }
            }

            totalPoints
        } catch (e: Exception) {
            Log.e(TAG, "Error getting team race points", e)
            0
        }
    }

    private suspend fun updateLeagueMemberPoints(userId: String, totalPoints: Int) {
        try {
            val globalLeague = leagueFirestoreService.ensureGlobalLeagueExists()
            leagueFirestoreService.updateMemberPoints(
                leagueId = globalLeague.id,
                userId = userId,
                points = totalPoints
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update league member points: ${e.message}")
        }
    }

    private suspend fun recalculateAllLeagueRankings() {
        try {
            val globalLeague = leagueFirestoreService.ensureGlobalLeagueExists()
            leagueFirestoreService.recalculateRankings(globalLeague.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recalculate league rankings: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "StageFantasyPoints"
    }
}

/**
 * Result of processing a single stage.
 */
data class StagePointsResult(
    val stageNumber: Int,
    val teamsProcessed: Int,
    val totalPointsAwarded: Int,
    val teamPoints: Map<String, Int>
)

/**
 * Result of applying final GC bonus.
 */
data class FinalGcBonusResult(
    val teamsProcessed: Int,
    val totalBonusAwarded: Int,
    val teamBonuses: Map<String, Int>
)
