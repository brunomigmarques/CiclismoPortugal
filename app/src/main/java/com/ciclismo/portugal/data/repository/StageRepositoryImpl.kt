package com.ciclismo.portugal.data.repository

import android.util.Log
import com.ciclismo.portugal.data.local.dao.StageResultDao
import com.ciclismo.portugal.data.local.entity.GcStandingEntity
import com.ciclismo.portugal.data.local.entity.StageResultEntity
import com.ciclismo.portugal.data.remote.firebase.StageFirestoreService
import com.ciclismo.portugal.domain.model.GcStanding
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.model.StageResult
import com.ciclismo.portugal.domain.repository.JerseyHolders
import com.ciclismo.portugal.domain.repository.StageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StageRepository that syncs between Room and Firebase.
 */
@Singleton
class StageRepositoryImpl @Inject constructor(
    private val stageResultDao: StageResultDao,
    private val stageFirestoreService: StageFirestoreService
) : StageRepository {

    companion object {
        private const val TAG = "StageRepositoryImpl"
    }

    // ==================== STAGE RESULTS ====================

    override suspend fun saveStageResults(results: List<StageResult>): Result<Int> {
        return try {
            // Save to local DB
            val entities = results.map { StageResultEntity.fromDomain(it) }
            stageResultDao.insertStageResults(entities)
            Log.d(TAG, "Saved ${entities.size} stage results to local DB")

            // Save to Firebase
            val firebaseResult = stageFirestoreService.saveStageResults(results)
            firebaseResult.onSuccess {
                Log.d(TAG, "Saved $it stage results to Firebase")
            }.onFailure {
                Log.e(TAG, "Failed to save to Firebase: ${it.message}")
            }

            Result.success(results.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stage results: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getStageResults(
        raceId: String,
        stageNumber: Int,
        season: Int
    ): Flow<List<StageResult>> {
        return stageResultDao.getStageResults(raceId, stageNumber)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getStageResultsOnce(
        raceId: String,
        stageNumber: Int,
        season: Int
    ): List<StageResult> {
        return stageResultDao.getStageResultsOnce(raceId, stageNumber)
            .map { it.toDomain() }
    }

    override fun getCyclistStageResults(
        raceId: String,
        cyclistId: String
    ): Flow<List<StageResult>> {
        return stageResultDao.getCyclistStageResults(raceId, cyclistId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getProcessedStages(raceId: String): List<Int> {
        return stageResultDao.getProcessedStages(raceId)
    }

    override suspend fun deleteStageResults(raceId: String, stageNumber: Int) {
        stageResultDao.deleteStageResults(raceId, stageNumber)
        Log.d(TAG, "Deleted stage results for race $raceId stage $stageNumber")
    }

    override suspend fun syncStageResults(
        raceId: String,
        stageNumber: Int,
        season: Int
    ): Result<Int> {
        return try {
            val results = stageFirestoreService.getStageResults(raceId, stageNumber, season)
            if (results.isNotEmpty()) {
                val entities = results.map { StageResultEntity.fromDomain(it) }
                stageResultDao.insertStageResults(entities)
                Log.d(TAG, "Synced ${results.size} stage results from Firebase")
            }
            Result.success(results.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing stage results: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== GC STANDINGS ====================

    override suspend fun saveGcStandings(standings: List<GcStanding>): Result<Int> {
        return try {
            // Save to local DB
            val entities = standings.map { GcStandingEntity.fromDomain(it) }
            stageResultDao.insertGcStandings(entities)
            Log.d(TAG, "Saved ${entities.size} GC standings to local DB")

            // Save to Firebase
            val firebaseResult = stageFirestoreService.saveGcStandings(standings)
            firebaseResult.onSuccess {
                Log.d(TAG, "Saved $it GC standings to Firebase")
            }.onFailure {
                Log.e(TAG, "Failed to save GC standings to Firebase: ${it.message}")
            }

            Result.success(standings.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving GC standings: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getGcStandings(raceId: String): Flow<List<GcStanding>> {
        return stageResultDao.getGcStandings(raceId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getGcStandingsOnce(raceId: String): List<GcStanding> {
        return stageResultDao.getGcStandingsOnce(raceId)
            .map { it.toDomain() }
    }

    override suspend fun getCyclistGcStanding(raceId: String, cyclistId: String): GcStanding? {
        return stageResultDao.getCyclistGcStanding(raceId, cyclistId)?.toDomain()
    }

    override suspend fun getJerseyHolders(raceId: String): JerseyHolders {
        val gcLeader = stageResultDao.getGcLeader(raceId)
        val pointsLeader = stageResultDao.getPointsLeader(raceId)
        val mountainsLeader = stageResultDao.getMountainsLeader(raceId)
        val youngLeader = stageResultDao.getYoungLeader(raceId)

        return JerseyHolders(
            gcLeaderId = gcLeader?.cyclistId,
            pointsLeaderId = pointsLeader?.cyclistId,
            mountainsLeaderId = mountainsLeader?.cyclistId,
            youngLeaderId = youngLeader?.cyclistId
        )
    }

    override suspend fun syncGcStandings(raceId: String, season: Int): Result<Int> {
        return try {
            val standings = stageFirestoreService.getGcStandings(raceId, season)
            if (standings.isNotEmpty()) {
                val entities = standings.map { GcStandingEntity.fromDomain(it) }
                stageResultDao.insertGcStandings(entities)
                Log.d(TAG, "Synced ${standings.size} GC standings from Firebase")
            }
            Result.success(standings.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing GC standings: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== RACE STATUS ====================

    override suspend fun updateRaceCurrentStage(
        raceId: String,
        currentStage: Int,
        season: Int
    ): Result<Unit> {
        return stageFirestoreService.updateRaceCurrentStage(raceId, currentStage, season)
    }

    override suspend fun markRaceCompleted(raceId: String, season: Int): Result<Unit> {
        return stageFirestoreService.markRaceCompleted(raceId, season)
    }

    // ==================== STAGE SCHEDULE ====================

    override suspend fun getStageSchedule(raceId: String, season: Int): List<Stage> {
        return try {
            stageFirestoreService.getStageSchedule(raceId, season)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stage schedule: ${e.message}")
            emptyList()
        }
    }

    // ==================== AGGREGATE QUERIES ====================

    override suspend fun getCyclistTotalPoints(raceId: String, cyclistId: String): Int {
        return stageResultDao.getCyclistTotalPoints(raceId, cyclistId)
    }

    override suspend fun getTopPerformers(raceId: String, limit: Int): List<Pair<String, Int>> {
        return stageResultDao.getTopPerformers(raceId, limit)
            .map { it.cyclistId to it.totalPoints }
    }
}
