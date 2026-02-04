package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.FantasyTeamEntity
import com.ciclismo.portugal.data.local.entity.TeamCyclistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FantasyTeamDao {

    // ========== FantasyTeam Operations ==========

    @Query("SELECT * FROM fantasy_teams WHERE userId = :userId AND season = :season LIMIT 1")
    fun getTeamByUserIdAndSeason(userId: String, season: Int): Flow<FantasyTeamEntity?>

    @Query("SELECT * FROM fantasy_teams WHERE userId = :userId ORDER BY season DESC LIMIT 1")
    fun getTeamByUserId(userId: String): Flow<FantasyTeamEntity?>

    @Query("SELECT * FROM fantasy_teams WHERE id = :teamId")
    suspend fun getTeamById(teamId: String): FantasyTeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: FantasyTeamEntity)

    @Update
    suspend fun updateTeam(team: FantasyTeamEntity)

    @Query("UPDATE fantasy_teams SET budget = :budget, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun updateBudget(teamId: String, budget: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET totalPoints = totalPoints + :points, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun addPoints(teamId: String, points: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET freeTransfers = :transfers, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun setFreeTransfers(teamId: String, transfers: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET wildcardUsed = 1, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun useWildcard(teamId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET tripleCaptainUsed = 1, tripleCaptainActive = 1, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun useTripleCaptain(teamId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET tripleCaptainActive = 0, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun deactivateTripleCaptain(teamId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET benchBoostUsed = 1, benchBoostActive = 1, benchBoostOriginalBench = :originalBench, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun useBenchBoost(teamId: String, originalBench: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET benchBoostActive = 0, benchBoostOriginalBench = NULL, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun deactivateBenchBoost(teamId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE team_cyclists SET isActive = 1 WHERE teamId = :teamId")
    suspend fun activateAllCyclists(teamId: String)

    @Query("SELECT cyclistId FROM team_cyclists WHERE teamId = :teamId AND isActive = 0")
    suspend fun getBenchCyclistIds(teamId: String): List<String>

    @Query("UPDATE fantasy_teams SET gameweek = :gameweek, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun setGameweek(teamId: String, gameweek: Int, updatedAt: Long = System.currentTimeMillis())

    // ========== Per-Race Wildcard Activation ==========

    @Query("UPDATE fantasy_teams SET tripleCaptainUsed = 1, tripleCaptainActive = 1, tripleCaptainRaceId = :raceId, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun activateTripleCaptainForRace(teamId: String, raceId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET tripleCaptainActive = 0, tripleCaptainRaceId = NULL, tripleCaptainUsed = 0, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun cancelTripleCaptainForRace(teamId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET benchBoostUsed = 1, benchBoostActive = 1, benchBoostRaceId = :raceId, benchBoostOriginalBench = :originalBench, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun activateBenchBoostForRace(teamId: String, raceId: String, originalBench: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET benchBoostActive = 0, benchBoostRaceId = NULL, benchBoostOriginalBench = NULL, benchBoostUsed = 0, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun cancelBenchBoostForRace(teamId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET wildcardUsed = 1, wildcardActive = 1, wildcardRaceId = :raceId, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun activateWildcardForRace(teamId: String, raceId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fantasy_teams SET wildcardActive = 0, wildcardRaceId = NULL, wildcardUsed = 0, updatedAt = :updatedAt WHERE id = :teamId")
    suspend fun cancelWildcardForRace(teamId: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTeam(team: FantasyTeamEntity)

    // ========== TeamCyclist Operations ==========

    @Query("SELECT * FROM team_cyclists WHERE teamId = :teamId")
    fun getTeamCyclists(teamId: String): Flow<List<TeamCyclistEntity>>

    @Query("SELECT * FROM team_cyclists WHERE teamId = :teamId AND isActive = 1")
    fun getActiveCyclists(teamId: String): Flow<List<TeamCyclistEntity>>

    @Query("SELECT * FROM team_cyclists WHERE teamId = :teamId AND isCaptain = 1 LIMIT 1")
    suspend fun getCaptain(teamId: String): TeamCyclistEntity?

    @Query("SELECT COUNT(*) FROM team_cyclists WHERE teamId = :teamId")
    suspend fun getTeamSize(teamId: String): Int

    @Query("SELECT COUNT(*) FROM team_cyclists WHERE teamId = :teamId AND isActive = 1")
    suspend fun getActiveCount(teamId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCyclistToTeam(teamCyclist: TeamCyclistEntity)

    @Query("DELETE FROM team_cyclists WHERE teamId = :teamId AND cyclistId = :cyclistId")
    suspend fun removeCyclistFromTeam(teamId: String, cyclistId: String)

    @Query("UPDATE team_cyclists SET isActive = :isActive WHERE teamId = :teamId AND cyclistId = :cyclistId")
    suspend fun setActive(teamId: String, cyclistId: String, isActive: Boolean)

    @Query("UPDATE team_cyclists SET isCaptain = 0 WHERE teamId = :teamId")
    suspend fun clearCaptain(teamId: String)

    @Query("UPDATE team_cyclists SET isCaptain = 1 WHERE teamId = :teamId AND cyclistId = :cyclistId")
    suspend fun setCaptain(teamId: String, cyclistId: String)

    @Query("DELETE FROM team_cyclists WHERE teamId = :teamId")
    suspend fun clearTeamCyclists(teamId: String)

    // Check if cyclist is already in team
    @Query("SELECT EXISTS(SELECT 1 FROM team_cyclists WHERE teamId = :teamId AND cyclistId = :cyclistId)")
    suspend fun isCyclistInTeam(teamId: String, cyclistId: String): Boolean

    // Count cyclists from same pro team (max 3 rule)
    @Query("""
        SELECT COUNT(*) FROM team_cyclists tc
        INNER JOIN cyclists c ON tc.cyclistId = c.id
        WHERE tc.teamId = :teamId AND c.teamId = :proTeamId
    """)
    suspend fun countCyclistsFromProTeam(teamId: String, proTeamId: String): Int

    // ========== Fantasy Points Calculation ==========

    @Query("SELECT * FROM fantasy_teams")
    suspend fun getAllTeams(): List<FantasyTeamEntity>

    @Query("SELECT * FROM fantasy_teams WHERE season = :season")
    suspend fun getAllTeamsForSeason(season: Int): List<FantasyTeamEntity>

    @Query("SELECT * FROM team_cyclists WHERE teamId = :teamId")
    suspend fun getTeamCyclistsSync(teamId: String): List<TeamCyclistEntity>

    // ========== Season History Operations ==========

    /**
     * Get all teams for a user across all seasons
     */
    @Query("SELECT * FROM fantasy_teams WHERE userId = :userId ORDER BY season DESC")
    fun getUserTeamsAllSeasons(userId: String): Flow<List<FantasyTeamEntity>>

    /**
     * Get all seasons a user has participated in
     */
    @Query("SELECT DISTINCT season FROM fantasy_teams WHERE userId = :userId ORDER BY season DESC")
    suspend fun getUserSeasons(userId: String): List<Int>

    /**
     * Get team for a specific user and season
     */
    @Query("SELECT * FROM fantasy_teams WHERE userId = :userId AND season = :season LIMIT 1")
    suspend fun getTeamForSeason(userId: String, season: Int): FantasyTeamEntity?

    /**
     * Check if user already has a team for the current season
     */
    @Query("SELECT EXISTS(SELECT 1 FROM fantasy_teams WHERE userId = :userId AND season = :season)")
    suspend fun hasTeamForSeason(userId: String, season: Int): Boolean

    // ========== Dynamic Pricing queries ==========

    /**
     * Count total teams for a season (for ownership calculation)
     */
    @Query("SELECT COUNT(*) FROM fantasy_teams WHERE season = :season AND isBot = 0")
    suspend fun getTeamCountForSeason(season: Int): Int

    /**
     * Count total teams including bots
     */
    @Query("SELECT COUNT(*) FROM fantasy_teams WHERE season = :season")
    suspend fun getTotalTeamCountForSeason(season: Int): Int

    /**
     * Count how many teams have a specific cyclist
     */
    @Query("""
        SELECT COUNT(DISTINCT tc.teamId) FROM team_cyclists tc
        INNER JOIN fantasy_teams ft ON tc.teamId = ft.id
        WHERE tc.cyclistId = :cyclistId AND ft.season = :season
    """)
    suspend fun countTeamsWithCyclist(cyclistId: String, season: Int): Int

    /**
     * Get all bot teams for a season
     */
    @Query("SELECT * FROM fantasy_teams WHERE season = :season AND isBot = 1")
    suspend fun getBotTeamsForSeason(season: Int): List<FantasyTeamEntity>

    /**
     * Delete all bot teams for a season
     */
    @Query("DELETE FROM fantasy_teams WHERE season = :season AND isBot = 1")
    suspend fun deleteBotTeamsForSeason(season: Int)

    /**
     * Count bot teams for a season
     */
    @Query("SELECT COUNT(*) FROM fantasy_teams WHERE season = :season AND isBot = 1")
    suspend fun getBotTeamCountForSeason(season: Int): Int
}
