package com.ciclismo.portugal.domain.model

data class StravaProfile(
    val id: Long,
    val name: String,
    val city: String,
    val country: String,
    val profileImageUrl: String?,
    val isConnected: Boolean = false
)

data class StravaYearStats(
    val year: Int,
    val totalKm: Float,
    val totalActivities: Int,
    val totalTimeHours: Float,
    val totalElevationMeters: Float,
    val averageSpeedKmh: Float
)

data class StravaGoal(
    val id: Long = 0,
    val type: GoalType,
    val targetValue: Float,
    val currentValue: Float,
    val year: Int
) {
    val progressPercentage: Float
        get() = if (targetValue > 0) (currentValue / targetValue * 100f).coerceIn(0f, 100f) else 0f

    val isCompleted: Boolean
        get() = currentValue >= targetValue
}

enum class GoalType {
    DISTANCE_KM,      // Quilômetros totais
    ACTIVITIES,       // Número de atividades
    ELEVATION_M,      // Elevação total
    TIME_HOURS        // Tempo total em horas
}
