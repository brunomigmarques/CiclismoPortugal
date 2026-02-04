package com.ciclismo.portugal.presentation.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.ciclismo.portugal.data.local.ai.AiActionExecutor
import com.ciclismo.portugal.data.local.ai.AiCoordinator
import com.ciclismo.portugal.data.local.ai.AiService
import com.ciclismo.portugal.data.local.ai.SuggestionEvent
import com.ciclismo.portugal.data.local.premium.PremiumManager
import com.ciclismo.portugal.domain.model.AiAction
import com.ciclismo.portugal.domain.model.AiActionResult
import com.ciclismo.portugal.domain.model.AiSuggestion
import com.ciclismo.portugal.domain.model.AiSuggestionType
import com.ciclismo.portugal.presentation.ai.components.TutorialStep
import com.ciclismo.portugal.presentation.ai.components.TutorialSteps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the global AI overlay.
 * Manages state for FAB, mini tips, expandable cards, and full chat.
 */
@HiltViewModel
class AiOverlayViewModel @Inject constructor(
    private val coordinator: AiCoordinator,
    private val actionExecutor: AiActionExecutor,
    private val premiumManager: PremiumManager,
    private val aiService: AiService
) : ViewModel() {

    companion object {
        private const val TAG = "AiOverlayVM"
    }

    // Overlay expansion state
    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    // Current suggestion (from coordinator)
    val currentSuggestion: StateFlow<AiSuggestion?> = coordinator.currentSuggestion

    // Whether to show mini tip vs expandable card
    private val _showMiniTip = MutableStateFlow(false)
    val showMiniTip: StateFlow<Boolean> = _showMiniTip.asStateFlow()

    private val _showExpandableCard = MutableStateFlow(false)
    val showExpandableCard: StateFlow<Boolean> = _showExpandableCard.asStateFlow()

    // Has unread suggestion (for FAB notification dot)
    private val _hasUnreadSuggestion = MutableStateFlow(false)
    val hasUnreadSuggestion: StateFlow<Boolean> = _hasUnreadSuggestion.asStateFlow()

    // Navigation events
    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    // Message events (snackbar, toast)
    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    // Premium status
    private val _hasPremiumAccess = MutableStateFlow(false)
    val hasPremiumAccess: StateFlow<Boolean> = _hasPremiumAccess.asStateFlow()

    // Tutorial spotlight state
    private val _currentTutorialStep = MutableStateFlow<TutorialStep?>(null)
    val currentTutorialStep: StateFlow<TutorialStep?> = _currentTutorialStep.asStateFlow()

    private val _tutorialStepIndex = MutableStateFlow(0)
    val tutorialStepIndex: StateFlow<Int> = _tutorialStepIndex.asStateFlow()

    private val _totalTutorialSteps = MutableStateFlow(1)
    val totalTutorialSteps: StateFlow<Int> = _totalTutorialSteps.asStateFlow()

    // Track which tutorials have been shown
    private val shownTutorials = mutableSetOf<String>()

    init {
        checkPremiumStatus()
        observeSuggestionEvents()
        observeCurrentSuggestion()
    }

    /**
     * Attach the coordinator to the NavController for automatic screen tracking.
     */
    fun attachNavController(navController: NavController) {
        coordinator.observeNavigation(navController)
        // Trigger initial evaluation
        coordinator.refreshTriggers()
    }

    /**
     * Toggle the expanded state (FAB click).
     */
    fun toggleExpanded() {
        _isExpanded.value = !_isExpanded.value
        if (_isExpanded.value) {
            // Clear unread when expanding
            _hasUnreadSuggestion.value = false
            // Hide suggestion popups when chat is open
            _showMiniTip.value = false
            _showExpandableCard.value = false
        }
    }

    /**
     * Expand from a mini tip or card to full chat.
     */
    fun expandToChat() {
        _showMiniTip.value = false
        _showExpandableCard.value = false
        _isExpanded.value = true
        _hasUnreadSuggestion.value = false
    }

    /**
     * Collapse the chat overlay.
     */
    fun collapse() {
        _isExpanded.value = false
    }

    /**
     * Dismiss the current suggestion.
     */
    fun dismissCurrentSuggestion() {
        coordinator.dismissCurrentSuggestion()
        _showMiniTip.value = false
        _showExpandableCard.value = false
        _hasUnreadSuggestion.value = false
    }

    /**
     * Execute a quick action from a suggestion.
     */
    fun executeQuickAction(suggestion: AiSuggestion) {
        viewModelScope.launch {
            val action = suggestion.quickAction ?: return@launch

            _showMiniTip.value = false
            _showExpandableCard.value = false

            executeAction(action)
        }
    }

    /**
     * Execute an AI action with proper user context.
     */
    private suspend fun executeAction(action: AiAction) {
        Log.d(TAG, "Executing action: ${action.type}")

        // Get user/team context from coordinator (which caches this data)
        val userId = coordinator.getCurrentUserId()
        val teamId = coordinator.getCurrentTeamId()

        Log.d(TAG, "Action context: userId=$userId, teamId=$teamId")

        when (val result = actionExecutor.executeAction(action, userId, teamId)) {
            is AiActionResult.Success -> {
                _messageEvent.emit(result.message)
                // Refresh triggers after successful action
                coordinator.refreshTriggers()
            }
            is AiActionResult.Error -> {
                _messageEvent.emit(result.message)
            }
            is AiActionResult.RequiresAuth -> {
                _messageEvent.emit(result.message)
                _navigationEvent.emit("auth")
            }
            is AiActionResult.NavigateTo -> {
                _navigationEvent.emit(result.route)
            }
        }
    }

    /**
     * Report user interaction (resets idle timer).
     */
    fun onUserInteraction() {
        coordinator.onUserInteraction()
    }

    /**
     * Update pending transfers count (called from Market screen).
     */
    fun updatePendingTransfers(count: Int) {
        coordinator.updatePendingTransfers(count)
    }

    /**
     * Manually refresh triggers.
     */
    fun refreshTriggers() {
        coordinator.refreshTriggers()
    }

    // ========== Tutorial Management ==========

    /**
     * Show a tutorial spotlight for a specific screen.
     * Returns true if tutorial was shown, false if already shown before.
     */
    fun showTutorialForScreen(screen: String): Boolean {
        val tutorialId = "tutorial_$screen"
        if (shownTutorials.contains(tutorialId)) {
            return false
        }

        val step = when (screen) {
            "fantasy/market" -> TutorialSteps.MARKET_INTRO
            "fantasy/team" -> TutorialSteps.TEAM_CAPTAIN
            "fantasy/leagues" -> TutorialSteps.LEAGUES_INTRO
            else -> return false
        }

        _currentTutorialStep.value = step
        _tutorialStepIndex.value = 1
        _totalTutorialSteps.value = 1
        return true
    }

    /**
     * Show a multi-step tutorial sequence.
     */
    fun showTutorialSequence(steps: List<TutorialStep>) {
        if (steps.isEmpty()) return

        _currentTutorialStep.value = steps.first()
        _tutorialStepIndex.value = 1
        _totalTutorialSteps.value = steps.size
    }

    /**
     * Advance to the next tutorial step.
     */
    fun nextTutorialStep(steps: List<TutorialStep>) {
        val currentIndex = _tutorialStepIndex.value
        if (currentIndex < steps.size) {
            _currentTutorialStep.value = steps[currentIndex] // 0-indexed, so currentIndex is next
            _tutorialStepIndex.value = currentIndex + 1
        } else {
            dismissTutorial()
        }
    }

    /**
     * Dismiss the current tutorial.
     */
    fun dismissTutorial() {
        _currentTutorialStep.value?.let { step ->
            shownTutorials.add(step.id)
        }
        _currentTutorialStep.value = null
        _tutorialStepIndex.value = 0
    }

    /**
     * Check if a tutorial has been shown.
     */
    fun hasTutorialBeenShown(tutorialId: String): Boolean {
        return shownTutorials.contains(tutorialId)
    }

    private fun checkPremiumStatus() {
        viewModelScope.launch {
            _hasPremiumAccess.value = premiumManager.hasAiAccess()
        }
    }

    private fun observeSuggestionEvents() {
        viewModelScope.launch {
            coordinator.suggestionEvents.collect { event ->
                when (event) {
                    is SuggestionEvent.NewSuggestion -> {
                        handleNewSuggestion(event.suggestion)
                    }
                    is SuggestionEvent.Dismissed -> {
                        _showMiniTip.value = false
                        _showExpandableCard.value = false
                    }
                    is SuggestionEvent.ExpandToChat -> {
                        expandToChat()
                    }
                    is SuggestionEvent.ExecuteAction -> {
                        executeAction(event.action)
                    }
                }
            }
        }
    }

    private fun observeCurrentSuggestion() {
        viewModelScope.launch {
            coordinator.currentSuggestion.collect { suggestion ->
                if (suggestion != null && !_isExpanded.value) {
                    handleNewSuggestion(suggestion)
                }
            }
        }
    }

    private fun handleNewSuggestion(suggestion: AiSuggestion) {
        // Don't show popups if chat is already expanded
        if (_isExpanded.value) {
            _hasUnreadSuggestion.value = true
            return
        }

        // Determine popup type based on suggestion
        when (suggestion.type) {
            AiSuggestionType.MINI_TIP,
            AiSuggestionType.TUTORIAL -> {
                _showExpandableCard.value = false
                _showMiniTip.value = true
            }
            AiSuggestionType.EXPANDABLE_CARD -> {
                _showMiniTip.value = false
                _showExpandableCard.value = true
            }
        }

        _hasUnreadSuggestion.value = true
        Log.d(TAG, "Showing suggestion: ${suggestion.type} - ${suggestion.title}")
    }
}
