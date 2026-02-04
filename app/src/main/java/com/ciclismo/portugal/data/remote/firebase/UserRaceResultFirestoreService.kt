package com.ciclismo.portugal.data.remote.firebase

import android.util.Log
import com.ciclismo.portugal.domain.model.UserRaceType
import com.ciclismo.portugal.domain.model.ResultSource
import com.ciclismo.portugal.domain.model.UserRaceResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore service for user's personal race results.
 * Stores results in cloud for cross-device sync and reinstall recovery.
 * Path: users/{userId}/race_results/{resultId}
 */
@Singleton
class UserRaceResultFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "UserRaceResultFirestore"
        private const val USERS_COLLECTION = "users"
        private const val RACE_RESULTS_COLLECTION = "race_results"
    }

    /**
     * Get the collection reference for a user's race results
     */
    private fun userResultsCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(RACE_RESULTS_COLLECTION)

    /**
     * Upload/save a single race result to Firestore
     */
    suspend fun saveResult(result: UserRaceResult): Result<Unit> {
        return try {
            val resultData = result.toFirestoreMap()

            userResultsCollection(result.userId)
                .document(result.id)
                .set(resultData)
                .await()

            Log.d(TAG, "Saved race result: ${result.raceName} for user ${result.userId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving race result", e)
            Result.failure(e)
        }
    }

    /**
     * Upload multiple race results at once (batch operation)
     */
    suspend fun saveResults(results: List<UserRaceResult>): Result<Int> {
        return try {
            if (results.isEmpty()) {
                return Result.success(0)
            }

            val batch = firestore.batch()

            results.forEach { result ->
                val docRef = userResultsCollection(result.userId).document(result.id)
                batch.set(docRef, result.toFirestoreMap())
            }

            batch.commit().await()

            Log.d(TAG, "Saved ${results.size} race results to Firestore")
            Result.success(results.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving race results batch", e)
            Result.failure(e)
        }
    }

    /**
     * Get all race results for a user
     */
    suspend fun getResultsForUser(userId: String): List<UserRaceResult> {
        return try {
            val snapshot = userResultsCollection(userId)
                .orderBy("raceDate", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toUserRaceResult()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing race result: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching race results for user: $userId", e)
            emptyList()
        }
    }

    /**
     * Get recent race results for a user
     */
    suspend fun getRecentResultsForUser(userId: String, limit: Int = 10): List<UserRaceResult> {
        return try {
            val snapshot = userResultsCollection(userId)
                .orderBy("raceDate", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toUserRaceResult()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing race result: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recent race results for user: $userId", e)
            emptyList()
        }
    }

    /**
     * Get a specific race result by ID
     */
    suspend fun getResultById(userId: String, resultId: String): UserRaceResult? {
        return try {
            val doc = userResultsCollection(userId)
                .document(resultId)
                .get()
                .await()

            if (doc.exists()) {
                doc.toUserRaceResult()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching race result: $resultId", e)
            null
        }
    }

    /**
     * Delete a race result
     */
    suspend fun deleteResult(userId: String, resultId: String): Result<Unit> {
        return try {
            userResultsCollection(userId)
                .document(resultId)
                .delete()
                .await()

            Log.d(TAG, "Deleted race result: $resultId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting race result", e)
            Result.failure(e)
        }
    }

    /**
     * Delete all race results for a user
     */
    suspend fun deleteAllResultsForUser(userId: String): Result<Unit> {
        return try {
            val snapshot = userResultsCollection(userId)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Log.d(TAG, "Deleted ${snapshot.documents.size} results for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all results for user", e)
            Result.failure(e)
        }
    }

    /**
     * Get results filtered by race type
     */
    suspend fun getResultsByType(userId: String, raceType: UserRaceType): List<UserRaceResult> {
        return try {
            val snapshot = userResultsCollection(userId)
                .whereEqualTo("raceType", raceType.name)
                .orderBy("raceDate", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toUserRaceResult()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing race result: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching results by type for user: $userId", e)
            emptyList()
        }
    }

    /**
     * Get user statistics
     */
    suspend fun getUserStats(userId: String): UserRaceStats {
        return try {
            val results = getResultsForUser(userId)

            UserRaceStats(
                totalRaces = results.size,
                totalWins = results.count { it.position == 1 },
                totalPodiums = results.count { it.position != null && it.position <= 3 },
                totalDistance = results.sumOf { it.distance?.toDouble() ?: 0.0 }.toFloat(),
                totalElevation = results.sumOf { it.elevation ?: 0 },
                averagePosition = results.mapNotNull { it.position }.average().takeIf { !it.isNaN() }?.toFloat()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating user stats", e)
            UserRaceStats()
        }
    }

    // Helper extension functions
    private fun UserRaceResult.toFirestoreMap(): Map<String, Any?> = hashMapOf(
        "userId" to userId,
        "raceName" to raceName,
        "raceDate" to raceDate,
        "raceLocation" to raceLocation,
        "raceType" to raceType.name,
        "distance" to distance,
        "elevation" to elevation,
        "bibNumber" to bibNumber,
        "position" to position,
        "totalParticipants" to totalParticipants,
        "categoryPosition" to categoryPosition,
        "categoryTotalParticipants" to categoryTotalParticipants,
        "category" to category,
        "finishTime" to finishTime,
        "avgSpeed" to avgSpeed,
        "resultSource" to resultSource.name,
        "sourceUrl" to sourceUrl,
        "eventUrl" to eventUrl,
        "organizerName" to organizerName,
        "notes" to notes,
        "timestamp" to timestamp
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserRaceResult(): UserRaceResult {
        return UserRaceResult(
            id = id,
            userId = getString("userId") ?: "",
            raceName = getString("raceName") ?: "",
            raceDate = getLong("raceDate") ?: 0L,
            raceLocation = getString("raceLocation") ?: "",
            raceType = try {
                UserRaceType.valueOf(getString("raceType") ?: "OTHER")
            } catch (e: Exception) {
                UserRaceType.OTHER
            },
            distance = getDouble("distance")?.toFloat(),
            elevation = getLong("elevation")?.toInt(),
            bibNumber = getLong("bibNumber")?.toInt(),
            position = getLong("position")?.toInt(),
            totalParticipants = getLong("totalParticipants")?.toInt(),
            categoryPosition = getLong("categoryPosition")?.toInt(),
            categoryTotalParticipants = getLong("categoryTotalParticipants")?.toInt(),
            category = getString("category"),
            finishTime = getString("finishTime"),
            avgSpeed = getDouble("avgSpeed")?.toFloat(),
            resultSource = try {
                ResultSource.valueOf(getString("resultSource") ?: "MANUAL")
            } catch (e: Exception) {
                ResultSource.MANUAL
            },
            sourceUrl = getString("sourceUrl"),
            eventUrl = getString("eventUrl"),
            organizerName = getString("organizerName"),
            notes = getString("notes"),
            timestamp = getLong("timestamp") ?: System.currentTimeMillis()
        )
    }
}

/**
 * User race statistics
 */
data class UserRaceStats(
    val totalRaces: Int = 0,
    val totalWins: Int = 0,
    val totalPodiums: Int = 0,
    val totalDistance: Float = 0f,
    val totalElevation: Int = 0,
    val averagePosition: Float? = null
)
