package com.ciclismo.portugal.data.repository

import android.util.Log
import com.ciclismo.portugal.data.local.dao.RaceDao
import com.ciclismo.portugal.data.local.entity.RaceEntity
import com.ciclismo.portugal.data.local.entity.RaceResultEntity
import com.ciclismo.portugal.data.local.entity.toDomain
import com.ciclismo.portugal.data.local.entity.toEntity
import com.ciclismo.portugal.data.remote.firebase.RaceFirestoreService
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceResult
import com.ciclismo.portugal.domain.repository.RaceRepository
import com.ciclismo.portugal.domain.scoring.PointsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RaceRepositoryImpl @Inject constructor(
    private val raceDao: RaceDao,
    private val raceFirestoreService: RaceFirestoreService
) : RaceRepository {

    companion object {
        private const val TAG = "RaceRepositoryImpl"
    }

    // ========== RACES ==========

    override fun getUpcomingRaces(): Flow<List<Race>> {
        // Combine local and Firestore data, prioritizing Firestore
        return raceFirestoreService.getUpcomingRaces()
    }

    override fun getActiveRaces(): Flow<List<Race>> {
        return raceFirestoreService.getActiveRaces()
    }

    override fun getNextUpcomingRaceForAi(): Flow<List<Race>> {
        return raceFirestoreService.getNextUpcomingRaceForAi()
    }

    override suspend fun getRacesForYear(year: Int): Result<List<Race>> {
        return raceFirestoreService.getRacesForYear(year)
    }

    override suspend fun getRaceById(raceId: String): Race? {
        // First try local database
        val localRace = raceDao.getRaceById(raceId)?.toDomain()
        if (localRace != null) {
            return localRace
        }

        // Fallback to Firestore
        Log.d(TAG, "Race not found locally, fetching from Firestore: $raceId")
        return raceFirestoreService.getRaceById(raceId)
    }

    // ========== TEAM FREEZE ==========

    override suspend fun isRaceHappeningToday(): Boolean {
        return getRaceHappeningToday() != null
    }

    override suspend fun getRaceHappeningToday(): Race? {
        val calendar = Calendar.getInstance()

        // Start of today (00:00:00)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        // End of today (23:59:59)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return raceDao.getRaceHappeningToday(startOfDay, endOfDay)?.toDomain()
    }

    override suspend fun getRacesStartingTomorrow(): List<Race> {
        val calendar = Calendar.getInstance()

        // Move to tomorrow
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        // Start of tomorrow (00:00:00)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val tomorrowStart = calendar.timeInMillis

        // End of tomorrow (23:59:59)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val tomorrowEnd = calendar.timeInMillis

        return raceDao.getRacesStartingTomorrow(tomorrowStart, tomorrowEnd).map { it.toDomain() }
    }

    override suspend fun syncRacesFromFirestore(): Result<Int> {
        return try {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val racesResult = raceFirestoreService.getRacesForYear(currentYear)

            racesResult.fold(
                onSuccess = { races ->
                    val entities = races.map { it.toEntity() }
                    raceDao.insertAllRaces(entities)
                    Log.d(TAG, "Synced ${entities.size} races from Firestore")
                    Result.success(entities.size)
                },
                onFailure = { error ->
                    Log.e(TAG, "Error syncing races", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing races from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadRace(race: Race): Result<Unit> {
        return raceFirestoreService.uploadRace(race)
    }

    override suspend fun uploadRaces(races: List<Race>): Result<Int> {
        return raceFirestoreService.uploadRaces(races)
    }

    override suspend fun setRaceActive(raceId: String, isActive: Boolean): Result<Unit> {
        return raceFirestoreService.setRaceActive(raceId, isActive)
    }

    override suspend fun setRaceFinished(raceId: String): Result<Unit> {
        return try {
            val finishedAt = System.currentTimeMillis()
            raceDao.setRaceFinished(raceId, isFinished = true, finishedAt = finishedAt)
            // Also update Firestore
            raceFirestoreService.setRaceFinished(raceId, finishedAt)
            Log.d(TAG, "Race $raceId marked as finished")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking race as finished", e)
            Result.failure(e)
        }
    }

    override suspend fun isRaceFinished(raceId: String): Boolean {
        return raceDao.isRaceFinished(raceId) ?: false
    }

    override suspend fun deleteRace(raceId: String): Result<Unit> {
        return raceFirestoreService.deleteRace(raceId)
    }

    // ========== RACE RESULTS ==========

    override fun getRaceResults(raceId: String): Flow<List<RaceResult>> {
        return raceFirestoreService.getRaceResults(raceId)
    }

    override suspend fun getRaceResultsOnce(raceId: String): List<RaceResult> {
        return raceFirestoreService.getRaceResultsOnce(raceId)
    }

    override suspend fun getCyclistResults(cyclistId: String): Result<List<RaceResult>> {
        return raceFirestoreService.getCyclistResults(cyclistId)
    }

    override suspend fun uploadRaceResult(result: RaceResult): Result<Unit> {
        return raceFirestoreService.uploadRaceResult(result)
    }

    // ========== FANTASY POINTS ==========

    override suspend fun calculateFantasyPointsForRace(raceId: String): Result<Map<String, Int>> {
        return try {
            val race = raceDao.getRaceById(raceId)?.toDomain()
                ?: return Result.failure(Exception("Race not found"))

            val results = raceFirestoreService.getRaceResults(raceId).first()

            // Calculate points for each cyclist
            val pointsMap = mutableMapOf<String, Int>()

            results.forEach { result ->
                val points = PointsCalculator.calculateResultPoints(result, race.type)
                val currentPoints = pointsMap.getOrDefault(result.cyclistId, 0)
                pointsMap[result.cyclistId] = currentPoints + points
            }

            Log.d(TAG, "Calculated points for ${pointsMap.size} cyclists in race ${race.name}")
            Result.success(pointsMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating fantasy points", e)
            Result.failure(e)
        }
    }
}
