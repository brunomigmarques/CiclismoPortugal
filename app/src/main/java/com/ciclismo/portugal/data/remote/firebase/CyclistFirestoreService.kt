package com.ciclismo.portugal.data.remote.firebase

import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CyclistFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_SEASONS = "seasons"
        private const val COLLECTION_CYCLISTS = "cyclists"
        private const val COLLECTION_SYNC_STATUS = "sync_status"
        private const val DOC_LATEST_SYNC = "latest"
    }

    /**
     * Get the collection path for cyclists in a specific season
     * Pattern: seasons/{season}/cyclists
     */
    private fun cyclistsCollection(season: Int = SeasonConfig.CURRENT_SEASON) =
        firestore.collection(COLLECTION_SEASONS)
            .document(season.toString())
            .collection(COLLECTION_CYCLISTS)

    /**
     * Get the sync status collection path for a specific season
     */
    private fun syncStatusCollection(season: Int = SeasonConfig.CURRENT_SEASON) =
        firestore.collection(COLLECTION_SEASONS)
            .document(season.toString())
            .collection(COLLECTION_SYNC_STATUS)

    /**
     * Upload a list of cyclists to Firestore (Admin only)
     * Sets validated = false by default, admin validates in Firebase Console
     * @param updateStatus whether to update sync status after upload (default true)
     * @param season the season to upload cyclists to (defaults to current season)
     */
    suspend fun uploadCyclists(
        cyclists: List<Cyclist>,
        updateStatus: Boolean = true,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Int> {
        if (cyclists.isEmpty()) {
            return Result.success(0)
        }

        android.util.Log.d("CyclistFirestore", "Fire-and-forget upload of ${cyclists.size} cyclists to season $season...")

        // Fire and forget - don't wait for callbacks
        cyclists.forEach { cyclist ->
            val docRef = cyclistsCollection(season).document(cyclist.id)
            val data = cyclist.toFirestoreMap(season)
            docRef.set(data)
                .addOnSuccessListener {
                    android.util.Log.d("CyclistFirestore", "OK: ${cyclist.fullName}")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("CyclistFirestore", "FAIL: ${cyclist.fullName} - ${e.message}")
                }
        }

        // Small delay to let Firebase queue the operations
        kotlinx.coroutines.delay(200)

        if (updateStatus) {
            val data = mapOf(
                "lastSyncTimestamp" to System.currentTimeMillis(),
                "cyclistsCount" to cyclists.size,
                "syncedBy" to "admin",
                "season" to season
            )
            syncStatusCollection(season)
                .document(DOC_LATEST_SYNC)
                .set(data)
        }

        return Result.success(cyclists.size)
    }

    /**
     * Get all validated cyclists from Firestore for a specific season
     * @param season the season to get cyclists from (defaults to current season)
     */
    fun getValidatedCyclists(season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<Cyclist>> = callbackFlow {
        android.util.Log.d("CyclistFirestore", "Setting up listener for validated cyclists (season $season)...")

        val listener = cyclistsCollection(season)
            .whereEqualTo("validated", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CyclistFirestore", "Error getting validated cyclists: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                android.util.Log.d("CyclistFirestore", "Snapshot received: ${snapshot?.documents?.size ?: 0} docs")

                val cyclists = snapshot?.documents?.mapNotNull { doc ->
                    doc.toCyclist(season)
                } ?: emptyList()

                android.util.Log.d("CyclistFirestore", "Parsed ${cyclists.size} validated cyclists")
                trySend(cyclists)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get all cyclists (including non-validated) - for admin view
     * @param season the season to get cyclists from (defaults to current season)
     */
    fun getAllCyclists(season: Int = SeasonConfig.CURRENT_SEASON): Flow<List<Cyclist>> = callbackFlow {
        val listener = cyclistsCollection(season)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val cyclists = snapshot?.documents?.mapNotNull { doc ->
                    doc.toCyclist(season)
                } ?: emptyList()

                trySend(cyclists)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get pending (non-validated) cyclists count
     * Uses callback to avoid blocking
     * @param season the season to count cyclists from (defaults to current season)
     */
    fun getPendingCyclistsCount(season: Int = SeasonConfig.CURRENT_SEASON, callback: (Int) -> Unit) {
        cyclistsCollection(season)
            .whereEqualTo("validated", false)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.size())
            }
            .addOnFailureListener {
                callback(0)
            }
    }

    /**
     * Get validated cyclists count
     * Uses callback to avoid blocking
     * @param season the season to count cyclists from (defaults to current season)
     */
    fun getValidatedCyclistsCount(season: Int = SeasonConfig.CURRENT_SEASON, callback: (Int) -> Unit) {
        cyclistsCollection(season)
            .whereEqualTo("validated", true)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.size())
            }
            .addOnFailureListener {
                callback(0)
            }
    }

    /**
     * Validate all pending cyclists (Admin action via Firebase Console or here)
     * Fire and forget approach - doesn't wait for completion
     * @param season the season to validate cyclists in (defaults to current season)
     */
    suspend fun validateAllCyclists(season: Int = SeasonConfig.CURRENT_SEASON): Result<Int> {
        android.util.Log.d("CyclistFirestore", "Starting validation for season $season (fire-and-forget)...")

        // Get pending count first (fire and forget the actual validation)
        var pendingCount = 0

        cyclistsCollection(season)
            .whereEqualTo("validated", false)
            .get()
            .addOnSuccessListener { snapshot ->
                pendingCount = snapshot.documents.size
                android.util.Log.d("CyclistFirestore", "Found $pendingCount pending cyclists to validate")

                if (snapshot.documents.isEmpty()) {
                    return@addOnSuccessListener
                }

                // Validate each document individually (fire and forget)
                snapshot.documents.forEach { doc ->
                    doc.reference.update("validated", true)
                        .addOnSuccessListener {
                            android.util.Log.d("CyclistFirestore", "Validated: ${doc.id}")
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("CyclistFirestore", "Failed to validate ${doc.id}: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CyclistFirestore", "Failed to get pending cyclists: ${e.message}")
            }

        // Return immediately - validation happens in background
        kotlinx.coroutines.delay(500)
        return Result.success(pendingCount)
    }

    /**
     * Delete all cyclists (for re-sync)
     * Fire and forget approach - doesn't wait for completion
     * @param season the season to delete cyclists from (defaults to current season)
     */
    suspend fun deleteAllCyclists(season: Int = SeasonConfig.CURRENT_SEASON): Result<Unit> {
        android.util.Log.d("CyclistFirestore", "Starting delete for season $season (fire-and-forget)...")

        cyclistsCollection(season)
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.documents.size
                android.util.Log.d("CyclistFirestore", "Found $count cyclists to delete")

                if (snapshot.documents.isEmpty()) {
                    return@addOnSuccessListener
                }

                // Delete each document individually (fire and forget)
                snapshot.documents.forEach { doc ->
                    doc.reference.delete()
                        .addOnSuccessListener {
                            android.util.Log.d("CyclistFirestore", "Deleted: ${doc.id}")
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("CyclistFirestore", "Failed to delete ${doc.id}: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CyclistFirestore", "Failed to get cyclists for deletion: ${e.message}")
            }

        // Return immediately - deletion happens in background
        kotlinx.coroutines.delay(500)
        return Result.success(Unit)
    }

    /**
     * Get all validated cyclists (one-time fetch, not Flow)
     * Used for photo matching
     * @param season the season to get cyclists from (defaults to current season)
     */
    suspend fun getValidatedCyclistsOnce(season: Int = SeasonConfig.CURRENT_SEASON): Result<List<Cyclist>> {
        return try {
            val snapshot = cyclistsCollection(season)
                .whereEqualTo("validated", true)
                .get()
                .await()

            val cyclists = snapshot.documents.mapNotNull { it.toCyclist(season) }
            android.util.Log.d("CyclistFirestore", "Fetched ${cyclists.size} validated cyclists for season $season")
            Result.success(cyclists)
        } catch (e: Exception) {
            android.util.Log.e("CyclistFirestore", "Failed to fetch cyclists: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update cyclist's photo URL in Firestore
     * @param season the season to update cyclist in (defaults to current season)
     */
    suspend fun updateCyclistPhotoUrl(
        cyclistId: String,
        photoUrl: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        return try {
            cyclistsCollection(season)
                .document(cyclistId)
                .update("photoUrl", photoUrl)
                .await()

            android.util.Log.d("CyclistFirestore", "Updated photoUrl for cyclist: $cyclistId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CyclistFirestore", "Failed to update photoUrl: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update cyclist's availability status in Firestore (Admin only)
     * Used to disable cyclists who are injured, dropped out, or suspended
     * @param cyclistId The cyclist's ID
     * @param isDisabled Whether the cyclist should be disabled
     * @param reason The reason for disabling (null when enabling)
     * @param season the season to update cyclist in (defaults to current season)
     */
    suspend fun updateCyclistAvailability(
        cyclistId: String,
        isDisabled: Boolean,
        reason: String?,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>(
                "isDisabled" to isDisabled,
                "disabledReason" to reason,
                "disabledAt" to if (isDisabled) System.currentTimeMillis() else null
            )

            cyclistsCollection(season)
                .document(cyclistId)
                .update(updates)
                .await()

            val action = if (isDisabled) "disabled" else "enabled"
            android.util.Log.d("CyclistFirestore", "Cyclist $cyclistId $action. Reason: ${reason ?: "N/A"}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CyclistFirestore", "Failed to update availability for $cyclistId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all disabled cyclists for a season
     * @param season the season to get disabled cyclists from (defaults to current season)
     */
    suspend fun getDisabledCyclistsOnce(season: Int = SeasonConfig.CURRENT_SEASON): Result<List<Cyclist>> {
        return try {
            val snapshot = cyclistsCollection(season)
                .whereEqualTo("isDisabled", true)
                .get()
                .await()

            val cyclists = snapshot.documents.mapNotNull { it.toCyclist(season) }
            android.util.Log.d("CyclistFirestore", "Fetched ${cyclists.size} disabled cyclists for season $season")
            Result.success(cyclists)
        } catch (e: Exception) {
            android.util.Log.e("CyclistFirestore", "Failed to fetch disabled cyclists: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update cyclist's points after a race
     * @param cyclistId The cyclist's ID
     * @param newPoints The new total points
     * @param racePoints Points earned in this race
     * @param raceName Name of the race (for history)
     * @param season the season to update cyclist in (defaults to current season)
     */
    suspend fun updateCyclistPoints(
        cyclistId: String,
        newPoints: Int,
        racePoints: Int,
        raceName: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "totalPoints" to newPoints,
                "lastRacePoints" to racePoints,
                "lastRaceName" to raceName,
                "lastRaceUpdate" to System.currentTimeMillis()
            )

            cyclistsCollection(season)
                .document(cyclistId)
                .update(updates)
                .await()

            android.util.Log.d("CyclistFirestore", "Updated points for $cyclistId: +$racePoints (total: $newPoints)")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CyclistFirestore", "Failed to update points for $cyclistId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update a cyclist's full details in Firestore (Admin only)
     * @param cyclist The updated cyclist data
     * @param season the season to update cyclist in (defaults to current season)
     */
    suspend fun updateCyclist(
        cyclist: Cyclist,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<Unit> {
        val docPath = "seasons/$season/cyclists/${cyclist.id}"
        android.util.Log.d("CyclistFirestore", "========== UPDATE CYCLIST ==========")
        android.util.Log.d("CyclistFirestore", "Document path: $docPath")
        android.util.Log.d("CyclistFirestore", "Cyclist: ${cyclist.fullName} (${cyclist.id})")
        android.util.Log.d("CyclistFirestore", "New price: ${cyclist.price}M")
        android.util.Log.d("CyclistFirestore", "Category: ${cyclist.category.name}")

        return try {
            val updates = mapOf(
                "firstName" to cyclist.firstName,
                "lastName" to cyclist.lastName,
                "fullName" to cyclist.fullName,
                "teamId" to cyclist.teamId,
                "teamName" to cyclist.teamName,
                "nationality" to cyclist.nationality,
                "photoUrl" to cyclist.photoUrl,
                "category" to cyclist.category.name,
                "price" to cyclist.price,
                "totalPoints" to cyclist.totalPoints,
                "form" to cyclist.form,
                "popularity" to cyclist.popularity,
                "age" to cyclist.age,
                "uciRanking" to cyclist.uciRanking,
                "speciality" to cyclist.speciality,
                "profileUrl" to cyclist.profileUrl,
                "lastUpdated" to System.currentTimeMillis(),
                // Also update availability fields
                "isDisabled" to cyclist.isDisabled,
                "disabledReason" to cyclist.disabledReason,
                "disabledAt" to cyclist.disabledAt
            )

            android.util.Log.d("CyclistFirestore", "Sending update to Firestore...")

            cyclistsCollection(season)
                .document(cyclist.id)
                .update(updates)
                .await()

            // Verify the update by reading back
            val verifyDoc = cyclistsCollection(season)
                .document(cyclist.id)
                .get()
                .await()

            val verifiedPrice = verifyDoc.getDouble("price")
            android.util.Log.d("CyclistFirestore", "========== UPDATE RESULT ==========")
            android.util.Log.d("CyclistFirestore", "SUCCESS: Updated ${cyclist.fullName}")
            android.util.Log.d("CyclistFirestore", "Verified price in Firestore: ${verifiedPrice}M")
            android.util.Log.d("CyclistFirestore", "Expected price: ${cyclist.price}M")
            android.util.Log.d("CyclistFirestore", "Match: ${verifiedPrice == cyclist.price}")
            android.util.Log.d("CyclistFirestore", "====================================")

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CyclistFirestore", "========== UPDATE FAILED ==========")
            android.util.Log.e("CyclistFirestore", "Error: ${e.message}", e)
            android.util.Log.e("CyclistFirestore", "Error type: ${e.javaClass.simpleName}")

            // If document doesn't exist, try to create it instead
            if (e.message?.contains("NOT_FOUND") == true ||
                e.message?.contains("No document to update") == true ||
                e.message?.contains("NOT FOUND") == true) {
                android.util.Log.w("CyclistFirestore", "Document doesn't exist at $docPath, creating...")
                return try {
                    val cyclistData = mapOf(
                        "id" to cyclist.id,
                        "firstName" to cyclist.firstName,
                        "lastName" to cyclist.lastName,
                        "fullName" to cyclist.fullName,
                        "teamId" to cyclist.teamId,
                        "teamName" to cyclist.teamName,
                        "nationality" to cyclist.nationality,
                        "photoUrl" to cyclist.photoUrl,
                        "category" to cyclist.category.name,
                        "price" to cyclist.price,
                        "totalPoints" to cyclist.totalPoints,
                        "form" to cyclist.form,
                        "popularity" to cyclist.popularity,
                        "age" to cyclist.age,
                        "uciRanking" to cyclist.uciRanking,
                        "speciality" to cyclist.speciality,
                        "profileUrl" to cyclist.profileUrl,
                        "lastUpdated" to System.currentTimeMillis(),
                        "validated" to true,  // Use 'validated' to match existing field name
                        "season" to season,
                        "isDisabled" to cyclist.isDisabled,
                        "disabledReason" to cyclist.disabledReason,
                        "disabledAt" to cyclist.disabledAt
                    )
                    cyclistsCollection(season)
                        .document(cyclist.id)
                        .set(cyclistData)
                        .await()
                    android.util.Log.d("CyclistFirestore", "SUCCESS: Created cyclist: ${cyclist.fullName} with price ${cyclist.price}M")
                    Result.success(Unit)
                } catch (createError: Exception) {
                    android.util.Log.e("CyclistFirestore", "FAILED to create cyclist: ${createError.message}")
                    Result.failure(createError)
                }
            }
            android.util.Log.e("CyclistFirestore", "====================================")
            Result.failure(e)
        }
    }

    /**
     * Search cyclists by name (partial match)
     * @param query The search query
     * @param season the season to search in (defaults to current season)
     */
    suspend fun searchCyclistsByName(
        query: String,
        season: Int = SeasonConfig.CURRENT_SEASON
    ): Result<List<Cyclist>> {
        return try {
            // Firebase doesn't support native text search, so we fetch all validated cyclists
            // and filter locally. For large datasets, consider using Algolia or similar.
            val snapshot = cyclistsCollection(season)
                .whereEqualTo("validated", true)
                .get()
                .await()

            val allCyclists = snapshot.documents.mapNotNull { it.toCyclist(season) }

            // Filter by name (case-insensitive partial match)
            val normalizedQuery = query.lowercase().trim()
            val filtered = if (normalizedQuery.isBlank()) {
                allCyclists.take(50) // Return first 50 if no query
            } else {
                allCyclists.filter { cyclist ->
                    cyclist.fullName.lowercase().contains(normalizedQuery) ||
                    cyclist.lastName.lowercase().contains(normalizedQuery) ||
                    cyclist.firstName.lowercase().contains(normalizedQuery)
                }.take(50)
            }

            android.util.Log.d("CyclistFirestore", "Search '$query' found ${filtered.size} cyclists")
            Result.success(filtered)
        } catch (e: Exception) {
            android.util.Log.e("CyclistFirestore", "Failed to search cyclists: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Clear photoUrl from all cyclists in Firestore
     * @param season the season to clear photo URLs from (defaults to current season)
     * @return Result with the count of updated cyclists
     */
    suspend fun clearAllPhotoUrls(season: Int = SeasonConfig.CURRENT_SEASON): Result<Int> {
        return try {
            val snapshot = cyclistsCollection(season)
                .get()
                .await()

            var updatedCount = 0
            for (doc in snapshot.documents) {
                try {
                    doc.reference.update("photoUrl", null).await()
                    updatedCount++
                    android.util.Log.d("CyclistFirestore", "Cleared photoUrl for: ${doc.id}")
                } catch (e: Exception) {
                    android.util.Log.e("CyclistFirestore", "Failed to clear photoUrl for ${doc.id}: ${e.message}")
                }
            }

            android.util.Log.d("CyclistFirestore", "Cleared photoUrl from $updatedCount cyclists")
            Result.success(updatedCount)
        } catch (e: Exception) {
            android.util.Log.e("CyclistFirestore", "Failed to clear photoUrls: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get sync status using callback
     * @param season the season to get sync status for (defaults to current season)
     */
    fun getSyncStatus(season: Int = SeasonConfig.CURRENT_SEASON, callback: (SyncStatus?) -> Unit) {
        syncStatusCollection(season)
            .document(DOC_LATEST_SYNC)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    callback(SyncStatus(
                        lastSyncTimestamp = doc.getLong("lastSyncTimestamp") ?: 0,
                        cyclistsCount = doc.getLong("cyclistsCount")?.toInt() ?: 0,
                        syncedBy = doc.getString("syncedBy") ?: "",
                        season = doc.getLong("season")?.toInt() ?: season
                    ))
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    private fun updateSyncStatus(count: Int, season: Int = SeasonConfig.CURRENT_SEASON) {
        val data = mapOf(
            "lastSyncTimestamp" to System.currentTimeMillis(),
            "cyclistsCount" to count,
            "syncedBy" to "admin",
            "season" to season
        )
        syncStatusCollection(season)
            .document(DOC_LATEST_SYNC)
            .set(data)
            .addOnSuccessListener {
                android.util.Log.d("CyclistFirestore", "Sync status updated for season $season")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CyclistFirestore", "Failed to update sync status: ${e.message}")
            }
    }

    private fun Cyclist.toFirestoreMap(season: Int = SeasonConfig.CURRENT_SEASON): Map<String, Any?> = mapOf(
        "id" to id,
        "firstName" to firstName,
        "lastName" to lastName,
        "fullName" to fullName,
        "teamId" to teamId,
        "teamName" to teamName,
        "nationality" to nationality,
        "photoUrl" to photoUrl,
        "category" to category.name,
        "price" to price,
        "totalPoints" to totalPoints,
        "form" to form,
        "popularity" to popularity,
        "age" to age,
        "uciRanking" to uciRanking,
        "speciality" to speciality,
        "profileUrl" to profileUrl,  // Link para ProCyclingStats
        "validated" to false,  // Always false on upload, admin validates in Console
        "uploadedAt" to System.currentTimeMillis(),
        "season" to season,
        // Availability fields
        "isDisabled" to isDisabled,
        "disabledReason" to disabledReason,
        "disabledAt" to disabledAt
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toCyclist(season: Int = SeasonConfig.CURRENT_SEASON): Cyclist? {
        return try {
            Cyclist(
                id = getString("id") ?: return null,
                firstName = getString("firstName") ?: "",
                lastName = getString("lastName") ?: "",
                fullName = getString("fullName") ?: "",
                teamId = getString("teamId") ?: "",
                teamName = getString("teamName") ?: "",
                nationality = getString("nationality") ?: "",
                photoUrl = getString("photoUrl"),
                category = try {
                    CyclistCategory.valueOf(getString("category") ?: "GC")
                } catch (e: Exception) {
                    CyclistCategory.GC
                },
                price = getDouble("price") ?: 5.0,
                totalPoints = getLong("totalPoints")?.toInt() ?: 0,
                form = getDouble("form") ?: 0.0,
                popularity = getDouble("popularity") ?: 0.0,
                age = getLong("age")?.toInt(),
                uciRanking = getLong("uciRanking")?.toInt(),
                speciality = getString("speciality"),
                profileUrl = getString("profileUrl"),
                season = getLong("season")?.toInt() ?: season,
                // Availability fields
                isDisabled = getBoolean("isDisabled") ?: false,
                disabledReason = getString("disabledReason"),
                disabledAt = getLong("disabledAt")
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class SyncStatus(
    val lastSyncTimestamp: Long,
    val cyclistsCount: Int,
    val syncedBy: String,
    val season: Int = SeasonConfig.CURRENT_SEASON
)
