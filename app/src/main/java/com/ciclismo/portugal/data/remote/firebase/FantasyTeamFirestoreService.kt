package com.ciclismo.portugal.data.remote.firebase

import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.TeamCyclist
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FantasyTeamFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_SEASONS = "seasons"
        private const val COLLECTION_TEAMS = "fantasy_teams"
        private const val COLLECTION_TEAM_CYCLISTS = "team_cyclists"
        private const val TAG = "FantasyTeamFirestore"
    }

    /**
     * Get the collection path for fantasy teams in a specific season
     * Pattern: seasons/{season}/fantasy_teams
     */
    private fun teamsCollection(season: Int = SeasonConfig.CURRENT_SEASON) =
        firestore.collection(COLLECTION_SEASONS)
            .document(season.toString())
            .collection(COLLECTION_TEAMS)

    /**
     * Check if user already has a team for a specific season
     * Returns the existing team ID if found, null otherwise
     * @param season the season to check for (defaults to current season)
     */
    suspend fun getUserTeamId(userId: String, season: Int = SeasonConfig.CURRENT_SEASON): String? {
        return try {
            val snapshot = teamsCollection(season)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.first().id
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error checking user team: ${e.message}")
            null
        }
    }

    /**
     * Check if user already has a team (returns boolean)
     * @param season the season to check for (defaults to current season)
     */
    suspend fun userHasTeam(userId: String, season: Int = SeasonConfig.CURRENT_SEASON): Boolean {
        return getUserTeamId(userId, season) != null
    }

    /**
     * Create a new team in Firestore
     * @param season the season to create the team in (defaults to current season)
     * @throws IllegalStateException if user already has a team
     */
    suspend fun createTeam(team: FantasyTeam, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            // First check if user already has a team for this season
            if (userHasTeam(team.userId, season)) {
                android.util.Log.w(TAG, "User ${team.userId} already has a team for season $season")
                return Result.failure(IllegalStateException("Utilizador já tem uma equipa para esta temporada. Apenas uma equipa por utilizador é permitida."))
            }

            val teamWithSeason = team.copy(season = season)
            val data = teamWithSeason.toFirestoreMap()
            teamsCollection(season)
                .document(team.id)
                .set(data)
                .await()

            android.util.Log.d(TAG, "Team created: ${team.id} for user ${team.userId} (season $season)")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating team: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update an existing team
     * @param season the season the team belongs to (defaults to current season)
     */
    suspend fun updateTeam(team: FantasyTeam, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            val data = team.toFirestoreMap()
            teamsCollection(season)
                .document(team.id)
                .set(data)
                .await()

            android.util.Log.d(TAG, "Team updated: ${team.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating team: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete a team from Firestore
     * @param season the season the team belongs to (defaults to current season)
     */
    suspend fun deleteTeam(teamId: String, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            // Delete team cyclists first
            val cyclistsSnapshot = teamsCollection(season)
                .document(teamId)
                .collection(COLLECTION_TEAM_CYCLISTS)
                .get()
                .await()

            for (doc in cyclistsSnapshot.documents) {
                doc.reference.delete().await()
            }

            // Then delete the team
            teamsCollection(season)
                .document(teamId)
                .delete()
                .await()

            android.util.Log.d(TAG, "Team deleted: $teamId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error deleting team: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get team by ID (one-time fetch)
     * @param season the season the team belongs to (defaults to current season)
     */
    suspend fun getTeamById(teamId: String, season: Int = SeasonConfig.CURRENT_SEASON): FantasyTeam? {
        return try {
            val doc = teamsCollection(season)
                .document(teamId)
                .get()
                .await()

            doc.toFantasyTeam(season)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting team: ${e.message}")
            null
        }
    }

    /**
     * Get team by user ID (Flow for real-time updates)
     * @param season the season to get the team from (defaults to current season)
     */
    fun getTeamByUserId(userId: String, season: Int = SeasonConfig.CURRENT_SEASON): Flow<FantasyTeam?> = callbackFlow {
        android.util.Log.d(TAG, "Setting up listener for user team: $userId (season $season)")

        val listener = teamsCollection(season)
            .whereEqualTo("userId", userId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e(TAG, "Error listening to team: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val team = snapshot?.documents?.firstOrNull()?.toFantasyTeam(season)
                android.util.Log.d(TAG, "Team update received: ${team?.id}")
                trySend(team)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Sync team cyclists to Firestore
     * @param season the season the team belongs to (defaults to current season)
     */
    suspend fun syncTeamCyclists(teamId: String, cyclists: List<TeamCyclist>, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            val teamRef = teamsCollection(season).document(teamId)
            val cyclistsRef = teamRef.collection(COLLECTION_TEAM_CYCLISTS)

            // Clear existing cyclists
            val existingDocs = cyclistsRef.get().await()
            for (doc in existingDocs.documents) {
                doc.reference.delete().await()
            }

            // Add new cyclists with season
            cyclists.forEach { tc ->
                val tcWithSeason = tc.copy(season = season)
                val data = tcWithSeason.toFirestoreMap()
                cyclistsRef.document(tc.cyclistId).set(data).await()
            }

            android.util.Log.d(TAG, "Synced ${cyclists.size} cyclists for team $teamId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error syncing team cyclists: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get team cyclists from Firestore
     * @param season the season the team belongs to (defaults to current season)
     */
    fun getTeamCyclists(teamId: String, season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<TeamCyclist>> = callbackFlow {
        val listener = teamsCollection(season)
            .document(teamId)
            .collection(COLLECTION_TEAM_CYCLISTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e(TAG, "Error listening to team cyclists: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val cyclists = snapshot?.documents?.mapNotNull { it.toTeamCyclist(season) } ?: emptyList()
                android.util.Log.d(TAG, "Team cyclists update: ${cyclists.size}")
                trySend(cyclists)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get all teams for ranking (sorted by total points)
     * @param season the season to get rankings from (defaults to current season)
     */
    fun getAllTeamsForRanking(season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<FantasyTeam>> = callbackFlow {
        android.util.Log.d(TAG, "Setting up listener for ranking (season $season)...")

        val listener = teamsCollection(season)
            .orderBy("totalPoints", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e(TAG, "Error listening to ranking: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val teams = snapshot?.documents?.mapNotNull { it.toFantasyTeam(season) } ?: emptyList()
                android.util.Log.d(TAG, "Ranking update: ${teams.size} teams")
                trySend(teams)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get top N teams for ranking (one-time fetch)
     * @param season the season to get rankings from (defaults to current season)
     */
    suspend fun getTopTeams(limit: Int = 100, season: Int = SeasonConfig.CURRENT_SEASON): Result<List<FantasyTeam>> {
        return try {
            val snapshot = teamsCollection(season)
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val teams = snapshot.documents.mapNotNull { it.toFantasyTeam(season) }
            android.util.Log.d(TAG, "Fetched top ${teams.size} teams")
            Result.success(teams)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching top teams: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update team points after a race
     * @param season the season the team belongs to (defaults to current season)
     */
    suspend fun updateTeamPoints(teamId: String, pointsToAdd: Int, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            val teamRef = teamsCollection(season).document(teamId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(teamRef)
                val currentPoints = snapshot.getLong("totalPoints")?.toInt() ?: 0
                transaction.update(teamRef, "totalPoints", currentPoints + pointsToAdd)
                transaction.update(teamRef, "updatedAt", System.currentTimeMillis())
            }.await()

            android.util.Log.d(TAG, "Updated points for team $teamId: +$pointsToAdd")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating team points: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get user's rank in the global ranking
     * @param season the season to get ranking from (defaults to current season)
     */
    suspend fun getUserRank(userId: String, season: Int = SeasonConfig.CURRENT_SEASON): Int? {
        return try {
            // First get user's team
            val userTeam = teamsCollection(season)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toFantasyTeam(season)
                ?: return null

            // Count teams with more points
            val teamsAhead = teamsCollection(season)
                .whereGreaterThan("totalPoints", userTeam.totalPoints)
                .get()
                .await()
                .size()

            teamsAhead + 1 // Rank is 1-indexed
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting user rank: ${e.message}")
            null
        }
    }

    /**
     * Get total number of teams
     * @param season the season to count teams from (defaults to current season)
     */
    suspend fun getTotalTeamsCount(season: Int = SeasonConfig.CURRENT_SEASON): Int {
        return try {
            val snapshot = teamsCollection(season).get().await()
            snapshot.size()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting teams count: ${e.message}")
            0
        }
    }

    /**
     * Reset all fantasy teams for a season.
     * This resets points, transfers, wildcards but keeps the team structure.
     * @param season the season to reset (defaults to current season)
     * @param deleteTeamCyclists if true, also removes all cyclists from teams
     * @return number of teams reset
     */
    suspend fun resetAllTeamsForSeason(
        season: Int = SeasonConfig.CURRENT_SEASON,
        deleteTeamCyclists: Boolean = false
    ): Result<Int> {
        return try {
            val snapshot = teamsCollection(season).get().await()
            var resetCount = 0

            for (doc in snapshot.documents) {
                val teamId = doc.id

                // Reset team data
                val resetData = mapOf(
                    "totalPoints" to 0,
                    "freeTransfers" to 2,
                    "transfersMadeThisWeek" to 0,
                    "gameweek" to 1,
                    "wildcardUsed" to false,
                    "wildcardActive" to false,
                    "tripleCaptainUsed" to false,
                    "tripleCaptainActive" to false,
                    "benchBoostUsed" to false,
                    "benchBoostActive" to false,
                    "updatedAt" to System.currentTimeMillis()
                )

                doc.reference.update(resetData).await()

                // Optionally delete team cyclists
                if (deleteTeamCyclists) {
                    val cyclistsSnapshot = doc.reference
                        .collection(COLLECTION_TEAM_CYCLISTS)
                        .get()
                        .await()

                    for (cyclistDoc in cyclistsSnapshot.documents) {
                        cyclistDoc.reference.delete().await()
                    }
                }

                resetCount++
                android.util.Log.d(TAG, "Reset team: $teamId")
            }

            android.util.Log.d(TAG, "Reset $resetCount teams for season $season")
            Result.success(resetCount)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error resetting teams: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete all fantasy teams for a season.
     * WARNING: This permanently deletes all teams and their cyclists.
     * @param season the season to delete teams from
     * @return number of teams deleted
     */
    suspend fun deleteAllTeamsForSeason(season: Int = SeasonConfig.CURRENT_SEASON): Result<Int> {
        return try {
            val snapshot = teamsCollection(season).get().await()
            var deleteCount = 0

            for (doc in snapshot.documents) {
                // Delete team cyclists first
                val cyclistsSnapshot = doc.reference
                    .collection(COLLECTION_TEAM_CYCLISTS)
                    .get()
                    .await()

                for (cyclistDoc in cyclistsSnapshot.documents) {
                    cyclistDoc.reference.delete().await()
                }

                // Delete the team
                doc.reference.delete().await()
                deleteCount++
                android.util.Log.d(TAG, "Deleted team: ${doc.id}")
            }

            android.util.Log.d(TAG, "Deleted $deleteCount teams for season $season")
            Result.success(deleteCount)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error deleting teams: ${e.message}")
            Result.failure(e)
        }
    }

    // Extension functions for conversion
    private fun FantasyTeam.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "teamName" to teamName,
        "budget" to budget,
        "totalPoints" to totalPoints,
        "freeTransfers" to freeTransfers,
        "gameweek" to gameweek,
        "wildcardUsed" to wildcardUsed,
        "tripleCaptainUsed" to tripleCaptainUsed,
        "benchBoostUsed" to benchBoostUsed,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "season" to season
    )

    private fun TeamCyclist.toFirestoreMap(): Map<String, Any?> = mapOf(
        "teamId" to teamId,
        "cyclistId" to cyclistId,
        "isActive" to isActive,
        "isCaptain" to isCaptain,
        "purchasePrice" to purchasePrice,
        "purchasedAt" to purchasedAt,
        "season" to season
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toFantasyTeam(season: Int = SeasonConfig.CURRENT_SEASON): FantasyTeam? {
        return try {
            FantasyTeam(
                id = getString("id") ?: id,
                userId = getString("userId") ?: return null,
                teamName = getString("teamName") ?: "Sem Nome",
                budget = getDouble("budget") ?: 100.0,
                totalPoints = getLong("totalPoints")?.toInt() ?: 0,
                freeTransfers = getLong("freeTransfers")?.toInt() ?: 2,
                gameweek = getLong("gameweek")?.toInt() ?: 1,
                wildcardUsed = getBoolean("wildcardUsed") ?: false,
                tripleCaptainUsed = getBoolean("tripleCaptainUsed") ?: false,
                benchBoostUsed = getBoolean("benchBoostUsed") ?: false,
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
                season = getLong("season")?.toInt() ?: season
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error parsing team: ${e.message}")
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toTeamCyclist(season: Int = SeasonConfig.CURRENT_SEASON): TeamCyclist? {
        return try {
            TeamCyclist(
                teamId = getString("teamId") ?: return null,
                cyclistId = getString("cyclistId") ?: id,
                isActive = getBoolean("isActive") ?: false,
                isCaptain = getBoolean("isCaptain") ?: false,
                purchasePrice = getDouble("purchasePrice") ?: 0.0,
                purchasedAt = getLong("purchasedAt") ?: System.currentTimeMillis(),
                season = getLong("season")?.toInt() ?: season
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error parsing team cyclist: ${e.message}")
            null
        }
    }
}
