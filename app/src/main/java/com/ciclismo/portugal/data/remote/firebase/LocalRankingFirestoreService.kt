package com.ciclismo.portugal.data.remote.firebase

import android.util.Log
import com.ciclismo.portugal.domain.model.LocalRanking
import com.ciclismo.portugal.domain.model.LocalRankingEntry
import com.ciclismo.portugal.domain.model.RankingPointsSystem
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.UserRaceType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore service for local/regional rankings.
 * Path: local_rankings/{rankingId}
 * Path: local_rankings/{rankingId}/entries/{entryId}
 */
@Singleton
class LocalRankingFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "LocalRankingFirestore"
        private const val RANKINGS_COLLECTION = "local_rankings"
        private const val ENTRIES_COLLECTION = "entries"
    }

    private val rankingsCollection
        get() = firestore.collection(RANKINGS_COLLECTION)

    // ==================== RANKING CRUD ====================

    /**
     * Create or update a ranking.
     */
    suspend fun saveRanking(ranking: LocalRanking): Result<Unit> {
        return try {
            val data = ranking.toFirestoreMap()
            rankingsCollection.document(ranking.id).set(data).await()
            Log.d(TAG, "Saved ranking: ${ranking.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving ranking", e)
            Result.failure(e)
        }
    }

    /**
     * Get all active rankings.
     */
    suspend fun getActiveRankings(season: Int = SeasonConfig.CURRENT_SEASON): List<LocalRanking> {
        return try {
            val snapshot = rankingsCollection
                .whereEqualTo("season", season)
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toLocalRanking()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing ranking: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching rankings", e)
            emptyList()
        }
    }

    /**
     * Get a specific ranking by ID.
     */
    suspend fun getRankingById(rankingId: String): LocalRanking? {
        return try {
            val doc = rankingsCollection.document(rankingId).get().await()
            if (doc.exists()) doc.toLocalRanking() else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ranking: $rankingId", e)
            null
        }
    }

    /**
     * Delete a ranking.
     */
    suspend fun deleteRanking(rankingId: String): Result<Unit> {
        return try {
            // Delete all entries first
            val entriesSnapshot = rankingsCollection
                .document(rankingId)
                .collection(ENTRIES_COLLECTION)
                .get()
                .await()

            val batch = firestore.batch()
            entriesSnapshot.documents.forEach { batch.delete(it.reference) }
            batch.delete(rankingsCollection.document(rankingId))
            batch.commit().await()

            Log.d(TAG, "Deleted ranking: $rankingId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting ranking", e)
            Result.failure(e)
        }
    }

    // ==================== RANKING ENTRIES ====================

    /**
     * Get all entries for a ranking, ordered by points.
     */
    suspend fun getRankingEntries(rankingId: String): List<LocalRankingEntry> {
        return try {
            val snapshot = rankingsCollection
                .document(rankingId)
                .collection(ENTRIES_COLLECTION)
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapIndexedNotNull { index, doc ->
                try {
                    doc.toLocalRankingEntry(index + 1)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing entry: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ranking entries", e)
            emptyList()
        }
    }

    /**
     * Get top N entries for a ranking.
     */
    suspend fun getTopEntries(rankingId: String, limit: Int = 10): List<LocalRankingEntry> {
        return try {
            val snapshot = rankingsCollection
                .document(rankingId)
                .collection(ENTRIES_COLLECTION)
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.mapIndexedNotNull { index, doc ->
                try {
                    doc.toLocalRankingEntry(index + 1)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching top entries", e)
            emptyList()
        }
    }

    /**
     * Update or create a user's entry in a ranking.
     */
    suspend fun updateEntry(rankingId: String, entry: LocalRankingEntry): Result<Unit> {
        return try {
            val data = entry.toFirestoreMap()
            rankingsCollection
                .document(rankingId)
                .collection(ENTRIES_COLLECTION)
                .document(entry.userId)
                .set(data)
                .await()

            Log.d(TAG, "Updated entry for user ${entry.userId} in ranking $rankingId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating entry", e)
            Result.failure(e)
        }
    }

    /**
     * Get a user's entry in a ranking.
     */
    suspend fun getUserEntry(rankingId: String, userId: String): LocalRankingEntry? {
        return try {
            val doc = rankingsCollection
                .document(rankingId)
                .collection(ENTRIES_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                // Need to calculate position
                val allEntries = getRankingEntries(rankingId)
                val position = allEntries.indexOfFirst { it.userId == userId } + 1
                doc.toLocalRankingEntry(position)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user entry", e)
            null
        }
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun LocalRanking.toFirestoreMap(): Map<String, Any?> = hashMapOf(
        "name" to name,
        "description" to description,
        "season" to season,
        "raceType" to raceType?.name,
        "region" to region,
        "selectedRaceIds" to selectedRaceIds,
        "selectedRaceNames" to selectedRaceNames,
        "pointsSystem" to pointsSystem.name,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toLocalRanking(): LocalRanking {
        return LocalRanking(
            id = id,
            name = getString("name") ?: "",
            description = getString("description"),
            season = getLong("season")?.toInt() ?: SeasonConfig.CURRENT_SEASON,
            raceType = getString("raceType")?.let {
                try { UserRaceType.valueOf(it) } catch (e: Exception) { null }
            },
            region = getString("region"),
            selectedRaceIds = (get("selectedRaceIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            selectedRaceNames = (get("selectedRaceNames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            pointsSystem = getString("pointsSystem")?.let {
                try { RankingPointsSystem.valueOf(it) } catch (e: Exception) { RankingPointsSystem.STANDARD }
            } ?: RankingPointsSystem.STANDARD,
            isActive = getBoolean("isActive") ?: true,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }

    private fun LocalRankingEntry.toFirestoreMap(): Map<String, Any?> = hashMapOf(
        "userId" to userId,
        "userName" to userName,
        "userPhotoUrl" to userPhotoUrl,
        "totalPoints" to totalPoints,
        "racesParticipated" to racesParticipated,
        "wins" to wins,
        "podiums" to podiums,
        "bestPosition" to bestPosition,
        "previousPosition" to previousPosition,
        "lastUpdated" to lastUpdated
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toLocalRankingEntry(position: Int): LocalRankingEntry {
        val userId = getString("userId") ?: id
        return LocalRankingEntry(
            id = id,
            rankingId = reference.parent.parent?.id ?: "",
            odoo = getString("odoo") ?: userId,
            userId = userId,
            userName = getString("userName") ?: "Desconhecido",
            userPhotoUrl = getString("userPhotoUrl"),
            totalPoints = getLong("totalPoints")?.toInt() ?: 0,
            racesParticipated = getLong("racesParticipated")?.toInt() ?: 0,
            wins = getLong("wins")?.toInt() ?: 0,
            podiums = getLong("podiums")?.toInt() ?: 0,
            bestPosition = getLong("bestPosition")?.toInt(),
            position = position,
            previousPosition = getLong("previousPosition")?.toInt(),
            lastUpdated = getLong("lastUpdated") ?: System.currentTimeMillis()
        )
    }

    // ==================== FAKE ENTRIES FOR TESTING ====================

    /**
     * Add fake entries to a ranking for testing purposes.
     * Fake entries have userId starting with "fake_user_" for easy identification and removal.
     */
    suspend fun addFakeEntriesToRanking(rankingId: String, count: Int): Result<Int> {
        return try {
            val portugueseFirstNames = listOf(
                "Joao", "Pedro", "Miguel", "Andre", "Rui", "Tiago", "Bruno", "Ricardo",
                "Carlos", "Paulo", "Luis", "Manuel", "Antonio", "Jose", "Fernando", "Jorge",
                "Nuno", "Hugo", "Sergio", "Daniel", "Marco", "Rafael", "Diogo", "David",
                "Francisco", "Vasco", "Gonçalo", "Filipe", "Alexandre", "Henrique"
            )
            val portugueseSurnames = listOf(
                "Silva", "Santos", "Ferreira", "Pereira", "Oliveira", "Costa", "Rodrigues",
                "Martins", "Jesus", "Sousa", "Fernandes", "Goncalves", "Gomes", "Lopes",
                "Marques", "Alves", "Almeida", "Ribeiro", "Pinto", "Carvalho", "Teixeira",
                "Moreira", "Correia", "Mendes", "Nunes", "Soares", "Vieira", "Monteiro"
            )

            val entriesCollection = rankingsCollection
                .document(rankingId)
                .collection(ENTRIES_COLLECTION)

            var addedCount = 0
            val batchSize = 500 // Firestore batch limit
            val batches = count / batchSize + if (count % batchSize > 0) 1 else 0

            for (batchIndex in 0 until batches) {
                val batch = firestore.batch()
                val start = batchIndex * batchSize
                val end = minOf(start + batchSize, count)

                for (i in start until end) {
                    val fakeUserId = "fake_user_${System.currentTimeMillis()}_$i"
                    val firstName = portugueseFirstNames.random()
                    val surname = portugueseSurnames.random()
                    val fakeName = "$firstName $surname"

                    // Generate random stats
                    val totalPoints = (0..500).random()
                    val racesParticipated = (1..20).random()
                    val wins = (0..minOf(3, racesParticipated)).random()
                    val podiums = wins + (0..minOf(5, racesParticipated - wins)).random()
                    val bestPosition = if (wins > 0) 1 else (1..10).random()

                    val entryData = hashMapOf(
                        "userId" to fakeUserId,
                        "userName" to fakeName,
                        "userPhotoUrl" to null,
                        "totalPoints" to totalPoints,
                        "racesParticipated" to racesParticipated,
                        "wins" to wins,
                        "podiums" to podiums,
                        "bestPosition" to bestPosition,
                        "previousPosition" to null,
                        "lastUpdated" to System.currentTimeMillis()
                    )

                    val docRef = entriesCollection.document(fakeUserId)
                    batch.set(docRef, entryData)
                    addedCount++
                }

                batch.commit().await()
                Log.d(TAG, "Committed batch ${batchIndex + 1}/$batches with ${end - start} entries")
            }

            Log.d(TAG, "Added $addedCount fake entries to ranking $rankingId")
            Result.success(addedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding fake entries", e)
            Result.failure(e)
        }
    }

    /**
     * Remove all fake entries from a ranking.
     * Identifies fake entries by userId starting with "fake_user_".
     */
    suspend fun removeFakeEntriesFromRanking(rankingId: String): Result<Int> {
        return try {
            val entriesCollection = rankingsCollection
                .document(rankingId)
                .collection(ENTRIES_COLLECTION)

            // Query all entries - we need to check userId prefix
            val allEntries = entriesCollection.get().await()

            val fakeEntries = allEntries.documents.filter { doc ->
                val userId = doc.getString("userId") ?: doc.id
                userId.startsWith("fake_user_")
            }

            if (fakeEntries.isEmpty()) {
                Log.d(TAG, "No fake entries found in ranking $rankingId")
                return Result.success(0)
            }

            var removedCount = 0
            val batchSize = 500
            val batches = fakeEntries.chunked(batchSize)

            for (batchDocs in batches) {
                val batch = firestore.batch()
                for (doc in batchDocs) {
                    batch.delete(doc.reference)
                    removedCount++
                }
                batch.commit().await()
            }

            Log.d(TAG, "Removed $removedCount fake entries from ranking $rankingId")
            Result.success(removedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing fake entries", e)
            Result.failure(e)
        }
    }

    /**
     * Get count of entries in a ranking.
     */
    suspend fun getEntryCount(rankingId: String): Int {
        return try {
            val snapshot = rankingsCollection
                .document(rankingId)
                .collection(ENTRIES_COLLECTION)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting entry count", e)
            0
        }
    }
}
