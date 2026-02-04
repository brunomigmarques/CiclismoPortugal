package com.ciclismo.portugal.data.remote.firebase

import android.util.Log
import com.ciclismo.portugal.domain.model.Prova
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore service for syncing provas (Portuguese races).
 * This allows the Cloud Function to access provas for video search.
 */
@Singleton
class ProvaFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "ProvaFirestoreService"
        private const val PROVAS_COLLECTION = "provas"
        private const val SYNC_METADATA_DOC = "sync_metadata"
    }

    /**
     * Upload provas to Firestore.
     * Called after local sync to make provas available to Cloud Function.
     */
    suspend fun uploadProvas(provas: List<Prova>): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val batch = firestore.batch()

            // Only upload future provas (no point storing past races)
            val futureProvas = provas.filter { it.data >= now }

            Log.d(TAG, "Uploading ${futureProvas.size} future provas to Firestore")

            // Clear existing provas (except metadata)
            val existingDocs = firestore.collection(PROVAS_COLLECTION)
                .get()
                .await()

            existingDocs.documents.forEach { doc ->
                if (doc.id != SYNC_METADATA_DOC) {
                    batch.delete(doc.reference)
                }
            }

            // Add new provas
            futureProvas.forEach { prova ->
                // Use a unique ID based on name + date + source
                val docId = "${prova.nome.hashCode()}_${prova.data}_${prova.source.hashCode()}"
                    .replace("-", "_")
                val docRef = firestore.collection(PROVAS_COLLECTION).document(docId)

                val provaData = hashMapOf(
                    "nome" to prova.nome,
                    "data" to prova.data,
                    "local" to prova.local,
                    "tipo" to prova.tipo,
                    "distancias" to prova.distancias,
                    "organizador" to prova.organizador,
                    "source" to prova.source,
                    "imageUrl" to prova.imageUrl,
                    "urlInscricao" to prova.urlInscricao,
                    "syncedAt" to now
                )
                batch.set(docRef, provaData)
            }

            // Update sync metadata
            val metadataRef = firestore.collection(PROVAS_COLLECTION).document(SYNC_METADATA_DOC)
            batch.set(metadataRef, hashMapOf(
                "lastSyncTimestamp" to now,
                "provaCount" to futureProvas.size,
                "syncedBy" to "app"
            ))

            batch.commit().await()

            Log.d(TAG, "Successfully uploaded ${futureProvas.size} provas to Firestore")
            Result.success(futureProvas.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading provas to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Get upcoming provas from Firestore (for Cloud Function fallback).
     */
    suspend fun getUpcomingProvas(limit: Int = 10): List<Prova> {
        return try {
            val now = System.currentTimeMillis()
            val snapshot = firestore.collection(PROVAS_COLLECTION)
                .whereGreaterThanOrEqualTo("data", now)
                .orderBy("data")
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    Prova(
                        id = 0,
                        nome = doc.getString("nome") ?: "",
                        data = doc.getLong("data") ?: 0L,
                        local = doc.getString("local") ?: "",
                        tipo = doc.getString("tipo") ?: "",
                        distancias = doc.getString("distancias") ?: "",
                        preco = "",
                        prazoInscricao = null,
                        organizador = doc.getString("organizador") ?: "",
                        descricao = "",
                        urlInscricao = doc.getString("urlInscricao"),
                        source = doc.getString("source") ?: "",
                        imageUrl = doc.getString("imageUrl")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing prova: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching provas from Firestore", e)
            emptyList()
        }
    }
}
