package com.ciclismo.portugal.presentation.profile

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.remote.firebase.AuthService
import com.ciclismo.portugal.data.remote.firebase.UserPreferencesFirestoreService
import com.ciclismo.portugal.data.remote.strava.StravaAthleteInfo
import com.ciclismo.portugal.data.remote.strava.StravaRepository
import com.ciclismo.portugal.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val authService: AuthService,
    private val stravaRepository: StravaRepository,
    private val userPreferencesFirestoreService: UserPreferencesFirestoreService
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileVM"
        private const val KEY_CYCLING_TYPES = "cycling_types"  // Multiple types (comma-separated)
        private const val KEY_CYCLING_TYPE = "cycling_type"    // Legacy - single type
        private const val KEY_EXPERIENCE_LEVEL = "experience_level"
        private const val KEY_FAVORITE_REGION = "favorite_region"
        private const val KEY_USER_GOALS = "user_goals"
        // Notification settings keys
        private const val KEY_NOTIF_NEW_PROVAS = "notif_new_provas"
        private const val KEY_NOTIF_PROVA_REMINDERS = "notif_prova_reminders"
        private const val KEY_NOTIF_REMINDER_DAYS = "notif_reminder_days"
        private const val KEY_NOTIF_ROAD_EVENTS = "notif_road_events"
        private const val KEY_NOTIF_BTT_EVENTS = "notif_btt_events"
        private const val KEY_NOTIF_GRAVEL_EVENTS = "notif_gravel_events"
        private const val KEY_NOTIF_ONLY_FAV_REGION = "notif_only_fav_region"
        private const val KEY_NOTIF_FANTASY_POINTS = "notif_fantasy_points"
        private const val KEY_NOTIF_FANTASY_RANKING = "notif_fantasy_ranking"
        private const val KEY_NOTIF_FANTASY_RACES = "notif_fantasy_races"
        private const val KEY_NOTIF_DAILY_TIP = "notif_daily_tip"
    }

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private val _stravaState = MutableStateFlow<StravaConnectionState>(StravaConnectionState.Disconnected)
    val stravaState: StateFlow<StravaConnectionState> = _stravaState.asStateFlow()

    init {
        loadUserState()
        loadPreferences()
        loadStravaState()
    }

    private fun loadStravaState() {
        if (stravaRepository.isConnected()) {
            val athlete = stravaRepository.getConnectedAthlete()
            if (athlete != null) {
                _stravaState.value = StravaConnectionState.Connected(athlete)
            }
        }
    }

    fun getStravaAuthUrl(): String {
        return stravaRepository.buildAuthorizationUrl()
    }

    fun handleStravaCallback(code: String) {
        viewModelScope.launch {
            _stravaState.value = StravaConnectionState.Connecting
            val result = stravaRepository.exchangeCodeForToken(code)
            result.onSuccess { athlete ->
                val athleteInfo = StravaAthleteInfo(
                    id = athlete.id,
                    name = listOfNotNull(athlete.firstName, athlete.lastName)
                        .joinToString(" ")
                        .ifEmpty { athlete.username ?: "Athlete" },
                    photoUrl = athlete.profileImageUrl
                )
                _stravaState.value = StravaConnectionState.Connected(athleteInfo)
            }.onFailure {
                _stravaState.value = StravaConnectionState.Error(it.message ?: "Failed to connect")
            }
        }
    }

    fun disconnectStrava() {
        stravaRepository.disconnect()
        _stravaState.value = StravaConnectionState.Disconnected
    }

    private fun loadUserState() {
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                _uiState.value = if (user != null) {
                    ProfileUiState.LoggedIn(user)
                } else {
                    ProfileUiState.Anonymous
                }
            }
        }
    }

    private fun loadPreferences() {
        // Load multiple cycling types (or fallback to legacy single type)
        val cyclingTypesString = sharedPreferences.getString(KEY_CYCLING_TYPES, null)
        val cyclingTypes: Set<CyclingType> = if (cyclingTypesString != null) {
            cyclingTypesString.split(",")
                .mapNotNull { runCatching { CyclingType.valueOf(it.trim()) }.getOrNull() }
                .toSet()
                .ifEmpty { setOf(CyclingType.ALL) }
        } else {
            // Fallback to legacy single type
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

        // Load notification settings
        val notificationSettings = NotificationSettings(
            newProvasEnabled = sharedPreferences.getBoolean(KEY_NOTIF_NEW_PROVAS, true),
            provaRemindersEnabled = sharedPreferences.getBoolean(KEY_NOTIF_PROVA_REMINDERS, true),
            reminderDaysBefore = sharedPreferences.getInt(KEY_NOTIF_REMINDER_DAYS, 3),
            notifyRoadEvents = sharedPreferences.getBoolean(KEY_NOTIF_ROAD_EVENTS, true),
            notifyBTTEvents = sharedPreferences.getBoolean(KEY_NOTIF_BTT_EVENTS, true),
            notifyGravelEvents = sharedPreferences.getBoolean(KEY_NOTIF_GRAVEL_EVENTS, true),
            notifyOnlyFavoriteRegion = sharedPreferences.getBoolean(KEY_NOTIF_ONLY_FAV_REGION, false),
            fantasyPointsEnabled = sharedPreferences.getBoolean(KEY_NOTIF_FANTASY_POINTS, true),
            fantasyRankingEnabled = sharedPreferences.getBoolean(KEY_NOTIF_FANTASY_RANKING, true),
            fantasyRaceRemindersEnabled = sharedPreferences.getBoolean(KEY_NOTIF_FANTASY_RACES, true),
            dailyTipEnabled = sharedPreferences.getBoolean(KEY_NOTIF_DAILY_TIP, true)
        )

        _preferences.value = UserPreferences(
            cyclingTypes = cyclingTypes,
            experienceLevel = experienceLevel,
            favoriteRegion = region,
            goals = goals,
            hasCompletedOnboarding = true,
            notificationSettings = notificationSettings
        )
    }

    fun toggleCyclingType(type: CyclingType) {
        val currentTypes = _preferences.value.cyclingTypes.toMutableSet()

        if (type == CyclingType.ALL) {
            // Selecting "All" clears others
            _preferences.value = _preferences.value.copy(cyclingTypes = setOf(CyclingType.ALL))
            savePreferences()
            return
        }

        // Remove ALL if selecting a specific type
        currentTypes.remove(CyclingType.ALL)

        if (type in currentTypes) {
            currentTypes.remove(type)
            // If no types left, default to ALL
            if (currentTypes.isEmpty()) {
                currentTypes.add(CyclingType.ALL)
            }
        } else {
            currentTypes.add(type)
        }

        _preferences.value = _preferences.value.copy(cyclingTypes = currentTypes)
        savePreferences()
    }

    // Legacy setter for backwards compatibility
    fun setCyclingType(type: CyclingType) {
        _preferences.value = _preferences.value.copy(cyclingTypes = setOf(type))
        savePreferences()
    }

    fun setExperienceLevel(level: ExperienceLevel) {
        _preferences.value = _preferences.value.copy(experienceLevel = level)
        savePreferences()
    }

    fun setFavoriteRegion(region: Region) {
        _preferences.value = _preferences.value.copy(favoriteRegion = region)
        savePreferences()
    }

    fun toggleGoal(goal: UserGoal) {
        val currentGoals = _preferences.value.goals.toMutableSet()
        if (goal in currentGoals) {
            currentGoals.remove(goal)
        } else {
            currentGoals.add(goal)
        }
        _preferences.value = _preferences.value.copy(goals = currentGoals)
        savePreferences()
    }

    // Notification settings methods
    fun updateNotificationSettings(settings: NotificationSettings) {
        _preferences.value = _preferences.value.copy(notificationSettings = settings)
        savePreferences()
    }

    fun toggleNewProvasNotification(enabled: Boolean) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(newProvasEnabled = enabled))
    }

    fun toggleProvaReminders(enabled: Boolean) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(provaRemindersEnabled = enabled))
    }

    fun setReminderDays(days: Int) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(reminderDaysBefore = days))
    }

    fun toggleRoadNotifications(enabled: Boolean) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(notifyRoadEvents = enabled))
    }

    fun toggleBTTNotifications(enabled: Boolean) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(notifyBTTEvents = enabled))
    }

    fun toggleGravelNotifications(enabled: Boolean) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(notifyGravelEvents = enabled))
    }

    fun toggleOnlyFavoriteRegion(enabled: Boolean) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(notifyOnlyFavoriteRegion = enabled))
    }

    fun toggleFantasyPointsNotification(enabled: Boolean) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(fantasyPointsEnabled = enabled))
    }

    fun toggleFantasyRankingNotification(enabled: Boolean) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(fantasyRankingEnabled = enabled))
    }

    fun toggleFantasyRaceNotification(enabled: Boolean) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(fantasyRaceRemindersEnabled = enabled))
    }

    fun toggleDailyTipNotification(enabled: Boolean) {
        val current = _preferences.value.notificationSettings
        updateNotificationSettings(current.copy(dailyTipEnabled = enabled))
    }

    private fun savePreferences() {
        val prefs = _preferences.value
        val notifSettings = prefs.notificationSettings

        // Save to local SharedPreferences
        sharedPreferences.edit().apply {
            putString(KEY_CYCLING_TYPES, prefs.cyclingTypes.joinToString(",") { it.name })
            putString(KEY_EXPERIENCE_LEVEL, prefs.experienceLevel.name)
            putString(KEY_FAVORITE_REGION, prefs.favoriteRegion.name)
            putString(KEY_USER_GOALS, prefs.goals.joinToString(",") { it.name })
            // Notification settings
            putBoolean(KEY_NOTIF_NEW_PROVAS, notifSettings.newProvasEnabled)
            putBoolean(KEY_NOTIF_PROVA_REMINDERS, notifSettings.provaRemindersEnabled)
            putInt(KEY_NOTIF_REMINDER_DAYS, notifSettings.reminderDaysBefore)
            putBoolean(KEY_NOTIF_ROAD_EVENTS, notifSettings.notifyRoadEvents)
            putBoolean(KEY_NOTIF_BTT_EVENTS, notifSettings.notifyBTTEvents)
            putBoolean(KEY_NOTIF_GRAVEL_EVENTS, notifSettings.notifyGravelEvents)
            putBoolean(KEY_NOTIF_ONLY_FAV_REGION, notifSettings.notifyOnlyFavoriteRegion)
            putBoolean(KEY_NOTIF_FANTASY_POINTS, notifSettings.fantasyPointsEnabled)
            putBoolean(KEY_NOTIF_FANTASY_RANKING, notifSettings.fantasyRankingEnabled)
            putBoolean(KEY_NOTIF_FANTASY_RACES, notifSettings.fantasyRaceRemindersEnabled)
            putBoolean(KEY_NOTIF_DAILY_TIP, notifSettings.dailyTipEnabled)
            apply()
        }

        // Sync to Firestore if user is logged in (for personalized rankings)
        syncPreferencesToFirestore(prefs)
    }

    private fun syncPreferencesToFirestore(prefs: UserPreferences) {
        viewModelScope.launch {
            authService.getCurrentUser()?.let { user ->
                userPreferencesFirestoreService.saveUserPreferences(
                    userId = user.id,
                    preferences = prefs,
                    country = "PT" // Portugal is the default country
                ).onFailure { e ->
                    Log.e(TAG, "Failed to sync preferences to Firestore: ${e.message}")
                }.onSuccess {
                    Log.d(TAG, "Synced preferences to Firestore: region=${prefs.favoriteRegion}, types=${prefs.cyclingTypes}")
                }
            }
        }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    object Anonymous : ProfileUiState()
    data class LoggedIn(val user: User) : ProfileUiState()
}

sealed class StravaConnectionState {
    object Disconnected : StravaConnectionState()
    object Connecting : StravaConnectionState()
    data class Connected(val athlete: StravaAthleteInfo) : StravaConnectionState()
    data class Error(val message: String) : StravaConnectionState()
}
