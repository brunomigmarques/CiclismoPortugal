package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Query("SELECT * FROM transfers WHERE teamId = :teamId ORDER BY timestamp DESC")
    fun getTransfersForTeam(teamId: String): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE teamId = :teamId AND gameweek = :gameweek ORDER BY timestamp DESC")
    fun getTransfersForGameweek(teamId: String, gameweek: Int): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE teamId = :teamId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransfers(teamId: String, limit: Int = 10): Flow<List<TransferEntity>>

    @Query("SELECT COUNT(*) FROM transfers WHERE teamId = :teamId AND gameweek = :gameweek")
    suspend fun getTransferCountForGameweek(teamId: String, gameweek: Int): Int

    @Query("SELECT SUM(pointsCost) FROM transfers WHERE teamId = :teamId AND gameweek = :gameweek")
    suspend fun getTotalPointsCostForGameweek(teamId: String, gameweek: Int): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity)

    @Delete
    suspend fun deleteTransfer(transfer: TransferEntity)

    @Query("DELETE FROM transfers WHERE teamId = :teamId")
    suspend fun deleteAllTransfersForTeam(teamId: String)

    @Query("DELETE FROM transfers WHERE timestamp < :date")
    suspend fun deleteOldTransfers(date: Long)
}
