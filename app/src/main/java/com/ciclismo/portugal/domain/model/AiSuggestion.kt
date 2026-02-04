package com.ciclismo.portugal.domain.model

/**
 * Represents a contextual AI suggestion that can appear as a popup.
 */
data class AiSuggestion(
    val id: String,
    val type: AiSuggestionType,
    val title: String,
    val shortText: String, // Max 50 chars for MiniTip
    val description: String,
    val priority: SuggestionPriority = SuggestionPriority.NORMAL,
    val quickAction: AiAction? = null, // Optional one-tap action
    val triggerScreen: String? = null, // Screen where this was triggered
    val dismissable: Boolean = true,
    val autoExpireMs: Long = 10000 // Auto-dismiss after 10s by default
)

enum class AiSuggestionType {
    MINI_TIP,        // Small badge above FAB
    EXPANDABLE_CARD, // Medium card with actions
    TUTORIAL         // First-visit tutorial tip
}

enum class SuggestionPriority {
    LOW,    // Can be delayed or skipped
    NORMAL, // Show when appropriate
    HIGH,   // Show soon
    URGENT  // Show immediately
}

/**
 * Trigger conditions that can generate AI suggestions.
 */
enum class AiTriggerType {
    // Critical setup issues
    NO_CAPTAIN,
    INCOMPLETE_TEAM,

    // Decision points
    TRANSFER_PENALTY_THRESHOLD,
    WILDCARD_OPPORTUNITY,

    // Deadlines
    RACE_DEADLINE,
    TRANSFER_DEADLINE,

    // First-time experiences
    FIRST_VISIT_MARKET,
    FIRST_VISIT_MY_TEAM,
    FIRST_VISIT_LEAGUES,

    // User behavior
    IDLE_ON_SCREEN,
    REPEATED_ERRORS,

    // Opportunities
    UNDERVALUED_CYCLIST,
    FORM_DROP_ALERT
}

/**
 * Context data used to evaluate triggers.
 */
data class AiTriggerContext(
    val currentScreen: String,
    val userId: String?,
    val teamId: String?,
    val hasCaptain: Boolean = false,
    val teamSize: Int = 0,
    val maxTeamSize: Int = 15,
    val pendingTransfers: Int = 0,
    val freeTransfers: Int = 2,
    val hasWildcard: Boolean = false,
    val wildcardActive: Boolean = false,
    val hoursUntilNextRace: Int? = null,
    val nextRaceName: String? = null,
    val idleTimeMs: Long = 0,
    val hasVisitedScreen: Boolean = true,
    val errorCount: Int = 0,
    val budget: Double = 0.0
)
