package com.ciclismo.portugal.data.repository

import com.ciclismo.portugal.data.local.dao.CyclistDao
import com.ciclismo.portugal.data.local.dao.FantasyTeamDao
import com.ciclismo.portugal.data.local.dao.RaceDao
import com.ciclismo.portugal.data.local.entity.FantasyTeamEntity
import com.ciclismo.portugal.data.local.entity.TeamCyclistEntity
import com.ciclismo.portugal.data.local.entity.toDomain
import com.ciclismo.portugal.data.remote.firebase.CyclistFirestoreService
import com.ciclismo.portugal.data.remote.firebase.FantasyTeamFirestoreService
import com.ciclismo.portugal.data.remote.firebase.LeagueFirestoreService
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.TeamCyclist
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FantasyTeamRepositoryImpl @Inject constructor(
    private val fantasyTeamDao: FantasyTeamDao,
    private val cyclistDao: CyclistDao,
    private val raceDao: RaceDao,
    private val cyclistFirestoreService: CyclistFirestoreService,
    private val fantasyTeamFirestoreService: FantasyTeamFirestoreService,
    private val leagueFirestoreService: LeagueFirestoreService
) : FantasyTeamRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val MAX_TEAM_SIZE = 15
        const val MAX_ACTIVE_CYCLISTS = 8
        const val MAX_FROM_SAME_TEAM = 3
        const val INITIAL_BUDGET = 100.0
        const val TRANSFER_PENALTY_POINTS = 4

        /**
         * Get the global league ID for a specific season
         * Format: "liga-portugal-global-{season}"
         */
        fun globalLeagueId(season: Int = SeasonConfig.CURRENT_SEASON) = "liga-portugal-global-$season"
    }

    // ========== RACE DAY FREEZE ==========

    /**
     * Check if there's an active race happening today.
     * If so, team changes are blocked (transfers, captain, lineup).
     */
    private suspend fun isTeamFrozen(): Boolean {
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

        val raceToday = raceDao.getRaceHappeningToday(startOfDay, endOfDay)
        return raceToday != null
    }

    /**
     * Get the name of the race happening today (for error messages).
     */
    private suspend fun getRaceTodayName(): String? {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return raceDao.getRaceHappeningToday(startOfDay, endOfDay)?.name
    }

    override fun getTeamByUserId(userId: String): Flow<FantasyTeam?> {
        return fantasyTeamDao.getTeamByUserId(userId).map { it?.toDomain() }
    }

    override suspend fun getTeamById(teamId: String): FantasyTeam? {
        return fantasyTeamDao.getTeamById(teamId)?.toDomain()
    }

    override suspend fun createTeam(userId: String, teamName: String): FantasyTeam {
        // Check if user already has a team (one team per user limit)
        val existingTeamId = fantasyTeamFirestoreService.getUserTeamId(userId)
        if (existingTeamId != null) {
            android.util.Log.w("FantasyRepo", "User $userId already has a team: $existingTeamId")
            throw IllegalStateException("Utilizador já tem uma equipa. Apenas uma equipa por utilizador é permitida.")
        }

        // Also check local database
        val localTeam = fantasyTeamDao.getTeamByUserId(userId).first()
        if (localTeam != null) {
            android.util.Log.w("FantasyRepo", "User $userId has local team: ${localTeam.id}")
            throw IllegalStateException("Utilizador já tem uma equipa. Apenas uma equipa por utilizador é permitida.")
        }

        val team = FantasyTeamEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            teamName = teamName,
            budget = INITIAL_BUDGET
        )

        // Save to local database
        fantasyTeamDao.insertTeam(team)

        // Sync to Firestore for rankings
        val domainTeam = team.toDomain()
        fantasyTeamFirestoreService.createTeam(domainTeam).onFailure { error ->
            android.util.Log.e("FantasyRepo", "Failed to sync team to Firestore: ${error.message}")
        }

        // Auto-join Liga Portugal (global league) - mandatory for all users
        try {
            val globalLeague = leagueFirestoreService.ensureGlobalLeagueExists()
            leagueFirestoreService.joinLeague(
                leagueId = globalLeague.id,
                userId = userId,
                teamId = team.id,
                teamName = teamName
            ).onSuccess {
                android.util.Log.d("FantasyRepo", "User $userId auto-joined Liga Portugal ${globalLeague.season}")
            }.onFailure { error ->
                android.util.Log.e("FantasyRepo", "Failed to auto-join Liga Portugal: ${error.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error auto-joining Liga Portugal: ${e.message}")
        }

        android.util.Log.d("FantasyRepo", "Team created and synced: ${team.id}")
        return domainTeam
    }

    override suspend fun updateTeam(team: FantasyTeam) {
        val entity = FantasyTeamEntity(
            id = team.id,
            userId = team.userId,
            teamName = team.teamName,
            budget = team.budget,
            totalPoints = team.totalPoints,
            freeTransfers = team.freeTransfers,
            gameweek = team.gameweek,
            wildcardUsed = team.wildcardUsed,
            tripleCaptainUsed = team.tripleCaptainUsed,
            benchBoostUsed = team.benchBoostUsed,
            createdAt = team.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        // Update local database
        fantasyTeamDao.updateTeam(entity)

        // Sync to Firestore
        fantasyTeamFirestoreService.updateTeam(team.copy(updatedAt = System.currentTimeMillis()))
            .onFailure { error ->
                android.util.Log.e("FantasyRepo", "Failed to sync team update to Firestore: ${error.message}")
            }
    }

    override suspend fun deleteTeam(teamId: String) {
        val team = fantasyTeamDao.getTeamById(teamId)
        if (team != null) {
            // Delete from local database
            fantasyTeamDao.clearTeamCyclists(teamId)
            fantasyTeamDao.deleteTeam(team)

            // Delete from Firestore
            fantasyTeamFirestoreService.deleteTeam(teamId).onFailure { error ->
                android.util.Log.e("FantasyRepo", "Failed to delete team from Firestore: ${error.message}")
            }

            android.util.Log.d("FantasyRepo", "Team deleted from local and Firestore: $teamId")
        }
    }

    override fun getTeamCyclists(teamId: String): Flow<List<TeamCyclist>> {
        return fantasyTeamDao.getTeamCyclists(teamId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTeamCyclistsWithDetails(teamId: String): Flow<List<Pair<TeamCyclist, Cyclist>>> {
        // Use Firestore cyclists and try both local DB and Firestore for team cyclists
        return combine(
            fantasyTeamDao.getTeamCyclists(teamId),
            fantasyTeamFirestoreService.getTeamCyclists(teamId),
            cyclistFirestoreService.getValidatedCyclists()
        ) { localTeamCyclists, firestoreTeamCyclists, cyclists ->
            // Prefer local DB, but fall back to Firestore if local is empty
            val teamCyclists = if (localTeamCyclists.isNotEmpty()) {
                android.util.Log.d("FantasyRepo", "Using ${localTeamCyclists.size} cyclists from local DB")
                localTeamCyclists.map { it.toDomain() }
            } else if (firestoreTeamCyclists.isNotEmpty()) {
                android.util.Log.d("FantasyRepo", "Local DB empty, using ${firestoreTeamCyclists.size} cyclists from Firestore")
                // Save to local DB for next time (fire and forget)
                scope.launch {
                    try {
                        for (tc in firestoreTeamCyclists) {
                            fantasyTeamDao.addCyclistToTeam(
                                TeamCyclistEntity(
                                    teamId = tc.teamId,
                                    cyclistId = tc.cyclistId,
                                    isActive = tc.isActive,
                                    isCaptain = tc.isCaptain,
                                    purchasePrice = tc.purchasePrice,
                                    purchasedAt = tc.purchasedAt
                                )
                            )
                        }
                        android.util.Log.d("FantasyRepo", "Saved ${firestoreTeamCyclists.size} cyclists to local DB")
                    } catch (e: Exception) {
                        android.util.Log.e("FantasyRepo", "Error saving cyclists to local DB: ${e.message}")
                    }
                }
                firestoreTeamCyclists
            } else {
                android.util.Log.d("FantasyRepo", "No team cyclists found in local DB or Firestore")
                emptyList()
            }

            android.util.Log.d("FantasyRepo", "getTeamCyclistsWithDetails: ${teamCyclists.size} team cyclists, ${cyclists.size} Firestore cyclists")
            val cyclistMap = cyclists.associateBy { it.id }
            val result = teamCyclists.mapNotNull { tc ->
                cyclistMap[tc.cyclistId]?.let { cyclist ->
                    tc to cyclist
                }
            }
            android.util.Log.d("FantasyRepo", "Matched ${result.size} cyclists with details")
            result
        }
    }

    override fun getActiveCyclists(teamId: String): Flow<List<TeamCyclist>> {
        return fantasyTeamDao.getActiveCyclists(teamId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addCyclistToTeam(teamId: String, cyclist: Cyclist): Result<Unit> {
        return try {
            android.util.Log.d("FantasyRepo", "addCyclistToTeam: teamId=$teamId, cyclist=${cyclist.fullName}")

            // Check if team is frozen (race day)
            if (isTeamFrozen()) {
                val raceName = getRaceTodayName() ?: "corrida"
                return Result.failure(Exception("Equipa bloqueada! $raceName está a decorrer hoje. Alterações não permitidas."))
            }

            // Check team size
            val currentSize = fantasyTeamDao.getTeamSize(teamId)
            android.util.Log.d("FantasyRepo", "Current team size: $currentSize")
            if (currentSize >= MAX_TEAM_SIZE) {
                return Result.failure(Exception("Equipa cheia. Máximo de $MAX_TEAM_SIZE ciclistas."))
            }

            // Check if already in team
            if (fantasyTeamDao.isCyclistInTeam(teamId, cyclist.id)) {
                return Result.failure(Exception("${cyclist.fullName} já está na tua equipa."))
            }

            // Check max from same pro team
            val sameTeamCount = fantasyTeamDao.countCyclistsFromProTeam(teamId, cyclist.teamId)
            android.util.Log.d("FantasyRepo", "Same team count for ${cyclist.teamName}: $sameTeamCount")
            if (sameTeamCount >= MAX_FROM_SAME_TEAM) {
                return Result.failure(Exception("Máximo de $MAX_FROM_SAME_TEAM ciclistas da mesma equipa (${cyclist.teamName})."))
            }

            // Check budget
            val team = fantasyTeamDao.getTeamById(teamId)
            if (team == null) {
                android.util.Log.e("FantasyRepo", "Team not found: $teamId")
                return Result.failure(Exception("Equipa não encontrada."))
            }

            android.util.Log.d("FantasyRepo", "Budget: ${team.budget}M, cyclist price: ${cyclist.price}M")
            if (team.budget < cyclist.price) {
                return Result.failure(Exception("Orçamento insuficiente. Tens ${String.format("%.1f", team.budget)}M, precisas de ${cyclist.displayPrice}."))
            }

            // Add cyclist
            android.util.Log.d("FantasyRepo", "Adding cyclist to team...")
            fantasyTeamDao.addCyclistToTeam(
                TeamCyclistEntity(
                    teamId = teamId,
                    cyclistId = cyclist.id,
                    isActive = false,
                    isCaptain = false,
                    purchasePrice = cyclist.price
                )
            )

            // Update budget
            val newBudget = team.budget - cyclist.price
            android.util.Log.d("FantasyRepo", "Updating budget: ${team.budget}M -> ${newBudget}M")
            fantasyTeamDao.updateBudget(teamId, newBudget)

            // Sync team cyclists to Firestore
            syncTeamCyclistsToFirestore(teamId)

            android.util.Log.d("FantasyRepo", "Cyclist added successfully: ${cyclist.fullName}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error adding cyclist: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sync all team cyclists to Firestore for rankings
     */
    private suspend fun syncTeamCyclistsToFirestore(teamId: String) {
        try {
            val cyclists = fantasyTeamDao.getTeamCyclists(teamId).first()
            val domainCyclists = cyclists.map { it.toDomain() }
            fantasyTeamFirestoreService.syncTeamCyclists(teamId, domainCyclists).onFailure { error ->
                android.util.Log.e("FantasyRepo", "Failed to sync cyclists to Firestore: ${error.message}")
            }

            // Also sync the team budget
            val team = fantasyTeamDao.getTeamById(teamId)
            if (team != null) {
                fantasyTeamFirestoreService.updateTeam(team.toDomain())
            }
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error syncing cyclists to Firestore: ${e.message}")
        }
    }

    override suspend fun removeCyclistFromTeam(teamId: String, cyclistId: String): Result<Unit> {
        return try {
            // Check if team is frozen (race day)
            if (isTeamFrozen()) {
                val raceName = getRaceTodayName() ?: "corrida"
                return Result.failure(Exception("Equipa bloqueada! $raceName está a decorrer hoje. Alterações não permitidas."))
            }

            // First try local database, then Firestore
            var cyclistPrice: Double? = cyclistDao.getCyclistById(cyclistId)?.price

            if (cyclistPrice == null) {
                // Try to find in Firestore
                val firestoreCyclists = cyclistFirestoreService.getValidatedCyclists().first()
                cyclistPrice = firestoreCyclists.find { it.id == cyclistId }?.price
            }

            if (cyclistPrice == null) {
                android.util.Log.e("FantasyRepo", "Cyclist not found for removal: $cyclistId")
                return Result.failure(Exception("Ciclista não encontrado."))
            }

            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            fantasyTeamDao.removeCyclistFromTeam(teamId, cyclistId)

            // Refund current price
            fantasyTeamDao.updateBudget(teamId, team.budget + cyclistPrice)

            // Sync team cyclists to Firestore
            syncTeamCyclistsToFirestore(teamId)

            android.util.Log.d("FantasyRepo", "Removed cyclist $cyclistId, refunded ${cyclistPrice}M")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error removing cyclist: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun setCaptain(teamId: String, cyclistId: String): Result<Unit> {
        return try {
            // Check if team is frozen (race day)
            if (isTeamFrozen()) {
                val raceName = getRaceTodayName() ?: "corrida"
                return Result.failure(Exception("Equipa bloqueada! $raceName está a decorrer hoje. Não podes mudar o capitão."))
            }

            fantasyTeamDao.clearCaptain(teamId)
            fantasyTeamDao.setCaptain(teamId, cyclistId)

            // Sync to Firestore
            syncTeamCyclistsToFirestore(teamId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setActive(teamId: String, cyclistId: String, isActive: Boolean): Result<Unit> {
        return try {
            // Check if team is frozen (race day)
            if (isTeamFrozen()) {
                val raceName = getRaceTodayName() ?: "corrida"
                return Result.failure(Exception("Equipa bloqueada! $raceName está a decorrer hoje. Não podes alterar a formação."))
            }

            if (isActive) {
                val activeCount = fantasyTeamDao.getActiveCount(teamId)
                if (activeCount >= MAX_ACTIVE_CYCLISTS) {
                    return Result.failure(Exception("Máximo de $MAX_ACTIVE_CYCLISTS ciclistas ativos."))
                }
            }

            fantasyTeamDao.setActive(teamId, cyclistId, isActive)

            // Sync to Firestore
            syncTeamCyclistsToFirestore(teamId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTeamSize(teamId: String): Int {
        return fantasyTeamDao.getTeamSize(teamId)
    }

    override suspend fun getActiveCount(teamId: String): Int {
        return fantasyTeamDao.getActiveCount(teamId)
    }

    override suspend fun isCyclistInTeam(teamId: String, cyclistId: String): Boolean {
        return fantasyTeamDao.isCyclistInTeam(teamId, cyclistId)
    }

    override suspend fun countCyclistsFromProTeam(teamId: String, proTeamId: String): Int {
        return fantasyTeamDao.countCyclistsFromProTeam(teamId, proTeamId)
    }

    override suspend fun useWildcard(teamId: String): Result<Unit> {
        return try {
            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            if (team.wildcardUsed) {
                return Result.failure(Exception("Wildcard já foi usado."))
            }

            fantasyTeamDao.useWildcard(teamId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun useTripleCaptain(teamId: String): Result<Unit> {
        return try {
            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            if (team.tripleCaptainUsed) {
                return Result.failure(Exception("Triple Captain já foi usado."))
            }

            fantasyTeamDao.useTripleCaptain(teamId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun useBenchBoost(teamId: String): Result<Unit> {
        return try {
            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            if (team.benchBoostUsed) {
                return Result.failure(Exception("Bench Boost já foi usado."))
            }

            // Get the IDs of cyclists currently on the bench
            val benchCyclistIds = fantasyTeamDao.getBenchCyclistIds(teamId)
            val originalBenchString = benchCyclistIds.joinToString(",")

            android.util.Log.d("FantasyRepo", "Activating Bench Boost. Original bench: $originalBenchString")

            // Mark bench boost as used and active, storing original bench cyclists
            fantasyTeamDao.useBenchBoost(teamId, originalBenchString)

            // Activate ALL cyclists (including bench)
            fantasyTeamDao.activateAllCyclists(teamId)

            // Sync to Firestore
            syncTeamCyclistsToFirestore(teamId)

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error using Bench Boost: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Reset Bench Boost after a race - puts bench cyclists back to inactive
     */
    suspend fun resetBenchBoost(teamId: String): Result<Unit> {
        return try {
            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            if (!team.benchBoostActive) {
                return Result.failure(Exception("Bench Boost não está ativo."))
            }

            val originalBench = team.benchBoostOriginalBench
            if (originalBench.isNullOrBlank()) {
                android.util.Log.w("FantasyRepo", "No original bench data found")
            } else {
                // Deactivate the original bench cyclists
                val benchIds = originalBench.split(",")
                for (cyclistId in benchIds) {
                    if (cyclistId.isNotBlank()) {
                        fantasyTeamDao.setActive(teamId, cyclistId, false)
                    }
                }
                android.util.Log.d("FantasyRepo", "Reset ${benchIds.size} cyclists to bench")
            }

            // Deactivate bench boost
            fantasyTeamDao.deactivateBenchBoost(teamId)

            // Sync to Firestore
            syncTeamCyclistsToFirestore(teamId)

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error resetting Bench Boost: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Cancel Triple Captain before race starts.
     * Refunds the wildcard (marks as not used, not active).
     */
    override suspend fun cancelTripleCaptain(teamId: String): Result<Unit> {
        return try {
            // Check if team is frozen (race day)
            if (isTeamFrozen()) {
                val raceName = getRaceTodayName() ?: "corrida"
                return Result.failure(Exception("Não podes cancelar! $raceName já começou."))
            }

            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            if (!team.tripleCaptainActive) {
                return Result.failure(Exception("Triple Captain não está ativo."))
            }

            // Deactivate Triple Captain and refund (mark as not used)
            fantasyTeamDao.deactivateTripleCaptain(teamId)
            // Also mark as not used so it can be used again
            fantasyTeamDao.updateTeam(team.copy(tripleCaptainUsed = false, tripleCaptainActive = false))

            android.util.Log.d("FantasyRepo", "Triple Captain cancelled for team $teamId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error cancelling Triple Captain: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Cancel Bench Boost before race starts.
     * Refunds the wildcard and restores original bench.
     */
    override suspend fun cancelBenchBoost(teamId: String): Result<Unit> {
        return try {
            // Check if team is frozen (race day)
            if (isTeamFrozen()) {
                val raceName = getRaceTodayName() ?: "corrida"
                return Result.failure(Exception("Não podes cancelar! $raceName já começou."))
            }

            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            if (!team.benchBoostActive) {
                return Result.failure(Exception("Bench Boost não está ativo."))
            }

            // Restore original bench cyclists to inactive
            val originalBench = team.benchBoostOriginalBench
            if (!originalBench.isNullOrBlank()) {
                val benchIds = originalBench.split(",")
                for (cyclistId in benchIds) {
                    if (cyclistId.isNotBlank()) {
                        fantasyTeamDao.setActive(teamId, cyclistId, false)
                    }
                }
                android.util.Log.d("FantasyRepo", "Restored ${benchIds.size} cyclists to bench")
            }

            // Deactivate Bench Boost and refund (mark as not used)
            fantasyTeamDao.deactivateBenchBoost(teamId)
            // Also mark as not used so it can be used again
            fantasyTeamDao.updateTeam(team.copy(benchBoostUsed = false, benchBoostActive = false, benchBoostOriginalBench = null))

            // Sync to Firestore
            syncTeamCyclistsToFirestore(teamId)

            android.util.Log.d("FantasyRepo", "Bench Boost cancelled for team $teamId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error cancelling Bench Boost: ${e.message}")
            Result.failure(e)
        }
    }

    // ========== Per-Race Wildcard Activation (New System) ==========

    /**
     * Activate Triple Captain for a specific race.
     * Captain's points are tripled for this race only.
     */
    override suspend fun activateTripleCaptainForRace(teamId: String, raceId: String): Result<Unit> {
        return try {
            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            if (team.tripleCaptainUsed) {
                return Result.failure(Exception("Triple Captain já foi usado esta temporada."))
            }

            fantasyTeamDao.activateTripleCaptainForRace(teamId, raceId)

            // Sync to Firestore
            val updatedTeam = fantasyTeamDao.getTeamById(teamId)
            if (updatedTeam != null) {
                fantasyTeamFirestoreService.updateTeam(updatedTeam.toDomain())
            }

            android.util.Log.d("FantasyRepo", "Triple Captain activated for race $raceId, team $teamId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error activating Triple Captain: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Activate Bench Boost for a specific race.
     * All bench cyclists score points for this race only.
     */
    override suspend fun activateBenchBoostForRace(teamId: String, raceId: String): Result<Unit> {
        return try {
            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            if (team.benchBoostUsed) {
                return Result.failure(Exception("Bench Boost já foi usado esta temporada."))
            }

            // Get the IDs of cyclists currently on the bench
            val benchCyclistIds = fantasyTeamDao.getBenchCyclistIds(teamId)
            val originalBenchString = benchCyclistIds.joinToString(",")

            android.util.Log.d("FantasyRepo", "Activating Bench Boost for race $raceId. Original bench: $originalBenchString")

            // Activate bench boost for race
            fantasyTeamDao.activateBenchBoostForRace(teamId, raceId, originalBenchString)

            // Activate ALL cyclists (including bench)
            fantasyTeamDao.activateAllCyclists(teamId)

            // Sync to Firestore
            syncTeamCyclistsToFirestore(teamId)

            android.util.Log.d("FantasyRepo", "Bench Boost activated for race $raceId, team $teamId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error activating Bench Boost: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Activate Wildcard for a specific race.
     * Unlimited transfers without penalty for this race only.
     */
    override suspend fun activateWildcardForRace(teamId: String, raceId: String): Result<Unit> {
        return try {
            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            if (team.wildcardUsed) {
                return Result.failure(Exception("Wildcard já foi usado esta temporada."))
            }

            fantasyTeamDao.activateWildcardForRace(teamId, raceId)

            // Sync to Firestore
            val updatedTeam = fantasyTeamDao.getTeamById(teamId)
            if (updatedTeam != null) {
                fantasyTeamFirestoreService.updateTeam(updatedTeam.toDomain())
            }

            android.util.Log.d("FantasyRepo", "Wildcard activated for race $raceId, team $teamId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error activating Wildcard: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Cancel Wildcard before race starts.
     * Refunds the wildcard (marks as not used).
     */
    override suspend fun cancelWildcard(teamId: String): Result<Unit> {
        return try {
            // Check if team is frozen (race day)
            if (isTeamFrozen()) {
                val raceName = getRaceTodayName() ?: "corrida"
                return Result.failure(Exception("Não podes cancelar! $raceName já começou."))
            }

            val team = fantasyTeamDao.getTeamById(teamId)
                ?: return Result.failure(Exception("Equipa não encontrada."))

            if (!team.wildcardActive) {
                return Result.failure(Exception("Wildcard não está ativo."))
            }

            fantasyTeamDao.cancelWildcardForRace(teamId)

            // Sync to Firestore
            val updatedTeam = fantasyTeamDao.getTeamById(teamId)
            if (updatedTeam != null) {
                fantasyTeamFirestoreService.updateTeam(updatedTeam.toDomain())
            }

            android.util.Log.d("FantasyRepo", "Wildcard cancelled for team $teamId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error cancelling Wildcard: ${e.message}")
            Result.failure(e)
        }
    }

    // Rankings - delegate to Firestore service
    override fun getAllTeamsForRanking(): Flow<List<FantasyTeam>> {
        return fantasyTeamFirestoreService.getAllTeamsForRanking()
    }

    override suspend fun getTopTeams(limit: Int): Result<List<FantasyTeam>> {
        return fantasyTeamFirestoreService.getTopTeams(limit)
    }

    override suspend fun getUserRank(userId: String): Int? {
        return fantasyTeamFirestoreService.getUserRank(userId)
    }

    override suspend fun getTotalTeamsCount(): Int {
        return fantasyTeamFirestoreService.getTotalTeamsCount()
    }

    override suspend fun userHasTeam(userId: String): Boolean {
        // Check both Firestore and local database
        val hasFirestoreTeam = fantasyTeamFirestoreService.userHasTeam(userId)
        val hasLocalTeam = fantasyTeamDao.getTeamByUserId(userId).first() != null
        return hasFirestoreTeam || hasLocalTeam
    }

    /**
     * Sync team from Firestore to local database.
     * Called when user logs in after reinstalling the app.
     */
    override suspend fun syncTeamFromFirestore(userId: String): Result<FantasyTeam?> {
        return try {
            android.util.Log.d("FantasyRepo", "Syncing team from Firestore for user: $userId")

            // Check if team already exists locally
            val localTeam = fantasyTeamDao.getTeamByUserId(userId).first()
            if (localTeam != null) {
                android.util.Log.d("FantasyRepo", "Team already exists locally: ${localTeam.id}")
                return Result.success(localTeam.toDomain())
            }

            // Try to get team from Firestore
            val firestoreTeam = fantasyTeamFirestoreService.getTeamByUserId(userId).first()
            if (firestoreTeam == null) {
                android.util.Log.d("FantasyRepo", "No team found in Firestore for user: $userId")
                return Result.success(null)
            }

            android.util.Log.d("FantasyRepo", "Found team in Firestore: ${firestoreTeam.id}, syncing to local...")

            // Save team to local database
            val teamEntity = FantasyTeamEntity(
                id = firestoreTeam.id,
                userId = firestoreTeam.userId,
                teamName = firestoreTeam.teamName,
                budget = firestoreTeam.budget,
                totalPoints = firestoreTeam.totalPoints,
                freeTransfers = firestoreTeam.freeTransfers,
                gameweek = firestoreTeam.gameweek,
                wildcardUsed = firestoreTeam.wildcardUsed,
                tripleCaptainUsed = firestoreTeam.tripleCaptainUsed,
                benchBoostUsed = firestoreTeam.benchBoostUsed,
                benchBoostActive = firestoreTeam.benchBoostActive,
                benchBoostOriginalBench = firestoreTeam.benchBoostOriginalBench,
                tripleCaptainActive = firestoreTeam.tripleCaptainActive,
                createdAt = firestoreTeam.createdAt,
                updatedAt = firestoreTeam.updatedAt
            )
            fantasyTeamDao.insertTeam(teamEntity)

            // Sync team cyclists from Firestore
            val firestoreCyclists = fantasyTeamFirestoreService.getTeamCyclists(firestoreTeam.id).first()
            android.util.Log.d("FantasyRepo", "Found ${firestoreCyclists.size} cyclists in Firestore team")

            for (tc in firestoreCyclists) {
                val cyclistEntity = TeamCyclistEntity(
                    teamId = tc.teamId,
                    cyclistId = tc.cyclistId,
                    isActive = tc.isActive,
                    isCaptain = tc.isCaptain,
                    purchasePrice = tc.purchasePrice,
                    purchasedAt = tc.purchasedAt
                )
                fantasyTeamDao.addCyclistToTeam(cyclistEntity)
            }

            // Ensure user is in Liga Portugal (auto-join if not)
            try {
                val globalLeague = leagueFirestoreService.ensureGlobalLeagueExists()
                val isMember = leagueFirestoreService.isMember(globalLeague.id, userId)
                if (!isMember) {
                    leagueFirestoreService.joinLeague(
                        leagueId = globalLeague.id,
                        userId = userId,
                        teamId = firestoreTeam.id,
                        teamName = firestoreTeam.teamName
                    )
                    android.util.Log.d("FantasyRepo", "User $userId auto-joined Liga Portugal ${globalLeague.season} after sync")
                }
            } catch (e: Exception) {
                android.util.Log.e("FantasyRepo", "Error checking/joining Liga Portugal: ${e.message}")
            }

            android.util.Log.d("FantasyRepo", "Team sync complete: ${firestoreTeam.teamName}")
            Result.success(firestoreTeam)
        } catch (e: Exception) {
            android.util.Log.e("FantasyRepo", "Error syncing team from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }
}
