package com.ciclismo.portugal.data.local.ai

import android.os.Bundle
import android.util.Log
import androidx.navigation.NavController
import com.ciclismo.portugal.data.local.premium.PremiumManager
import com.ciclismo.portugal.data.remote.firebase.AuthService
import com.ciclismo.portugal.domain.model.AiSuggestion
import com.ciclismo.portugal.domain.model.AiTriggerContext
import com.ciclismo.portugal.domain.model.AiTriggerType
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.repository.RaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central coordinator for contextual AI suggestions.
 * Listens to navigation events, collects context, and triggers appropriate suggestions.
 */
@Singleton
class AiCoordinator @Inject constructor(
    private val triggerEngine: AiTriggerEngine,
    private val authService: AuthService,
    private val fantasyTeamRepository: FantasyTeamRepository,
    private val raceRepository: RaceRepository,
    private val premiumManager: PremiumManager
) {
    companion object {
        private const val TAG = "AiCoordinator"
        private const val IDLE_CHECK_INTERVAL_MS = 10_000L // Check every 10 seconds
        private const val IDLE_THRESHOLD_MS = 30_000L // 30 seconds idle before trigger
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Idle detection job
    private var idleCheckerJob: Job? = null

    // Current screen being displayed
    private val _currentScreen = MutableStateFlow("")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Current suggestion to display
    private val _currentSuggestion = MutableStateFlow<AiSuggestion?>(null)
    val currentSuggestion: StateFlow<AiSuggestion?> = _currentSuggestion.asStateFlow()

    // Events for the overlay to react to
    private val _suggestionEvents = MutableSharedFlow<SuggestionEvent>()
    val suggestionEvents: SharedFlow<SuggestionEvent> = _suggestionEvents.asSharedFlow()

    // Cached context data
    private var cachedTeam: FantasyTeam? = null
    private var cachedNextRace: Race? = null
    private var lastContextRefresh: Long = 0

    // Track visited screens for first-visit triggers
    private val visitedScreens = mutableSetOf<String>()

    // Cached team cyclists data
    private var cachedTeamSize: Int = 0
    private var cachedHasCaptain: Boolean = false

    // Track idle time per screen
    private var screenEnteredTime: Long = 0
    private var lastInteractionTime: Long = 0

    // Track pending transfers (updated by Market screen)
    private var pendingTransfersCount: Int = 0

    /**
     * Attach to NavController to automatically track screen changes.
     */
    fun observeNavigation(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val route = destination.route ?: return@addOnDestinationChangedListener
            onScreenChanged(route, arguments)
        }
    }

    /**
     * Called when the screen changes.
     */
    fun onScreenChanged(screen: String, arguments: Bundle? = null) {
        Log.d(TAG, "Screen changed: $screen")
        _currentScreen.value = screen
        screenEnteredTime = System.currentTimeMillis()
        lastInteractionTime = screenEnteredTime

        // Check if first visit and mark as visited
        val isFirstVisit = !visitedScreens.contains(screen)
        if (isFirstVisit) {
            visitedScreens.add(screen)
        }

        // Start/restart idle checker for screens that support it
        if (shouldMonitorIdleOnScreen(screen)) {
            startIdleChecker()
        } else {
            stopIdleChecker()
        }

        // Evaluate triggers for new screen
        scope.launch {
            evaluateTriggersForCurrentScreen(isFirstVisit = isFirstVisit)
        }
    }

    /**
     * Check if idle detection should be active on this screen.
     */
    private fun shouldMonitorIdleOnScreen(screen: String): Boolean {
        return screen == "fantasy/market" || screen == "fantasy/team"
    }

    /**
     * Start the idle detection checker.
     */
    private fun startIdleChecker() {
        // Cancel existing job if any
        idleCheckerJob?.cancel()

        idleCheckerJob = scope.launch {
            while (isActive) {
                delay(IDLE_CHECK_INTERVAL_MS)

                val idleTime = System.currentTimeMillis() - lastInteractionTime
                if (idleTime >= IDLE_THRESHOLD_MS) {
                    Log.d(TAG, "User idle for ${idleTime / 1000}s, checking triggers")
                    evaluateTriggersForCurrentScreen()
                }
            }
        }
        Log.d(TAG, "Idle checker started for ${_currentScreen.value}")
    }

    /**
     * Stop the idle detection checker.
     */
    private fun stopIdleChecker() {
        idleCheckerJob?.cancel()
        idleCheckerJob = null
        Log.d(TAG, "Idle checker stopped")
    }

    /**
     * Called when user interacts with the screen (resets idle timer).
     */
    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    /**
     * Update pending transfers count (called from Market screen).
     */
    fun updatePendingTransfers(count: Int) {
        if (pendingTransfersCount != count) {
            pendingTransfersCount = count
            Log.d(TAG, "Pending transfers updated: $count")
            scope.launch {
                evaluateTriggersForCurrentScreen()
            }
        }
    }

    /**
     * Dismiss the current suggestion.
     */
    fun dismissCurrentSuggestion() {
        val suggestion = _currentSuggestion.value ?: return

        // Find the trigger type that generated this suggestion
        val triggerType = findTriggerTypeForSuggestion(suggestion)
        if (triggerType != null) {
            triggerEngine.dismissTrigger(triggerType)
        }

        _currentSuggestion.value = null
        scope.launch {
            _suggestionEvents.emit(SuggestionEvent.Dismissed(suggestion.id))
        }
    }

    /**
     * User wants to expand to full chat from a suggestion.
     */
    fun expandToChat() {
        scope.launch {
            _suggestionEvents.emit(SuggestionEvent.ExpandToChat)
        }
    }

    /**
     * Execute a quick action from a suggestion.
     */
    fun executeQuickAction(suggestion: AiSuggestion) {
        val action = suggestion.quickAction ?: return
        scope.launch {
            _suggestionEvents.emit(SuggestionEvent.ExecuteAction(action))
        }
        _currentSuggestion.value = null
    }

    /**
     * Manually trigger evaluation (e.g., after significant data change).
     */
    fun refreshTriggers() {
        scope.launch {
            refreshContext()
            evaluateTriggersForCurrentScreen()
        }
    }

    /**
     * Get current user ID for action execution.
     */
    fun getCurrentUserId(): String = authService.getCurrentUser()?.id ?: ""

    /**
     * Get current team ID for action execution.
     */
    fun getCurrentTeamId(): String? = cachedTeam?.id

    /**
     * Evaluate triggers for the current screen context.
     */
    private suspend fun evaluateTriggersForCurrentScreen(isFirstVisit: Boolean = false) {
        // Check premium access with logging
        val hasAccess = premiumManager.hasAiAccess()
        Log.d(TAG, "========== EVALUATING TRIGGERS ==========")
        Log.d(TAG, "hasAiAccess: $hasAccess, isFirstVisit: $isFirstVisit")

        if (!hasAccess) {
            Log.w(TAG, "No AI access, skipping triggers")
            return
        }

        // Always refresh context to ensure current data
        refreshContext()

        val context = buildTriggerContext(isFirstVisit)
        Log.d(TAG, "Context: screen=${context.currentScreen}, teamSize=${context.teamSize}, hasCaptain=${context.hasCaptain}, hoursUntilRace=${context.hoursUntilNextRace}")

        val suggestion = triggerEngine.evaluateTriggers(context)

        if (suggestion != null && suggestion != _currentSuggestion.value) {
            Log.d(TAG, "✅ NEW SUGGESTION: ${suggestion.title} (type: ${suggestion.type})")
            _currentSuggestion.value = suggestion
            _suggestionEvents.emit(SuggestionEvent.NewSuggestion(suggestion))
        }
    }

    /**
     * Refresh cached context data from repositories.
     */
    private suspend fun refreshContext() {
        try {
            // Get current user's team
            val currentUser = authService.getCurrentUser()
            Log.d(TAG, "refreshContext: currentUser=${currentUser?.id}, email=${currentUser?.email}")

            currentUser?.let { user ->
                Log.d(TAG, "refreshContext: Fetching team for user ${user.id}")
                cachedTeam = fantasyTeamRepository.getTeamByUserId(user.id).first()
                Log.d(TAG, "refreshContext: cachedTeam=${cachedTeam?.id}, name=${cachedTeam?.teamName}")

                // Get team size and captain status
                cachedTeam?.let { team ->
                    cachedTeamSize = fantasyTeamRepository.getTeamSize(team.id)
                    val teamCyclists = fantasyTeamRepository.getTeamCyclists(team.id).first()
                    cachedHasCaptain = teamCyclists.any { it.isCaptain }
                    Log.d(TAG, "Team context: size=$cachedTeamSize, hasCaptain=$cachedHasCaptain")
                } ?: run {
                    Log.w(TAG, "refreshContext: No team found for user ${user.id}")
                    cachedTeamSize = 0
                    cachedHasCaptain = false
                }
            } ?: run {
                Log.w(TAG, "refreshContext: No current user!")
                cachedTeamSize = 0
                cachedHasCaptain = false
            }

            // Get next upcoming race
            cachedNextRace = raceRepository.getUpcomingRaces().first().firstOrNull()

            lastContextRefresh = System.currentTimeMillis()
            Log.d(TAG, "Context refreshed: team=${cachedTeam?.teamName}, nextRace=${cachedNextRace?.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing context: ${e.message}")
        }
    }

    /**
     * Build the trigger context from current state.
     */
    private fun buildTriggerContext(isFirstVisit: Boolean): AiTriggerContext {
        val team = cachedTeam
        val nextRace = cachedNextRace

        // Calculate hours until next race
        val hoursUntilRace = nextRace?.let {
            try {
                val raceStartMs = it.startDate
                val nowMs = System.currentTimeMillis()
                val diffMs = raceStartMs - nowMs
                val hours = (diffMs / (1000 * 60 * 60)).toInt()
                hours.takeIf { h -> h >= 0 }
            } catch (e: Exception) {
                null
            }
        }

        // Calculate idle time
        val idleTimeMs = System.currentTimeMillis() - lastInteractionTime

        return AiTriggerContext(
            currentScreen = _currentScreen.value,
            userId = authService.getCurrentUser()?.id,
            teamId = team?.id,
            hasCaptain = cachedHasCaptain,
            teamSize = cachedTeamSize,
            maxTeamSize = 15,
            pendingTransfers = pendingTransfersCount,
            freeTransfers = team?.remainingFreeTransfers ?: 2,
            hasWildcard = team?.hasWildcard ?: false,
            wildcardActive = team?.hasUnlimitedTransfers ?: false,
            hoursUntilNextRace = hoursUntilRace,
            nextRaceName = nextRace?.name,
            idleTimeMs = idleTimeMs,
            hasVisitedScreen = !isFirstVisit,
            errorCount = 0,
            budget = team?.budget ?: 100.0
        )
    }

    /**
     * Find the trigger type that generated a suggestion.
     */
    private fun findTriggerTypeForSuggestion(suggestion: AiSuggestion): AiTriggerType? {
        return when {
            suggestion.title.contains("Capitao", ignoreCase = true) -> AiTriggerType.NO_CAPTAIN
            suggestion.title.contains("incompleta", ignoreCase = true) -> AiTriggerType.INCOMPLETE_TEAM
            suggestion.title.contains("Penalizacao", ignoreCase = true) -> AiTriggerType.TRANSFER_PENALTY_THRESHOLD
            suggestion.title.contains("Wildcard", ignoreCase = true) -> AiTriggerType.WILDCARD_OPPORTUNITY
            suggestion.title.contains("Deadline", ignoreCase = true) -> AiTriggerType.TRANSFER_DEADLINE
            suggestion.title.contains("Corrida em", ignoreCase = true) -> AiTriggerType.RACE_DEADLINE
            suggestion.title.contains("Bem-vindo ao Mercado", ignoreCase = true) -> AiTriggerType.FIRST_VISIT_MARKET
            suggestion.title.contains("A tua equipa", ignoreCase = true) -> AiTriggerType.FIRST_VISIT_MY_TEAM
            suggestion.title.contains("Ligas Fantasy", ignoreCase = true) -> AiTriggerType.FIRST_VISIT_LEAGUES
            suggestion.title.contains("Precisa de ajuda", ignoreCase = true) -> AiTriggerType.IDLE_ON_SCREEN
            else -> null
        }
    }
}

/**
 * Events emitted by the coordinator for the overlay to react to.
 */
sealed class SuggestionEvent {
    data class NewSuggestion(val suggestion: AiSuggestion) : SuggestionEvent()
    data class Dismissed(val suggestionId: String) : SuggestionEvent()
    object ExpandToChat : SuggestionEvent()
    data class ExecuteAction(val action: com.ciclismo.portugal.domain.model.AiAction) : SuggestionEvent()
}
