package com.ciclismo.portugal.data.repository

import com.ciclismo.portugal.data.local.dao.CyclistDao
import com.ciclismo.portugal.data.local.entity.CyclistEntity
import com.ciclismo.portugal.data.local.entity.toDomain
import com.ciclismo.portugal.data.remote.cycling.CyclingDataSource
import com.ciclismo.portugal.data.remote.firebase.CyclistFirestoreService
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.repository.CyclistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CyclistRepositoryImpl @Inject constructor(
    private val cyclistDao: CyclistDao,
    private val cyclingDataSource: CyclingDataSource,
    private val cyclistFirestoreService: CyclistFirestoreService
) : CyclistRepository {

    override fun getAllCyclists(): Flow<List<Cyclist>> {
        return cyclistDao.getAllCyclists().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCyclistById(id: String): Flow<Cyclist?> {
        return flow {
            // First try local database
            val entity = cyclistDao.getCyclistById(id)
            if (entity != null) {
                emit(entity.toDomain())
                return@flow
            }

            // If not found locally, search in Firestore validated cyclists
            val firestoreCyclists = cyclistFirestoreService.getValidatedCyclists().first()
            val cyclist = firestoreCyclists.find { it.id == id }
            emit(cyclist)
        }
    }

    override fun getCyclistsByTeam(teamId: String): Flow<List<Cyclist>> {
        return cyclistDao.getCyclistsByTeam(teamId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCyclistsByCategory(category: CyclistCategory): Flow<List<Cyclist>> {
        return cyclistDao.getCyclistsByCategory(category.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchCyclists(query: String): Flow<List<Cyclist>> {
        return cyclistDao.searchCyclists(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCyclistsByPriceRange(minPrice: Double, maxPrice: Double): Flow<List<Cyclist>> {
        return cyclistDao.getCyclistsByMaxPrice(maxPrice).map { entities ->
            entities.filter { it.price >= minPrice }.map { it.toDomain() }
        }
    }

    override suspend fun syncCyclists(): Result<Int> {
        return try {
            val result = cyclingDataSource.getTopCyclists(200)

            result.fold(
                onSuccess = { cyclistDtos ->
                    val entities = cyclistDtos.map { it.toEntity() }
                    cyclistDao.insertAllCyclists(entities)
                    Result.success(entities.size)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncCyclistDetails(cyclistId: String): Result<Cyclist> {
        return try {
            val result = cyclingDataSource.getCyclistDetails(cyclistId)

            result.fold(
                onSuccess = { dto ->
                    val entity = dto.toEntity()
                    cyclistDao.insertCyclist(entity)
                    Result.success(entity.toDomain())
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get validated cyclists from Firestore
     * This returns cyclists that have been approved by admin
     */
    override fun getValidatedCyclists(): Flow<List<Cyclist>> {
        return cyclistFirestoreService.getValidatedCyclists()
    }

    /**
     * Sync validated cyclists from Firestore to local database
     * This allows the app to work offline with cached data
     */
    override suspend fun syncFromFirestore(): Result<Int> {
        return try {
            val cyclists = cyclistFirestoreService.getValidatedCyclists().first()

            if (cyclists.isEmpty()) {
                return Result.success(0)
            }

            val entities = cyclists.map { cyclist ->
                CyclistEntity(
                    id = cyclist.id,
                    firstName = cyclist.firstName,
                    lastName = cyclist.lastName,
                    teamId = cyclist.teamId,
                    teamName = cyclist.teamName,
                    nationality = cyclist.nationality,
                    photoUrl = cyclist.photoUrl,
                    category = cyclist.category.name,
                    price = cyclist.price,
                    totalPoints = cyclist.totalPoints,
                    form = cyclist.form,
                    popularity = cyclist.popularity,
                    syncedAt = System.currentTimeMillis()
                )
            }

            cyclistDao.insertAllCyclists(entities)
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Cyclist Availability (Admin) ==========

    override fun getDisabledCyclists(): Flow<List<Cyclist>> {
        return cyclistDao.getDisabledCyclistsForSeason(SeasonConfig.CURRENT_SEASON).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAvailableCyclists(): Flow<List<Cyclist>> {
        return cyclistDao.getAvailableCyclistsForSeason(SeasonConfig.CURRENT_SEASON).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCyclistsForSeason(season: Int): Flow<List<Cyclist>> {
        return cyclistDao.getCyclistsForSeason(season).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun disableCyclist(cyclistId: String, reason: String): Result<Unit> {
        return try {
            // Update local database
            cyclistDao.disableCyclist(cyclistId, reason)

            // Also update Firestore for sync across devices
            cyclistFirestoreService.updateCyclistAvailability(
                cyclistId = cyclistId,
                isDisabled = true,
                reason = reason
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enableCyclist(cyclistId: String): Result<Unit> {
        return try {
            // Update local database
            cyclistDao.enableCyclist(cyclistId)

            // Also update Firestore for sync across devices
            cyclistFirestoreService.updateCyclistAvailability(
                cyclistId = cyclistId,
                isDisabled = false,
                reason = null
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDisabledCyclistCount(): Int {
        return cyclistDao.getDisabledCyclistCountForSeason(SeasonConfig.CURRENT_SEASON)
    }

    // ========== Cyclist Price Management (Admin) ==========

    override suspend fun updateCyclist(cyclist: Cyclist): Result<Unit> {
        return try {
            android.util.Log.d("CyclistRepository", "Updating cyclist: ${cyclist.fullName} with price ${cyclist.price}M")
            cyclistFirestoreService.updateCyclist(cyclist, SeasonConfig.CURRENT_SEASON)
        } catch (e: Exception) {
            android.util.Log.e("CyclistRepository", "Error updating cyclist: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateCyclistPrice(cyclistId: String, newPrice: Double): Result<Unit> {
        return try {
            android.util.Log.d("CyclistRepository", "Updating price for cyclist $cyclistId to ${newPrice}M")

            // Get current cyclist data from Firestore
            val cyclists = cyclistFirestoreService.getValidatedCyclists().first()
            val cyclist = cyclists.find { it.id == cyclistId }

            if (cyclist == null) {
                android.util.Log.e("CyclistRepository", "Cyclist not found: $cyclistId")
                return Result.failure(Exception("Cyclist not found: $cyclistId"))
            }

            // Update with new price
            val updatedCyclist = cyclist.copy(price = newPrice)
            cyclistFirestoreService.updateCyclist(updatedCyclist, SeasonConfig.CURRENT_SEASON)
        } catch (e: Exception) {
            android.util.Log.e("CyclistRepository", "Error updating cyclist price: ${e.message}", e)
            Result.failure(e)
        }
    }
}
