package com.ciclismo.portugal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ciclismo.portugal.data.local.entity.CyclistPriceHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CyclistPriceHistoryDao {

    /**
     * Obtem todo o historico de precos de um ciclista
     */
    @Query("SELECT * FROM cyclist_price_history WHERE cyclistId = :cyclistId ORDER BY timestamp DESC")
    fun getPriceHistory(cyclistId: String): Flow<List<CyclistPriceHistoryEntity>>

    /**
     * Obtem historico de precos recente de um ciclista
     */
    @Query("SELECT * FROM cyclist_price_history WHERE cyclistId = :cyclistId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPriceHistory(cyclistId: String, limit: Int): Flow<List<CyclistPriceHistoryEntity>>

    /**
     * Obtem ultimo registo de preco de um ciclista
     */
    @Query("SELECT * FROM cyclist_price_history WHERE cyclistId = :cyclistId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPriceChange(cyclistId: String): CyclistPriceHistoryEntity?

    /**
     * Obtem todas as alteracoes de preco de uma temporada desde um timestamp
     */
    @Query("SELECT * FROM cyclist_price_history WHERE season = :season AND timestamp >= :since ORDER BY timestamp DESC")
    fun getSeasonPriceChanges(season: Int, since: Long): Flow<List<CyclistPriceHistoryEntity>>

    /**
     * Obtem todas as alteracoes de preco de hoje
     */
    @Query("SELECT * FROM cyclist_price_history WHERE timestamp >= :todayStart ORDER BY timestamp DESC")
    fun getTodayPriceChanges(todayStart: Long): Flow<List<CyclistPriceHistoryEntity>>

    /**
     * Obtem alteracoes por razao (ex: PRE_RACE_BOOST)
     */
    @Query("SELECT * FROM cyclist_price_history WHERE reason = :reason AND season = :season ORDER BY timestamp DESC")
    fun getPriceChangesByReason(reason: String, season: Int): Flow<List<CyclistPriceHistoryEntity>>

    /**
     * Insere um novo registo de alteracao de preco
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: CyclistPriceHistoryEntity)

    /**
     * Insere multiplos registos
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<CyclistPriceHistoryEntity>)

    /**
     * Apaga historico de uma temporada
     */
    @Query("DELETE FROM cyclist_price_history WHERE season = :season")
    suspend fun deleteForSeason(season: Int)

    /**
     * Apaga historico antigo (antes de um timestamp)
     */
    @Query("DELETE FROM cyclist_price_history WHERE timestamp < :before")
    suspend fun deleteOldHistory(before: Long)

    /**
     * Conta total de alteracoes de preco
     */
    @Query("SELECT COUNT(*) FROM cyclist_price_history WHERE season = :season")
    suspend fun countForSeason(season: Int): Int
}
