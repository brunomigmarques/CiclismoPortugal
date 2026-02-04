package com.ciclismo.portugal.data.remote.firebase

import android.util.Log
import com.ciclismo.portugal.domain.model.NewsArticle
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore service for syncing news articles.
 * This allows the Cloud Function to access recent news for video search.
 */
@Singleton
class NewsFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "NewsFirestoreService"
        private const val NEWS_COLLECTION = "news_articles"
        private const val SYNC_METADATA_DOC = "sync_metadata"
    }

    /**
     * Upload news articles to Firestore.
     * Called after local sync to make news available to Cloud Function.
     */
    suspend fun uploadNews(articles: List<NewsArticle>): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val threeDaysAgo = now - (3L * 24 * 60 * 60 * 1000)
            val batch = firestore.batch()

            // Only upload recent articles (last 3 days)
            val recentArticles = articles.filter { it.publishedAt >= threeDaysAgo }
                .sortedByDescending { it.publishedAt }
                .take(20) // Keep max 20 articles

            Log.d(TAG, "Uploading ${recentArticles.size} recent news articles to Firestore")

            // Clear existing articles (except metadata)
            val existingDocs = firestore.collection(NEWS_COLLECTION)
                .get()
                .await()

            existingDocs.documents.forEach { doc ->
                if (doc.id != SYNC_METADATA_DOC) {
                    batch.delete(doc.reference)
                }
            }

            // Add new articles
            recentArticles.forEach { article ->
                val docId = article.url.hashCode().toString().replace("-", "_")
                val docRef = firestore.collection(NEWS_COLLECTION).document(docId)

                val articleData = hashMapOf(
                    "title" to article.title,
                    "summary" to article.summary,
                    "url" to article.url,
                    "imageUrl" to article.imageUrl,
                    "source" to article.source,
                    "publishedAt" to article.publishedAt,
                    "syncedAt" to now
                )
                batch.set(docRef, articleData)
            }

            // Update sync metadata
            val metadataRef = firestore.collection(NEWS_COLLECTION).document(SYNC_METADATA_DOC)
            batch.set(metadataRef, hashMapOf(
                "lastSyncTimestamp" to now,
                "articleCount" to recentArticles.size,
                "syncedBy" to "app"
            ))

            batch.commit().await()

            Log.d(TAG, "Successfully uploaded ${recentArticles.size} news articles to Firestore")
            Result.success(recentArticles.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading news to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Get recent news from Firestore.
     */
    suspend fun getRecentNews(limit: Int = 10): List<NewsArticle> {
        return try {
            val snapshot = firestore.collection(NEWS_COLLECTION)
                .orderBy("publishedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val url = doc.getString("url") ?: ""
                    val title = doc.getString("title") ?: ""
                    NewsArticle(
                        id = 0,
                        title = title,
                        summary = doc.getString("summary") ?: "",
                        url = url,
                        imageUrl = doc.getString("imageUrl"),
                        source = doc.getString("source") ?: "",
                        publishedAt = doc.getLong("publishedAt") ?: 0L,
                        contentHash = "$url|$title".hashCode().toString()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing news article: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching news from Firestore", e)
            emptyList()
        }
    }
}
