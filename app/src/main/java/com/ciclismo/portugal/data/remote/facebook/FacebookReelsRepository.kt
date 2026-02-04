package com.ciclismo.portugal.data.remote.facebook

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Facebook Reels data.
 * Handles token storage and reel fetching.
 *
 * NOTE: For production, use EncryptedSharedPreferences by adding:
 * implementation("androidx.security:security-crypto:1.1.0-alpha06")
 */
@Singleton
class FacebookReelsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val facebookGraphService: FacebookGraphService
) {
    companion object {
        private const val TAG = "FacebookReelsRepo"
        private const val PREFS_NAME = "facebook_config"
        private const val KEY_ACCESS_TOKEN = "fb_access_token"
    }

    private val _reels = MutableStateFlow<List<FacebookReel>>(emptyList())
    val reels: Flow<List<FacebookReel>> = _reels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    init {
        // Load token from storage on initialization
        loadStoredToken()
    }

    /**
     * Load the stored access token and configure the service.
     */
    private fun loadStoredToken() {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (!token.isNullOrBlank()) {
            facebookGraphService.setAccessToken(token)
            Log.d(TAG, "Loaded stored Facebook access token")
        }
    }

    /**
     * Save and set the Facebook access token.
     * This should be called by admin/settings when configuring the token.
     */
    fun setAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
        facebookGraphService.setAccessToken(token)
        Log.d(TAG, "Saved Facebook access token")
    }

    /**
     * Check if Facebook Reels are configured and available.
     */
    fun isConfigured(): Boolean = facebookGraphService.isConfigured()

    /**
     * Fetch reels from all configured cycling pages.
     */
    suspend fun refreshReels(): Result<List<FacebookReel>> {
        if (!isConfigured()) {
            Log.w(TAG, "Facebook not configured, returning empty list")
            return Result.success(emptyList())
        }

        _isLoading.value = true

        return try {
            val result = facebookGraphService.fetchAllReels(limit = 10)
            result.getOrNull()?.let { reelsList ->
                _reels.value = reelsList
            }
            result
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Get static placeholder reels for when Facebook is not configured.
     * These link to the pages directly.
     */
    fun getPlaceholderReels(): List<FacebookReel> {
        return listOf(
            // Portuguese Cycling Federation
            FacebookReel(
                id = "placeholder_1",
                description = "Federação Portuguesa de Ciclismo",
                permalinkUrl = "https://www.facebook.com/fpciclismo/reels/",
                thumbnailUrl = "",
                createdTime = "",
                length = 0.0,
                pageName = "FPC",
                pageId = ""
            ),
            // Volta a Portugal
            FacebookReel(
                id = "placeholder_2",
                description = "Volta a Portugal em Bicicleta",
                permalinkUrl = "https://www.facebook.com/voltaportugal/reels/",
                thumbnailUrl = "",
                createdTime = "",
                length = 0.0,
                pageName = "Volta a Portugal",
                pageId = ""
            ),
            // UCI - Union Cycliste Internationale
            FacebookReel(
                id = "placeholder_3",
                description = "UCI - Ciclismo Mundial",
                permalinkUrl = "https://www.facebook.com/UnionCyclisteInternationale/reels/",
                thumbnailUrl = "",
                createdTime = "",
                length = 0.0,
                pageName = "UCI",
                pageId = ""
            ),
            // Tour de France
            FacebookReel(
                id = "placeholder_4",
                description = "Tour de France",
                permalinkUrl = "https://www.facebook.com/letour/reels/",
                thumbnailUrl = "",
                createdTime = "",
                length = 0.0,
                pageName = "Tour de France",
                pageId = ""
            ),
            // Giro d'Italia
            FacebookReel(
                id = "placeholder_5",
                description = "Giro d'Italia",
                permalinkUrl = "https://www.facebook.com/gaborone/reels/",
                thumbnailUrl = "",
                createdTime = "",
                length = 0.0,
                pageName = "Giro d'Italia",
                pageId = ""
            ),
            // La Vuelta
            FacebookReel(
                id = "placeholder_6",
                description = "La Vuelta a España",
                permalinkUrl = "https://www.facebook.com/lavuelta/reels/",
                thumbnailUrl = "",
                createdTime = "",
                length = 0.0,
                pageName = "La Vuelta",
                pageId = ""
            ),
            // GCN - Global Cycling Network
            FacebookReel(
                id = "placeholder_7",
                description = "Global Cycling Network",
                permalinkUrl = "https://www.facebook.com/globalcyclingnetwork/reels/",
                thumbnailUrl = "",
                createdTime = "",
                length = 0.0,
                pageName = "GCN",
                pageId = ""
            ),
            // Red Bull Cycling
            FacebookReel(
                id = "placeholder_8",
                description = "Red Bull Bike",
                permalinkUrl = "https://www.facebook.com/redbullbike/reels/",
                thumbnailUrl = "",
                createdTime = "",
                length = 0.0,
                pageName = "Red Bull Bike",
                pageId = ""
            ),
            // Portuguese Cycling Magazine
            FacebookReel(
                id = "placeholder_9",
                description = "Revista de Ciclismo",
                permalinkUrl = "https://www.facebook.com/portuguesecyclingmag/reels/",
                thumbnailUrl = "",
                createdTime = "",
                length = 0.0,
                pageName = "PCM",
                pageId = ""
            ),
            // Cycling Pro Net
            FacebookReel(
                id = "placeholder_10",
                description = "Cycling Pro Net",
                permalinkUrl = "https://www.facebook.com/cyclingpronet/reels/",
                thumbnailUrl = "",
                createdTime = "",
                length = 0.0,
                pageName = "CyclingPro",
                pageId = ""
            )
        )
    }
}
