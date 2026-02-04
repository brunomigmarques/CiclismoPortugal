package com.ciclismo.portugal.domain.model

data class Cyclist(
    val id: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val teamId: String,
    val teamName: String,
    val nationality: String,
    val photoUrl: String?,
    val category: CyclistCategory,
    val price: Double,
    val totalPoints: Int = 0,
    val form: Double = 0.0,
    val popularity: Double = 0.0,
    val age: Int? = null,
    val uciRanking: Int? = null,
    val speciality: String? = null,
    val profileUrl: String? = null, // Link para ProCyclingStats ou CyclingRanking
    val season: Int = SeasonConfig.CURRENT_SEASON,
    // Dynamic pricing fields
    val basePrice: Double = 0.0, // Preco original antes de ajustes de mercado
    val priceBoostActive: Boolean = false, // Tem boost de pre-corrida ativo
    val priceBoostRaceId: String? = null, // Corrida que ativou o boost
    val lastPriceUpdate: Long = System.currentTimeMillis(),
    // Availability status
    val isDisabled: Boolean = false, // Ciclista indisponivel (lesao, abandono, etc)
    val disabledReason: String? = null, // Motivo da indisponibilidade
    val disabledAt: Long? = null // Data em que foi desativado
) {
    val displayPrice: String
        get() = "${String.format("%.1f", price)}M"

    val displayInfo: String
        get() = buildString {
            age?.let { append("${it}a") }
            uciRanking?.let {
                if (isNotEmpty()) append(" | ")
                append("#$it UCI")
            }
            speciality?.let {
                if (isNotEmpty()) append(" | ")
                append(it)
            }
        }

    /** Diferenca entre preco atual e preco base */
    val priceChange: Double
        get() = if (basePrice > 0) price - basePrice else 0.0

    /** Percentagem de variacao de preco */
    val priceChangePercent: Double
        get() = if (basePrice > 0) ((price - basePrice) / basePrice) * 100 else 0.0

    /** Display do preco base */
    val displayBasePrice: String
        get() = if (basePrice > 0) "${String.format("%.1f", basePrice)}M" else displayPrice

    /** Display da variacao de preco */
    val displayPriceChange: String
        get() = when {
            priceChange > 0 -> "+${String.format("%.1f", priceChange)}M"
            priceChange < 0 -> "${String.format("%.1f", priceChange)}M"
            else -> "0.0M"
        }

    /** Indica se o ciclista esta disponivel para compra/selecao */
    val isAvailable: Boolean
        get() = !isDisabled

    /** Display do status de disponibilidade */
    val displayStatus: String
        get() = if (isDisabled) {
            disabledReason ?: "Indisponível"
        } else {
            "Disponível"
        }
}

enum class CyclistCategory {
    CLIMBER,     // Escaladores
    HILLS,       // Especialistas em colinas/puncheurs
    TT,          // Especialistas em contra-relogio
    SPRINT,      // Sprinters
    GC,          // Candidatos a General Classification
    ONEDAY       // Especialistas em classicas de um dia
}
