package com.ciclismo.portugal.presentation.onboarding

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.remote.firebase.AuthService
import com.ciclismo.portugal.data.remote.firebase.UserPreferencesFirestoreService
import com.ciclismo.portugal.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val authService: AuthService,
    private val userPreferencesFirestoreService: UserPreferencesFirestoreService
) : ViewModel() {

    companion object {
        private const val TAG = "OnboardingVM"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_CYCLING_TYPES = "cycling_types"  // Now stores multiple
        private const val KEY_CYCLING_TYPE = "cycling_type"    // Legacy - single type
        private const val KEY_EXPERIENCE_LEVEL = "experience_level"
        private const val KEY_FAVORITE_REGION = "favorite_region"
        private const val KEY_USER_GOALS = "user_goals"

        // Set to true to force onboarding to show for testing
        private const val FORCE_SHOW_ONBOARDING = false
    }

    private val _currentStep = MutableStateFlow(OnboardingStep.Welcome)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        // Load multiple cycling types (new format) or fall back to legacy single type
        val cyclingTypesString = sharedPreferences.getString(KEY_CYCLING_TYPES, null)
        val cyclingTypes = if (cyclingTypesString != null) {
            cyclingTypesString.split(",")
                .mapNotNull { runCatching { CyclingType.valueOf(it.trim()) }.getOrNull() }
                .toSet()
                .ifEmpty { setOf(CyclingType.ALL) }
        } else {
            // Legacy: load single type
            val legacyType = sharedPreferences.getString(KEY_CYCLING_TYPE, null)
                ?.let { runCatching { CyclingType.valueOf(it) }.getOrNull() }
                ?: CyclingType.ALL
            setOf(legacyType)
        }

        val experienceLevel = sharedPreferences.getString(KEY_EXPERIENCE_LEVEL, null)
            ?.let { runCatching { ExperienceLevel.valueOf(it) }.getOrNull() }
            ?: ExperienceLevel.RECREATIONAL

        val region = sharedPreferences.getString(KEY_FAVORITE_REGION, null)
            ?.let { runCatching { Region.valueOf(it) }.getOrNull() }
            ?: Region.ALL

        val goalsString = sharedPreferences.getString(KEY_USER_GOALS, null)
        val goals = goalsString?.split(",")
            ?.mapNotNull { runCatching { UserGoal.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: emptySet()

        val hasCompleted = sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)

        _preferences.value = UserPreferences(
            cyclingTypes = cyclingTypes,
            experienceLevel = experienceLevel,
            favoriteRegion = region,
            goals = goals,
            hasCompletedOnboarding = hasCompleted
        )
    }

    fun hasCompletedOnboarding(): Boolean {
        // For testing: return false to always show onboarding
        if (FORCE_SHOW_ONBOARDING) return false
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun toggleCyclingType(type: CyclingType) {
        val currentTypes = _preferences.value.cyclingTypes.toMutableSet()

        // If selecting "ALL", clear others and just select ALL
        if (type == CyclingType.ALL) {
            _preferences.value = _preferences.value.copy(cyclingTypes = setOf(CyclingType.ALL))
            return
        }

        // Remove ALL if selecting a specific type
        currentTypes.remove(CyclingType.ALL)

        if (type in currentTypes) {
            currentTypes.remove(type)
            // If nothing selected, default to ALL
            if (currentTypes.isEmpty()) {
                currentTypes.add(CyclingType.ALL)
            }
        } else {
            currentTypes.add(type)
        }
        _preferences.value = _preferences.value.copy(cyclingTypes = currentTypes)
    }

    fun setExperienceLevel(level: ExperienceLevel) {
        _preferences.value = _preferences.value.copy(experienceLevel = level)
    }

    fun setFavoriteRegion(region: Region) {
        _preferences.value = _preferences.value.copy(favoriteRegion = region)
    }

    fun toggleGoal(goal: UserGoal) {
        val currentGoals = _preferences.value.goals.toMutableSet()
        if (goal in currentGoals) {
            currentGoals.remove(goal)
        } else {
            currentGoals.add(goal)
        }
        _preferences.value = _preferences.value.copy(goals = currentGoals)
    }

    fun nextStep() {
        _currentStep.value = when (_currentStep.value) {
            OnboardingStep.Welcome -> OnboardingStep.CyclingType
            OnboardingStep.CyclingType -> OnboardingStep.Goals
            OnboardingStep.Goals -> OnboardingStep.Complete
            OnboardingStep.Complete -> OnboardingStep.Complete
        }
    }

    fun previousStep() {
        _currentStep.value = when (_currentStep.value) {
            OnboardingStep.Welcome -> OnboardingStep.Welcome
            OnboardingStep.CyclingType -> OnboardingStep.Welcome
            OnboardingStep.Goals -> OnboardingStep.CyclingType
            OnboardingStep.Complete -> OnboardingStep.Goals
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val prefs = _preferences.value

            // Save to local SharedPreferences
            sharedPreferences.edit().apply {
                putBoolean(KEY_ONBOARDING_COMPLETED, true)
                putString(KEY_CYCLING_TYPES, prefs.cyclingTypes.joinToString(",") { it.name })
                putString(KEY_EXPERIENCE_LEVEL, prefs.experienceLevel.name)
                putString(KEY_FAVORITE_REGION, prefs.favoriteRegion.name)
                putString(KEY_USER_GOALS, prefs.goals.joinToString(",") { it.name })
                apply()
            }

            // Sync to Firestore if user is logged in (for personalized rankings)
            authService.getCurrentUser()?.let { user ->
                userPreferencesFirestoreService.saveUserPreferences(
                    userId = user.id,
                    preferences = prefs,
                    country = "PT" // Portugal is the default country
                ).onFailure { e ->
                    Log.e(TAG, "Failed to sync preferences to Firestore: ${e.message}")
                }.onSuccess {
                    Log.d(TAG, "Synced user preferences to Firestore: region=${prefs.favoriteRegion}, types=${prefs.cyclingTypes}")
                }
            }

            _isComplete.value = true
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            sharedPreferences.edit().apply {
                putBoolean(KEY_ONBOARDING_COMPLETED, true)
                apply()
            }
            _isComplete.value = true
        }
    }
}

enum class OnboardingStep(val stepNumber: Int, val totalSteps: Int = 3) {
    Welcome(0),
    CyclingType(1),  // Combined with Experience
    Goals(2),        // Skip Region - can be set in Profile later
    Complete(3)
}
