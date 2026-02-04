package com.ciclismo.portugal.data.repository

import com.ciclismo.portugal.data.local.dao.LeagueDao
import com.ciclismo.portugal.data.local.entity.LeagueEntity
import com.ciclismo.portugal.data.local.entity.LeagueMemberEntity
import com.ciclismo.portugal.data.local.entity.toDomain
import com.ciclismo.portugal.data.local.entity.toEntity
import com.ciclismo.portugal.data.remote.firebase.LeagueFirestoreService
import com.ciclismo.portugal.domain.model.League
import com.ciclismo.portugal.domain.model.LeagueMember
import com.ciclismo.portugal.domain.model.LeagueType
import com.ciclismo.portugal.domain.repository.LeagueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeagueRepositoryImpl @Inject constructor(
    private val leagueDao: LeagueDao,
    private val leagueFirestoreService: LeagueFirestoreService
) : LeagueRepository {

    override fun getAllLeagues(): Flow<List<League>> {
        // Use Firestore as primary source for multiplayer
        return leagueFirestoreService.getAllLeagues()
    }

    override fun getLeaguesByType(type: LeagueType): Flow<List<League>> {
        // Use Firestore as primary source
        return leagueFirestoreService.getLeaguesByType(type)
    }

    override fun getLeaguesForUser(userId: String): Flow<List<League>> {
        // Combine Firestore and local database for resilience
        // If Firestore fails or returns empty due to missing index, fall back to local
        return leagueFirestoreService.getLeaguesForUser(userId)
            .combine(leagueDao.getLeaguesForUser(userId).map { entities ->
                entities.map { it.toDomain() }
            }) { firestoreLeagues, localLeagues ->
                // If Firestore returns leagues, use them and update local cache
                if (firestoreLeagues.isNotEmpty()) {
                    firestoreLeagues
                } else {
                    // Fallback to local leagues
                    android.util.Log.d("LeagueRepo", "Using local leagues as fallback: ${localLeagues.size}")
                    localLeagues
                }
            }
            .catch { e ->
                android.util.Log.e("LeagueRepo", "Error in getLeaguesForUser: ${e.message}")
                emit(emptyList())
            }
    }

    override suspend fun getLeagueById(leagueId: String): League? {
        // Try Firestore first
        return leagueFirestoreService.getLeagueById(leagueId)
            ?: leagueDao.getLeagueById(leagueId)?.toDomain()
    }

    override suspend fun getLeagueByCode(code: String): League? {
        // Try Firestore first
        return leagueFirestoreService.getLeagueByCode(code.uppercase())
            ?: leagueDao.getLeagueByCode(code.uppercase())?.toDomain()
    }

    override suspend fun createLeague(league: League): Result<League> {
        return try {
            // Create in Firestore (primary)
            leagueFirestoreService.createLeague(league).onFailure { error ->
                android.util.Log.e("LeagueRepo", "Failed to create league in Firestore: ${error.message}")
            }

            // Also cache locally
            val entity = league.toEntity()
            leagueDao.insertLeague(entity)

            android.util.Log.d("LeagueRepo", "League created: ${league.id}")
            Result.success(league)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteLeague(leagueId: String): Result<Unit> {
        return try {
            // Delete from Firestore (primary)
            leagueFirestoreService.deleteLeague(leagueId).onFailure { error ->
                android.util.Log.e("LeagueRepo", "Failed to delete league from Firestore: ${error.message}")
            }

            // Also delete locally
            leagueDao.removeAllMembers(leagueId)
            leagueDao.deleteLeagueById(leagueId)

            android.util.Log.d("LeagueRepo", "League deleted: $leagueId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLeagueMembers(leagueId: String): Flow<List<LeagueMember>> {
        // Use Firestore for real-time multiplayer rankings
        return leagueFirestoreService.getLeagueMembers(leagueId)
    }

    override fun getLeagueTopMembers(leagueId: String, limit: Int): Flow<List<LeagueMember>> {
        // Use Firestore for real-time multiplayer rankings
        return leagueFirestoreService.getLeagueMembers(leagueId)
    }

    override suspend fun joinLeague(leagueId: String, userId: String, teamId: String): Result<Unit> {
        return joinLeagueWithTeamName(leagueId, userId, teamId, "Equipa")
    }

    /**
     * Join league with team name
     */
    suspend fun joinLeagueWithTeamName(leagueId: String, userId: String, teamId: String, teamName: String): Result<Unit> {
        return try {
            // Join in Firestore (primary)
            val result = leagueFirestoreService.joinLeague(leagueId, userId, teamId, teamName)

            if (result.isSuccess) {
                // Also cache locally
                val memberCount = leagueDao.getMemberCount(leagueId)
                val member = LeagueMemberEntity(
                    leagueId = leagueId,
                    userId = userId,
                    teamId = teamId,
                    teamName = teamName,
                    rank = memberCount + 1,
                    points = 0,
                    previousRank = memberCount + 1,
                    joinedAt = System.currentTimeMillis()
                )
                leagueDao.insertMember(member)
                leagueDao.incrementMemberCount(leagueId)
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveLeague(leagueId: String, userId: String): Result<Unit> {
        return try {
            // Leave in Firestore (primary)
            leagueFirestoreService.leaveLeague(leagueId, userId).onFailure { error ->
                android.util.Log.e("LeagueRepo", "Failed to leave league in Firestore: ${error.message}")
            }

            // Also update locally
            leagueDao.removeMember(leagueId, userId)
            leagueDao.decrementMemberCount(leagueId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isMember(leagueId: String, userId: String): Boolean {
        // Check Firestore first
        return leagueFirestoreService.isMember(leagueId, userId)
    }

    override suspend fun updateMemberRanking(
        leagueId: String,
        userId: String,
        rank: Int,
        previousRank: Int,
        points: Int
    ): Result<Unit> {
        return try {
            // Update in Firestore
            leagueFirestoreService.updateMemberPoints(leagueId, userId, points).onFailure { error ->
                android.util.Log.e("LeagueRepo", "Failed to update member in Firestore: ${error.message}")
            }

            // Also update locally
            leagueDao.updateMemberRanking(leagueId, userId, rank, previousRank, points)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateUniqueCode(): String {
        // Use Firestore to ensure uniqueness across all users
        return leagueFirestoreService.generateUniqueCode()
    }

    override suspend fun ensureGlobalLeagueExists(): League {
        // Use Firestore to ensure global league is available to all users
        val firestoreLeague = leagueFirestoreService.ensureGlobalLeagueExists()

        // Also cache locally using the actual league ID (season-based)
        val existingLocal = leagueDao.getLeagueById(firestoreLeague.id)
        if (existingLocal == null) {
            val localEntity = LeagueEntity(
                id = firestoreLeague.id,
                name = firestoreLeague.name,
                type = LeagueType.GLOBAL.name,
                code = null,
                ownerId = null,
                region = null,
                memberCount = firestoreLeague.memberCount,
                createdAt = firestoreLeague.createdAt,
                season = firestoreLeague.season
            )
            leagueDao.insertLeague(localEntity)
        }

        return firestoreLeague
    }

    /**
     * Recalculate rankings for all members in a league
     */
    suspend fun recalculateRankings(leagueId: String): Result<Unit> {
        return leagueFirestoreService.recalculateRankings(leagueId)
    }

    // ========== Pagination for large leagues (1000+ users) ==========

    override suspend fun getLeagueMembersPaginated(
        leagueId: String,
        pageSize: Int,
        lastPoints: Int?,
        lastUserId: String?
    ): List<LeagueMember> {
        return leagueFirestoreService.getLeagueMembersPaginated(
            leagueId = leagueId,
            pageSize = pageSize,
            lastPoints = lastPoints,
            lastUserId = lastUserId
        )
    }

    override suspend fun getUserPositionInLeague(leagueId: String, userId: String): LeagueMember? {
        return leagueFirestoreService.getUserPositionInLeague(leagueId, userId)
    }

    override suspend fun getMembersSurroundingUser(
        leagueId: String,
        userId: String,
        countAbove: Int,
        countBelow: Int
    ): List<LeagueMember> {
        return leagueFirestoreService.getMembersSurroundingUser(
            leagueId = leagueId,
            userId = userId,
            countAbove = countAbove,
            countBelow = countBelow
        )
    }

    override suspend fun getLeagueMemberCount(leagueId: String): Int {
        return leagueFirestoreService.getLeagueMemberCount(leagueId)
    }

    override suspend fun addFakeTeamsToLeague(leagueId: String, count: Int): Result<Int> {
        return leagueFirestoreService.addFakeTeamsToLeague(leagueId, count)
    }

    override suspend fun removeFakeTeamsFromLeague(leagueId: String): Result<Int> {
        return leagueFirestoreService.removeFakeTeamsFromLeague(leagueId)
    }
}
