package com.ciclismo.portugal.data.remote.firebase

import android.util.Log
import com.ciclismo.portugal.data.remote.video.CyclingVideo
import com.ciclismo.portugal.data.remote.video.VideoSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore service for cycling videos.
 * Videos are synced daily by a Cloud Function to avoid API quota issues.
 * All users read from Firestore - no API calls needed.
 */
@Singleton
class VideoFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "VideoFirestoreService"
        private const val VIDEOS_COLLECTION = "cycling_videos"
        private const val SYNC_METADATA_DOC = "sync_metadata"

        // Cache validity in hours (refresh more often to match calendar changes)
        private const val CACHE_VALIDITY_HOURS = 6
    }

    /**
     * Get all cached videos from Firestore.
     * Returns a Flow that updates in real-time.
     */
    fun getVideos(): Flow<List<CyclingVideo>> = callbackFlow {
        // Simplified query - sorting done in-memory to avoid composite index requirement
        val listener = firestore.collection(VIDEOS_COLLECTION)
            .limit(50) // Get more to filter metadata doc
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to videos", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val videos = snapshot?.documents
                    ?.filter { doc -> doc.id != SYNC_METADATA_DOC }
                    ?.mapNotNull { doc ->
                        try {
                            val priority = doc.getLong("priority")?.toInt() ?: 999
                            val timestamp = doc.getLong("timestamp") ?: 0L
                            Triple(
                                priority,
                                timestamp,
                                CyclingVideo(
                                    id = doc.getString("videoId") ?: doc.id,
                                    title = doc.getString("title") ?: "",
                                    description = doc.getString("description") ?: "",
                                    thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                                    videoUrl = doc.getString("videoUrl") ?: "",
                                    channelName = doc.getString("channelName") ?: "",
                                    durationSeconds = doc.getLong("durationSeconds")?.toInt() ?: 0,
                                    source = VideoSource.YOUTUBE
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing video: ${doc.id}", e)
                            null
                        }
                    }
                    ?.sortedWith(compareBy({ it.first }, { -it.second }))
                    ?.map { it.third }
                    ?.take(20)
                    ?: emptyList()

                Log.d(TAG, "Received ${videos.size} videos from Firestore")
                trySend(videos)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get videos once (no real-time updates).
     * Simplified query - sorting done in-memory to avoid composite index requirement
     */
    suspend fun getVideosOnce(): List<CyclingVideo> {
        return try {
            val snapshot = firestore.collection(VIDEOS_COLLECTION)
                .limit(50) // Get more to filter metadata doc
                .get()
                .await()

            snapshot.documents
                .filter { doc -> doc.id != SYNC_METADATA_DOC }
                .mapNotNull { doc ->
                    try {
                        val priority = doc.getLong("priority")?.toInt() ?: 999
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        Triple(
                            priority,
                            timestamp,
                            CyclingVideo(
                                id = doc.getString("videoId") ?: doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                                videoUrl = doc.getString("videoUrl") ?: "",
                                channelName = doc.getString("channelName") ?: "",
                                durationSeconds = doc.getLong("durationSeconds")?.toInt() ?: 0,
                                source = VideoSource.YOUTUBE
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing video: ${doc.id}", e)
                        null
                    }
                }
                .sortedWith(compareBy({ it.first }, { -it.second }))
                .map { it.third }
                .take(20)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching videos", e)
            emptyList()
        }
    }

    /**
     * Check if Firestore cache is valid (synced within last 24 hours).
     */
    suspend fun isCacheValid(): Boolean {
        return try {
            val doc = firestore.collection(VIDEOS_COLLECTION)
                .document(SYNC_METADATA_DOC)
                .get()
                .await()

            if (!doc.exists()) return false

            val lastSync = doc.getLong("lastSyncTimestamp") ?: 0L
            val now = System.currentTimeMillis()
            val hoursSinceSync = (now - lastSync) / (1000 * 60 * 60)

            val isValid = hoursSinceSync < CACHE_VALIDITY_HOURS
            Log.d(TAG, "Cache valid: $isValid (${hoursSinceSync}h since last sync)")
            isValid
        } catch (e: Exception) {
            Log.w(TAG, "Error checking cache validity", e)
            false
        }
    }

    /**
     * Upload videos to Firestore (used by admin sync or Cloud Function).
     */
    suspend fun uploadVideos(videos: List<CyclingVideo>): Result<Int> {
        return try {
            val batch = firestore.batch()

            // Clear existing videos (except metadata)
            val existingDocs = firestore.collection(VIDEOS_COLLECTION)
                .whereNotEqualTo("__name__", SYNC_METADATA_DOC)
                .get()
                .await()

            existingDocs.documents.forEach { doc ->
                if (doc.id != SYNC_METADATA_DOC) {
                    batch.delete(doc.reference)
                }
            }

            // Add new videos
            videos.forEachIndexed { index, video ->
                val docRef = firestore.collection(VIDEOS_COLLECTION).document(video.id)
                val videoData = hashMapOf(
                    "videoId" to video.id,
                    "title" to video.title,
                    "description" to video.description,
                    "thumbnailUrl" to video.thumbnailUrl,
                    "videoUrl" to video.videoUrl,
                    "channelName" to video.channelName,
                    "durationSeconds" to video.durationSeconds,
                    "priority" to index,
                    "timestamp" to System.currentTimeMillis(),
                    "source" to video.source.name
                )
                batch.set(docRef, videoData)
            }

            // Update sync metadata
            val metadataRef = firestore.collection(VIDEOS_COLLECTION).document(SYNC_METADATA_DOC)
            batch.set(metadataRef, hashMapOf(
                "lastSyncTimestamp" to System.currentTimeMillis(),
                "videoCount" to videos.size,
                "syncedBy" to "app"
            ))

            batch.commit().await()

            Log.d(TAG, "Uploaded ${videos.size} videos to Firestore")
            Result.success(videos.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading videos", e)
            Result.failure(e)
        }
    }

    /**
     * Get last sync timestamp.
     */
    suspend fun getLastSyncTime(): Long? {
        return try {
            val doc = firestore.collection(VIDEOS_COLLECTION)
                .document(SYNC_METADATA_DOC)
                .get()
                .await()

            doc.getLong("lastSyncTimestamp")
        } catch (e: Exception) {
            null
        }
    }
}
