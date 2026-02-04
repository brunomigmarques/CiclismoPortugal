package com.ciclismo.portugal.domain.model

data class League(
    val id: String,
    val name: String,
    val type: LeagueType,
    val code: String?,
    val ownerId: String?,
    val region: String?,
    val memberCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val season: Int = SeasonConfig.CURRENT_SEASON
) {
    val isPrivate: Boolean
        get() = type == LeagueType.PRIVATE

    val isGlobal: Boolean
        get() = type == LeagueType.GLOBAL
}

enum class LeagueType {
    GLOBAL,     // Liga Geral Portugal (todos participam)
    PRIVATE,    // Ligas privadas com codigo
    REGIONAL,   // Ligas por distrito
    MONTHLY     // Liga mensal com reset
}
