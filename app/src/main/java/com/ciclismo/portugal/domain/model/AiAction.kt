package com.ciclismo.portugal.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Represents an action that the AI can suggest to the user.
 * Actions can be executed directly from the chat when user approves.
 */
data class AiAction(
    val id: String,
    val type: AiActionType,
    val title: String,
    val description: String,
    val parameters: Map<String, String> = emptyMap(),
    val priority: ActionPriority = ActionPriority.NORMAL,
    val requiresConfirmation: Boolean = true
)

/**
 * Types of actions the AI can suggest.
 * Extended for full app-wide tutoring capabilities.
 */
enum class AiActionType {
    // Team management (Fantasy)
    @SerializedName("buy_cyclist") BUY_CYCLIST,
    @SerializedName("sell_cyclist") SELL_CYCLIST,
    @SerializedName("set_captain") SET_CAPTAIN,
    @SerializedName("activate_cyclist") ACTIVATE_CYCLIST,
    @SerializedName("deactivate_cyclist") DEACTIVATE_CYCLIST,

    // Extras/Power-ups (Fantasy)
    @SerializedName("use_triple_captain") USE_TRIPLE_CAPTAIN,
    @SerializedName("use_wildcard") USE_WILDCARD,

    // Navigation - General
    @SerializedName("navigate_to") NAVIGATE_TO,
    @SerializedName("view_cyclist") VIEW_CYCLIST,
    @SerializedName("view_race") VIEW_RACE,

    // Navigation - App Screens
    @SerializedName("view_prova") VIEW_PROVA,
    @SerializedName("view_news") VIEW_NEWS,
    @SerializedName("view_rankings") VIEW_RANKINGS,
    @SerializedName("view_profile") VIEW_PROFILE,
    @SerializedName("view_video") VIEW_VIDEO,

    // User Results
    @SerializedName("view_my_results") VIEW_MY_RESULTS,
    @SerializedName("add_result") ADD_RESULT,

    // Information
    @SerializedName("show_standings") SHOW_STANDINGS,
    @SerializedName("show_calendar") SHOW_CALENDAR,
    @SerializedName("show_help") SHOW_HELP,
    @SerializedName("show_tutorial") SHOW_TUTORIAL,

    // Settings
    @SerializedName("enable_notifications") ENABLE_NOTIFICATIONS,
    @SerializedName("set_reminder") SET_REMINDER,

    // Premium
    @SerializedName("view_premium") VIEW_PREMIUM
}

enum class ActionPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Response from AI containing message and optional actions.
 */
data class AiResponse(
    val message: String,
    val actions: List<AiAction> = emptyList(),
    val context: AiContext? = null
)

/**
 * Context about the current app state for AI awareness.
 */
data class AiContext(
    val currentScreen: String,
    val userId: String?,
    val teamId: String?,
    val teamName: String?,
    val teamBudget: Double?,
    val teamCyclistCount: Int?,
    val activeCyclistCount: Int?,
    val captainName: String?,
    val nextRaceName: String?,
    val nextRaceDate: String?,
    val upcomingRaceCount: Int?,
    val totalPoints: Int?,
    val leagueRank: Int?,
    val recentActions: List<String> = emptyList()
)

/**
 * Result of executing an AI action.
 */
sealed class AiActionResult {
    data class Success(val message: String) : AiActionResult()
    data class Error(val message: String) : AiActionResult()
    data class RequiresAuth(val message: String) : AiActionResult()
    data class NavigateTo(val route: String) : AiActionResult()
}
