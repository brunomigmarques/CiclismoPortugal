package com.ciclismo.portugal.domain.pricing

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Calculador de precos dinamicos para ciclistas.
 *
 * Regras de preco:
 * 1. Procura: +0.1M por cada 5% de aumento de ownership
 * 2. Pre-corrida: +1% por dia nos 5 dias antes da corrida (max +5%)
 * 3. Limite diario: Max 5% de variacao por dia
 * 4. Limites absolutos: Min 1.0M, Max 25.0M
 */
@Singleton
class DynamicPriceCalculator @Inject constructor() {

    companion object {
        // Limites de preco
        const val MIN_PRICE = 1.0
        const val MAX_PRICE = 25.0

        // Limite de variacao diaria
        const val MAX_DAILY_CHANGE_PERCENT = 0.05 // 5% max por dia

        // Ajuste por procura
        const val OWNERSHIP_THRESHOLD = 5.0 // 5% de ownership
        const val PRICE_CHANGE_PER_THRESHOLD = 0.1 // 0.1M por threshold

        // Boost pre-corrida
        const val PRE_RACE_BOOST_PER_DAY = 0.01 // 1% por dia
        const val PRE_RACE_BOOST_DAYS = 5 // 5 dias antes da corrida

        // Ganhos por resultados
        const val POINTS_TO_MONEY_RATIO = 0.01 // 1 ponto = 0.01M
    }

    /**
     * Calcula o novo preco baseado na mudanca de ownership (procura).
     *
     * @param currentPrice Preco atual do ciclista
     * @param previousOwnership Percentagem de ownership anterior
     * @param currentOwnership Percentagem de ownership atual
     * @return Novo preco ajustado pela procura
     */
    fun calculateDemandPriceChange(
        currentPrice: Double,
        previousOwnership: Double,
        currentOwnership: Double
    ): Double {
        val ownershipChange = currentOwnership - previousOwnership
        val thresholds = (ownershipChange / OWNERSHIP_THRESHOLD).toInt()
        val priceChange = thresholds * PRICE_CHANGE_PER_THRESHOLD
        val newPrice = currentPrice + priceChange
        return newPrice.coerceIn(MIN_PRICE, MAX_PRICE)
    }

    /**
     * Calcula o boost de preco pre-corrida.
     * +1% por cada dia nos 5 dias antes da corrida.
     *
     * @param basePrice Preco base do ciclista
     * @param daysUntilRace Dias ate a corrida comecar
     * @return Preco com boost aplicado
     */
    fun calculatePreRaceBoost(
        basePrice: Double,
        daysUntilRace: Int
    ): Double {
        if (daysUntilRace < 0 || daysUntilRace > PRE_RACE_BOOST_DAYS) {
            return basePrice
        }

        // Dias de boost = 5 - dias restantes
        // Dia -5: 1 dia de boost = +1%
        // Dia -4: 2 dias de boost = +2%
        // ...
        // Dia -1: 5 dias de boost = +5%
        val boostDays = PRE_RACE_BOOST_DAYS - daysUntilRace
        val boostPercentage = boostDays * PRE_RACE_BOOST_PER_DAY
        val boostedPrice = basePrice * (1 + boostPercentage)

        return boostedPrice.coerceIn(MIN_PRICE, MAX_PRICE)
    }

    /**
     * Calcula o boost total de pre-corrida em percentagem.
     *
     * @param daysUntilRace Dias ate a corrida
     * @return Percentagem de boost (0.0 a 0.05)
     */
    fun getPreRaceBoostPercent(daysUntilRace: Int): Double {
        if (daysUntilRace < 0 || daysUntilRace > PRE_RACE_BOOST_DAYS) {
            return 0.0
        }
        val boostDays = PRE_RACE_BOOST_DAYS - daysUntilRace
        return boostDays * PRE_RACE_BOOST_PER_DAY
    }

    /**
     * Aplica o limite de variacao diaria (max 5% por dia).
     *
     * @param currentPrice Preco atual
     * @param calculatedPrice Preco calculado (antes do limite)
     * @return Preco final com limite aplicado
     */
    fun applyDailyLimit(currentPrice: Double, calculatedPrice: Double): Double {
        val maxIncrease = currentPrice * (1 + MAX_DAILY_CHANGE_PERCENT)
        val maxDecrease = currentPrice * (1 - MAX_DAILY_CHANGE_PERCENT)
        return calculatedPrice
            .coerceIn(maxDecrease, maxIncrease)
            .coerceIn(MIN_PRICE, MAX_PRICE)
    }

    /**
     * Calcula os ganhos em dinheiro baseados em pontos.
     * 1 ponto = 0.01M
     *
     * @param points Pontos ganhos
     * @return Dinheiro a adicionar ao budget
     */
    fun calculatePointsEarnings(points: Int): Double {
        return points * POINTS_TO_MONEY_RATIO
    }

    /**
     * Calcula o preco final combinando todos os fatores.
     *
     * @param basePrice Preco base original
     * @param currentPrice Preco atual
     * @param previousOwnership Ownership anterior (%)
     * @param currentOwnership Ownership atual (%)
     * @param daysUntilRace Dias ate a corrida (-1 se nao aplicavel)
     * @return Preco final ajustado
     */
    fun calculateFinalPrice(
        basePrice: Double,
        currentPrice: Double,
        previousOwnership: Double,
        currentOwnership: Double,
        daysUntilRace: Int = -1
    ): Double {
        // 1. Calcular ajuste de procura
        var newPrice = calculateDemandPriceChange(
            currentPrice = currentPrice,
            previousOwnership = previousOwnership,
            currentOwnership = currentOwnership
        )

        // 2. Aplicar boost de pre-corrida (se aplicavel)
        if (daysUntilRace in 0..PRE_RACE_BOOST_DAYS) {
            val boostPercent = getPreRaceBoostPercent(daysUntilRace)
            newPrice = newPrice * (1 + boostPercent)
        }

        // 3. Aplicar limite diario
        newPrice = applyDailyLimit(currentPrice, newPrice)

        return roundPrice(newPrice)
    }

    /**
     * Arredonda o preco para 1 casa decimal.
     */
    fun roundPrice(price: Double): Double {
        return (price * 10).roundToInt() / 10.0
    }

    /**
     * Calcula a variacao de preco em percentagem.
     */
    fun calculatePriceChangePercent(oldPrice: Double, newPrice: Double): Double {
        if (oldPrice <= 0) return 0.0
        return ((newPrice - oldPrice) / oldPrice) * 100
    }

    /**
     * Verifica se o preco pode subir mais (nao atingiu limite max).
     */
    fun canPriceIncrease(currentPrice: Double): Boolean {
        return currentPrice < MAX_PRICE
    }

    /**
     * Verifica se o preco pode descer mais (nao atingiu limite min).
     */
    fun canPriceDecrease(currentPrice: Double): Boolean {
        return currentPrice > MIN_PRICE
    }

    /**
     * Calcula ownership percentage.
     */
    fun calculateOwnershipPercent(ownersCount: Int, totalTeams: Int): Double {
        if (totalTeams <= 0) return 0.0
        return (ownersCount.toDouble() / totalTeams) * 100
    }
}
