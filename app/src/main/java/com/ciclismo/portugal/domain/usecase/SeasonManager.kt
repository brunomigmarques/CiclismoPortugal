package com.ciclismo.portugal.domain.usecase

import android.content.SharedPreferences
import android.util.Log
import com.ciclismo.portugal.data.remote.firebase.LeagueFirestoreService
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SeasonManager handles all season-related operations:
 * - Getting/setting the current season
 * - Initializing new seasons in Firestore
 * - Getting available seasons
 * - Season statistics
 */
@Singleton
class SeasonManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sharedPreferences: SharedPreferences,
    private val leagueFirestoreService: LeagueFirestoreService
) {
    companion object {
        private const val TAG = "SeasonManager"
        private const val PREF_CURRENT_SEASON = "current_season"
        private const val COLLECTION_SEASONS = "seasons"
    }

    /**
     * Get the current active season
     * First checks SharedPreferences, falls back to SeasonConfig.CURRENT_SEASON
     */
    fun getCurrentSeason(): Int {
        return sharedPreferences.getInt(PREF_CURRENT_SEASON, SeasonConfig.CURRENT_SEASON)
    }

    /**
     * Set the current active season (admin only)
     * Persists to SharedPreferences
     */
    fun setCurrentSeason(season: Int) {
        sharedPreferences.edit()
            .putInt(PREF_CURRENT_SEASON, season)
            .apply()
        Log.d(TAG, "Current season set to $season")
    }

    /**
     * Get all available seasons from Firestore
     * Returns a list of years that have been initialized
     */
    suspend fun getAvailableSeasons(): List<Int> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SEASONS)
                .get()
                .await()

            val seasons = snapshot.documents.mapNotNull { doc ->
                doc.id.toIntOrNull()
            }.sortedDescending()

            Log.d(TAG, "Available seasons: $seasons")

            // If no seasons exist, return at least the current one
            if (seasons.isEmpty()) {
                listOf(SeasonConfig.CURRENT_SEASON)
            } else {
                seasons
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available seasons: ${e.message}")
            listOf(SeasonConfig.CURRENT_SEASON)
        }
    }

    /**
     * Initialize a new season in Firestore
     * Creates the season document and global league
     */
    suspend fun initializeSeason(season: Int): Result<Unit> {
        return try {
            // Create the season document with metadata
            val seasonData = mapOf(
                "season" to season,
                "createdAt" to System.currentTimeMillis(),
                "status" to "active"
            )

            firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .set(seasonData)
                .await()

            Log.d(TAG, "Season $season document created")

            // Create the global league for this season
            leagueFirestoreService.ensureGlobalLeagueExists(season)
            Log.d(TAG, "Global league created for season $season")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing season $season: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Check if a season has been initialized in Firestore
     */
    suspend fun isSeasonInitialized(season: Int): Boolean {
        return try {
            val doc = firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())
                .get()
                .await()

            doc.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking season: ${e.message}")
            false
        }
    }

    /**
     * Get statistics for a specific season
     */
    suspend fun getSeasonStats(season: Int): SeasonStats {
        return try {
            val seasonDoc = firestore.collection(COLLECTION_SEASONS)
                .document(season.toString())

            // Count cyclists
            val cyclistsCount = try {
                seasonDoc.collection("cyclists")
                    .get()
                    .await()
                    .size()
            } catch (e: Exception) { 0 }

            // Count races
            val racesCount = try {
                seasonDoc.collection("races")
                    .get()
                    .await()
                    .size()
            } catch (e: Exception) { 0 }

            // Count fantasy teams
            val teamsCount = try {
                seasonDoc.collection("fantasy_teams")
                    .get()
                    .await()
                    .size()
            } catch (e: Exception) { 0 }

            // Count leagues
            val leaguesCount = try {
                seasonDoc.collection("leagues")
                    .get()
                    .await()
                    .size()
            } catch (e: Exception) { 0 }

            SeasonStats(
                season = season,
                cyclistsCount = cyclistsCount,
                racesCount = racesCount,
                teamsCount = teamsCount,
                leaguesCount = leaguesCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting season stats: ${e.message}")
            SeasonStats(season = season)
        }
    }

    /**
     * Start a new season (admin action)
     * 1. Initializes the new season in Firestore
     * 2. Creates global league
     * 3. Updates current season
     */
    suspend fun startNewSeason(newSeason: Int): Result<Unit> {
        return try {
            // Check if season already exists
            if (isSeasonInitialized(newSeason)) {
                Log.w(TAG, "Season $newSeason already initialized")
                // Just switch to it
                setCurrentSeason(newSeason)
                return Result.success(Unit)
            }

            // Initialize the new season
            initializeSeason(newSeason).getOrThrow()

            // Update current season
            setCurrentSeason(newSeason)

            Log.d(TAG, "Successfully started season $newSeason")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting new season: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fix migration by copying only team_cyclists subcollections that may have been missed.
     * This is a one-time fix for migrations that ran before team_cyclists copy was added.
     */
    suspend fun fixTeamCyclistsMigration(targetSeason: Int): Result<Int> {
        Log.d(TAG, "Fixing team_cyclists migration for season $targetSeason")
        var count = 0

        try {
            val targetSeasonDoc = firestore.collection(COLLECTION_SEASONS)
                .document(targetSeason.toString())

            // Get all fantasy teams from the OLD flat structure
            val teamsSnapshot = firestore.collection("fantasy_teams")
                .get()
                .await()

            Log.d(TAG, "Found ${teamsSnapshot.size()} teams to fix team_cyclists")

            for (teamDoc in teamsSnapshot.documents) {
                try {
                    val teamCyclistsSnapshot = firestore.collection("fantasy_teams")
                        .document(teamDoc.id)
                        .collection("team_cyclists")
                        .get()
                        .await()

                    Log.d(TAG, "Team ${teamDoc.id}: ${teamCyclistsSnapshot.size()} cyclists to copy")

                    for (cyclistDoc in teamCyclistsSnapshot.documents) {
                        try {
                            val cyclistData = cyclistDoc.data?.toMutableMap() ?: continue
                            cyclistData["season"] = targetSeason

                            targetSeasonDoc.collection("fantasy_teams")
                                .document(teamDoc.id)
                                .collection("team_cyclists")
                                .document(cyclistDoc.id)
                                .set(cyclistData)
                                .await()

                            count++
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to copy cyclist ${cyclistDoc.id}: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fixing team ${teamDoc.id}: ${e.message}")
                }
            }

            Log.d(TAG, "Fixed $count team_cyclists total")
            return Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Fix migration failed: ${e.message}")
            return Result.failure(e)
        }
    }

    /**
     * Migrate data from old flat collection structure to new season-based structure.
     * This copies data from:
     *   - cyclists/ -> seasons/{season}/cyclists/
     *   - races/ -> seasons/{season}/races/
     *   - race_results/ -> seasons/{season}/race_results/
     *   - fantasy_teams/ -> seasons/{season}/fantasy_teams/
     *   - leagues/ -> seasons/{season}/leagues/
     *
     * @param targetSeason The season to migrate data to (e.g., 2026)
     * @return Result with migration statistics
     */
    suspend fun migrateFromFlatStructure(targetSeason: Int): Result<MigrationStats> {
        Log.d(TAG, "Starting migration from flat structure to season $targetSeason")

        val stats = MigrationStats()

        try {
            // First, ensure the season document exists
            initializeSeason(targetSeason)

            val targetSeasonDoc = firestore.collection(COLLECTION_SEASONS)
                .document(targetSeason.toString())

            // Migrate cyclists
            try {
                val cyclistsSnapshot = firestore.collection("cyclists")
                    .get()
                    .await()

                Log.d(TAG, "Found ${cyclistsSnapshot.size()} cyclists in old collection")

                for (doc in cyclistsSnapshot.documents) {
                    try {
                        val data = doc.data?.toMutableMap() ?: continue
                        data["season"] = targetSeason

                        targetSeasonDoc.collection("cyclists")
                            .document(doc.id)
                            .set(data)
                            .await()

                        stats.cyclistsMigrated++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to migrate cyclist ${doc.id}: ${e.message}")
                        stats.errors++
                    }
                }
                Log.d(TAG, "Migrated ${stats.cyclistsMigrated} cyclists")
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating cyclists: ${e.message}")
            }

            // Migrate races
            try {
                val racesSnapshot = firestore.collection("races")
                    .get()
                    .await()

                Log.d(TAG, "Found ${racesSnapshot.size()} races in old collection")

                for (doc in racesSnapshot.documents) {
                    try {
                        val data = doc.data?.toMutableMap() ?: continue
                        data["season"] = targetSeason

                        targetSeasonDoc.collection("races")
                            .document(doc.id)
                            .set(data)
                            .await()

                        stats.racesMigrated++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to migrate race ${doc.id}: ${e.message}")
                        stats.errors++
                    }
                }
                Log.d(TAG, "Migrated ${stats.racesMigrated} races")
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating races: ${e.message}")
            }

            // Migrate race_results
            try {
                val resultsSnapshot = firestore.collection("race_results")
                    .get()
                    .await()

                Log.d(TAG, "Found ${resultsSnapshot.size()} race results in old collection")

                for (doc in resultsSnapshot.documents) {
                    try {
                        val data = doc.data?.toMutableMap() ?: continue
                        data["season"] = targetSeason

                        targetSeasonDoc.collection("race_results")
                            .document(doc.id)
                            .set(data)
                            .await()

                        stats.resultsMigrated++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to migrate result ${doc.id}: ${e.message}")
                        stats.errors++
                    }
                }
                Log.d(TAG, "Migrated ${stats.resultsMigrated} race results")
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating race results: ${e.message}")
            }

            // Migrate fantasy_teams (including team_cyclists subcollections)
            try {
                val teamsSnapshot = firestore.collection("fantasy_teams")
                    .get()
                    .await()

                Log.d(TAG, "Found ${teamsSnapshot.size()} fantasy teams in old collection")

                for (doc in teamsSnapshot.documents) {
                    try {
                        val data = doc.data?.toMutableMap() ?: continue
                        data["season"] = targetSeason

                        // Copy the team document
                        targetSeasonDoc.collection("fantasy_teams")
                            .document(doc.id)
                            .set(data)
                            .await()

                        stats.teamsMigrated++

                        // Also migrate the team_cyclists subcollection
                        try {
                            val teamCyclistsSnapshot = firestore.collection("fantasy_teams")
                                .document(doc.id)
                                .collection("team_cyclists")
                                .get()
                                .await()

                            Log.d(TAG, "Found ${teamCyclistsSnapshot.size()} team_cyclists for team ${doc.id}")

                            for (cyclistDoc in teamCyclistsSnapshot.documents) {
                                try {
                                    val cyclistData = cyclistDoc.data?.toMutableMap() ?: continue
                                    cyclistData["season"] = targetSeason

                                    targetSeasonDoc.collection("fantasy_teams")
                                        .document(doc.id)
                                        .collection("team_cyclists")
                                        .document(cyclistDoc.id)
                                        .set(cyclistData)
                                        .await()

                                    stats.teamCyclistsMigrated++
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to migrate team_cyclist ${cyclistDoc.id}: ${e.message}")
                                    stats.errors++
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error migrating team_cyclists for team ${doc.id}: ${e.message}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to migrate team ${doc.id}: ${e.message}")
                        stats.errors++
                    }
                }
                Log.d(TAG, "Migrated ${stats.teamsMigrated} fantasy teams with ${stats.teamCyclistsMigrated} team_cyclists")
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating fantasy teams: ${e.message}")
            }

            // Migrate leagues (skip the global league as it's created by initializeSeason)
            try {
                val leaguesSnapshot = firestore.collection("leagues")
                    .get()
                    .await()

                Log.d(TAG, "Found ${leaguesSnapshot.size()} leagues in old collection")

                for (doc in leaguesSnapshot.documents) {
                    try {
                        // Skip global leagues - they're created per season
                        if (doc.id.contains("global")) continue

                        val data = doc.data?.toMutableMap() ?: continue
                        data["season"] = targetSeason

                        targetSeasonDoc.collection("leagues")
                            .document(doc.id)
                            .set(data)
                            .await()

                        stats.leaguesMigrated++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to migrate league ${doc.id}: ${e.message}")
                        stats.errors++
                    }
                }
                Log.d(TAG, "Migrated ${stats.leaguesMigrated} leagues")
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating leagues: ${e.message}")
            }

            // Migrate team_race_results
            try {
                val teamResultsSnapshot = firestore.collection("team_race_results")
                    .get()
                    .await()

                Log.d(TAG, "Found ${teamResultsSnapshot.size()} team race results in old collection")

                for (doc in teamResultsSnapshot.documents) {
                    try {
                        val data = doc.data?.toMutableMap() ?: continue
                        data["season"] = targetSeason

                        targetSeasonDoc.collection("team_race_results")
                            .document(doc.id)
                            .set(data)
                            .await()

                        stats.teamResultsMigrated++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to migrate team result ${doc.id}: ${e.message}")
                        stats.errors++
                    }
                }
                Log.d(TAG, "Migrated ${stats.teamResultsMigrated} team race results")
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating team race results: ${e.message}")
            }

            Log.d(TAG, "Migration complete: $stats")
            return Result.success(stats)

        } catch (e: Exception) {
            Log.e(TAG, "Migration failed: ${e.message}")
            return Result.failure(e)
        }
    }
}

/**
 * Statistics from a migration operation
 */
data class MigrationStats(
    var cyclistsMigrated: Int = 0,
    var racesMigrated: Int = 0,
    var resultsMigrated: Int = 0,
    var teamsMigrated: Int = 0,
    var teamCyclistsMigrated: Int = 0,
    var leaguesMigrated: Int = 0,
    var teamResultsMigrated: Int = 0,
    var errors: Int = 0
) {
    val totalMigrated: Int
        get() = cyclistsMigrated + racesMigrated + resultsMigrated +
                teamsMigrated + teamCyclistsMigrated + leaguesMigrated + teamResultsMigrated

    override fun toString(): String {
        return "Cyclists: $cyclistsMigrated, Races: $racesMigrated, Results: $resultsMigrated, " +
               "Teams: $teamsMigrated, TeamCyclists: $teamCyclistsMigrated, Leagues: $leaguesMigrated, " +
               "TeamResults: $teamResultsMigrated, Errors: $errors"
    }
}

/**
 * Statistics for a season
 */
data class SeasonStats(
    val season: Int,
    val cyclistsCount: Int = 0,
    val racesCount: Int = 0,
    val teamsCount: Int = 0,
    val leaguesCount: Int = 0
)
