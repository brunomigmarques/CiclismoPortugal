package com.ciclismo.portugal.data.local.dao

import androidx.room.*
import com.ciclismo.portugal.data.local.entity.TeamRaceResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamRaceResultDao {

    /**
     * Obter todos os resultados de uma equipa ordenados por data (mais recente primeiro)
     */
    @Query("SELECT * FROM team_race_results WHERE teamId = :teamId ORDER BY processedAt DESC")
    fun getTeamResults(teamId: String): Flow<List<TeamRaceResultEntity>>

    /**
     * Obter resultados de uma equipa de forma síncrona
     */
    @Query("SELECT * FROM team_race_results WHERE teamId = :teamId ORDER BY processedAt DESC")
    suspend fun getTeamResultsSync(teamId: String): List<TeamRaceResultEntity>

    /**
     * Obter o resultado de uma equipa numa corrida específica
     */
    @Query("SELECT * FROM team_race_results WHERE teamId = :teamId AND raceId = :raceId")
    suspend fun getTeamResultForRace(teamId: String, raceId: String): TeamRaceResultEntity?

    /**
     * Verificar se os resultados de uma corrida já foram processados para uma equipa
     */
    @Query("SELECT EXISTS(SELECT 1 FROM team_race_results WHERE teamId = :teamId AND raceId = :raceId)")
    suspend fun hasTeamResultForRace(teamId: String, raceId: String): Boolean

    /**
     * Obter todos os resultados de uma corrida (todas as equipas)
     */
    @Query("SELECT * FROM team_race_results WHERE raceId = :raceId ORDER BY pointsEarned DESC")
    fun getRaceResults(raceId: String): Flow<List<TeamRaceResultEntity>>

    /**
     * Obter todos os resultados de uma corrida de forma síncrona
     */
    @Query("SELECT * FROM team_race_results WHERE raceId = :raceId ORDER BY pointsEarned DESC")
    suspend fun getResultsForRaceSync(raceId: String): List<TeamRaceResultEntity>

    /**
     * Obter total de pontos ganhos por uma equipa em todas as corridas
     */
    @Query("SELECT COALESCE(SUM(pointsEarned), 0) FROM team_race_results WHERE teamId = :teamId")
    suspend fun getTotalPointsForTeam(teamId: String): Int

    // ===== Season-specific queries =====

    /**
     * Obter resultados de uma equipa para uma temporada específica, ordenados por data da corrida
     */
    @Query("SELECT * FROM team_race_results WHERE teamId = :teamId AND season = :season ORDER BY raceDate DESC")
    fun getTeamResultsForSeason(teamId: String, season: Int): Flow<List<TeamRaceResultEntity>>

    /**
     * Obter resultados de uma equipa para uma temporada de forma síncrona
     */
    @Query("SELECT * FROM team_race_results WHERE teamId = :teamId AND season = :season ORDER BY raceDate DESC")
    suspend fun getTeamResultsForSeasonSync(teamId: String, season: Int): List<TeamRaceResultEntity>

    /**
     * Obter total de pontos de uma equipa numa temporada específica
     */
    @Query("SELECT COALESCE(SUM(pointsEarned), 0) FROM team_race_results WHERE teamId = :teamId AND season = :season")
    suspend fun getTotalPointsForSeason(teamId: String, season: Int): Int

    /**
     * Obter número de corridas em que a equipa participou numa temporada
     */
    @Query("SELECT COUNT(*) FROM team_race_results WHERE teamId = :teamId AND season = :season")
    suspend fun getRaceCountForSeason(teamId: String, season: Int): Int

    /**
     * Obter melhor resultado de uma equipa numa temporada
     */
    @Query("SELECT * FROM team_race_results WHERE teamId = :teamId AND season = :season ORDER BY pointsEarned DESC LIMIT 1")
    suspend fun getBestResultForSeason(teamId: String, season: Int): TeamRaceResultEntity?

    /**
     * Obter todas as temporadas em que a equipa participou
     */
    @Query("SELECT DISTINCT season FROM team_race_results WHERE teamId = :teamId ORDER BY season DESC")
    suspend fun getSeasonsForTeam(teamId: String): List<Int>

    /**
     * Obter todas as temporadas para um utilizador (através das suas equipas)
     */
    @Query("""
        SELECT DISTINCT trr.season
        FROM team_race_results trr
        INNER JOIN fantasy_teams ft ON trr.teamId = ft.id
        WHERE ft.userId = :userId
        ORDER BY trr.season DESC
    """)
    suspend fun getSeasonsForUser(userId: String): List<Int>

    /**
     * Inserir um resultado
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: TeamRaceResultEntity)

    /**
     * Inserir vários resultados
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllResults(results: List<TeamRaceResultEntity>)

    /**
     * Apagar um resultado
     */
    @Delete
    suspend fun deleteResult(result: TeamRaceResultEntity)

    /**
     * Apagar todos os resultados de uma corrida
     */
    @Query("DELETE FROM team_race_results WHERE raceId = :raceId")
    suspend fun deleteRaceResults(raceId: String)

    /**
     * Apagar todos os resultados de uma equipa
     */
    @Query("DELETE FROM team_race_results WHERE teamId = :teamId")
    suspend fun deleteTeamResults(teamId: String)
}
