package com.ciclismo.portugal.data.repository

import android.util.Log
import com.ciclismo.portugal.data.local.dao.UserRaceResultDao
import com.ciclismo.portugal.data.local.entity.toDomain
import com.ciclismo.portugal.data.local.entity.toEntity
import com.ciclismo.portugal.data.remote.firebase.AuthService
import com.ciclismo.portugal.data.remote.firebase.UserRaceResultFirestoreService
import com.ciclismo.portugal.data.remote.firebase.UserRaceStats
import com.ciclismo.portugal.domain.model.UserRaceType
import com.ciclismo.portugal.domain.model.UserRaceResult
import com.ciclismo.portugal.domain.repository.UserRaceResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRaceResultRepositoryImpl @Inject constructor(
    private val userRaceResultDao: UserRaceResultDao,
    private val firestoreService: UserRaceResultFirestoreService,
    private val authService: AuthService
) : UserRaceResultRepository {

    companion object {
        private const val TAG = "UserRaceResultRepo"
    }

    private val currentUserId: String?
        get() = authService.getCurrentUser()?.id

    override fun getResults(): Flow<List<UserRaceResult>> {
        val userId = currentUserId ?: return emptyFlow()
        return userRaceResultDao.getResultsForUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentResults(limit: Int): Flow<List<UserRaceResult>> {
        val userId = currentUserId ?: return emptyFlow()
        return userRaceResultDao.getRecentResultsForUser(userId, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getResultById(id: String): UserRaceResult? {
        return userRaceResultDao.getResultById(id)?.toDomain()
    }

    override fun getResultsByType(raceType: UserRaceType): Flow<List<UserRaceResult>> {
        val userId = currentUserId ?: return emptyFlow()
        return userRaceResultDao.getResultsByType(userId, raceType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getResultsInDateRange(startDate: Long, endDate: Long): Flow<List<UserRaceResult>> {
        val userId = currentUserId ?: return emptyFlow()
        return userRaceResultDao.getResultsInDateRange(userId, startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveResult(result: UserRaceResult): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = currentUserId ?: return@withContext Result.failure(
            IllegalStateException("User not logged in")
        )

        try {
            // Create result with current user ID
            val resultWithUser = result.copy(userId = userId)

            // Save locally first
            userRaceResultDao.insert(resultWithUser.toEntity())
            Log.d(TAG, "Saved result locally: ${result.raceName}")

            // Then sync to Firebase
            val firebaseResult = firestoreService.saveResult(resultWithUser)
            if (firebaseResult.isFailure) {
                Log.w(TAG, "Failed to sync result to Firebase, will retry later")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving result", e)
            Result.failure(e)
        }
    }

    override suspend fun updateResult(result: UserRaceResult): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Update locally
            userRaceResultDao.update(result.toEntity())
            Log.d(TAG, "Updated result locally: ${result.raceName}")

            // Sync to Firebase
            val firebaseResult = firestoreService.saveResult(result)
            if (firebaseResult.isFailure) {
                Log.w(TAG, "Failed to sync updated result to Firebase")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating result", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteResult(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = currentUserId ?: return@withContext Result.failure(
            IllegalStateException("User not logged in")
        )

        try {
            // Delete locally
            userRaceResultDao.deleteById(id)
            Log.d(TAG, "Deleted result locally: $id")

            // Delete from Firebase
            val firebaseResult = firestoreService.deleteResult(userId, id)
            if (firebaseResult.isFailure) {
                Log.w(TAG, "Failed to delete result from Firebase")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting result", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserStats(): UserRaceStats = withContext(Dispatchers.IO) {
        val userId = currentUserId ?: return@withContext UserRaceStats()

        try {
            UserRaceStats(
                totalRaces = userRaceResultDao.getResultCount(userId),
                totalWins = userRaceResultDao.getWinCount(userId),
                totalPodiums = userRaceResultDao.getPodiumCount(userId),
                totalDistance = userRaceResultDao.getTotalDistance(userId) ?: 0f,
                totalElevation = userRaceResultDao.getTotalElevation(userId) ?: 0,
                averagePosition = userRaceResultDao.getAveragePosition(userId)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user stats", e)
            UserRaceStats()
        }
    }

    override suspend fun syncWithFirebase(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = currentUserId ?: return@withContext Result.failure(
            IllegalStateException("User not logged in")
        )

        try {
            // Get results from Firebase
            val firebaseResults = firestoreService.getResultsForUser(userId)
            Log.d(TAG, "Found ${firebaseResults.size} results in Firebase")

            // Insert/update in local database
            firebaseResults.forEach { result ->
                userRaceResultDao.insert(result.toEntity())
            }

            Log.d(TAG, "Synced ${firebaseResults.size} results from Firebase")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with Firebase", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllRaceNames(): List<String> = withContext(Dispatchers.IO) {
        try {
            userRaceResultDao.getAllRaceNames()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting race names", e)
            emptyList()
        }
    }

    override suspend fun getAllLocations(): List<String> = withContext(Dispatchers.IO) {
        try {
            userRaceResultDao.getAllLocations()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting locations", e)
            emptyList()
        }
    }

    override suspend fun getAllCategories(): List<String> = withContext(Dispatchers.IO) {
        try {
            userRaceResultDao.getAllCategories()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting categories", e)
            emptyList()
        }
    }
}
