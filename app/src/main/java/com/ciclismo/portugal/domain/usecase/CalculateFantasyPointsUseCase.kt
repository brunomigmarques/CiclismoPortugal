package com.ciclismo.portugal.domain.usecase

import android.util.Log
import com.ciclismo.portugal.data.local.dao.FantasyTeamDao
import com.ciclismo.portugal.data.local.dao.RaceDao
import com.ciclismo.portugal.data.local.dao.TeamRaceResultDao
import com.ciclismo.portugal.data.local.entity.TeamRaceResultEntity
import com.ciclismo.portugal.data.local.entity.toDomain
import com.ciclismo.portugal.data.remote.firebase.CyclistFirestoreService
import com.ciclismo.portugal.data.remote.firebase.FantasyTeamFirestoreService
import com.ciclismo.portugal.data.remote.firebase.LeagueFirestoreService
import com.ciclismo.portugal.data.remote.firebase.TeamResultFirestoreService
import com.ciclismo.portugal.domain.model.RaceResult
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.repository.RaceRepository
import com.ciclismo.portugal.domain.scoring.PointsCalculator
import com.ciclismo.portugal.notifications.NotificationHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to calculate and apply fantasy points for all teams after a race
 */
@Singleton
class CalculateFantasyPointsUseCase @Inject constructor(
    private val raceRepository: RaceRepository,
    private val fantasyTeamDao: FantasyTeamDao,
    private val raceDao: RaceDao,
    private val teamRaceResultDao: TeamRaceResultDao,
    private val teamResultFirestoreService: TeamResultFirestoreService,
    private val fantasyTeamFirestoreService: FantasyTeamFirestoreService,
    private val leagueFirestoreService: LeagueFirestoreService,
    private val cyclistFirestoreService: CyclistFirestoreService,
    private val notificationHelper: NotificationHelper
) {
    /**
     * Calculate and apply points for all fantasy teams based on race results
     * @param raceId The race ID to process
     * @param currentUserId Optional user ID to send notification for (only that user gets notified)
     * @param forceReprocess If true, reprocess even if race was already marked as finished
     * @return Map of teamId to points earned in this race
     */
    suspend operator fun invoke(
        raceId: String,
        currentUserId: String? = null,
        forceReprocess: Boolean = false
    ): Result<Map<String, Int>> {
        return try {
            Log.d(TAG, "Calculating fantasy points for race: $raceId (force=$forceReprocess)")

            // Get the race - try local first, then Firestore
            var race = raceDao.getRaceById(raceId)?.toDomain()
            if (race == null) {
                Log.d(TAG, "Race not in local DB, fetching from Firestore...")
                race = raceRepository.getRaceById(raceId)
            }
            if (race == null) {
                return Result.failure(Exception("Race not found: $raceId"))
            }

            // Check if race already finished (results already processed)
            // Allow reprocess if forceReprocess is true or if no history exists
            val existingHistory = teamRaceResultDao.getResultsForRaceSync(raceId)
            if (race.isFinished && !forceReprocess) {
                if (existingHistory.isNotEmpty()) {
                    Log.w(TAG, "Race $raceId already processed with ${existingHistory.size} team results, skipping")
                    return Result.failure(Exception("Race results already processed"))
                }
                Log.d(TAG, "Race marked as finished but no history found, reprocessing...")
            }

            // If force reprocessing and history exists, clear it first to avoid duplicates
            if (forceReprocess && existingHistory.isNotEmpty()) {
                Log.d(TAG, "Force reprocess: clearing ${existingHistory.size} existing history entries")
                teamRaceResultDao.deleteRaceResults(raceId)
            }

            // Get race results (use direct read instead of Flow for immediate data)
            val results = raceRepository.getRaceResultsOnce(raceId)
            Log.d(TAG, "Found ${results.size} race results in Firestore for race: $raceId")

            if (results.isEmpty()) {
                Log.w(TAG, "No results found for race: $raceId - marking as finished anyway")
                // Still mark as finished even with no results
                raceRepository.setRaceFinished(raceId)
                return Result.success(emptyMap())
            }

            // Get all fantasy teams
            val allTeams = fantasyTeamDao.getAllTeams()
            if (allTeams.isEmpty()) {
                Log.w(TAG, "No fantasy teams found")
                return Result.success(emptyMap())
            }

            // Fetch all cyclists to build a lookup map for names
            val allCyclists = cyclistFirestoreService.getValidatedCyclistsOnce().getOrNull() ?: emptyList()
            val cyclistMap = allCyclists.associateBy { it.id }
            Log.d(TAG, "Loaded ${cyclistMap.size} cyclists for breakdown lookup")

            val teamPoints = mutableMapOf<String, Int>()
            val teamRaceResults = mutableListOf<TeamRaceResultEntity>()
            val processedAt = System.currentTimeMillis()

            // For each team, calculate points based on their cyclists' results
            allTeams.forEach { team ->
                val teamCyclists = fantasyTeamDao.getTeamCyclistsSync(team.id)
                var totalPoints = 0

                // Check if wildcards are active
                val isBenchBoostActive = team.benchBoostActive
                val isTripleCaptainActive = team.tripleCaptainActive

                // Track cyclist breakdown for this team
                val cyclistBreakdownList = mutableListOf<JSONObject>()
                var captainName: String? = null

                teamCyclists.forEach { teamCyclist ->
                    // Count active cyclists, OR all cyclists if Bench Boost is active
                    val shouldCount = teamCyclist.isActive || isBenchBoostActive

                    // Get cyclist info for breakdown
                    val cyclist = cyclistMap[teamCyclist.cyclistId]
                    val cyclistName = cyclist?.fullName ?: "Ciclista Desconhecido"
                    val cyclistTeamName = cyclist?.teamName ?: ""

                    // Track captain name
                    if (teamCyclist.isCaptain) {
                        captainName = cyclistName
                    }

                    if (shouldCount) {
                        // Find this cyclist's results in the race
                        val cyclistResults = results.filter { it.cyclistId == teamCyclist.cyclistId }
                        var cyclistTotalPoints = 0
                        var position: Int? = null
                        var status = "" // DNF, DNS, DSQ, or DNP

                        if (cyclistResults.isEmpty()) {
                            // Cyclist not in race results = Did Not Participate
                            status = "DNP"
                        } else {
                            cyclistResults.forEach { result ->
                                val points = PointsCalculator.calculateCyclistPoints(
                                    result = result,
                                    raceType = race.type,
                                    isCaptain = teamCyclist.isCaptain,
                                    isTripleCaptain = teamCyclist.isCaptain && isTripleCaptainActive
                                )
                                cyclistTotalPoints += points
                                if (position == null) {
                                    position = result.position
                                }
                                // Use status from result (DNF, DNS, DSQ)
                                if (status.isBlank() && result.status.isNotBlank()) {
                                    status = result.status
                                }
                                val suffix = when {
                                    teamCyclist.isCaptain && isTripleCaptainActive -> " (3x Captain)"
                                    teamCyclist.isCaptain -> " (2x Captain)"
                                    else -> ""
                                }
                                Log.d(TAG, "Cyclist ${teamCyclist.cyclistId}: +$points pts$suffix (status: $status)")
                            }
                        }

                        totalPoints += cyclistTotalPoints

                        // Add to breakdown (include all 8 active cyclists)
                        val breakdownItem = JSONObject().apply {
                            put("cyclistId", teamCyclist.cyclistId)
                            put("name", cyclistName)
                            put("teamName", cyclistTeamName)
                            put("points", cyclistTotalPoints)
                            put("isCaptain", teamCyclist.isCaptain)
                            put("position", position ?: JSONObject.NULL)
                            put("status", status) // DNF, DNS, DSQ, DNP, or empty
                        }
                        cyclistBreakdownList.add(breakdownItem)
                    }
                }

                // Convert breakdown to JSON string
                val breakdownJson = JSONArray(cyclistBreakdownList).toString()

                // Ensure minimum points is 0 (never negative)
                val finalPoints = totalPoints.coerceAtLeast(0)

                // Update team's total points (local DB)
                fantasyTeamDao.addPoints(team.id, finalPoints)
                teamPoints[team.id] = finalPoints
                val newTotal = (team.totalPoints + finalPoints).coerceAtLeast(0)
                Log.d(TAG, "Team ${team.teamName}: +$finalPoints pts (total: $newTotal)")

                // Update team points in Firestore for community rankings
                try {
                    fantasyTeamFirestoreService.updateTeamPoints(team.id, finalPoints)
                    Log.d(TAG, "Synced team ${team.id} points to Firestore")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync team points to Firestore: ${e.message}")
                }

                // Update league member points for all leagues the user is in
                try {
                    updateLeagueMemberPoints(team.userId, newTotal)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update league member points: ${e.message}")
                }

                // Save team race result for history with full breakdown
                teamRaceResults.add(
                    TeamRaceResultEntity(
                        id = UUID.randomUUID().toString(),
                        teamId = team.id,
                        raceId = raceId,
                        raceName = race.name,
                        pointsEarned = finalPoints,
                        raceDate = race.startDate,
                        season = SeasonConfig.CURRENT_SEASON,
                        processedAt = processedAt,
                        cyclistBreakdownJson = breakdownJson,
                        captainName = captainName,
                        wasTripleCaptainActive = isTripleCaptainActive,
                        wasBenchBoostActive = isBenchBoostActive
                    )
                )

                // Send notification if this is the current user's team
                if (currentUserId != null && team.userId == currentUserId) {
                    notificationHelper.showFantasyPointsNotification(
                        raceName = race.name,
                        pointsEarned = finalPoints,
                        totalPoints = newTotal
                    )
                }

                // Deactivate wildcards after processing this race
                if (isBenchBoostActive) {
                    fantasyTeamDao.deactivateBenchBoost(team.id)
                    Log.d(TAG, "Deactivated Bench Boost for team ${team.id}")
                }
                if (isTripleCaptainActive) {
                    fantasyTeamDao.deactivateTripleCaptain(team.id)
                    Log.d(TAG, "Deactivated Triple Captain for team ${team.id}")
                }
            }

            // Save all team race results to local DB
            teamRaceResultDao.insertAllResults(teamRaceResults)
            Log.d(TAG, "Saved ${teamRaceResults.size} team race results to local DB")

            // Also upload to Firestore for cloud backup/sync
            try {
                teamResultFirestoreService.uploadTeamResults(teamRaceResults)
                Log.d(TAG, "Uploaded ${teamRaceResults.size} team race results to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload team results to Firestore: ${e.message}")
                // Don't fail the operation - local save succeeded
            }

            // Mark race as finished
            raceRepository.setRaceFinished(raceId)
            Log.d(TAG, "Race $raceId marked as finished")

            // Recalculate rankings for all leagues after race processing
            try {
                recalculateAllLeagueRankings()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to recalculate league rankings: ${e.message}")
            }

            Log.d(TAG, "Processed ${teamPoints.size} teams with points")
            Result.success(teamPoints)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating fantasy points", e)
            Result.failure(e)
        }
    }

    /**
     * Get detailed points breakdown for a specific team in a race
     */
    suspend fun getTeamPointsBreakdown(
        teamId: String,
        raceId: String
    ): Result<List<CyclistPointsBreakdown>> {
        return try {
            val race = raceDao.getRaceById(raceId)?.toDomain()
                ?: return Result.failure(Exception("Race not found"))

            val results = raceRepository.getRaceResultsOnce(raceId)
            val teamCyclists = fantasyTeamDao.getTeamCyclistsSync(teamId)

            val breakdown = teamCyclists.mapNotNull { teamCyclist ->
                val cyclistResults = results.filter { it.cyclistId == teamCyclist.cyclistId }
                if (cyclistResults.isEmpty()) return@mapNotNull null

                val totalPoints = cyclistResults.sumOf { result ->
                    PointsCalculator.calculateCyclistPoints(
                        result = result,
                        raceType = race.type,
                        isCaptain = teamCyclist.isCaptain,
                        isTripleCaptain = false
                    )
                }

                CyclistPointsBreakdown(
                    cyclistId = teamCyclist.cyclistId,
                    points = totalPoints,
                    isCaptain = teamCyclist.isCaptain,
                    results = cyclistResults
                )
            }

            Result.success(breakdown)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting points breakdown", e)
            Result.failure(e)
        }
    }

    /**
     * Update a user's points in all leagues they belong to
     */
    private suspend fun updateLeagueMemberPoints(userId: String, totalPoints: Int) {
        // Update points in the global league (Liga Portugal) for current season
        try {
            val globalLeague = leagueFirestoreService.ensureGlobalLeagueExists()
            leagueFirestoreService.updateMemberPoints(
                leagueId = globalLeague.id,
                userId = userId,
                points = totalPoints
            )
            Log.d(TAG, "Updated user $userId points in global league ${globalLeague.season}: $totalPoints")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update global league points for user $userId: ${e.message}")
        }

        // TODO: Also update other leagues the user is part of
        // This would require fetching the user's leagues first
        // For now, the global league is the main community ranking
    }

    /**
     * Recalculate rankings for all leagues after race processing
     */
    private suspend fun recalculateAllLeagueRankings() {
        try {
            // Recalculate global league rankings for current season
            val globalLeague = leagueFirestoreService.ensureGlobalLeagueExists()
            leagueFirestoreService.recalculateRankings(globalLeague.id)
            Log.d(TAG, "Recalculated global league ${globalLeague.season} rankings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recalculate global league rankings: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "CalculateFantasyPoints"
    }
}

data class CyclistPointsBreakdown(
    val cyclistId: String,
    val points: Int,
    val isCaptain: Boolean,
    val results: List<RaceResult>
)
