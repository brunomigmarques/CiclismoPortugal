package com.ciclismo.portugal.domain.repository

import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceResult
import kotlinx.coroutines.flow.Flow

interface RaceRepository {
    // Races
    fun getUpcomingRaces(): Flow<List<Race>>
    fun getActiveRaces(): Flow<List<Race>>
    fun getNextUpcomingRaceForAi(): Flow<List<Race>>
    suspend fun getRacesForYear(year: Int): Result<List<Race>>
    suspend fun getRaceById(raceId: String): Race?

    // Team freeze check
    suspend fun isRaceHappeningToday(): Boolean
    suspend fun getRaceHappeningToday(): Race?
    suspend fun getRacesStartingTomorrow(): List<Race>

    // Sync from Firestore to local
    suspend fun syncRacesFromFirestore(): Result<Int>

    // Admin operations
    suspend fun uploadRace(race: Race): Result<Unit>
    suspend fun uploadRaces(races: List<Race>): Result<Int>
    suspend fun setRaceActive(raceId: String, isActive: Boolean): Result<Unit>
    suspend fun setRaceFinished(raceId: String): Result<Unit>
    suspend fun isRaceFinished(raceId: String): Boolean
    suspend fun deleteRace(raceId: String): Result<Unit>

    // Race Results
    fun getRaceResults(raceId: String): Flow<List<RaceResult>>
    suspend fun getRaceResultsOnce(raceId: String): List<RaceResult>
    suspend fun getCyclistResults(cyclistId: String): Result<List<RaceResult>>
    suspend fun uploadRaceResult(result: RaceResult): Result<Unit>

    // Fantasy points calculation
    suspend fun calculateFantasyPointsForRace(raceId: String): Result<Map<String, Int>>
}
