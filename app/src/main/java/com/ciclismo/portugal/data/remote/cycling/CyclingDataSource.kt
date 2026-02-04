package com.ciclismo.portugal.data.remote.cycling

interface CyclingDataSource {
    suspend fun getTopCyclists(limit: Int = 200): Result<List<CyclistDto>>
    suspend fun getCyclistDetails(cyclistId: String): Result<CyclistDto>
    suspend fun getUpcomingRaces(): Result<List<RaceDto>>
    suspend fun getRaceResults(raceId: String): Result<List<RaceResultDto>>
    suspend fun searchCyclists(query: String): Result<List<CyclistDto>>

    /**
     * Get cyclists from a specific team page URL
     * @param teamUrl Full URL of the team page (e.g., "https://www.procyclingstats.com/team/alpecin-premier-tech-2026")
     * @param teamName Display name of the team
     * @return List of cyclists with ranking, age, and speciality
     */
    suspend fun getCyclistsFromTeamUrl(teamUrl: String, teamName: String): Result<List<CyclistDto>>

    /**
     * Get cyclist details from a full URL
     * @param url Full URL of the cyclist page (e.g., "https://www.procyclingstats.com/rider/tadej-pogacar")
     * @param teamName Optional team name to associate with the cyclist
     * @return Cyclist data
     */
    suspend fun getCyclistFromUrl(url: String, teamName: String = ""): Result<CyclistDto>
}
