package com.ciclismo.portugal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ciclismo.portugal.data.local.entity.CyclistDemandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CyclistDemandDao {

    /**
     * Obtem demand de um ciclista para um periodo especifico
     */
    @Query("SELECT * FROM cyclist_demand WHERE cyclistId = :cyclistId AND periodStart = :periodStart")
    suspend fun getDemand(cyclistId: String, periodStart: Long): CyclistDemandEntity?

    /**
     * Obtem todos os registos de demand de um ciclista
     */
    @Query("SELECT * FROM cyclist_demand WHERE cyclistId = :cyclistId ORDER BY periodStart DESC")
    fun getCyclistDemandHistory(cyclistId: String): Flow<List<CyclistDemandEntity>>

    /**
     * Obtem todo o demand para um periodo (todos os ciclistas)
     */
    @Query("SELECT * FROM cyclist_demand WHERE periodStart = :periodStart AND season = :season")
    suspend fun getAllDemandForPeriod(periodStart: Long, season: Int): List<CyclistDemandEntity>

    /**
     * Insere ou atualiza um registo de demand
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(demand: CyclistDemandEntity)

    /**
     * Incrementa contador de compras
     */
    @Query("UPDATE cyclist_demand SET buyCount = buyCount + 1 WHERE cyclistId = :cyclistId AND periodStart = :periodStart")
    suspend fun incrementBuy(cyclistId: String, periodStart: Long)

    /**
     * Incrementa contador de vendas
     */
    @Query("UPDATE cyclist_demand SET sellCount = sellCount + 1 WHERE cyclistId = :cyclistId AND periodStart = :periodStart")
    suspend fun incrementSell(cyclistId: String, periodStart: Long)

    /**
     * Atualiza contagem de ownership
     */
    @Query("UPDATE cyclist_demand SET ownershipCount = :count, totalTeams = :totalTeams WHERE cyclistId = :cyclistId AND periodStart = :periodStart")
    suspend fun updateOwnership(cyclistId: String, periodStart: Long, count: Int, totalTeams: Int)

    /**
     * Obtem ciclistas mais comprados num periodo
     */
    @Query("""
        SELECT * FROM cyclist_demand
        WHERE periodStart = :periodStart AND season = :season
        ORDER BY buyCount DESC
        LIMIT :limit
    """)
    suspend fun getMostBought(periodStart: Long, season: Int, limit: Int = 10): List<CyclistDemandEntity>

    /**
     * Obtem ciclistas mais vendidos num periodo
     */
    @Query("""
        SELECT * FROM cyclist_demand
        WHERE periodStart = :periodStart AND season = :season
        ORDER BY sellCount DESC
        LIMIT :limit
    """)
    suspend fun getMostSold(periodStart: Long, season: Int, limit: Int = 10): List<CyclistDemandEntity>

    /**
     * Obtem ciclistas com maior net demand (compras - vendas)
     */
    @Query("""
        SELECT * FROM cyclist_demand
        WHERE periodStart = :periodStart AND season = :season
        ORDER BY (buyCount - sellCount) DESC
        LIMIT :limit
    """)
    suspend fun getHighestNetDemand(periodStart: Long, season: Int, limit: Int = 10): List<CyclistDemandEntity>

    /**
     * Apaga registos de demand antigos
     */
    @Query("DELETE FROM cyclist_demand WHERE periodStart < :before")
    suspend fun deleteOldDemand(before: Long)

    /**
     * Apaga demand de uma temporada
     */
    @Query("DELETE FROM cyclist_demand WHERE season = :season")
    suspend fun deleteForSeason(season: Int)
}
