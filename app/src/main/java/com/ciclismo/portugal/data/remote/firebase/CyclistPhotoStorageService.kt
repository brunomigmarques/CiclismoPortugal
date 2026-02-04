package com.ciclismo.portugal.data.remote.firebase

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for uploading cyclist photos to Firebase Storage
 */
@Singleton
class CyclistPhotoStorageService @Inject constructor(
    private val storage: FirebaseStorage
) {
    companion object {
        private const val CYCLISTS_PHOTOS_PATH = "cyclists/photos"
    }

    /**
     * Upload a single photo for a cyclist
     * @param cyclistId The cyclist's ID
     * @param imageUri The local URI of the image (content:// URI from document picker)
     * @param context Android context for reading the file
     * @return Result with the download URL or error
     */
    suspend fun uploadCyclistPhoto(
        cyclistId: String,
        imageUri: Uri,
        context: Context
    ): Result<String> {
        return try {
            // Read bytes from content URI (required for SAF document picker URIs)
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return Result.failure(Exception("Cannot open input stream for URI"))

            val bytes = inputStream.use { it.readBytes() }

            if (bytes.isEmpty()) {
                return Result.failure(Exception("File is empty"))
            }

            android.util.Log.d("PhotoStorage", "Read ${bytes.size} bytes from URI for $cyclistId")

            val fileName = "${cyclistId}_${System.currentTimeMillis()}.jpg"
            val photoRef = storage.reference.child("$CYCLISTS_PHOTOS_PATH/$fileName")

            // Upload bytes instead of file URI
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            photoRef.putBytes(bytes, metadata).await()

            // Get download URL
            val downloadUrl = photoRef.downloadUrl.await().toString()

            android.util.Log.d("PhotoStorage", "Uploaded photo for $cyclistId: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            android.util.Log.e("PhotoStorage", "Failed to upload photo for $cyclistId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload multiple photos in batch
     * Photos are matched to cyclists by filename (without extension)
     * Example: "pogacar-tadej.jpg" matches cyclist with id "pogacar-tadej" or name "Tadej Pogacar"
     *
     * @param photoUris Map of cyclist identifier (id or name) to image URI
     * @param context Android context
     * @param onProgress Callback for progress updates (uploaded count, total count, current name)
     * @return Map of cyclist identifier to download URL
     */
    suspend fun uploadPhotosInBatch(
        photoUris: Map<String, Uri>,
        context: Context,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Map<String, String> {
        val results = mutableMapOf<String, String>()
        var uploadedCount = 0
        val totalCount = photoUris.size

        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        for ((identifier, uri) in photoUris) {
            onProgress(uploadedCount, totalCount, identifier)

            try {
                // Read bytes from content URI
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    android.util.Log.e("PhotoStorage", "Cannot open stream for $identifier")
                    uploadedCount++
                    continue
                }

                val bytes = inputStream.use { it.readBytes() }

                val fileName = "${identifier.lowercase().replace(" ", "-")}_${UUID.randomUUID().toString().take(8)}.jpg"
                val photoRef = storage.reference.child("$CYCLISTS_PHOTOS_PATH/$fileName")

                // Upload bytes
                photoRef.putBytes(bytes, metadata).await()

                // Get download URL
                val downloadUrl = photoRef.downloadUrl.await().toString()
                results[identifier] = downloadUrl

                android.util.Log.d("PhotoStorage", "Uploaded: $identifier -> $downloadUrl")
            } catch (e: Exception) {
                android.util.Log.e("PhotoStorage", "Failed to upload $identifier: ${e.message}")
            }

            uploadedCount++
        }

        onProgress(uploadedCount, totalCount, "Concluido!")
        return results
    }

    /**
     * Delete a cyclist's photo from storage
     * @param photoUrl The full download URL of the photo
     */
    suspend fun deletePhoto(photoUrl: String): Result<Unit> {
        return try {
            val photoRef = storage.getReferenceFromUrl(photoUrl)
            photoRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PhotoStorage", "Failed to delete photo: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete all photos from Firebase Storage
     * @return Result with the count of deleted photos
     */
    suspend fun deleteAllPhotos(): Result<Int> {
        return try {
            val photosRef = storage.reference.child(CYCLISTS_PHOTOS_PATH)
            val listResult = photosRef.listAll().await()

            var deletedCount = 0
            for (item in listResult.items) {
                try {
                    item.delete().await()
                    deletedCount++
                    android.util.Log.d("PhotoStorage", "Deleted: ${item.name}")
                } catch (e: Exception) {
                    android.util.Log.e("PhotoStorage", "Failed to delete ${item.name}: ${e.message}")
                }
            }

            android.util.Log.d("PhotoStorage", "Deleted $deletedCount photos from Firebase Storage")
            Result.success(deletedCount)
        } catch (e: Exception) {
            android.util.Log.e("PhotoStorage", "Failed to list/delete photos: ${e.message}")
            Result.failure(e)
        }
    }
}
