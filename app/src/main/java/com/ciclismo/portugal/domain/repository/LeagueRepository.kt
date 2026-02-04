package com.ciclismo.portugal.domain.repository

import com.ciclismo.portugal.domain.model.League
import com.ciclismo.portugal.domain.model.LeagueMember
import com.ciclismo.portugal.domain.model.LeagueType
import kotlinx.coroutines.flow.Flow

interface LeagueRepository {

    // League operations
    fun getAllLeagues(): Flow<List<League>>
    fun getLeaguesByType(type: LeagueType): Flow<List<League>>
    fun getLeaguesForUser(userId: String): Flow<List<League>>
    suspend fun getLeagueById(leagueId: String): League?
    suspend fun getLeagueByCode(code: String): League?
    suspend fun createLeague(league: League): Result<League>
    suspend fun deleteLeague(leagueId: String): Result<Unit>

    // Member operations
    fun getLeagueMembers(leagueId: String): Flow<List<LeagueMember>>
    fun getLeagueTopMembers(leagueId: String, limit: Int = 10): Flow<List<LeagueMember>>
    suspend fun joinLeague(leagueId: String, userId: String, teamId: String): Result<Unit>
    suspend fun leaveLeague(leagueId: String, userId: String): Result<Unit>
    suspend fun isMember(leagueId: String, userId: String): Boolean
    suspend fun updateMemberRanking(leagueId: String, userId: String, rank: Int, previousRank: Int, points: Int): Result<Unit>

    // Pagination for large leagues (1000+ users)
    suspend fun getLeagueMembersPaginated(
        leagueId: String,
        pageSize: Int = 50,
        lastPoints: Int? = null,
        lastUserId: String? = null
    ): List<LeagueMember>

    suspend fun getUserPositionInLeague(leagueId: String, userId: String): LeagueMember?
    suspend fun getMembersSurroundingUser(
        leagueId: String,
        userId: String,
        countAbove: Int = 3,
        countBelow: Int = 3
    ): List<LeagueMember>

    suspend fun getLeagueMemberCount(leagueId: String): Int

    // Admin/Testing
    suspend fun addFakeTeamsToLeague(leagueId: String, count: Int): Result<Int>
    suspend fun removeFakeTeamsFromLeague(leagueId: String): Result<Int>

    // Utility
    suspend fun generateUniqueCode(): String
    suspend fun ensureGlobalLeagueExists(): League
}
