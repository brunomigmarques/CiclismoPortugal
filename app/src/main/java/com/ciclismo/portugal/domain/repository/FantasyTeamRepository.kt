package com.ciclismo.portugal.domain.repository

import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.TeamCyclist
import kotlinx.coroutines.flow.Flow

interface FantasyTeamRepository {
    fun getTeamByUserId(userId: String): Flow<FantasyTeam?>
    suspend fun getTeamById(teamId: String): FantasyTeam?

    suspend fun createTeam(userId: String, teamName: String): FantasyTeam
    suspend fun updateTeam(team: FantasyTeam)
    suspend fun deleteTeam(teamId: String)

    // Cyclist management
    fun getTeamCyclists(teamId: String): Flow<List<TeamCyclist>>
    fun getTeamCyclistsWithDetails(teamId: String): Flow<List<Pair<TeamCyclist, Cyclist>>>
    fun getActiveCyclists(teamId: String): Flow<List<TeamCyclist>>

    suspend fun addCyclistToTeam(teamId: String, cyclist: Cyclist): Result<Unit>
    suspend fun removeCyclistFromTeam(teamId: String, cyclistId: String): Result<Unit>

    suspend fun setCaptain(teamId: String, cyclistId: String): Result<Unit>
    suspend fun setActive(teamId: String, cyclistId: String, isActive: Boolean): Result<Unit>

    suspend fun getTeamSize(teamId: String): Int
    suspend fun getActiveCount(teamId: String): Int
    suspend fun isCyclistInTeam(teamId: String, cyclistId: String): Boolean
    suspend fun countCyclistsFromProTeam(teamId: String, proTeamId: String): Int

    // Wildcards (legacy - global activation)
    suspend fun useWildcard(teamId: String): Result<Unit>
    suspend fun useTripleCaptain(teamId: String): Result<Unit>
    suspend fun useBenchBoost(teamId: String): Result<Unit>

    // Cancel active wildcards (before race starts)
    suspend fun cancelTripleCaptain(teamId: String): Result<Unit>
    suspend fun cancelBenchBoost(teamId: String): Result<Unit>

    // Per-race wildcard activation (new system)
    suspend fun activateTripleCaptainForRace(teamId: String, raceId: String): Result<Unit>
    suspend fun activateBenchBoostForRace(teamId: String, raceId: String): Result<Unit>
    suspend fun activateWildcardForRace(teamId: String, raceId: String): Result<Unit>
    suspend fun cancelWildcard(teamId: String): Result<Unit>

    // Rankings
    fun getAllTeamsForRanking(): Flow<List<FantasyTeam>>
    suspend fun getTopTeams(limit: Int = 100): Result<List<FantasyTeam>>
    suspend fun getUserRank(userId: String): Int?
    suspend fun getTotalTeamsCount(): Int

    // User team validation
    suspend fun userHasTeam(userId: String): Boolean

    // Sync from Firestore (for reinstall scenarios)
    suspend fun syncTeamFromFirestore(userId: String): Result<FantasyTeam?>
}
