package com.ciclismo.portugal.data.local.ai

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ciclismo.portugal.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiUsageDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_usage")

/**
 * Tracks AI API usage to stay within free tier limits.
 *
 * Free tier limits (Google Gemini):
 * - 1000 requests/day
 * - 15 requests/minute
 * - 250,000 tokens/minute
 *
 * We set a conservative limit of 800/day (80% of limit) to have buffer.
 */
@Singleton
class AiUsageTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AiUsageTracker"

        // Preferences keys
        private val KEY_DAILY_COUNT = intPreferencesKey("daily_request_count")
        private val KEY_LAST_RESET_DATE = longPreferencesKey("last_reset_date")
        private val KEY_TOTAL_REQUESTS = intPreferencesKey("total_requests_all_time")

        // Limits - configurable via BuildConfig
        val DAILY_LIMIT = BuildConfig.AI_DAILY_LIMIT
        private const val MINUTE_LIMIT = 5 // Very conservative: 5 req/min (Google limit is 15)
        private const val MIN_REQUEST_INTERVAL_MS = 5000L // Minimum 5 seconds between requests
    }

    private val dataStore = context.aiUsageDataStore

    // Track requests in the current minute for rate limiting
    private var minuteRequestTimes = mutableListOf<Long>()
    private var lastRequestTime = 0L

    /**
     * Check if we can make another AI request.
     * Returns true if within limits, false if limit reached.
     */
    suspend fun canMakeRequest(): Boolean {
        // First, check and reset if it's a new day
        checkAndResetDaily()

        // Check daily limit
        val dailyCount = getDailyCount()
        if (dailyCount >= DAILY_LIMIT) {
            Log.w(TAG, "Daily limit reached: $dailyCount/$DAILY_LIMIT")
            return false
        }

        // Check minimum interval between requests
        val now = System.currentTimeMillis()
        if (lastRequestTime > 0 && now - lastRequestTime < MIN_REQUEST_INTERVAL_MS) {
            val waitTime = (MIN_REQUEST_INTERVAL_MS - (now - lastRequestTime)) / 1000
            Log.w(TAG, "Too fast! Wait $waitTime seconds between requests")
            return false
        }

        // Check minute rate limit
        minuteRequestTimes = minuteRequestTimes.filter {
            now - it < 60_000 // Keep only requests from last minute
        }.toMutableList()

        if (minuteRequestTimes.size >= MINUTE_LIMIT) {
            Log.w(TAG, "Minute rate limit reached: ${minuteRequestTimes.size}/$MINUTE_LIMIT")
            return false
        }

        return true
    }

    /**
     * Record a successful API request.
     */
    suspend fun recordRequest() {
        // Record for minute tracking and last request time
        val now = System.currentTimeMillis()
        minuteRequestTimes.add(now)
        lastRequestTime = now

        // Increment daily count
        dataStore.edit { prefs ->
            val current = prefs[KEY_DAILY_COUNT] ?: 0
            prefs[KEY_DAILY_COUNT] = current + 1

            // Also track total all-time
            val total = prefs[KEY_TOTAL_REQUESTS] ?: 0
            prefs[KEY_TOTAL_REQUESTS] = total + 1
        }

        val newCount = getDailyCount()
        Log.d(TAG, "Request recorded. Daily count: $newCount/$DAILY_LIMIT")
    }

    /**
     * Get current daily request count.
     */
    suspend fun getDailyCount(): Int {
        checkAndResetDaily()
        return dataStore.data.first()[KEY_DAILY_COUNT] ?: 0
    }

    /**
     * Get remaining requests for today.
     */
    suspend fun getRemainingRequests(): Int {
        return DAILY_LIMIT - getDailyCount()
    }

    /**
     * Get usage statistics.
     */
    suspend fun getUsageStats(): AiUsageStats {
        checkAndResetDaily()
        val prefs = dataStore.data.first()

        return AiUsageStats(
            dailyCount = prefs[KEY_DAILY_COUNT] ?: 0,
            dailyLimit = DAILY_LIMIT,
            remainingToday = getRemainingRequests(),
            totalAllTime = prefs[KEY_TOTAL_REQUESTS] ?: 0,
            percentUsed = ((prefs[KEY_DAILY_COUNT] ?: 0) * 100) / DAILY_LIMIT
        )
    }

    /**
     * Flow of usage stats for UI observation.
     */
    fun observeUsageStats(): Flow<AiUsageStats> {
        return dataStore.data.map { prefs ->
            val dailyCount = prefs[KEY_DAILY_COUNT] ?: 0
            AiUsageStats(
                dailyCount = dailyCount,
                dailyLimit = DAILY_LIMIT,
                remainingToday = DAILY_LIMIT - dailyCount,
                totalAllTime = prefs[KEY_TOTAL_REQUESTS] ?: 0,
                percentUsed = (dailyCount * 100) / DAILY_LIMIT
            )
        }
    }

    /**
     * Check if it's a new day and reset counter if needed.
     */
    private suspend fun checkAndResetDaily() {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val lastReset = dataStore.data.first()[KEY_LAST_RESET_DATE] ?: 0L

        if (today > lastReset) {
            Log.d(TAG, "New day detected, resetting daily counter")
            dataStore.edit { prefs ->
                prefs[KEY_DAILY_COUNT] = 0
                prefs[KEY_LAST_RESET_DATE] = today
            }
        }
    }

    /**
     * Get a user-friendly message about usage status.
     */
    suspend fun getUsageMessage(): String {
        val stats = getUsageStats()
        return when {
            stats.percentUsed >= 100 -> "Limite diario atingido. Tenta novamente amanha!"
            stats.percentUsed >= 90 -> "Quase no limite! Restam ${stats.remainingToday} pedidos hoje."
            stats.percentUsed >= 75 -> "Usaste ${stats.percentUsed}% do limite diario."
            else -> "${stats.remainingToday} pedidos restantes hoje."
        }
    }

    /**
     * Reset daily counter (for debugging/testing only).
     */
    suspend fun resetDailyCount() {
        Log.d(TAG, "Resetting daily counter for debug")
        dataStore.edit { prefs ->
            prefs[KEY_DAILY_COUNT] = 0
            prefs[KEY_LAST_RESET_DATE] = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        }
    }
}

/**
 * AI usage statistics.
 */
data class AiUsageStats(
    val dailyCount: Int,
    val dailyLimit: Int,
    val remainingToday: Int,
    val totalAllTime: Int,
    val percentUsed: Int
)
