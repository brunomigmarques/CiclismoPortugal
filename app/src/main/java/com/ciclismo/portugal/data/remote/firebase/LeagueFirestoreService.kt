package com.ciclismo.portugal.data.remote.firebase

import com.ciclismo.portugal.domain.model.League
import com.ciclismo.portugal.domain.model.LeagueMember
import com.ciclismo.portugal.domain.model.LeagueType
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeagueFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_SEASONS = "seasons"
        private const val COLLECTION_LEAGUES = "leagues"
        private const val COLLECTION_MEMBERS = "members"
        private const val TAG = "LeagueFirestore"
    }

    /**
     * Get the global league ID for a specific season
     */
    private fun globalLeagueId(season: Int) = "liga-portugal-global-$season"

    /**
     * Get the collection path for leagues in a specific season
     * Pattern: seasons/{season}/leagues
     */
    private fun leaguesCollection(season: Int = SeasonConfig.CURRENT_SEASON) =
        firestore.collection(COLLECTION_SEASONS)
            .document(season.toString())
            .collection(COLLECTION_LEAGUES)

    /**
     * Ensure the global league exists in Firestore for a specific season
     * @param season the season to create/get the global league for
     */
    suspend fun ensureGlobalLeagueExists(season: Int = SeasonConfig.CURRENT_SEASON): League {
        val leagueId = globalLeagueId(season)
        return try {
            val doc = leaguesCollection(season)
                .document(leagueId)
                .get()
                .await()

            if (doc.exists()) {
                doc.toLeague(season)!!
            } else {
                // Create global league for this season
                val globalLeague = League(
                    id = leagueId,
                    name = "Liga Portugal $season",
                    type = LeagueType.GLOBAL,
                    code = null,
                    ownerId = null,
                    region = null,
                    memberCount = 0,
                    createdAt = System.currentTimeMillis(),
                    season = season
                )

                leaguesCollection(season)
                    .document(leagueId)
                    .set(globalLeague.toFirestoreMap())
                    .await()

                android.util.Log.d(TAG, "Created global league for season $season")
                globalLeague
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error ensuring global league: ${e.message}")
            // Return a fallback
            League(
                id = leagueId,
                name = "Liga Portugal $season",
                type = LeagueType.GLOBAL,
                code = null,
                ownerId = null,
                region = null,
                memberCount = 0,
                createdAt = System.currentTimeMillis(),
                season = season
            )
        }
    }

    /**
     * Create a new league
     * @param season the season to create the league in (defaults to current season)
     */
    suspend fun createLeague(league: League, season: Int = SeasonConfig.CURRENT_SEASON): Result<League> {
        return try {
            val leagueWithSeason = league.copy(season = season)
            leaguesCollection(season)
                .document(league.id)
                .set(leagueWithSeason.toFirestoreMap())
                .await()

            android.util.Log.d(TAG, "League created: ${league.id} (season $season)")
            Result.success(leagueWithSeason)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating league: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete a league
     * @param season the season the league belongs to (defaults to current season)
     */
    suspend fun deleteLeague(leagueId: String, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            // Delete all members first
            val membersSnapshot = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .get()
                .await()

            for (doc in membersSnapshot.documents) {
                doc.reference.delete().await()
            }

            // Delete the league
            leaguesCollection(season)
                .document(leagueId)
                .delete()
                .await()

            android.util.Log.d(TAG, "League deleted: $leagueId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error deleting league: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all leagues (real-time)
     * @param season the season to get leagues from (defaults to current season)
     */
    fun getAllLeagues(season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<League>> = callbackFlow {
        val listener = leaguesCollection(season)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e(TAG, "Error getting leagues: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val leagues = snapshot?.documents?.mapNotNull { it.toLeague(season) } ?: emptyList()
                android.util.Log.d(TAG, "Leagues update: ${leagues.size}")
                trySend(leagues)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get leagues by type
     * @param season the season to get leagues from (defaults to current season)
     */
    fun getLeaguesByType(type: LeagueType, season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<League>> = callbackFlow {
        val listener = leaguesCollection(season)
            .whereEqualTo("type", type.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val leagues = snapshot?.documents?.mapNotNull { it.toLeague(season) } ?: emptyList()
                trySend(leagues)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get leagues for a specific user
     * Note: This uses collectionGroup query which requires a Firestore index.
     * If index is missing, it will emit empty list and log the error.
     * @param season the season to get leagues from (defaults to current season)
     */
    fun getLeaguesForUser(userId: String, season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<League>> = callbackFlow {
        android.util.Log.d(TAG, "Getting leagues for user: $userId (season $season)")

        try {
            val listener = firestore.collectionGroup(COLLECTION_MEMBERS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("season", season)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e(TAG, "Error getting user leagues: ${error.message}")
                        // Don't close the flow, just send empty list and log the error
                        // This allows the app to continue working even without the index
                        if (error.message?.contains("index") == true) {
                            android.util.Log.w(TAG, "Missing Firestore index for collectionGroup query. Create index in Firebase Console.")
                        }
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    // Get league IDs from member documents
                    val leagueIds = snapshot?.documents?.mapNotNull { doc ->
                        doc.getString("leagueId")
                    } ?: emptyList()

                    android.util.Log.d(TAG, "User is member of ${leagueIds.size} leagues")

                    // Fetch league details
                    if (leagueIds.isEmpty()) {
                        trySend(emptyList())
                    } else {
                        leaguesCollection(season)
                            .whereIn("id", leagueIds.take(10)) // Firestore limit
                            .get()
                            .addOnSuccessListener { leaguesSnapshot ->
                                val leagues = leaguesSnapshot.documents.mapNotNull { it.toLeague(season) }
                                trySend(leagues)
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e(TAG, "Error fetching league details: ${e.message}")
                                trySend(emptyList())
                            }
                    }
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception in getLeaguesForUser: ${e.message}")
            trySend(emptyList())
            awaitClose { }
        }
    }

    /**
     * Get league by ID
     * @param season the season the league belongs to (defaults to current season)
     */
    suspend fun getLeagueById(leagueId: String, season: Int = SeasonConfig.CURRENT_SEASON): League? {
        return try {
            val doc = leaguesCollection(season)
                .document(leagueId)
                .get()
                .await()

            doc.toLeague(season)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting league: ${e.message}")
            null
        }
    }

    /**
     * Get league by code
     * @param season the season to search in (defaults to current season)
     */
    suspend fun getLeagueByCode(code: String, season: Int = SeasonConfig.CURRENT_SEASON): League? {
        return try {
            val snapshot = leaguesCollection(season)
                .whereEqualTo("code", code.uppercase())
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toLeague(season)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting league by code: ${e.message}")
            null
        }
    }

    /**
     * Join a league
     * @param season the season the league belongs to (defaults to current season)
     */
    suspend fun joinLeague(
        leagueId: String,
        userId: String,
        teamId: String,
        teamName: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        return try {
            // Check if already a member
            val existingMember = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .document(userId)
                .get()
                .await()

            if (existingMember.exists()) {
                return Result.failure(Exception("Ja es membro desta liga"))
            }

            // Get current member count
            val league = getLeagueById(leagueId, season)
            val memberCount = league?.memberCount ?: 0

            val member = LeagueMember(
                leagueId = leagueId,
                userId = userId,
                teamId = teamId,
                teamName = teamName,
                rank = memberCount + 1,
                points = 0,
                previousRank = memberCount + 1,
                joinedAt = System.currentTimeMillis(),
                season = season
            )

            // Add member
            leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .document(userId)
                .set(member.toFirestoreMap())
                .await()

            // Update member count
            leaguesCollection(season)
                .document(leagueId)
                .update("memberCount", memberCount + 1)
                .await()

            android.util.Log.d(TAG, "User $userId joined league $leagueId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error joining league: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Leave a league
     * @param season the season the league belongs to (defaults to current season)
     */
    suspend fun leaveLeague(leagueId: String, userId: String, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            // Remove member
            leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .document(userId)
                .delete()
                .await()

            // Decrement member count
            val league = getLeagueById(leagueId, season)
            val newCount = ((league?.memberCount ?: 1) - 1).coerceAtLeast(0)

            leaguesCollection(season)
                .document(leagueId)
                .update("memberCount", newCount)
                .await()

            android.util.Log.d(TAG, "User $userId left league $leagueId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error leaving league: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Check if user is a member of a league
     * @param season the season the league belongs to (defaults to current season)
     */
    suspend fun isMember(leagueId: String, userId: String, season: Int = SeasonConfig.CURRENT_SEASON): Boolean {
        return try {
            val doc = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .document(userId)
                .get()
                .await()

            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get league members (real-time, sorted by rank)
     * For small leagues (<100 members). For large leagues, use paginated version.
     * @param season the season the league belongs to (defaults to current season)
     */
    fun getLeagueMembers(leagueId: String, season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<LeagueMember>> = callbackFlow {
        val listener = leaguesCollection(season)
            .document(leagueId)
            .collection(COLLECTION_MEMBERS)
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(100) // Limit for performance - use pagination for more
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e(TAG, "Error getting members: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val members = snapshot?.documents?.mapNotNull { it.toLeagueMember(season) } ?: emptyList()
                android.util.Log.d(TAG, "League $leagueId members: ${members.size}")
                trySend(members)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get paginated league members for large leagues (1000+ users)
     * Uses cursor-based pagination for efficiency
     * @param leagueId the league ID
     * @param pageSize number of members per page (default 50)
     * @param lastPoints points of the last member from previous page (for cursor-based pagination)
     * @param lastUserId user ID of the last member (for tie-breaking when points are equal)
     * @param season the season the league belongs to
     * @return list of members for the requested page
     */
    suspend fun getLeagueMembersPaginated(
        leagueId: String,
        pageSize: Int = 50,
        lastPoints: Int? = null,
        lastUserId: String? = null,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): List<LeagueMember> {
        return try {
            var query = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .orderBy("points", Query.Direction.DESCENDING)
                .orderBy("userId", Query.Direction.ASCENDING) // Secondary sort for stable pagination

            // If we have a cursor, start after it
            if (lastPoints != null && lastUserId != null) {
                query = query.startAfter(lastPoints, lastUserId)
            }

            val snapshot = query.limit(pageSize.toLong()).get().await()
            val members = snapshot.documents.mapNotNull { it.toLeagueMember(season) }

            android.util.Log.d(TAG, "Paginated members for $leagueId: ${members.size} (page size: $pageSize)")
            members
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting paginated members: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get a specific user's position in the ranking
     * Calculates rank by counting members with more points
     * @param leagueId the league ID
     * @param userId the user ID to find
     * @param season the season the league belongs to
     * @return the user's member data with calculated rank, or null if not found
     */
    suspend fun getUserPositionInLeague(
        leagueId: String,
        userId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): LeagueMember? {
        return try {
            // Get the user's member document
            val userDoc = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                android.util.Log.d(TAG, "User $userId not found in league $leagueId")
                return null
            }

            val userMember = userDoc.toLeagueMember(season) ?: return null
            val userPoints = userMember.points

            // Count how many members have more points (to calculate rank)
            val higherRankedSnapshot = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .whereGreaterThan("points", userPoints)
                .get()
                .await()

            val rank = higherRankedSnapshot.size() + 1

            android.util.Log.d(TAG, "User $userId rank in league $leagueId: $rank (${userMember.points} pts)")

            // Return member with calculated rank
            userMember.copy(rank = rank)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting user position: ${e.message}")
            null
        }
    }

    /**
     * Get members surrounding a specific user in the ranking
     * Useful for showing context without loading all members
     * @param leagueId the league ID
     * @param userId the user ID
     * @param countAbove number of members above the user to fetch
     * @param countBelow number of members below the user to fetch
     * @param season the season
     * @return list of members including the user, ordered by rank
     */
    suspend fun getMembersSurroundingUser(
        leagueId: String,
        userId: String,
        countAbove: Int = 3,
        countBelow: Int = 3,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): List<LeagueMember> {
        return try {
            // Get the user's points
            val userDoc = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) return emptyList()

            val userPoints = userDoc.getLong("points")?.toInt() ?: 0

            // Get members above (higher points)
            val aboveSnapshot = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .whereGreaterThan("points", userPoints)
                .orderBy("points", Query.Direction.ASCENDING)
                .limit(countAbove.toLong())
                .get()
                .await()

            // Get members below or equal (including user)
            val belowSnapshot = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .whereLessThanOrEqualTo("points", userPoints)
                .orderBy("points", Query.Direction.DESCENDING)
                .limit((countBelow + 1).toLong())
                .get()
                .await()

            val aboveMembers = aboveSnapshot.documents.mapNotNull { it.toLeagueMember(season) }.reversed()
            val belowMembers = belowSnapshot.documents.mapNotNull { it.toLeagueMember(season) }

            // Combine: above + below (user is in belowMembers)
            val combined = aboveMembers + belowMembers
            android.util.Log.d(TAG, "Surrounding members for $userId: ${combined.size}")

            combined
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting surrounding members: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get the total member count for a league
     */
    suspend fun getLeagueMemberCount(leagueId: String, season: Int = SeasonConfig.CURRENT_SEASON): Int {
        return try {
            val league = getLeagueById(leagueId, season)
            league?.memberCount ?: 0
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting member count: ${e.message}")
            0
        }
    }

    /**
     * Add multiple fake teams to a league for testing/demo purposes
     * @param leagueId the league to add teams to
     * @param count number of fake teams to create
     * @param season the season
     * @return number of teams actually added
     */
    suspend fun addFakeTeamsToLeague(
        leagueId: String,
        count: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Int> {
        return try {
            val portugueseNames = listOf(
                "Pedro", "Joao", "Miguel", "Ricardo", "Andre", "Bruno", "Tiago", "Rui",
                "Hugo", "Luis", "Carlos", "Antonio", "Manuel", "Francisco", "Paulo", "Jose",
                "Rafael", "Daniel", "Nuno", "Sergio", "Marco", "Filipe", "David", "Vitor",
                "Ana", "Maria", "Sofia", "Ines", "Mariana", "Catarina", "Rita", "Beatriz"
            )
            val teamSuffixes = listOf(
                "Racing", "Cycling", "Pro Team", "Elite", "Champions", "Riders",
                "Velocidade", "Pedaladas", "Ciclismo", "Amadores", "Unidos", "Furia",
                "Tubarao", "Aguia", "Leao", "Lobo", "FC", "SC", "Club", "Team"
            )

            var addedCount = 0
            val currentMemberCount = getLeagueMemberCount(leagueId, season)

            for (i in 1..count) {
                val fakeName = portugueseNames.random()
                val fakeSuffix = teamSuffixes.random()
                val fakeUserId = "fake_user_${System.currentTimeMillis()}_$i"
                val fakeTeamId = "fake_team_${System.currentTimeMillis()}_$i"
                val fakeTeamName = "$fakeName $fakeSuffix"

                // Random points between 0 and 500
                val fakePoints = (0..500).random()

                val member = LeagueMember(
                    leagueId = leagueId,
                    userId = fakeUserId,
                    teamId = fakeTeamId,
                    teamName = fakeTeamName,
                    rank = currentMemberCount + i, // Will be recalculated
                    points = fakePoints,
                    previousRank = currentMemberCount + i,
                    joinedAt = System.currentTimeMillis() - (0..30).random() * 24 * 60 * 60 * 1000L,
                    season = season
                )

                leaguesCollection(season)
                    .document(leagueId)
                    .collection(COLLECTION_MEMBERS)
                    .document(fakeUserId)
                    .set(member.toFirestoreMap())
                    .await()

                addedCount++
            }

            // Update member count
            leaguesCollection(season)
                .document(leagueId)
                .update("memberCount", currentMemberCount + addedCount)
                .await()

            android.util.Log.d(TAG, "Added $addedCount fake teams to league $leagueId")

            // Recalculate rankings
            recalculateRankings(leagueId, season)

            Result.success(addedCount)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error adding fake teams: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Remove all fake teams from a league
     * Fake teams are identified by userId starting with "fake_user_"
     * @param leagueId the league to remove fake teams from
     * @param season the season
     * @return number of fake teams removed
     */
    suspend fun removeFakeTeamsFromLeague(
        leagueId: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Int> {
        return try {
            // Get all members and filter fake ones
            val membersSnapshot = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .get()
                .await()

            var removedCount = 0
            val fakeMembers = membersSnapshot.documents.filter { doc ->
                val userId = doc.getString("userId") ?: doc.id
                userId.startsWith("fake_user_")
            }

            // Delete each fake member
            for (doc in fakeMembers) {
                doc.reference.delete().await()
                removedCount++
            }

            // Update member count
            val currentCount = getLeagueMemberCount(leagueId, season)
            val newCount = (currentCount - removedCount).coerceAtLeast(0)
            leaguesCollection(season)
                .document(leagueId)
                .update("memberCount", newCount)
                .await()

            android.util.Log.d(TAG, "Removed $removedCount fake teams from league $leagueId")

            // Recalculate rankings for remaining real members
            if (removedCount > 0) {
                recalculateRankings(leagueId, season)
            }

            Result.success(removedCount)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error removing fake teams: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update member points and ranking
     * @param season the season the league belongs to (defaults to current season)
     */
    suspend fun updateMemberPoints(
        leagueId: String,
        userId: String,
        points: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        return try {
            val memberRef = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .document(userId)

            // Get current rank
            val current = memberRef.get().await()
            val currentRank = current.getLong("rank")?.toInt() ?: 0

            memberRef.update(
                mapOf(
                    "points" to points,
                    "previousRank" to currentRank
                )
            ).await()

            android.util.Log.d(TAG, "Updated member points: $userId -> $points pts")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating member points: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Recalculate all rankings for a league
     * @param season the season the league belongs to (defaults to current season)
     */
    suspend fun recalculateRankings(leagueId: String, season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        return try {
            val membersSnapshot = leaguesCollection(season)
                .document(leagueId)
                .collection(COLLECTION_MEMBERS)
                .orderBy("points", Query.Direction.DESCENDING)
                .get()
                .await()

            membersSnapshot.documents.forEachIndexed { index, doc ->
                val previousRank = doc.getLong("rank")?.toInt() ?: (index + 1)
                doc.reference.update(
                    mapOf(
                        "rank" to (index + 1),
                        "previousRank" to previousRank
                    )
                ).await()
            }

            android.util.Log.d(TAG, "Recalculated rankings for league $leagueId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error recalculating rankings: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Generate a unique 6-character code
     * @param season the season to check for code uniqueness (defaults to current season)
     */
    suspend fun generateUniqueCode(season: Int = SeasonConfig.CURRENT_SEASON): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var code: String
        var attempts = 0

        do {
            code = (1..6).map { chars.random() }.joinToString("")
            val existing = getLeagueByCode(code, season)
            attempts++
        } while (existing != null && attempts < 10)

        return code
    }

    /**
     * Clear all members from all leagues for a season.
     * This is used when resetting all fantasy teams.
     * @param season the season to clear members from
     * @return number of members cleared
     */
    suspend fun clearAllMembersForSeason(season: Int = SeasonConfig.CURRENT_SEASON): Result<Int> {
        return try {
            var clearedCount = 0

            // Get all leagues for this season
            val leaguesSnapshot = leaguesCollection(season).get().await()

            for (leagueDoc in leaguesSnapshot.documents) {
                // Get all members for this league
                val membersSnapshot = leagueDoc.reference
                    .collection(COLLECTION_MEMBERS)
                    .get()
                    .await()

                // Delete all members
                for (memberDoc in membersSnapshot.documents) {
                    memberDoc.reference.delete().await()
                    clearedCount++
                }

                // Update member count to 0
                leagueDoc.reference.update("memberCount", 0).await()

                android.util.Log.d(TAG, "Cleared ${membersSnapshot.size()} members from league ${leagueDoc.id}")
            }

            android.util.Log.d(TAG, "Cleared $clearedCount total members for season $season")
            Result.success(clearedCount)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error clearing members: ${e.message}")
            Result.failure(e)
        }
    }

    // Conversion functions
    private fun League.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "type" to type.name,
        "code" to code,
        "ownerId" to ownerId,
        "region" to region,
        "memberCount" to memberCount,
        "createdAt" to createdAt,
        "season" to season
    )

    private fun LeagueMember.toFirestoreMap(): Map<String, Any?> = mapOf(
        "leagueId" to leagueId,
        "userId" to userId,
        "teamId" to teamId,
        "teamName" to teamName,
        "rank" to rank,
        "points" to points,
        "previousRank" to previousRank,
        "joinedAt" to joinedAt,
        "season" to season
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toLeague(season: Int = SeasonConfig.CURRENT_SEASON): League? {
        return try {
            League(
                id = getString("id") ?: id,
                name = getString("name") ?: "Sem Nome",
                type = try {
                    LeagueType.valueOf(getString("type") ?: "PRIVATE")
                } catch (e: Exception) {
                    LeagueType.PRIVATE
                },
                code = getString("code"),
                ownerId = getString("ownerId"),
                region = getString("region"),
                memberCount = getLong("memberCount")?.toInt() ?: 0,
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                season = getLong("season")?.toInt() ?: season
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error parsing league: ${e.message}")
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toLeagueMember(season: Int = SeasonConfig.CURRENT_SEASON): LeagueMember? {
        return try {
            LeagueMember(
                leagueId = getString("leagueId") ?: return null,
                userId = getString("userId") ?: return null,
                teamId = getString("teamId") ?: "",
                teamName = getString("teamName") ?: "Sem Nome",
                rank = getLong("rank")?.toInt() ?: 0,
                points = getLong("points")?.toInt() ?: 0,
                previousRank = getLong("previousRank")?.toInt() ?: 0,
                joinedAt = getLong("joinedAt") ?: System.currentTimeMillis(),
                season = getLong("season")?.toInt() ?: season
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error parsing member: ${e.message}")
            null
        }
    }

    // ========== Admin: Global League Fake Teams ==========

    /**
     * Add fake teams to the global Liga Portugal league for the current season.
     * Wrapper method for admin use.
     */
    suspend fun addFakeTeamsToGlobalLeague(
        count: Int,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Int> {
        val leagueId = globalLeagueId(season)
        return addFakeTeamsToLeague(leagueId, count, season)
    }

    /**
     * Remove all fake teams from the global Liga Portugal league.
     * Wrapper method for admin use.
     */
    suspend fun removeFakeTeamsFromGlobalLeague(
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Int> {
        val leagueId = globalLeagueId(season)
        return removeFakeTeamsFromLeague(leagueId, season)
    }
}
