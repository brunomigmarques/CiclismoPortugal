package com.ciclismo.portugal.presentation.fantasy.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.local.dao.FantasyTeamDao
import com.ciclismo.portugal.data.local.dao.TeamRaceResultDao
import com.ciclismo.portugal.data.local.entity.toHistoryItem
import com.ciclismo.portugal.data.remote.firebase.TeamResultFirestoreService
import com.ciclismo.portugal.data.remote.firebase.TeamStageResultData
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.SeasonSummary
import com.ciclismo.portugal.domain.model.TeamRaceHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

@HiltViewModel
class SeasonHistoryViewModel @Inject constructor(
    private val fantasyTeamDao: FantasyTeamDao,
    private val teamRaceResultDao: TeamRaceResultDao,
    private val teamResultFirestoreService: TeamResultFirestoreService
) : ViewModel() {

    companion object {
        private const val TAG = "SeasonHistoryVM"
    }

    private val _uiState = MutableStateFlow<SeasonHistoryUiState>(SeasonHistoryUiState.Loading)
    val uiState: StateFlow<SeasonHistoryUiState> = _uiState.asStateFlow()

    private val _availableSeasons = MutableStateFlow<List<Int>>(listOf(SeasonConfig.CURRENT_SEASON))
    val availableSeasons: StateFlow<List<Int>> = _availableSeasons.asStateFlow()

    private val _selectedSeason = MutableStateFlow(SeasonConfig.CURRENT_SEASON)
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    private val _seasonSummary = MutableStateFlow<SeasonSummary?>(null)
    val seasonSummary: StateFlow<SeasonSummary?> = _seasonSummary.asStateFlow()

    private val _raceHistory = MutableStateFlow<List<TeamRaceHistoryItem>>(emptyList())
    val raceHistory: StateFlow<List<TeamRaceHistoryItem>> = _raceHistory.asStateFlow()

    // Race detail dialog state
    private val _selectedRaceDetail = MutableStateFlow<RaceDetailState?>(null)
    val selectedRaceDetail: StateFlow<RaceDetailState?> = _selectedRaceDetail.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    private var currentUserId: String? = null
    private var currentTeamId: String? = null

    fun loadHistory(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            _uiState.value = SeasonHistoryUiState.Loading

            try {
                // Get all seasons user participated in
                val seasons = fantasyTeamDao.getUserSeasons(userId)
                _availableSeasons.value = seasons.ifEmpty { listOf(SeasonConfig.CURRENT_SEASON) }

                // Load data for current season
                loadSeasonData(userId, _selectedSeason.value)
            } catch (e: Exception) {
                _uiState.value = SeasonHistoryUiState.Error("Erro ao carregar histórico: ${e.message}")
            }
        }
    }

    fun selectSeason(season: Int) {
        if (season == _selectedSeason.value) return
        _selectedSeason.value = season
        currentUserId?.let { userId ->
            viewModelScope.launch {
                loadSeasonData(userId, season)
            }
        }
    }

    private suspend fun loadSeasonData(userId: String, season: Int) {
        _uiState.value = SeasonHistoryUiState.Loading

        try {
            // Get team for this season
            val team = fantasyTeamDao.getTeamForSeason(userId, season)

            if (team == null) {
                _uiState.value = SeasonHistoryUiState.NoTeam(season)
                _seasonSummary.value = null
                _raceHistory.value = emptyList()
                return
            }

            currentTeamId = team.id

            // Get race results for this team and season from local DB
            var results = teamRaceResultDao.getTeamResultsForSeasonSync(team.id, season)

            // If local DB is empty, try to sync from Firestore
            if (results.isEmpty()) {
                Log.d("SeasonHistory", "Local history empty, syncing from Firestore...")
                try {
                    val firestoreResults = teamResultFirestoreService.getTeamResultsForSeason(team.id, season)
                    if (firestoreResults.isNotEmpty()) {
                        Log.d("SeasonHistory", "Found ${firestoreResults.size} results in Firestore, saving to local DB")
                        teamRaceResultDao.insertAllResults(firestoreResults)
                        results = firestoreResults
                    }
                } catch (e: Exception) {
                    Log.e("SeasonHistory", "Failed to sync from Firestore: ${e.message}")
                    // Continue with empty results
                }
            }

            _raceHistory.value = results.map { it.toHistoryItem() }

            // Calculate season summary
            val totalPoints = results.sumOf { it.pointsEarned }
            val racesParticipated = results.size
            val bestResult = results.maxByOrNull { it.pointsEarned }

            _seasonSummary.value = SeasonSummary(
                season = season,
                teamName = team.teamName,
                totalPoints = totalPoints,
                racesParticipated = racesParticipated,
                bestRacePoints = bestResult?.pointsEarned ?: 0,
                bestRaceName = bestResult?.raceName ?: "-",
                rank = null // TODO: Calculate league rank if in a league
            )

            _uiState.value = SeasonHistoryUiState.Success
        } catch (e: Exception) {
            _uiState.value = SeasonHistoryUiState.Error("Erro ao carregar temporada: ${e.message}")
        }
    }

    /**
     * Force sync team history from Firestore to local DB
     */
    fun syncFromFirestore() {
        val teamId = currentTeamId ?: return
        val season = _selectedSeason.value

        viewModelScope.launch {
            try {
                Log.d("SeasonHistory", "Manual sync from Firestore for team: $teamId")
                val firestoreResults = teamResultFirestoreService.getTeamResultsForSeason(teamId, season)
                if (firestoreResults.isNotEmpty()) {
                    // Clear existing and insert fresh data
                    teamRaceResultDao.insertAllResults(firestoreResults)
                    _raceHistory.value = firestoreResults.map { it.toHistoryItem() }
                    Log.d("SeasonHistory", "Synced ${firestoreResults.size} results from Firestore")
                }
            } catch (e: Exception) {
                Log.e("SeasonHistory", "Failed to sync from Firestore: ${e.message}")
            }
        }
    }

    /**
     * Load race details when a race is clicked
     * Uses stored breakdown data from the database instead of calculating dynamically
     * Also checks for Grand Tour stage results
     */
    fun loadRaceDetails(race: TeamRaceHistoryItem) {
        val teamId = currentTeamId ?: return
        val season = _selectedSeason.value

        viewModelScope.launch {
            _isLoadingDetail.value = true

            try {
                // Get team info
                val team = fantasyTeamDao.getTeamById(teamId)

                // Check if this race has stage results (Grand Tour)
                val stageResults = try {
                    teamResultFirestoreService.getTeamStageResults(teamId, race.raceId, season)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching stage results: ${e.message}")
                    emptyList()
                }

                val isGrandTour = stageResults.isNotEmpty()
                Log.d(TAG, "Race ${race.raceName}: isGrandTour=$isGrandTour, stages=${stageResults.size}")

                // Get stored race result with breakdown from local DB
                var raceResult = teamRaceResultDao.getTeamResultForRace(teamId, race.raceId)

                // If not in local DB, try to get from Firestore
                if (raceResult == null) {
                    Log.d(TAG, "Race result not in local DB, checking Firestore...")
                    try {
                        val firestoreResults = teamResultFirestoreService.getTeamResultsForSeason(teamId, season)
                        val firestoreResult = firestoreResults.find { it.raceId == race.raceId }
                        if (firestoreResult != null) {
                            // Save to local DB for future
                            teamRaceResultDao.insertResult(firestoreResult)
                            raceResult = firestoreResult
                            Log.d(TAG, "Found race result in Firestore")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch from Firestore: ${e.message}")
                    }
                }

                // Parse cyclist breakdown from stored JSON (for one-day races)
                val cyclistDetails = mutableListOf<CyclistRaceDetail>()
                if (!isGrandTour) {
                    raceResult?.cyclistBreakdownJson?.let { json ->
                        try {
                            val jsonArray = JSONArray(json)
                            for (i in 0 until jsonArray.length()) {
                                val item = jsonArray.getJSONObject(i)
                                cyclistDetails.add(
                                    CyclistRaceDetail(
                                        cyclistId = item.getString("cyclistId"),
                                        name = item.getString("name"),
                                        team = item.optString("teamName", ""),
                                        points = item.getInt("points"),
                                        isCaptain = item.getBoolean("isCaptain"),
                                        position = if (item.isNull("position")) null else item.getInt("position"),
                                        status = item.optString("status", "") // DNF, DNS, DSQ, DNP
                                    )
                                )
                            }
                            Log.d(TAG, "Parsed ${cyclistDetails.size} cyclists from breakdown")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing cyclist breakdown JSON: ${e.message}")
                        }
                    }
                }

                // Convert stage results to StageResultItem
                val stageItems = stageResults.map { stage ->
                    StageResultItem(
                        stageNumber = stage.stageNumber,
                        stageName = stage.raceName,
                        pointsEarned = stage.pointsEarned,
                        wasTripleCaptainActive = stage.wasTripleCaptainActive,
                        wasBenchBoostActive = stage.wasBenchBoostActive
                    )
                }.sortedBy { it.stageNumber }

                // Calculate total for Grand Tour from stages
                val totalPoints = if (isGrandTour) {
                    stageItems.sumOf { it.pointsEarned }
                } else {
                    race.pointsEarned
                }

                _selectedRaceDetail.value = RaceDetailState(
                    raceName = race.raceName,
                    raceDate = race.raceDate,
                    totalPoints = totalPoints,
                    teamName = team?.teamName ?: "",
                    captainName = raceResult?.captainName,
                    wasTripleCaptainActive = raceResult?.wasTripleCaptainActive ?: false,
                    wasBenchBoostActive = raceResult?.wasBenchBoostActive ?: false,
                    cyclistBreakdown = cyclistDetails.sortedByDescending { it.points },
                    isGrandTour = isGrandTour,
                    stageResults = stageItems
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading race details: ${e.message}")
                // Show basic info without breakdown
                _selectedRaceDetail.value = RaceDetailState(
                    raceName = race.raceName,
                    raceDate = race.raceDate,
                    totalPoints = race.pointsEarned,
                    teamName = "",
                    captainName = null,
                    wasTripleCaptainActive = false,
                    wasBenchBoostActive = false,
                    cyclistBreakdown = emptyList(),
                    isGrandTour = false,
                    stageResults = emptyList()
                )
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    fun dismissRaceDetail() {
        _selectedRaceDetail.value = null
    }
}

/**
 * State for race detail dialog
 */
data class RaceDetailState(
    val raceName: String,
    val raceDate: Long,
    val totalPoints: Int,
    val teamName: String,
    val captainName: String?,
    val wasTripleCaptainActive: Boolean,
    val wasBenchBoostActive: Boolean,
    val cyclistBreakdown: List<CyclistRaceDetail>,
    val isGrandTour: Boolean = false,
    val stageResults: List<StageResultItem> = emptyList()
)

/**
 * Stage result for Grand Tour breakdown
 */
data class StageResultItem(
    val stageNumber: Int,
    val stageName: String,
    val pointsEarned: Int,
    val wasTripleCaptainActive: Boolean = false,
    val wasBenchBoostActive: Boolean = false
)

data class CyclistRaceDetail(
    val cyclistId: String,
    val name: String,
    val team: String,
    val points: Int,
    val isCaptain: Boolean,
    val position: Int?,
    val status: String = "" // DNF, DNS, DSQ, DNP, or empty for normal finish
)

sealed class SeasonHistoryUiState {
    object Loading : SeasonHistoryUiState()
    object Success : SeasonHistoryUiState()
    data class NoTeam(val season: Int) : SeasonHistoryUiState()
    data class Error(val message: String) : SeasonHistoryUiState()
}
