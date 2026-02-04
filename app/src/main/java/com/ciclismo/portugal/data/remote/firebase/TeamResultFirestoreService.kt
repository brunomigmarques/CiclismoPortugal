package com.ciclismo.portugal.data.remote.firebase

import android.util.Log
import com.ciclismo.portugal.data.local.entity.TeamRaceResultEntity
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore service for team race results.
 * Stores team results in cloud for cross-device sync and reinstall recovery.
 */
@Singleton
class TeamResultFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "TeamResultFirestore"
        private const val COLLECTION_SEASONS = "seasons"
        private const val TEAM_RESULTS_COLLECTION = "team_race_results"
    }

    /**
     * Get the collection path for team race results in a specific season
     * Pattern: seasons/{season}/team_race_results
     */
    private fun teamResultsCollection(season: Int = SeasonConfig.CURRENT_SEASON) =
        firestore.collection(COLLECTION_SEASONS)
            .document(season.toString())
            .collection(TEAM_RESULTS_COLLECTION)

    /**
     * Upload a single team race result to Firestore
     * Uses the season from the result entity to determine collection path
     */
    suspend fun uploadTeamResult(result: TeamRaceResultEntity): Result<Unit> {
        return try {
            val resultData = hashMapOf(
                "teamId" to result.teamId,
                "raceId" to result.raceId,
                "raceName" to result.raceName,
                "pointsEarned" to result.pointsEarned,
                "raceDate" to result.raceDate,
                "season" to result.season,
                "processedAt" to result.processedAt
            )

            teamResultsCollection(result.season)
                .document(result.id)
                .set(resultData)
                .await()

            Log.d(TAG, "Uploaded team result: ${result.raceName} - ${result.pointsEarned} pts (season ${result.season})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading team result", e)
            Result.failure(e)
        }
    }

    /**
     * Upload multiple team race results at once (batch operation)
     * @param season the season to upload results to (defaults to current season)
     */
    suspend fun uploadTeamResults(results: List<TeamRaceResultEntity>, season: Int = SeasonConfig.CURRENT_SEASON): Result<Int> {
        return try {
            if (results.isEmpty()) {
                return Result.success(0)
            }

            val batch = firestore.batch()

            results.forEach { result ->
                val docRef = teamResultsCollection(season).document(result.id)
                val resultData = hashMapOf(
                    "teamId" to result.teamId,
                    "raceId" to result.raceId,
                    "raceName" to result.raceName,
                    "pointsEarned" to result.pointsEarned,
                    "raceDate" to result.raceDate,
                    "season" to season,
                    "processedAt" to result.processedAt,
                    "cyclistBreakdownJson" to result.cyclistBreakdownJson,
                    "captainName" to result.captainName,
                    "wasTripleCaptainActive" to result.wasTripleCaptainActive,
                    "wasBenchBoostActive" to result.wasBenchBoostActive
                )
                batch.set(docRef, resultData)
            }

            batch.commit().await()

            Log.d(TAG, "Uploaded ${results.size} team results to Firestore (season $season)")
            Result.success(results.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading team results batch", e)
            Result.failure(e)
        }
    }

    /**
     * Get all team results for a specific team from Firestore
     * @param season the season to get results from (defaults to current season)
     */
    suspend fun getTeamResults(teamId: String, season: Int = SeasonConfig.CURRENT_SEASON): List<TeamRaceResultEntity> {
        return try {
            val snapshot = teamResultsCollection(season)
                .whereEqualTo("teamId", teamId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    TeamRaceResultEntity(
                        id = doc.id,
                        teamId = doc.getString("teamId") ?: "",
                        raceId = doc.getString("raceId") ?: "",
                        raceName = doc.getString("raceName") ?: "",
                        pointsEarned = doc.getLong("pointsEarned")?.toInt() ?: 0,
                        raceDate = doc.getLong("raceDate") ?: 0L,
                        season = doc.getLong("season")?.toInt() ?: season,
                        processedAt = doc.getLong("processedAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing team result: ${doc.id}", e)
                    null
                }
            }.sortedByDescending { it.processedAt }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching team results for team: $teamId", e)
            emptyList()
        }
    }

    /**
     * Get team results for a specific season
     */
    suspend fun getTeamResultsForSeason(teamId: String, season: Int): List<TeamRaceResultEntity> {
        return try {
            val snapshot = teamResultsCollection(season)
                .whereEqualTo("teamId", teamId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    TeamRaceResultEntity(
                        id = doc.id,
                        teamId = doc.getString("teamId") ?: "",
                        raceId = doc.getString("raceId") ?: "",
                        raceName = doc.getString("raceName") ?: "",
                        pointsEarned = doc.getLong("pointsEarned")?.toInt() ?: 0,
                        raceDate = doc.getLong("raceDate") ?: 0L,
                        season = doc.getLong("season")?.toInt() ?: season,
                        processedAt = doc.getLong("processedAt") ?: System.currentTimeMillis(),
                        cyclistBreakdownJson = doc.getString("cyclistBreakdownJson"),
                        captainName = doc.getString("captainName"),
                        wasTripleCaptainActive = doc.getBoolean("wasTripleCaptainActive") ?: false,
                        wasBenchBoostActive = doc.getBoolean("wasBenchBoostActive") ?: false
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing team result: ${doc.id}", e)
                    null
                }
            }.sortedByDescending { it.raceDate }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching team results for season: $season", e)
            emptyList()
        }
    }

    /**
     * Check if team result already exists for a race
     * @param season the season to check in (defaults to current season)
     */
    suspend fun hasResultForRace(teamId: String, raceId: String, season: Int = SeasonConfig.CURRENT_SEASON): Boolean {
        return try {
            val snapshot = teamResultsCollection(season)
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("raceId", raceId)
                .limit(1)
                .get()
                .await()

            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking team result existence", e)
            false
        }
    }

    /**
     * Delete all results for a team (used when team is deleted)
     * @param season the season to delete results from (defaults to current season)
     */
    suspend fun deleteTeamResults(teamId: String, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            val snapshot = teamResultsCollection(season)
                .whereEqualTo("teamId", teamId)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Log.d(TAG, "Deleted ${snapshot.documents.size} results for team: $teamId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting team results", e)
            Result.failure(e)
        }
    }

    // ==================== STAGE-SPECIFIC RESULTS ====================

    /**
     * Upload team stage result for a Grand Tour stage.
     * Path: seasons/{season}/team_stage_results/{id}
     */
    suspend fun uploadTeamStageResult(
        teamId: String,
        raceId: String,
        raceName: String,
        stageNumber: Int,
        pointsEarned: Int,
        cyclistBreakdownJson: String? = null,
        captainName: String? = null,
        wasTripleCaptainActive: Boolean = false,
        wasBenchBoostActive: Boolean = false,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        return try {
            val id = "${teamId}_${raceId}_stage_$stageNumber"
            val resultData = hashMapOf(
                "teamId" to teamId,
                "raceId" to raceId,
                "raceName" to raceName,
                "stageNumber" to stageNumber,
                "pointsEarned" to pointsEarned,
                "season" to season,
                "processedAt" to System.currentTimeMillis(),
                "cyclistBreakdownJson" to cyclistBreakdownJson,
                "captainName" to captainName,
                "wasTripleCaptainActive" to wasTripleCaptainActive,
                "wasBenchBoostActive" to wasBenchBoostActive
            )

            firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection("team_stage_results")
                .document(id)
                .set(resultData)
                .await()

            Log.d(TAG, "Uploaded team stage result: $raceName Stage $stageNumber - $pointsEarned pts")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading team stage result", e)
            Result.failure(e)
        }
    }

    /**
     * Upload multiple team stage results at once (batch operation).
     */
    suspend fun uploadTeamStageResults(
        results: List<TeamStageResultData>,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Int> {
        return try {
            if (results.isEmpty()) return Result.success(0)

            val batch = firestore.batch()

            results.forEach { result ->
                val id = "${result.teamId}_${result.raceId}_stage_${result.stageNumber}"
                val docRef = firestore.collection(COLLECTION_SEASONS)
                    .document(season.toString())
                    .collection("team_stage_results")
                    .document(id)

                val resultData = hashMapOf(
                    "teamId" to result.teamId,
                    "raceId" to result.raceId,
                    "raceName" to result.raceName,
                    "stageNumber" to result.stageNumber,
                    "pointsEarned" to result.pointsEarned,
                    "season" to season,
                    "processedAt" to System.currentTimeMillis(),
                    "cyclistBreakdownJson" to result.cyclistBreakdownJson,
                    "captainName" to result.captainName,
                    "wasTripleCaptainActive" to result.wasTripleCaptainActive,
                    "wasBenchBoostActive" to result.wasBenchBoostActive
                )
                batch.set(docRef, resultData)
            }

            batch.commit().await()
            Log.d(TAG, "Uploaded ${results.size} team stage results to Firestore")
            Result.success(results.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading team stage results batch", e)
            Result.failure(e)
        }
    }

    /**
     * Get all stage results for a team in a specific race.
     */
    suspend fun getTeamStageResults(
        teamId: String,
        raceId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): List<TeamStageResultData> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection("team_stage_results")
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("raceId", raceId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    TeamStageResultData(
                        teamId = doc.getString("teamId") ?: "",
                        raceId = doc.getString("raceId") ?: "",
                        raceName = doc.getString("raceName") ?: "",
                        stageNumber = doc.getLong("stageNumber")?.toInt() ?: 0,
                        pointsEarned = doc.getLong("pointsEarned")?.toInt() ?: 0,
                        cyclistBreakdownJson = doc.getString("cyclistBreakdownJson"),
                        captainName = doc.getString("captainName"),
                        wasTripleCaptainActive = doc.getBoolean("wasTripleCaptainActive") ?: false,
                        wasBenchBoostActive = doc.getBoolean("wasBenchBoostActive") ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.stageNumber }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting team stage results", e)
            emptyList()
        }
    }
}

/**
 * Data class for team stage result upload.
 */
data class TeamStageResultData(
    val teamId: String,
    val raceId: String,
    val raceName: String,
    val stageNumber: Int,
    val pointsEarned: Int,
    val cyclistBreakdownJson: String? = null,
    val captainName: String? = null,
    val wasTripleCaptainActive: Boolean = false,
    val wasBenchBoostActive: Boolean = false
)
