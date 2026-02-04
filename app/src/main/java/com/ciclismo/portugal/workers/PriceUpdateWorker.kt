package com.ciclismo.portugal.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ciclismo.portugal.data.local.dao.CyclistDao
import com.ciclismo.portugal.data.local.dao.CyclistDemandDao
import com.ciclismo.portugal.data.local.dao.CyclistPriceHistoryDao
import com.ciclismo.portugal.data.local.dao.FantasyTeamDao
import com.ciclismo.portugal.data.local.dao.RaceDao
import com.ciclismo.portugal.data.local.dao.RaceParticipantDao
import com.ciclismo.portugal.data.local.entity.CyclistDemandEntity
import com.ciclismo.portugal.data.local.entity.CyclistPriceHistoryEntity
import com.ciclismo.portugal.data.local.entity.PriceChangeReason
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.pricing.DynamicPriceCalculator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Worker que corre diariamente para atualizar os precos dos ciclistas.
 *
 * Responsabilidades:
 * 1. Calcular ownership percentages de cada ciclista
 * 2. Aplicar ajustes de procura baseados em compras/vendas
 * 3. Aplicar boosts de pre-corrida (+1% por dia, max 5%)
 * 4. Resetar boosts apos corridas terminarem
 * 5. Guardar historico de alteracoes de preco
 */
@HiltWorker
class PriceUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val cyclistDao: CyclistDao,
    private val cyclistDemandDao: CyclistDemandDao,
    private val cyclistPriceHistoryDao: CyclistPriceHistoryDao,
    private val fantasyTeamDao: FantasyTeamDao,
    private val raceDao: RaceDao,
    private val raceParticipantDao: RaceParticipantDao,
    private val priceCalculator: DynamicPriceCalculator
) : CoroutineWorker(appContext, workerParams) {

    private val season = SeasonConfig.CURRENT_SEASON

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting daily price update...")

            // 1. Calcular ownership percentages
            val ownershipUpdated = updateOwnershipPercentages()
            Log.d(TAG, "Updated ownership for $ownershipUpdated cyclists")

            // 2. Aplicar ajustes de procura
            val demandAdjusted = applyDemandPriceChanges()
            Log.d(TAG, "Applied demand adjustments to $demandAdjusted cyclists")

            // 3. Aplicar boosts de pre-corrida
            val boostsApplied = applyPreRaceBoosts()
            Log.d(TAG, "Applied pre-race boosts to $boostsApplied cyclists")

            // 4. Resetar boosts de corridas terminadas
            val boostsReset = resetFinishedRaceBoosts()
            Log.d(TAG, "Reset boosts for $boostsReset cyclists")

            Log.d(TAG, "Daily price update completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating prices: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * Atualiza as percentagens de ownership de cada ciclista.
     */
    private suspend fun updateOwnershipPercentages(): Int {
        val periodStart = getTodayStartTimestamp()
        val cyclists = cyclistDao.getAllCyclistsForSeasonOnce(season)
        val totalTeams = fantasyTeamDao.getTeamCountForSeason(season)

        if (totalTeams == 0) {
            Log.d(TAG, "No teams found, skipping ownership update")
            return 0
        }

        var updated = 0
        for (cyclist in cyclists) {
            val ownerCount = fantasyTeamDao.countTeamsWithCyclist(cyclist.id, season)
            val ownershipPercent = priceCalculator.calculateOwnershipPercent(ownerCount, totalTeams)

            // Atualizar popularidade no ciclista
            if (cyclist.popularity != ownershipPercent) {
                cyclistDao.updatePopularity(cyclist.id, ownershipPercent)
            }

            // Guardar ou atualizar demand record
            val existingDemand = cyclistDemandDao.getDemand(cyclist.id, periodStart)
            if (existingDemand != null) {
                cyclistDemandDao.updateOwnership(cyclist.id, periodStart, ownerCount, totalTeams)
            } else {
                cyclistDemandDao.insert(
                    CyclistDemandEntity(
                        cyclistId = cyclist.id,
                        periodStart = periodStart,
                        ownershipCount = ownerCount,
                        totalTeams = totalTeams,
                        season = season
                    )
                )
            }
            updated++
        }

        return updated
    }

    /**
     * Aplica ajustes de preco baseados na procura (compras/vendas).
     */
    private suspend fun applyDemandPriceChanges(): Int {
        val periodStart = getTodayStartTimestamp()
        val yesterdayStart = periodStart - TimeUnit.DAYS.toMillis(1)

        val todayDemands = cyclistDemandDao.getAllDemandForPeriod(periodStart, season)
        val yesterdayDemands = cyclistDemandDao.getAllDemandForPeriod(yesterdayStart, season)
            .associateBy { it.cyclistId }

        var adjusted = 0
        for (demand in todayDemands) {
            val cyclist = cyclistDao.getCyclistById(demand.cyclistId) ?: continue
            val previousDemand = yesterdayDemands[demand.cyclistId]

            val previousOwnership = previousDemand?.ownershipPercent ?: cyclist.popularity
            val currentOwnership = demand.ownershipPercent

            // Calcular novo preco baseado na procura
            val newPrice = priceCalculator.calculateDemandPriceChange(
                currentPrice = cyclist.price,
                previousOwnership = previousOwnership,
                currentOwnership = currentOwnership
            )

            // Aplicar limite diario
            val limitedPrice = priceCalculator.applyDailyLimit(cyclist.price, newPrice)

            // So atualizar se houve mudanca significativa
            if (limitedPrice != cyclist.price) {
                // Guardar historico
                cyclistPriceHistoryDao.insert(
                    CyclistPriceHistoryEntity(
                        cyclistId = cyclist.id,
                        oldPrice = cyclist.price,
                        newPrice = limitedPrice,
                        reason = PriceChangeReason.DEMAND.name,
                        timestamp = System.currentTimeMillis(),
                        season = season
                    )
                )

                // Atualizar preco
                cyclistDao.updatePrice(cyclist.id, limitedPrice)
                cyclistDao.updateLastPriceUpdate(cyclist.id, System.currentTimeMillis())
                adjusted++
            }
        }

        return adjusted
    }

    /**
     * Aplica boosts de preco para ciclistas em corridas proximas.
     */
    private suspend fun applyPreRaceBoosts(): Int {
        val now = System.currentTimeMillis()
        val fiveDaysFromNow = now + TimeUnit.DAYS.toMillis(5)

        // Obter corridas que comecam nos proximos 5 dias
        val upcomingRaces = raceDao.getUpcomingRacesInRangeOnce(now, fiveDaysFromNow, season)

        var boostsApplied = 0
        for (race in upcomingRaces) {
            val daysUntilRace = TimeUnit.MILLISECONDS.toDays(race.startDate - now).toInt()

            // Obter participantes confirmados
            val participantIds = raceParticipantDao.getConfirmedParticipantIds(race.id)

            for (cyclistId in participantIds) {
                val cyclist = cyclistDao.getCyclistById(cyclistId) ?: continue

                // Verificar se ja tem boost para esta corrida
                if (cyclist.priceBoostActive && cyclist.priceBoostRaceId == race.id) {
                    continue // Ja tem boost, verificar se precisa atualizar
                }

                // Calcular preco com boost
                val basePrice = if (cyclist.basePrice > 0) cyclist.basePrice else cyclist.price
                val boostedPrice = priceCalculator.calculatePreRaceBoost(basePrice, daysUntilRace)
                val limitedPrice = priceCalculator.applyDailyLimit(cyclist.price, boostedPrice)

                if (limitedPrice != cyclist.price) {
                    // Guardar historico
                    cyclistPriceHistoryDao.insert(
                        CyclistPriceHistoryEntity(
                            cyclistId = cyclistId,
                            oldPrice = cyclist.price,
                            newPrice = limitedPrice,
                            reason = PriceChangeReason.PRE_RACE_BOOST.name,
                            raceId = race.id,
                            timestamp = System.currentTimeMillis(),
                            season = season
                        )
                    )

                    // Atualizar ciclista
                    cyclistDao.updatePrice(cyclistId, limitedPrice)
                    cyclistDao.updatePriceBoost(cyclistId, true, race.id)
                    cyclistDao.updateLastPriceUpdate(cyclistId, System.currentTimeMillis())

                    // Se nao tinha basePrice, guardar o preco original
                    if (cyclist.basePrice <= 0) {
                        cyclistDao.updateBasePrice(cyclistId, cyclist.price)
                    }

                    boostsApplied++
                }
            }
        }

        return boostsApplied
    }

    /**
     * Reseta os boosts de corridas que ja terminaram.
     */
    private suspend fun resetFinishedRaceBoosts(): Int {
        // Obter corridas terminadas recentemente
        val finishedRaces = raceDao.getFinishedRacesForSeasonOnce(season)

        var boostsReset = 0
        for (race in finishedRaces) {
            // Obter ciclistas com boost desta corrida
            val cyclistsWithBoost = cyclistDao.getCyclistsWithPriceBoostForRace(race.id)

            for (cyclist in cyclistsWithBoost) {
                if (cyclist.basePrice > 0) {
                    // Guardar historico
                    cyclistPriceHistoryDao.insert(
                        CyclistPriceHistoryEntity(
                            cyclistId = cyclist.id,
                            oldPrice = cyclist.price,
                            newPrice = cyclist.basePrice,
                            reason = PriceChangeReason.RACE_RESET.name,
                            raceId = race.id,
                            timestamp = System.currentTimeMillis(),
                            season = season
                        )
                    )

                    // Reset preco para base
                    cyclistDao.updatePrice(cyclist.id, cyclist.basePrice)
                }

                // Desativar boost
                cyclistDao.updatePriceBoost(cyclist.id, false, null)
                cyclistDao.updateLastPriceUpdate(cyclist.id, System.currentTimeMillis())

                boostsReset++
            }
        }

        return boostsReset
    }

    /**
     * Obtem o timestamp do inicio do dia (00:00:00).
     */
    private fun getTodayStartTimestamp(): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Lisbon"))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    companion object {
        private const val TAG = "PriceUpdateWorker"
        const val WORK_NAME = "PriceUpdateWork"
    }
}
