package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.ProvaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProvaDao {

    @Query("SELECT * FROM provas WHERE data >= :todayTimestamp AND hidden = 0 ORDER BY data ASC")
    fun getAll(todayTimestamp: Long = System.currentTimeMillis()): Flow<List<ProvaEntity>>

    @Query("SELECT * FROM provas WHERE id = :id")
    suspend fun getById(id: Long): ProvaEntity?

    @Query("SELECT * FROM provas WHERE inCalendar = 1 AND data >= :todayTimestamp AND hidden = 0 ORDER BY data ASC")
    fun getCalendarProvas(todayTimestamp: Long = System.currentTimeMillis()): Flow<List<ProvaEntity>>

    @Query("SELECT * FROM provas WHERE (nome LIKE '%' || :query || '%' OR local LIKE '%' || :query || '%') AND data >= :todayTimestamp AND hidden = 0 ORDER BY data ASC")
    fun searchByName(query: String, todayTimestamp: Long = System.currentTimeMillis()): Flow<List<ProvaEntity>>

    @Query("SELECT * FROM provas WHERE tipo = :tipo ORDER BY data ASC")
    fun getByTipo(tipo: String): Flow<List<ProvaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prova: ProvaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(provas: List<ProvaEntity>)

    @Update
    suspend fun update(prova: ProvaEntity)

    @Delete
    suspend fun delete(prova: ProvaEntity)

    @Query("DELETE FROM provas WHERE data < :timestamp AND inCalendar = 0")
    suspend fun deleteOldProvas(timestamp: Long)

    @Query("DELETE FROM provas WHERE data < :timestamp")
    suspend fun deleteAllOldProvas(timestamp: Long)

    @Query("DELETE FROM provas")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM provas")
    suspend fun getCount(): Int

    @Query("SELECT DISTINCT tipo FROM provas WHERE data >= :todayTimestamp AND hidden = 0 ORDER BY tipo ASC")
    fun getDistinctTipos(todayTimestamp: Long = System.currentTimeMillis()): Flow<List<String>>

    @Query("SELECT DISTINCT local FROM provas WHERE data >= :todayTimestamp AND hidden = 0 ORDER BY local ASC")
    fun getDistinctLocais(todayTimestamp: Long = System.currentTimeMillis()): Flow<List<String>>

    // Admin queries - mostram todos os eventos incluindo ocultos
    @Query("SELECT * FROM provas WHERE data >= :todayTimestamp ORDER BY data ASC")
    fun getAllAdmin(todayTimestamp: Long = System.currentTimeMillis()): Flow<List<ProvaEntity>>

    @Query("UPDATE provas SET hidden = :hidden WHERE id = :provaId")
    suspend fun setHidden(provaId: Long, hidden: Boolean)

    @Query("DELETE FROM provas WHERE id = :provaId")
    suspend fun deleteById(provaId: Long)
}
