package com.ciclismo.portugal.domain.model

/**
 * AI-generated content for the Fantasy hub.
 * Generated automatically based on upcoming races and cyclist data.
 */
data class AiGeneratedContent(
    val id: String,
    val type: AiContentType,
    val title: String,
    val content: String,
    val raceId: String? = null,
    val raceName: String? = null,
    val generatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // 24 hours
    val season: Int = 2026
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
}

enum class AiContentType {
    TOP_CANDIDATES,      // Top 3 cyclists for next race
    STAGE_INSIGHT,       // Insights about the stage/race
    DAILY_TIP,           // Daily strategy tip
    TRANSFER_SUGGESTION  // Who to buy/sell
}

/**
 * Top cyclist candidate for a race.
 */
data class TopCandidate(
    val rank: Int,
    val cyclistId: String,
    val cyclistName: String,
    val teamName: String,
    val category: String,
    val price: Double,
    val reason: String,
    val expectedPoints: String,
    val photoUrl: String? = null
)

/**
 * AI insights for a race/stage.
 */
data class RaceInsight(
    val raceId: String,
    val raceName: String,
    val stageNumber: Int? = null,
    val stageType: String? = null,
    val keyFactors: List<String>,
    val recommendation: String,
    val difficulty: String, // Easy, Medium, Hard, Extreme
    val bestCategories: List<String> // GC, Climber, Sprinter, etc.
)
