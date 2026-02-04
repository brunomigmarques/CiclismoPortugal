package com.ciclismo.portugal.data.remote.firebase

import android.util.Log
import com.ciclismo.portugal.domain.model.CyclingType
import com.ciclismo.portugal.domain.model.ExperienceLevel
import com.ciclismo.portugal.domain.model.Region
import com.ciclismo.portugal.domain.model.UserGoal
import com.ciclismo.portugal.domain.model.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing user preferences to Firestore.
 * This enables personalized rankings and filtering based on user location and preferences.
 *
 * Firestore structure:
 * users/{userId}/
 *   preferences: {
 *     cyclingTypes: ["ROAD", "GRAVEL"]
 *     experienceLevel: "COMPETITIVE"
 *     region: "CENTRO"
 *     country: "PT"
 *     goals: ["FITNESS", "COMPETE"]
 *     updatedAt: timestamp
 *   }
 */
@Singleton
class UserPreferencesFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "UserPrefsFirestore"
        private const val USERS_COLLECTION = "users"
        private const val PREFERENCES_FIELD = "preferences"
    }

    /**
     * Save user preferences to Firestore.
     * Used for personalized rankings and regional filtering.
     */
    suspend fun saveUserPreferences(
        userId: String,
        preferences: UserPreferences,
        country: String = "PT"
    ): Result<Unit> {
        return try {
            val preferencesData = mapOf(
                "cyclingTypes" to preferences.cyclingTypes.map { it.name },
                "experienceLevel" to preferences.experienceLevel.name,
                "region" to preferences.favoriteRegion.name,
                "country" to country,
                "goals" to preferences.goals.map { it.name },
                "updatedAt" to System.currentTimeMillis()
            )

            val userData = mapOf(PREFERENCES_FIELD to preferencesData)

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userData, SetOptions.merge())
                .await()

            Log.d(TAG, "Saved preferences for user $userId: region=${preferences.favoriteRegion}, types=${preferences.cyclingTypes}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving preferences for user $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get user preferences from Firestore.
     */
    suspend fun getUserPreferences(userId: String): Result<UserPreferences?> {
        return try {
            val doc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.success(null)
            }

            @Suppress("UNCHECKED_CAST")
            val prefsData = doc.get(PREFERENCES_FIELD) as? Map<String, Any>
                ?: return Result.success(null)

            val cyclingTypesRaw = prefsData["cyclingTypes"] as? List<String> ?: listOf("ALL")
            val cyclingTypes = cyclingTypesRaw.mapNotNull {
                runCatching { CyclingType.valueOf(it) }.getOrNull()
            }.toSet().ifEmpty { setOf(CyclingType.ALL) }

            val experienceLevel = (prefsData["experienceLevel"] as? String)?.let {
                runCatching { ExperienceLevel.valueOf(it) }.getOrNull()
            } ?: ExperienceLevel.RECREATIONAL

            val region = (prefsData["region"] as? String)?.let {
                runCatching { Region.valueOf(it) }.getOrNull()
            } ?: Region.ALL

            val goalsRaw = prefsData["goals"] as? List<String> ?: emptyList()
            val goals = goalsRaw.mapNotNull {
                runCatching { UserGoal.valueOf(it) }.getOrNull()
            }.toSet()

            Result.success(
                UserPreferences(
                    cyclingTypes = cyclingTypes,
                    experienceLevel = experienceLevel,
                    favoriteRegion = region,
                    goals = goals,
                    hasCompletedOnboarding = true
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting preferences for user $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get users by region for regional rankings.
     */
    suspend fun getUsersByRegion(region: Region): Result<List<String>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("$PREFERENCES_FIELD.region", region.name)
                .get()
                .await()

            val userIds = snapshot.documents.map { it.id }
            Log.d(TAG, "Found ${userIds.size} users in region ${region.name}")
            Result.success(userIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting users by region: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get users by cycling type for filtered rankings.
     */
    suspend fun getUsersByCyclingType(cyclingType: CyclingType): Result<List<String>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereArrayContains("$PREFERENCES_FIELD.cyclingTypes", cyclingType.name)
                .get()
                .await()

            val userIds = snapshot.documents.map { it.id }
            Log.d(TAG, "Found ${userIds.size} users with cycling type ${cyclingType.name}")
            Result.success(userIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting users by cycling type: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update only the region preference.
     */
    suspend fun updateRegion(userId: String, region: Region): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                    "$PREFERENCES_FIELD.region", region.name,
                    "$PREFERENCES_FIELD.updatedAt", System.currentTimeMillis()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating region: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update only cycling types preference.
     */
    suspend fun updateCyclingTypes(userId: String, cyclingTypes: Set<CyclingType>): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                    "$PREFERENCES_FIELD.cyclingTypes", cyclingTypes.map { it.name },
                    "$PREFERENCES_FIELD.updatedAt", System.currentTimeMillis()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cycling types: ${e.message}", e)
            Result.failure(e)
        }
    }
}
