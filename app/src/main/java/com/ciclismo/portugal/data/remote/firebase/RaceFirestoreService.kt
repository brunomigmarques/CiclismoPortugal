package com.ciclismo.portugal.data.remote.firebase

import android.util.Log
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceResult
import com.ciclismo.portugal.domain.model.RaceType
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RaceFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "RaceFirestoreService"
        private const val COLLECTION_SEASONS = "seasons"
        private const val RACES_COLLECTION = "races"
        private const val RACE_RESULTS_COLLECTION = "race_results"
    }

    /**
     * Get the collection path for races in a specific season
     * Pattern: seasons/{season}/races
     */
    private fun racesCollection(season: Int = SeasonConfig.CURRENT_SEASON) =
        firestore.collection(COLLECTION_SEASONS)
            .document(season.toString())
            .collection(RACES_COLLECTION)

    /**
     * Get the collection path for race results in a specific season
     * Pattern: seasons/{season}/race_results
     */
    private fun raceResultsCollection(season: Int = SeasonConfig.CURRENT_SEASON) =
        firestore.collection(COLLECTION_SEASONS)
            .document(season.toString())
            .collection(RACE_RESULTS_COLLECTION)

    /**
     * Get a single race by ID from Firestore
     * @param season the season to get the race from (defaults to current season)
     */
    suspend fun getRaceById(raceId: String, season: Int = SeasonConfig.CURRENT_SEASON): Race? {
        return try {
            val doc = racesCollection(season)
                .document(raceId)
                .get()
                .await()

            if (doc.exists()) {
                Race(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    type = RaceType.valueOf(doc.getString("type") ?: "ONE_DAY"),
                    startDate = doc.getLong("startDate") ?: 0L,
                    endDate = doc.getLong("endDate"),
                    stages = doc.getLong("stages")?.toInt() ?: 1,
                    country = doc.getString("country") ?: "",
                    category = doc.getString("category") ?: "WT",
                    isActive = doc.getBoolean("isActive") ?: false,
                    isFinished = doc.getBoolean("isFinished") ?: false,
                    finishedAt = doc.getLong("finishedAt"),
                    profileUrl = doc.getString("profileUrl"),
                    season = doc.getLong("season")?.toInt() ?: season
                )
            } else {
                Log.w(TAG, "Race not found: $raceId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching race by ID: $raceId", e)
            null
        }
    }

    /**
     * Get all upcoming races from Firestore (for Fantasy)
     * Includes races happening today (uses start of today, not current time)
     * @param season the season to get races from (defaults to current season)
     */
    fun getUpcomingRaces(season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<Race>> = callbackFlow {
        // Use start of today to include races happening today
        val startOfToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val listener = racesCollection(season)
            .whereGreaterThanOrEqualTo("startDate", startOfToday)
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to races", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val races = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        // Skip finished races - they shouldn't appear in upcoming
                        val isFinished = doc.getBoolean("isFinished") ?: false
                        if (isFinished) return@mapNotNull null

                        Race(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            type = RaceType.valueOf(doc.getString("type") ?: "ONE_DAY"),
                            startDate = doc.getLong("startDate") ?: 0L,
                            endDate = doc.getLong("endDate"),
                            stages = doc.getLong("stages")?.toInt() ?: 1,
                            country = doc.getString("country") ?: "",
                            category = doc.getString("category") ?: "WT",
                            isActive = doc.getBoolean("isActive") ?: false,
                            isFinished = false,
                            finishedAt = doc.getLong("finishedAt"),
                            profileUrl = doc.getString("profileUrl"),
                            season = doc.getLong("season")?.toInt() ?: season
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing race: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Received ${races.size} upcoming races from Firestore (excluding finished)")
                trySend(races)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get active races (currently happening)
     * A race is considered active if:
     * 1. isActive flag is explicitly true, OR
     * 2. The race is within its date range (startDate <= today <= endDate) and not finished
     * @param season the season to get races from (defaults to current season)
     */
    fun getActiveRaces(season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<Race>> = callbackFlow {
        // Get start of today for comparison
        val startOfToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        // End of today for comparison
        val endOfToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Query races that started on or before today
        val listener = racesCollection(season)
            .whereLessThanOrEqualTo("startDate", endOfToday)
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to active races", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val races = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val startDate = doc.getLong("startDate") ?: 0L
                        val endDate = doc.getLong("endDate")
                        val isActive = doc.getBoolean("isActive") ?: false
                        val isFinished = doc.getBoolean("isFinished") ?: false

                        // Skip finished races
                        if (isFinished) return@mapNotNull null

                        // Check if race is active:
                        // 1. Explicitly marked as active, OR
                        // 2. Within date range (startDate <= today <= endDate)
                        val isWithinDateRange = if (endDate != null) {
                            startDate <= endOfToday && endDate >= startOfToday
                        } else {
                            // One-day race: check if startDate is today
                            startDate >= startOfToday && startDate <= endOfToday
                        }

                        if (!isActive && !isWithinDateRange) return@mapNotNull null

                        Race(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            type = RaceType.valueOf(doc.getString("type") ?: "ONE_DAY"),
                            startDate = startDate,
                            endDate = endDate,
                            stages = doc.getLong("stages")?.toInt() ?: 1,
                            country = doc.getString("country") ?: "",
                            category = doc.getString("category") ?: "WT",
                            isActive = isActive || isWithinDateRange,
                            isFinished = false,
                            finishedAt = doc.getLong("finishedAt"),
                            profileUrl = doc.getString("profileUrl"),
                            season = doc.getLong("season")?.toInt() ?: season
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing race: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Found ${races.size} active races (by date range or isActive flag)")
                trySend(races)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get the next upcoming race for AI recommendations.
     * Returns races that are either:
     * - Currently in progress (started but not finished)
     * - Starting within the next 90 days (extended from 30 for better coverage)
     * Sorted by start date, so the most imminent race is first.
     */
    fun getNextUpcomingRaceForAi(season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<Race>> = callbackFlow {
        val startOfToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Look 90 days ahead for upcoming races (extended from 30 for better coverage)
        val ninetyDaysFromNow = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 90)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis

        Log.d(TAG, "getNextUpcomingRaceForAi: Querying season=$season, startOfToday=$startOfToday, ninetyDaysFromNow=$ninetyDaysFromNow")

        val listener = racesCollection(season)
            .whereLessThanOrEqualTo("startDate", ninetyDaysFromNow)
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to upcoming races for AI: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val totalDocs = snapshot?.documents?.size ?: 0
                Log.d(TAG, "getNextUpcomingRaceForAi: Firestore returned $totalDocs documents")

                val races = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val startDate = doc.getLong("startDate") ?: 0L
                        val endDate = doc.getLong("endDate")
                        val isFinished = doc.getBoolean("isFinished") ?: false
                        val raceName = doc.getString("name") ?: "Unknown"
                        val raceType = try { RaceType.valueOf(doc.getString("type") ?: "ONE_DAY") } catch (e: Exception) { RaceType.ONE_DAY }

                        // Skip explicitly finished races
                        if (isFinished) {
                            Log.d(TAG, "getNextUpcomingRaceForAi: Skipping finished race: $raceName")
                            return@mapNotNull null
                        }

                        // Calculate race end date for filtering
                        // For one-day races without endDate, the race ends on the start day
                        // For stage races, use endDate if available, otherwise estimate based on stages
                        val effectiveEndDate = endDate ?: when (raceType) {
                            RaceType.ONE_DAY -> startDate + (24 * 60 * 60 * 1000) // End of start day
                            RaceType.STAGE_RACE -> {
                                val stages = doc.getLong("stages")?.toInt() ?: 1
                                startDate + (stages * 24 * 60 * 60 * 1000L)
                            }
                            RaceType.GRAND_TOUR -> startDate + (21 * 24 * 60 * 60 * 1000L) // ~21 days
                        }

                        // Include race if it hasn't ended yet (effectiveEndDate >= today)
                        val hasNotEnded = effectiveEndDate >= startOfToday

                        if (!hasNotEnded) {
                            Log.d(TAG, "getNextUpcomingRaceForAi: Skipping ended race: $raceName (startDate=$startDate, effectiveEndDate=$effectiveEndDate, today=$startOfToday)")
                            return@mapNotNull null
                        }

                        // Also skip races that started more than 3 days ago and are not stage races
                        // (defensive check for one-day races not properly marked as finished)
                        val threeDaysAgo = startOfToday - (3 * 24 * 60 * 60 * 1000L)
                        if (raceType == RaceType.ONE_DAY && startDate < threeDaysAgo) {
                            Log.d(TAG, "getNextUpcomingRaceForAi: Skipping old one-day race: $raceName (started more than 3 days ago)")
                            return@mapNotNull null
                        }

                        Log.d(TAG, "getNextUpcomingRaceForAi: Including race: $raceName (startDate=$startDate, type=$raceType)")

                        Race(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            type = RaceType.valueOf(doc.getString("type") ?: "ONE_DAY"),
                            startDate = startDate,
                            endDate = endDate,
                            stages = doc.getLong("stages")?.toInt() ?: 1,
                            country = doc.getString("country") ?: "",
                            category = doc.getString("category") ?: "WT",
                            isActive = doc.getBoolean("isActive") ?: false,
                            isFinished = false,
                            finishedAt = doc.getLong("finishedAt"),
                            profileUrl = doc.getString("profileUrl"),
                            season = doc.getLong("season")?.toInt() ?: season
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing race for AI: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Found ${races.size} upcoming races for AI (next 90 days, not finished)")
                trySend(races)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get all races for a specific year/season
     * @param year the year (season) to get races for
     */
    suspend fun getRacesForYear(year: Int): Result<List<Race>> {
        return try {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, 0, 1, 0, 0, 0)
            val startOfYear = calendar.timeInMillis
            calendar.set(year, 11, 31, 23, 59, 59)
            val endOfYear = calendar.timeInMillis

            val snapshot = racesCollection(year)
                .whereGreaterThanOrEqualTo("startDate", startOfYear)
                .whereLessThanOrEqualTo("startDate", endOfYear)
                .orderBy("startDate", Query.Direction.ASCENDING)
                .get()
                .await()

            val races = snapshot.documents.mapNotNull { doc ->
                try {
                    Race(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        type = RaceType.valueOf(doc.getString("type") ?: "ONE_DAY"),
                        startDate = doc.getLong("startDate") ?: 0L,
                        endDate = doc.getLong("endDate"),
                        stages = doc.getLong("stages")?.toInt() ?: 1,
                        country = doc.getString("country") ?: "",
                        category = doc.getString("category") ?: "WT",
                        isActive = doc.getBoolean("isActive") ?: false,
                        profileUrl = doc.getString("profileUrl"),
                        season = doc.getLong("season")?.toInt() ?: year
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing race: ${doc.id}", e)
                    null
                }
            }

            Result.success(races)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching races for year $year", e)
            Result.failure(e)
        }
    }

    /**
     * Upload a race to Firestore (admin only)
     * @param season the season to upload the race to (defaults to current season)
     */
    suspend fun uploadRace(race: Race, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            val raceData = hashMapOf(
                "name" to race.name,
                "type" to race.type.name,
                "startDate" to race.startDate,
                "endDate" to race.endDate,
                "stages" to race.stages,
                "country" to race.country,
                "category" to race.category,
                "isActive" to race.isActive,
                "isFinished" to race.isFinished,
                "finishedAt" to race.finishedAt,
                "profileUrl" to race.profileUrl,
                "updatedAt" to System.currentTimeMillis(),
                "season" to season
            )

            racesCollection(season)
                .document(race.id)
                .set(raceData)
                .await()

            Log.d(TAG, "Race uploaded: ${race.name} (season $season)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading race: ${race.name}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload multiple races to Firestore (admin only)
     * @param season the season to upload races to (defaults to current season)
     */
    suspend fun uploadRaces(races: List<Race>, season: Int = SeasonConfig.CURRENT_SEASON): Result<Int> {
        return try {
            var count = 0
            races.forEach { race ->
                uploadRace(race, season).onSuccess { count++ }
            }
            Log.d(TAG, "Uploaded $count races to Firestore (season $season)")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading races", e)
            Result.failure(e)
        }
    }

    /**
     * Set race as active/inactive
     * @param season the season the race belongs to (defaults to current season)
     */
    suspend fun setRaceActive(raceId: String, isActive: Boolean, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            racesCollection(season)
                .document(raceId)
                .update("isActive", isActive)
                .await()

            Log.d(TAG, "Race $raceId set active: $isActive")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating race active status", e)
            Result.failure(e)
        }
    }

    /**
     * Mark race as finished (results processed)
     * @param season the season the race belongs to (defaults to current season)
     */
    suspend fun setRaceFinished(raceId: String, finishedAt: Long, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            racesCollection(season)
                .document(raceId)
                .update(
                    mapOf(
                        "isFinished" to true,
                        "finishedAt" to finishedAt,
                        "isActive" to false
                    )
                )
                .await()

            Log.d(TAG, "Race $raceId marked as finished")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking race as finished", e)
            Result.failure(e)
        }
    }

    // ========== RACE RESULTS ==========

    /**
     * Get results for a specific race
     * Note: Using simple query without orderBy to avoid composite index requirement
     * @param season the season to get results from (defaults to current season)
     */
    fun getRaceResults(raceId: String, season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<RaceResult>> = callbackFlow {
        val listener = raceResultsCollection(season)
            .whereEqualTo("raceId", raceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to race results", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val results = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        RaceResult(
                            id = doc.id,
                            raceId = doc.getString("raceId") ?: "",
                            cyclistId = doc.getString("cyclistId") ?: "",
                            stageNumber = doc.getLong("stageNumber")?.toInt(),
                            position = doc.getLong("position")?.toInt(),
                            points = doc.getLong("points")?.toInt() ?: 0,
                            bonusPoints = doc.getLong("bonusPoints")?.toInt() ?: 0,
                            isGcLeader = doc.getBoolean("isGcLeader") ?: false,
                            isMountainsLeader = doc.getBoolean("isMountainsLeader") ?: false,
                            isPointsLeader = doc.getBoolean("isPointsLeader") ?: false,
                            isYoungLeader = doc.getBoolean("isYoungLeader") ?: false,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            season = doc.getLong("season")?.toInt() ?: season
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing result: ${doc.id}", e)
                        null
                    }
                }?.sortedWith(compareBy({ it.stageNumber ?: 0 }, { it.position ?: 999 })) ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get results for a specific race (one-time read, no listener)
     * @param season the season to get results from (defaults to current season)
     */
    suspend fun getRaceResultsOnce(raceId: String, season: Int = SeasonConfig.CURRENT_SEASON): List<RaceResult> {
        return try {
            val snapshot = raceResultsCollection(season)
                .whereEqualTo("raceId", raceId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    RaceResult(
                        id = doc.id,
                        raceId = doc.getString("raceId") ?: "",
                        cyclistId = doc.getString("cyclistId") ?: "",
                        stageNumber = doc.getLong("stageNumber")?.toInt(),
                        position = doc.getLong("position")?.toInt(),
                        points = doc.getLong("points")?.toInt() ?: 0,
                        bonusPoints = doc.getLong("bonusPoints")?.toInt() ?: 0,
                        isGcLeader = doc.getBoolean("isGcLeader") ?: false,
                        isMountainsLeader = doc.getBoolean("isMountainsLeader") ?: false,
                        isPointsLeader = doc.getBoolean("isPointsLeader") ?: false,
                        isYoungLeader = doc.getBoolean("isYoungLeader") ?: false,
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        season = doc.getLong("season")?.toInt() ?: season
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing result: ${doc.id}", e)
                    null
                }
            }.sortedWith(compareBy({ it.stageNumber ?: 0 }, { it.position ?: 999 }))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching race results: $raceId", e)
            emptyList()
        }
    }

    /**
     * Get results for a specific cyclist
     * Note: Using simple query without orderBy to avoid composite index requirement
     * @param season the season to get results from (defaults to current season)
     */
    suspend fun getCyclistResults(cyclistId: String, season: Int = SeasonConfig.CURRENT_SEASON): Result<List<RaceResult>> {
        return try {
            val snapshot = raceResultsCollection(season)
                .whereEqualTo("cyclistId", cyclistId)
                .get()
                .await()

            val results = snapshot.documents.mapNotNull { doc ->
                try {
                    RaceResult(
                        id = doc.id,
                        raceId = doc.getString("raceId") ?: "",
                        cyclistId = doc.getString("cyclistId") ?: "",
                        stageNumber = doc.getLong("stageNumber")?.toInt(),
                        position = doc.getLong("position")?.toInt(),
                        points = doc.getLong("points")?.toInt() ?: 0,
                        bonusPoints = doc.getLong("bonusPoints")?.toInt() ?: 0,
                        isGcLeader = doc.getBoolean("isGcLeader") ?: false,
                        isMountainsLeader = doc.getBoolean("isMountainsLeader") ?: false,
                        isPointsLeader = doc.getBoolean("isPointsLeader") ?: false,
                        isYoungLeader = doc.getBoolean("isYoungLeader") ?: false,
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        season = doc.getLong("season")?.toInt() ?: season
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.timestamp }

            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cyclist results", e)
            Result.failure(e)
        }
    }

    /**
     * Upload race result (admin only)
     * @param season the season to upload result to (defaults to current season)
     */
    suspend fun uploadRaceResult(result: RaceResult, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            val resultData = hashMapOf(
                "raceId" to result.raceId,
                "cyclistId" to result.cyclistId,
                "stageNumber" to result.stageNumber,
                "position" to result.position,
                "points" to result.points,
                "bonusPoints" to result.bonusPoints,
                "isGcLeader" to result.isGcLeader,
                "isMountainsLeader" to result.isMountainsLeader,
                "isPointsLeader" to result.isPointsLeader,
                "isYoungLeader" to result.isYoungLeader,
                "timestamp" to result.timestamp,
                "season" to season
            )

            raceResultsCollection(season)
                .document(result.id)
                .set(resultData)
                .await()

            Log.d(TAG, "Race result uploaded: ${result.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading race result", e)
            Result.failure(e)
        }
    }

    /**
     * Upload multiple race results at once (admin only - batch operation)
     * Used when applying race results from admin sync
     * @param season the season to upload results to (defaults to current season)
     */
    suspend fun uploadRaceResults(results: List<RaceResult>, season: Int = SeasonConfig.CURRENT_SEASON): Result<Int> {
        return try {
            if (results.isEmpty()) {
                return Result.success(0)
            }

            val batch = firestore.batch()

            results.forEach { result ->
                val docRef = raceResultsCollection(season).document(result.id)
                val resultData = hashMapOf(
                    "raceId" to result.raceId,
                    "cyclistId" to result.cyclistId,
                    "stageNumber" to result.stageNumber,
                    "position" to result.position,
                    "points" to result.points,
                    "bonusPoints" to result.bonusPoints,
                    "isGcLeader" to result.isGcLeader,
                    "isMountainsLeader" to result.isMountainsLeader,
                    "isPointsLeader" to result.isPointsLeader,
                    "isYoungLeader" to result.isYoungLeader,
                    "timestamp" to result.timestamp,
                    "season" to season
                )
                batch.set(docRef, resultData)
            }

            batch.commit().await()

            Log.d(TAG, "Uploaded ${results.size} race results to Firestore (season $season)")
            Result.success(results.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading race results batch", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a race and all its results
     * @param season the season the race belongs to (defaults to current season)
     */
    suspend fun deleteRace(raceId: String, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            // Delete all results for this race
            val resultsSnapshot = raceResultsCollection(season)
                .whereEqualTo("raceId", raceId)
                .get()
                .await()

            resultsSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            // Delete the race
            racesCollection(season)
                .document(raceId)
                .delete()
                .await()

            Log.d(TAG, "Race deleted: $raceId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting race", e)
            Result.failure(e)
        }
    }
}
