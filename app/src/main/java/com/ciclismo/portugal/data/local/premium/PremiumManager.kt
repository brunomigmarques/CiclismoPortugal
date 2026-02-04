package com.ciclismo.portugal.data.local.premium

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.premiumDataStore: DataStore<Preferences> by preferencesDataStore(name = "premium_status")

/**
 * Manages premium features and trial period.
 *
 * Trial period: 7 days from first use of AI features.
 * After trial: AI Assistant requires premium subscription.
 *
 * Premium features:
 * - AI Assistant (chat, recommendations, insights)
 * - Top 3 candidates for races
 * - Race insights and predictions
 * - Ad-free experience (banner, interstitial, rewarded ads hidden)
 *
 * Ad-free implementation:
 * - AdManager observes isPremium via observePremiumStatus()
 * - BannerAdView checks isPremium before rendering
 * - InterstitialAd skips loading/showing for premium users
 */
@Singleton
class PremiumManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PremiumManager"

        // Preferences keys
        private val KEY_TRIAL_START_DATE = longPreferencesKey("trial_start_date")
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_PREMIUM_EXPIRY = longPreferencesKey("premium_expiry_date")
        private val KEY_HAS_USED_AI = booleanPreferencesKey("has_used_ai")

        // Trial duration: 7 days
        private const val TRIAL_DURATION_DAYS = 7L
        private val TRIAL_DURATION_MS = TimeUnit.DAYS.toMillis(TRIAL_DURATION_DAYS)
    }

    private val dataStore = context.premiumDataStore

    /**
     * Check if user has access to AI features (trial active OR premium).
     */
    suspend fun hasAiAccess(): Boolean {
        return isPremium() || isTrialActive()
    }

    /**
     * Check if trial period is still active.
     */
    suspend fun isTrialActive(): Boolean {
        val prefs = dataStore.data.first()
        val trialStart = prefs[KEY_TRIAL_START_DATE] ?: return true // Trial not started yet
        val now = System.currentTimeMillis()
        return (now - trialStart) < TRIAL_DURATION_MS
    }

    /**
     * Check if user has premium subscription.
     */
    suspend fun isPremium(): Boolean {
        val prefs = dataStore.data.first()
        val isPremium = prefs[KEY_IS_PREMIUM] ?: false
        if (!isPremium) return false

        // Check expiry
        val expiry = prefs[KEY_PREMIUM_EXPIRY] ?: 0L
        if (expiry > 0 && System.currentTimeMillis() > expiry) {
            // Premium expired
            setPremiumStatus(false, 0L)
            return false
        }

        return true
    }

    /**
     * Start the trial period (called on first AI use).
     */
    suspend fun startTrial() {
        val prefs = dataStore.data.first()
        if (prefs[KEY_TRIAL_START_DATE] == null) {
            dataStore.edit { prefs ->
                prefs[KEY_TRIAL_START_DATE] = System.currentTimeMillis()
                prefs[KEY_HAS_USED_AI] = true
            }
            Log.d(TAG, "Trial period started")
        }
    }

    /**
     * Get trial status information.
     */
    suspend fun getTrialStatus(): TrialStatus {
        val prefs = dataStore.data.first()
        val trialStart = prefs[KEY_TRIAL_START_DATE]
        val isPremium = isPremium()

        return when {
            isPremium -> TrialStatus.PREMIUM
            trialStart == null -> TrialStatus.NOT_STARTED
            isTrialActive() -> {
                val daysRemaining = getRemainingTrialDays()
                TrialStatus.ACTIVE(daysRemaining)
            }
            else -> TrialStatus.EXPIRED
        }
    }

    /**
     * Get remaining trial days.
     */
    suspend fun getRemainingTrialDays(): Int {
        val prefs = dataStore.data.first()
        val trialStart = prefs[KEY_TRIAL_START_DATE] ?: return TRIAL_DURATION_DAYS.toInt()
        val elapsed = System.currentTimeMillis() - trialStart
        val remaining = TRIAL_DURATION_MS - elapsed
        return TimeUnit.MILLISECONDS.toDays(remaining).coerceAtLeast(0).toInt()
    }

    /**
     * Set premium status (called after successful purchase).
     */
    suspend fun setPremiumStatus(isPremium: Boolean, expiryDate: Long = 0L) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = isPremium
            prefs[KEY_PREMIUM_EXPIRY] = expiryDate
        }
        Log.d(TAG, "Premium status updated: isPremium=$isPremium, expiry=$expiryDate")
    }

    /**
     * Grant lifetime premium (for testing or special cases).
     */
    suspend fun grantLifetimePremium() {
        setPremiumStatus(isPremium = true, expiryDate = 0L)
        Log.d(TAG, "Lifetime premium granted")
    }

    /**
     * Observe premium status changes.
     */
    fun observePremiumStatus(): Flow<PremiumStatus> {
        return dataStore.data.map { prefs ->
            val isPremium = prefs[KEY_IS_PREMIUM] ?: false
            val trialStart = prefs[KEY_TRIAL_START_DATE]

            when {
                isPremium -> PremiumStatus(
                    hasAccess = true,
                    isPremium = true,
                    isTrialActive = false,
                    trialDaysRemaining = 0,
                    message = "Premium ativo"
                )
                trialStart == null -> PremiumStatus(
                    hasAccess = true,
                    isPremium = false,
                    isTrialActive = true,
                    trialDaysRemaining = TRIAL_DURATION_DAYS.toInt(),
                    message = "Experimenta gratis por $TRIAL_DURATION_DAYS dias!"
                )
                else -> {
                    val elapsed = System.currentTimeMillis() - trialStart
                    val isActive = elapsed < TRIAL_DURATION_MS
                    val remaining = if (isActive) {
                        TimeUnit.MILLISECONDS.toDays(TRIAL_DURATION_MS - elapsed).coerceAtLeast(0).toInt()
                    } else 0

                    PremiumStatus(
                        hasAccess = isActive,
                        isPremium = false,
                        isTrialActive = isActive,
                        trialDaysRemaining = remaining,
                        message = if (isActive) {
                            "Trial: $remaining dias restantes"
                        } else {
                            "Trial expirado. Atualiza para Premium!"
                        }
                    )
                }
            }
        }
    }

    /**
     * Check if user has ever used AI (for trial tracking).
     */
    suspend fun hasUsedAi(): Boolean {
        return dataStore.data.first()[KEY_HAS_USED_AI] ?: false
    }

    /**
     * Record AI usage (starts trial if not started).
     */
    suspend fun recordAiUsage() {
        startTrial()
    }

    /**
     * Get a user-friendly message about premium status.
     */
    suspend fun getStatusMessage(): String {
        return when (val status = getTrialStatus()) {
            TrialStatus.PREMIUM -> "Premium ativo"
            TrialStatus.NOT_STARTED -> "Experimenta o AI gratis por 7 dias!"
            is TrialStatus.ACTIVE -> "Trial: ${status.daysRemaining} dias restantes"
            TrialStatus.EXPIRED -> "Trial expirado. Atualiza para Premium para continuar a usar o AI!"
        }
    }
}

/**
 * Trial status sealed class.
 */
sealed class TrialStatus {
    data object PREMIUM : TrialStatus()
    data object NOT_STARTED : TrialStatus()
    data class ACTIVE(val daysRemaining: Int) : TrialStatus()
    data object EXPIRED : TrialStatus()
}

/**
 * Premium status data class for UI.
 */
data class PremiumStatus(
    val hasAccess: Boolean,
    val isPremium: Boolean,
    val isTrialActive: Boolean,
    val trialDaysRemaining: Int,
    val message: String
)
