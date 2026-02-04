package com.ciclismo.portugal.data.remote.firebase

import android.util.Log
import com.ciclismo.portugal.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Firestore service for stage results in multi-stage races.
 *
 * Collection structure:
 * seasons/{season}/stage_results/{stageResultId}
 * seasons/{season}/gc_standings/{gcStandingId}
 * seasons/{season}/races/{raceId}/stages/{stageNumber}
 */
@Singleton
class StageFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "StageFirestoreService"
        private const val COLLECTION_SEASONS = "seasons"
        private const val COLLECTION_STAGE_RESULTS = "stage_results"
        private const val COLLECTION_GC_STANDINGS = "gc_standings"
        private const val COLLECTION_RACES = "races"
        private const val COLLECTION_STAGES = "stages"
    }

    // ==================== STAGE RESULTS ====================

    /**
     * Save a single stage result to Firebase.
     */
    suspend fun saveStageResult(result: StageResult): Result<Unit> {
        return try {
            val data = mapOf(
                "id" to result.id,
                "raceId" to result.raceId,
                "stageNumber" to result.stageNumber,
                "stageType" to result.stageType.name,
                "cyclistId" to result.cyclistId,
                "position" to result.position,
                "points" to result.points,
                "jerseyBonus" to result.jerseyBonus,
                "isGcLeader" to result.isGcLeader,
                "isMountainsLeader" to result.isMountainsLeader,
                "isPointsLeader" to result.isPointsLeader,
                "isYoungLeader" to result.isYoungLeader,
                "time" to result.time,
                "status" to result.status,
                "timestamp" to result.timestamp,
                "season" to result.season
            )

            firestore.collection(COLLECTION_SEASONS)
                .document(result.season.toString())
                .collection(COLLECTION_STAGE_RESULTS)
                .document(result.id)
                .set(data)
                .await()

            Log.d(TAG, "Saved stage result: ${result.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stage result: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Save multiple stage results (batch operation).
     */
    suspend fun saveStageResults(results: List<StageResult>): Result<Int> {
        if (results.isEmpty()) return Result.success(0)

        return try {
            val season = results.first().season
            val batch = firestore.batch()
            var count = 0

            results.forEach { result ->
                val data = mapOf(
                    "id" to result.id,
                    "raceId" to result.raceId,
                    "stageNumber" to result.stageNumber,
                    "stageType" to result.stageType.name,
                    "cyclistId" to result.cyclistId,
                    "position" to result.position,
                    "points" to result.points,
                    "jerseyBonus" to result.jerseyBonus,
                    "isGcLeader" to result.isGcLeader,
                    "isMountainsLeader" to result.isMountainsLeader,
                    "isPointsLeader" to result.isPointsLeader,
                    "isYoungLeader" to result.isYoungLeader,
                    "time" to result.time,
                    "status" to result.status,
                    "timestamp" to result.timestamp,
                    "season" to result.season
                )

                val docRef = firestore.collection(COLLECTION_SEASONS)
                    .document(season.toString())
                    .collection(COLLECTION_STAGE_RESULTS)
                    .document(result.id)

                batch.set(docRef, data)
                count++

                // Firebase batch limit is 500
                if (count % 450 == 0) {
                    batch.commit().await()
                }
            }

            batch.commit().await()
            Log.d(TAG, "Saved $count stage results")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stage results: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all stage results for a specific race and stage.
     */
    suspend fun getStageResults(
        raceId: String,
        stageNumber: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): List<StageResult> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection(COLLECTION_STAGE_RESULTS)
                .whereEqualTo("raceId", raceId)
                .whereEqualTo("stageNumber", stageNumber)
                .orderBy("position", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toStageResult()
            }.also {
                Log.d(TAG, "Got ${it.size} results for stage $stageNumber of $raceId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stage results: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get all stage results for a cyclist in a race.
     */
    suspend fun getCyclistStageResults(
        raceId: String,
        cyclistId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): List<StageResult> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection(COLLECTION_STAGE_RESULTS)
                .whereEqualTo("raceId", raceId)
                .whereEqualTo("cyclistId", cyclistId)
                .orderBy("stageNumber", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toStageResult()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cyclist stage results: ${e.message}")
            emptyList()
        }
    }

    /**
     * Observe stage results for a race in real-time.
     */
    fun observeStageResults(
        raceId: String,
        stageNumber: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Flow<List<StageResult>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_SEASONS)
            .document(season.toString())
            .collection(COLLECTION_STAGE_RESULTS)
            .whereEqualTo("raceId", raceId)
            .whereEqualTo("stageNumber", stageNumber)
            .orderBy("position", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing stage results: ${error.message}")
                    return@addSnapshotListener
                }

                val results = snapshot?.documents?.mapNotNull { it.toStageResult() } ?: emptyList()
                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    // ==================== GC STANDINGS ====================

    /**
     * Save GC standing for a cyclist.
     */
    suspend fun saveGcStanding(standing: GcStanding): Result<Unit> {
        return try {
            val data = mapOf(
                "id" to standing.id,
                "raceId" to standing.raceId,
                "cyclistId" to standing.cyclistId,
                "gcPosition" to standing.gcPosition,
                "gcTime" to standing.gcTime,
                "gcGap" to standing.gcGap,
                "totalPoints" to standing.totalPoints,
                "stageWins" to standing.stageWins,
                "stagePodiums" to standing.stagePodiums,
                "lastUpdatedStage" to standing.lastUpdatedStage,
                "isGcLeader" to standing.isGcLeader,
                "isMountainsLeader" to standing.isMountainsLeader,
                "isPointsLeader" to standing.isPointsLeader,
                "isYoungLeader" to standing.isYoungLeader,
                "timestamp" to standing.timestamp,
                "season" to standing.season
            )

            firestore.collection(COLLECTION_SEASONS)
                .document(standing.season.toString())
                .collection(COLLECTION_GC_STANDINGS)
                .document(standing.id)
                .set(data)
                .await()

            Log.d(TAG, "Saved GC standing: ${standing.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving GC standing: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Save multiple GC standings (batch operation).
     */
    suspend fun saveGcStandings(standings: List<GcStanding>): Result<Int> {
        if (standings.isEmpty()) return Result.success(0)

        return try {
            val season = standings.first().season
            val batch = firestore.batch()

            standings.forEach { standing ->
                val data = mapOf(
                    "id" to standing.id,
                    "raceId" to standing.raceId,
                    "cyclistId" to standing.cyclistId,
                    "gcPosition" to standing.gcPosition,
                    "gcTime" to standing.gcTime,
                    "gcGap" to standing.gcGap,
                    "totalPoints" to standing.totalPoints,
                    "stageWins" to standing.stageWins,
                    "stagePodiums" to standing.stagePodiums,
                    "lastUpdatedStage" to standing.lastUpdatedStage,
                    "isGcLeader" to standing.isGcLeader,
                    "isMountainsLeader" to standing.isMountainsLeader,
                    "isPointsLeader" to standing.isPointsLeader,
                    "isYoungLeader" to standing.isYoungLeader,
                    "timestamp" to standing.timestamp,
                    "season" to standing.season
                )

                val docRef = firestore.collection(COLLECTION_SEASONS)
                    .document(season.toString())
                    .collection(COLLECTION_GC_STANDINGS)
                    .document(standing.id)

                batch.set(docRef, data)
            }

            batch.commit().await()
            Log.d(TAG, "Saved ${standings.size} GC standings")
            Result.success(standings.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving GC standings: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get current GC standings for a race.
     */
    suspend fun getGcStandings(
        raceId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): List<GcStanding> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection(COLLECTION_GC_STANDINGS)
                .whereEqualTo("raceId", raceId)
                .orderBy("gcPosition", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toGcStanding()
            }.also {
                Log.d(TAG, "Got ${it.size} GC standings for $raceId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting GC standings: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get GC standing for a specific cyclist.
     */
    suspend fun getCyclistGcStanding(
        raceId: String,
        cyclistId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): GcStanding? {
        return try {
            val snapshot = firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection(COLLECTION_GC_STANDINGS)
                .whereEqualTo("raceId", raceId)
                .whereEqualTo("cyclistId", cyclistId)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toGcStanding()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cyclist GC standing: ${e.message}")
            null
        }
    }

    /**
     * Observe GC standings in real-time.
     */
    fun observeGcStandings(
        raceId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Flow<List<GcStanding>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_SEASONS)
            .document(season.toString())
            .collection(COLLECTION_GC_STANDINGS)
            .whereEqualTo("raceId", raceId)
            .orderBy("gcPosition", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing GC standings: ${error.message}")
                    return@addSnapshotListener
                }

                val standings = snapshot?.documents?.mapNotNull { it.toGcStanding() } ?: emptyList()
                trySend(standings)
            }

        awaitClose { listener.remove() }
    }

    // ==================== STAGE SCHEDULE (METADATA) ====================

    /**
     * Save all stages schedule for a race.
     * Path: seasons/{season}/races/{raceId}/stages/{stageNumber}
     */
    suspend fun saveStageSchedule(stages: List<Stage>): Result<Int> {
        if (stages.isEmpty()) return Result.success(0)

        return try {
            val season = stages.first().season
            val raceId = stages.first().raceId
            val batch = firestore.batch()

            stages.forEach { stage ->
                val data = mapOf(
                    "id" to stage.id,
                    "raceId" to stage.raceId,
                    "stageNumber" to stage.stageNumber,
                    "stageType" to stage.stageType.name,
                    "name" to stage.name,
                    "distance" to stage.distance,
                    "elevationGain" to stage.elevationGain,
                    "startLocation" to stage.startLocation,
                    "finishLocation" to stage.finishLocation,
                    "date" to stage.date,
                    "dateString" to stage.dateString,
                    "dayOfWeek" to stage.dayOfWeek,
                    "isRestDayAfter" to stage.isRestDayAfter,
                    "isProcessed" to stage.isProcessed,
                    "season" to stage.season
                )

                val docRef = firestore.collection(COLLECTION_SEASONS)
                    .document(season.toString())
                    .collection(COLLECTION_RACES)
                    .document(raceId)
                    .collection(COLLECTION_STAGES)
                    .document(stage.stageNumber.toString())

                batch.set(docRef, data)
            }

            batch.commit().await()
            Log.d(TAG, "Saved ${stages.size} stage schedules for race $raceId")
            Result.success(stages.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stage schedule: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all stages for a race.
     */
    suspend fun getStageSchedule(
        raceId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): List<Stage> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection(COLLECTION_RACES)
                .document(raceId)
                .collection(COLLECTION_STAGES)
                .orderBy("stageNumber", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toStage()
            }.also {
                Log.d(TAG, "Got ${it.size} stages for race $raceId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stage schedule: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get a single stage by number.
     */
    suspend fun getStage(
        raceId: String,
        stageNumber: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Stage? {
        return try {
            val doc = firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection(COLLECTION_RACES)
                .document(raceId)
                .collection(COLLECTION_STAGES)
                .document(stageNumber.toString())
                .get()
                .await()

            doc.toStage()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stage $stageNumber: ${e.message}")
            null
        }
    }

    /**
     * Mark a stage as processed after results are entered.
     */
    suspend fun markStageProcessed(
        raceId: String,
        stageNumber: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection(COLLECTION_RACES)
                .document(raceId)
                .collection(COLLECTION_STAGES)
                .document(stageNumber.toString())
                .update("isProcessed", true)
                .await()

            Log.d(TAG, "Marked stage $stageNumber as processed for race $raceId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking stage processed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete all stages for a race.
     */
    suspend fun deleteStageSchedule(
        raceId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection(COLLECTION_RACES)
                .document(raceId)
                .collection(COLLECTION_STAGES)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Log.d(TAG, "Deleted ${snapshot.size()} stages for race $raceId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting stage schedule: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== RACE STATUS UPDATE ====================

    /**
     * Update the current stage number for a race.
     */
    suspend fun updateRaceCurrentStage(
        raceId: String,
        currentStage: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection(COLLECTION_RACES)
                .document(raceId)
                .update("currentStage", currentStage)
                .await()

            Log.d(TAG, "Updated race $raceId current stage to $currentStage")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating race current stage: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Mark a race as completed.
     */
    suspend fun markRaceCompleted(
        raceId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .collection(COLLECTION_RACES)
                .document(raceId)
                .update(
                    mapOf(
                        "isActive" to false,
                        "isCompleted" to true,
                        "completedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "Marked race $raceId as completed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking race completed: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== HELPER EXTENSIONS ====================

    private fun com.google.firebase.firestore.DocumentSnapshot.toStageResult(): StageResult? {
        return try {
            StageResult(
                id = getString("id") ?: id,
                raceId = getString("raceId") ?: return null,
                stageNumber = getLong("stageNumber")?.toInt() ?: return null,
                stageType = getString("stageType")?.let { StageType.fromString(it) } ?: StageType.FLAT,
                cyclistId = getString("cyclistId") ?: return null,
                position = getLong("position")?.toInt(),
                points = getLong("points")?.toInt() ?: 0,
                jerseyBonus = getLong("jerseyBonus")?.toInt() ?: 0,
                isGcLeader = getBoolean("isGcLeader") ?: false,
                isMountainsLeader = getBoolean("isMountainsLeader") ?: false,
                isPointsLeader = getBoolean("isPointsLeader") ?: false,
                isYoungLeader = getBoolean("isYoungLeader") ?: false,
                time = getString("time") ?: "",
                status = getString("status") ?: "",
                timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
                season = getLong("season")?.toInt() ?: SeasonConfig.CURRENT_SEASON
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stage result: ${e.message}")
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toGcStanding(): GcStanding? {
        return try {
            GcStanding(
                id = getString("id") ?: id,
                raceId = getString("raceId") ?: return null,
                cyclistId = getString("cyclistId") ?: return null,
                gcPosition = getLong("gcPosition")?.toInt() ?: 0,
                gcTime = getString("gcTime") ?: "",
                gcGap = getString("gcGap") ?: "",
                totalPoints = getLong("totalPoints")?.toInt() ?: 0,
                stageWins = getLong("stageWins")?.toInt() ?: 0,
                stagePodiums = getLong("stagePodiums")?.toInt() ?: 0,
                lastUpdatedStage = getLong("lastUpdatedStage")?.toInt() ?: 0,
                isGcLeader = getBoolean("isGcLeader") ?: false,
                isMountainsLeader = getBoolean("isMountainsLeader") ?: false,
                isPointsLeader = getBoolean("isPointsLeader") ?: false,
                isYoungLeader = getBoolean("isYoungLeader") ?: false,
                timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
                season = getLong("season")?.toInt() ?: SeasonConfig.CURRENT_SEASON
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GC standing: ${e.message}")
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toStage(): Stage? {
        return try {
            Stage(
                id = getString("id") ?: id,
                raceId = getString("raceId") ?: return null,
                stageNumber = getLong("stageNumber")?.toInt() ?: return null,
                stageType = getString("stageType")?.let { StageType.fromString(it) } ?: StageType.FLAT,
                name = getString("name") ?: "",
                distance = getDouble("distance"),
                elevationGain = getLong("elevationGain")?.toInt(),
                startLocation = getString("startLocation") ?: "",
                finishLocation = getString("finishLocation") ?: "",
                date = getLong("date") ?: 0L,
                dateString = getString("dateString") ?: "",
                dayOfWeek = getString("dayOfWeek") ?: "",
                isRestDayAfter = getBoolean("isRestDayAfter") ?: false,
                isProcessed = getBoolean("isProcessed") ?: false,
                season = getLong("season")?.toInt() ?: SeasonConfig.CURRENT_SEASON
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stage: ${e.message}")
            null
        }
    }
}
