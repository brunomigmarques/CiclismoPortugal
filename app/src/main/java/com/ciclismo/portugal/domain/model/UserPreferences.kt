package com.ciclismo.portugal.domain.model

/**
 * User preferences collected during onboarding.
 * Stored in SharedPreferences and used to personalize the app experience.
 */
data class UserPreferences(
    val cyclingTypes: Set<CyclingType> = setOf(CyclingType.ALL),  // Multiple types allowed
    val experienceLevel: ExperienceLevel = ExperienceLevel.RECREATIONAL,
    val favoriteRegion: Region = Region.ALL,
    val goals: Set<UserGoal> = emptySet(),
    val hasCompletedOnboarding: Boolean = false,
    val notificationSettings: NotificationSettings = NotificationSettings()
) {
    // Convenience property for backwards compatibility
    val cyclingType: CyclingType
        get() = cyclingTypes.firstOrNull() ?: CyclingType.ALL
}

/**
 * Notification preferences for personalized alerts.
 */
data class NotificationSettings(
    // Event notifications
    val newProvasEnabled: Boolean = true,
    val provaRemindersEnabled: Boolean = true,
    val reminderDaysBefore: Int = 3, // Days before prova to send reminder

    // Filter notifications by cycling type
    val notifyRoadEvents: Boolean = true,
    val notifyBTTEvents: Boolean = true,
    val notifyGravelEvents: Boolean = true,

    // Filter by region - only notify for favorite region
    val notifyOnlyFavoriteRegion: Boolean = false,

    // Fantasy notifications
    val fantasyPointsEnabled: Boolean = true,
    val fantasyRankingEnabled: Boolean = true,
    val fantasyRaceRemindersEnabled: Boolean = true,

    // Daily tip notification (enabled by default on app startup)
    val dailyTipEnabled: Boolean = true,
    val dailyTipTime: Int = 8 // Hour of day (0-23)
)

/**
 * Type of cycling the user is most interested in.
 */
enum class CyclingType(val displayName: String, val emoji: String) {
    ROAD("Estrada", "🚴"),
    BTT("BTT / Mountain Bike", "🚵"),
    GRAVEL("Gravel", "🛤️"),
    ALL("Todos os tipos", "🚲")
}

/**
 * User's cycling experience level.
 */
enum class ExperienceLevel(val displayName: String, val description: String) {
    COMPETITIVE("Competitivo", "Participo em provas regularmente"),
    RECREATIONAL("Recreativo", "Pedalo por lazer e fitness"),
    BEGINNER("Iniciante", "Estou a começar no ciclismo")
}

/**
 * Portuguese regions for event filtering.
 */
enum class Region(val displayName: String) {
    NORTE("Norte"),
    CENTRO("Centro"),
    LISBOA("Lisboa e Vale do Tejo"),
    ALENTEJO("Alentejo"),
    ALGARVE("Algarve"),
    ACORES("Açores"),
    MADEIRA("Madeira"),
    ALL("Todo Portugal")
}

/**
 * User's goals with the app.
 */
enum class UserGoal(val displayName: String, val emoji: String) {
    FITNESS("Melhorar fitness", "💪"),
    COMPETE("Competir em provas", "🏆"),
    DISCOVER("Descobrir novos percursos", "🗺️"),
    SOCIALIZE("Conhecer outros ciclistas", "👥"),
    FOLLOW_PROS("Acompanhar ciclismo profissional", "🌟"),
    FANTASY("Jogar Fantasy Cycling", "🎮")
}
