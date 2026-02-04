package com.ciclismo.portugal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ciclismo.portugal.data.local.entity.RaceParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceParticipantDao {

    /**
     * Obtem todos os participantes de uma corrida
     */
    @Query("SELECT * FROM race_participants WHERE raceId = :raceId ORDER BY startListNumber ASC")
    fun getParticipants(raceId: String): Flow<List<RaceParticipantEntity>>

    /**
     * Obtem participantes confirmados de uma corrida
     */
    @Query("SELECT * FROM race_participants WHERE raceId = :raceId AND status = 'CONFIRMED' ORDER BY startListNumber ASC")
    fun getConfirmedParticipants(raceId: String): Flow<List<RaceParticipantEntity>>

    /**
     * Obtem IDs dos ciclistas confirmados para uma corrida
     */
    @Query("SELECT cyclistId FROM race_participants WHERE raceId = :raceId AND status = 'CONFIRMED'")
    suspend fun getConfirmedParticipantIds(raceId: String): List<String>

    /**
     * Obtem todas as corridas de um ciclista na temporada
     */
    @Query("SELECT * FROM race_participants WHERE cyclistId = :cyclistId AND season = :season")
    fun getCyclistRaces(cyclistId: String, season: Int): Flow<List<RaceParticipantEntity>>

    /**
     * Verifica se um ciclista esta inscrito numa corrida
     */
    @Query("SELECT EXISTS(SELECT 1 FROM race_participants WHERE raceId = :raceId AND cyclistId = :cyclistId AND status = 'CONFIRMED')")
    suspend fun isParticipant(raceId: String, cyclistId: String): Boolean

    /**
     * Conta participantes de uma corrida
     */
    @Query("SELECT COUNT(*) FROM race_participants WHERE raceId = :raceId AND status = 'CONFIRMED'")
    suspend fun countParticipants(raceId: String): Int

    /**
     * Insere um participante
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(participant: RaceParticipantEntity)

    /**
     * Insere multiplos participantes
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(participants: List<RaceParticipantEntity>)

    /**
     * Atualiza status de um participante
     */
    @Query("UPDATE race_participants SET status = :status WHERE raceId = :raceId AND cyclistId = :cyclistId")
    suspend fun updateStatus(raceId: String, cyclistId: String, status: String)

    /**
     * Remove participante de uma corrida
     */
    @Query("DELETE FROM race_participants WHERE raceId = :raceId AND cyclistId = :cyclistId")
    suspend fun removeParticipant(raceId: String, cyclistId: String)

    /**
     * Remove todos os participantes de uma corrida
     */
    @Query("DELETE FROM race_participants WHERE raceId = :raceId")
    suspend fun deleteForRace(raceId: String)

    /**
     * Apaga participantes de uma temporada
     */
    @Query("DELETE FROM race_participants WHERE season = :season")
    suspend fun deleteForSeason(season: Int)

    /**
     * Obtem ciclistas participando em corridas que comecam nos proximos X dias
     */
    @Query("""
        SELECT rp.* FROM race_participants rp
        INNER JOIN races r ON rp.raceId = r.id
        WHERE rp.status = 'CONFIRMED'
        AND r.startDate BETWEEN :fromDate AND :toDate
        AND rp.season = :season
    """)
    suspend fun getParticipantsForUpcomingRaces(fromDate: Long, toDate: Long, season: Int): List<RaceParticipantEntity>
}
