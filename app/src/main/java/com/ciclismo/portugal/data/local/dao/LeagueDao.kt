package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.LeagueEntity
import com.ciclismo.portugal.data.local.entity.LeagueMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeagueDao {

    // ========== League Operations ==========

    @Query("SELECT * FROM leagues ORDER BY type ASC, name ASC")
    fun getAllLeagues(): Flow<List<LeagueEntity>>

    @Query("SELECT * FROM leagues WHERE id = :leagueId")
    suspend fun getLeagueById(leagueId: String): LeagueEntity?

    @Query("SELECT * FROM leagues WHERE type = :type ORDER BY name ASC")
    fun getLeaguesByType(type: String): Flow<List<LeagueEntity>>

    @Query("SELECT * FROM leagues WHERE code = :code LIMIT 1")
    suspend fun getLeagueByCode(code: String): LeagueEntity?

    @Query("SELECT * FROM leagues WHERE ownerId = :userId")
    fun getLeaguesOwnedByUser(userId: String): Flow<List<LeagueEntity>>

    @Query("SELECT * FROM leagues WHERE region = :region AND type = 'REGIONAL'")
    fun getRegionalLeagues(region: String): Flow<List<LeagueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeague(league: LeagueEntity)

    @Update
    suspend fun updateLeague(league: LeagueEntity)

    @Query("UPDATE leagues SET memberCount = memberCount + 1 WHERE id = :leagueId")
    suspend fun incrementMemberCount(leagueId: String)

    @Query("UPDATE leagues SET memberCount = memberCount - 1 WHERE id = :leagueId AND memberCount > 0")
    suspend fun decrementMemberCount(leagueId: String)

    @Delete
    suspend fun deleteLeague(league: LeagueEntity)

    @Query("DELETE FROM leagues WHERE id = :leagueId")
    suspend fun deleteLeagueById(leagueId: String)

    // ========== LeagueMember Operations ==========

    @Query("SELECT * FROM league_members WHERE leagueId = :leagueId ORDER BY rank ASC")
    fun getLeagueMembers(leagueId: String): Flow<List<LeagueMemberEntity>>

    @Query("SELECT * FROM league_members WHERE leagueId = :leagueId ORDER BY rank ASC LIMIT :limit")
    fun getLeagueTopMembers(leagueId: String, limit: Int = 10): Flow<List<LeagueMemberEntity>>

    @Query("SELECT * FROM league_members WHERE userId = :userId")
    fun getUserLeagueMemberships(userId: String): Flow<List<LeagueMemberEntity>>

    @Query("SELECT * FROM league_members WHERE leagueId = :leagueId AND userId = :userId LIMIT 1")
    suspend fun getMembership(leagueId: String, userId: String): LeagueMemberEntity?

    @Query("SELECT l.* FROM leagues l INNER JOIN league_members lm ON l.id = lm.leagueId WHERE lm.userId = :userId")
    fun getLeaguesForUser(userId: String): Flow<List<LeagueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: LeagueMemberEntity)

    @Update
    suspend fun updateMember(member: LeagueMemberEntity)

    @Query("UPDATE league_members SET rank = :rank, previousRank = :previousRank, points = :points WHERE leagueId = :leagueId AND userId = :userId")
    suspend fun updateMemberRanking(leagueId: String, userId: String, rank: Int, previousRank: Int, points: Int)

    @Query("DELETE FROM league_members WHERE leagueId = :leagueId AND userId = :userId")
    suspend fun removeMember(leagueId: String, userId: String)

    @Query("DELETE FROM league_members WHERE leagueId = :leagueId")
    suspend fun removeAllMembers(leagueId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM league_members WHERE leagueId = :leagueId AND userId = :userId)")
    suspend fun isMember(leagueId: String, userId: String): Boolean

    @Query("SELECT COUNT(*) FROM league_members WHERE leagueId = :leagueId")
    suspend fun getMemberCount(leagueId: String): Int

    // ========== Season-specific queries ==========

    @Query("SELECT * FROM leagues WHERE season = :season ORDER BY type ASC, name ASC")
    fun getLeaguesForSeason(season: Int): Flow<List<LeagueEntity>>

    @Query("SELECT * FROM leagues WHERE season = :season AND type = 'GLOBAL' LIMIT 1")
    suspend fun getGlobalLeagueForSeason(season: Int): LeagueEntity?

    @Query("SELECT * FROM leagues WHERE season = :season AND type = :type ORDER BY name ASC")
    fun getLeaguesByTypeForSeason(type: String, season: Int): Flow<List<LeagueEntity>>

    @Query("SELECT * FROM league_members WHERE season = :season AND leagueId = :leagueId ORDER BY rank ASC")
    fun getLeagueMembersForSeason(leagueId: String, season: Int): Flow<List<LeagueMemberEntity>>

    @Query("SELECT l.* FROM leagues l INNER JOIN league_members lm ON l.id = lm.leagueId WHERE lm.userId = :userId AND lm.season = :season")
    fun getLeaguesForUserAndSeason(userId: String, season: Int): Flow<List<LeagueEntity>>

    @Query("SELECT COUNT(*) FROM leagues WHERE season = :season")
    suspend fun getLeagueCountForSeason(season: Int): Int

    @Query("SELECT COUNT(*) FROM league_members WHERE leagueId = :leagueId AND season = :season")
    suspend fun getMemberCountForSeason(leagueId: String, season: Int): Int
}
