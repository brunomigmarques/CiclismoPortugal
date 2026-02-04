package com.ciclismo.portugal.presentation.fantasy.hub

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.local.ai.AiContentGenerator
import com.ciclismo.portugal.data.local.premium.PremiumManager
import com.ciclismo.portugal.data.local.premium.PremiumStatus
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.LeagueMember
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceInsight
import com.ciclismo.portugal.domain.model.RaceType
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.model.TopCandidate
import com.ciclismo.portugal.domain.repository.CyclistRepository
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.repository.LeagueRepository
import com.ciclismo.portugal.domain.repository.RaceRepository
import com.ciclismo.portugal.domain.repository.StageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class FantasyHubViewModel @Inject constructor(
    private val fantasyTeamRepository: FantasyTeamRepository,
    private val leagueRepository: LeagueRepository,
    private val raceRepository: RaceRepository,
    private val stageRepository: StageRepository,
    private val cyclistRepository: CyclistRepository,
    private val aiContentGenerator: AiContentGenerator,
    private val premiumManager: PremiumManager
) : ViewModel() {

    companion object {
        private const val TAG = "FantasyHubViewModel"
    }

    private val _uiState = MutableStateFlow(FantasyHubUiState())
    val uiState: StateFlow<FantasyHubUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _upcomingRaceReminder = MutableStateFlow<RaceReminder?>(null)
    val upcomingRaceReminder: StateFlow<RaceReminder?> = _upcomingRaceReminder.asStateFlow()

    private val _activeRacesWithStages = MutableStateFlow<List<RaceWithStageInfo>>(emptyList())
    val activeRacesWithStages: StateFlow<List<RaceWithStageInfo>> = _activeRacesWithStages.asStateFlow()

    // AI Content states
    private val _topCandidates = MutableStateFlow<List<TopCandidate>>(emptyList())
    val topCandidates: StateFlow<List<TopCandidate>> = _topCandidates.asStateFlow()

    private val _raceInsight = MutableStateFlow<RaceInsight?>(null)
    val raceInsight: StateFlow<RaceInsight?> = _raceInsight.asStateFlow()

    private val _premiumStatus = MutableStateFlow(PremiumStatus(
        hasAccess = true,
        isPremium = false,
        isTrialActive = true,
        trialDaysRemaining = 7,
        message = "Experimenta gratis por 7 dias!"
    ))
    val premiumStatus: StateFlow<PremiumStatus> = _premiumStatus.asStateFlow()

    private val _isLoadingAiContent = MutableStateFlow(false)
    val isLoadingAiContent: StateFlow<Boolean> = _isLoadingAiContent.asStateFlow()

    init {
        checkUpcomingRaces()
        loadActiveRaces()
        observePremiumStatus()
    }

    /**
     * Observe premium status changes.
     */
    private fun observePremiumStatus() {
        viewModelScope.launch {
            premiumManager.observePremiumStatus().collect { status ->
                _premiumStatus.value = status
            }
        }
    }

    /**
     * Load upcoming races for AI content and their stage info for multi-stage races.
     * Uses getNextUpcomingRaceForAi() to include upcoming races, not just active ones.
     */
    private fun loadActiveRaces() {
        viewModelScope.launch {
            try {
                raceRepository.getNextUpcomingRaceForAi().collect { races ->
                    val racesWithStages = races.map { race ->
                        if (race.type == RaceType.GRAND_TOUR || race.type == RaceType.STAGE_RACE) {
                            val stages = stageRepository.getStageSchedule(race.id)
                            val today = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis

                            // Find current stage (today's stage) or next stage
                            val sortedStages = stages.sortedBy { it.stageNumber }
                            val todayStage = sortedStages.find { stage ->
                                val stageDay = Calendar.getInstance().apply {
                                    timeInMillis = stage.date
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                stageDay == today
                            }
                            val nextStage = sortedStages.find { stage ->
                                val stageDay = Calendar.getInstance().apply {
                                    timeInMillis = stage.date
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                stageDay > today
                            }

                            RaceWithStageInfo(
                                race = race,
                                totalStages = race.stages,
                                currentStage = todayStage,
                                nextStage = if (todayStage == null) nextStage else sortedStages.getOrNull(todayStage.stageNumber)
                            )
                        } else {
                            RaceWithStageInfo(
                                race = race,
                                totalStages = 1,
                                currentStage = null,
                                nextStage = null
                            )
                        }
                    }
                    _activeRacesWithStages.value = racesWithStages
                }
            } catch (e: Exception) {
                // Ignore errors - active races are not critical
            }
        }
    }

    /**
     * Check for races starting tomorrow and show a reminder.
     */
    private fun checkUpcomingRaces() {
        viewModelScope.launch {
            try {
                val racesTomorrow = raceRepository.getRacesStartingTomorrow()
                if (racesTomorrow.isNotEmpty()) {
                    val race = racesTomorrow.first()
                    _upcomingRaceReminder.value = RaceReminder(
                        raceName = race.name,
                        message = "A equipa congela hoje às 24:00!",
                        raceCount = racesTomorrow.size
                    )
                }
            } catch (e: Exception) {
                // Ignore errors - reminder is not critical
            }
        }
    }

    fun dismissRaceReminder() {
        _upcomingRaceReminder.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    /**
     * Load AI-generated content (Top 3 candidates and race insights).
     * Only loads if user has premium access (premium or trial active).
     */
    fun loadAiContent() {
        viewModelScope.launch {
            // Check premium access
            if (!premiumManager.hasAiAccess()) {
                Log.d(TAG, "AI content not loaded: no premium access")
                return@launch
            }

            _isLoadingAiContent.value = true

            try {
                // Record AI usage (starts trial if not started)
                premiumManager.recordAiUsage()

                // Get the next race for predictions
                val activeRaces = _activeRacesWithStages.value
                val nextRace = activeRaces.firstOrNull()?.race

                if (nextRace == null) {
                    Log.d(TAG, "No active races for AI content")
                    _isLoadingAiContent.value = false
                    return@launch
                }

                // Get current/next stage if multi-stage race
                val currentStage = activeRaces.firstOrNull()?.currentStage
                    ?: activeRaces.firstOrNull()?.nextStage

                // Get available cyclists for recommendations
                val cyclists = cyclistRepository.getAvailableCyclists().first()

                // Generate top candidates
                val candidatesResult = aiContentGenerator.generateTopCandidates(
                    race = nextRace,
                    stage = currentStage,
                    availableCyclists = cyclists
                )

                candidatesResult.onSuccess { candidates ->
                    _topCandidates.value = candidates
                    Log.d(TAG, "Loaded ${candidates.size} top candidates")
                }

                // Generate race insight
                val insightResult = aiContentGenerator.generateRaceInsight(
                    race = nextRace,
                    stage = currentStage
                )

                insightResult.onSuccess { insight ->
                    _raceInsight.value = insight
                    Log.d(TAG, "Loaded race insight")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading AI content: ${e.message}", e)
            } finally {
                _isLoadingAiContent.value = false
            }
        }
    }

    /**
     * Refresh AI content manually.
     */
    fun refreshAiContent() {
        _topCandidates.value = emptyList()
        _raceInsight.value = null
        loadAiContent()
    }

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // First, sync team from Firestore if needed (for reinstall scenarios)
                fantasyTeamRepository.syncTeamFromFirestore(userId)

                // Load user's team
                fantasyTeamRepository.getTeamByUserId(userId).collect { team ->
                    if (team != null) {
                        // Get global league rank
                        val globalLeague = leagueRepository.ensureGlobalLeagueExists()
                        val members = leagueRepository.getLeagueMembers(globalLeague.id).first()
                        val userMember = members.find { it.userId == userId }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasTeam = true,
                            team = team,
                            globalRank = userMember?.rank,
                            totalMembers = members.size
                        )

                        // Load AI content after user data is loaded
                        loadAiContent()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasTeam = false,
                            team = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Check if user has AI access (premium or trial).
     */
    suspend fun hasAiAccess(): Boolean = premiumManager.hasAiAccess()
}

data class FantasyHubUiState(
    val isLoading: Boolean = false,
    val hasTeam: Boolean = false,
    val team: FantasyTeam? = null,
    val globalRank: Int? = null,
    val totalMembers: Int = 0,
    val error: String? = null
) {
    val displayPoints: String
        get() = team?.totalPoints?.toString() ?: "0"

    val displayRank: String
        get() = globalRank?.toString() ?: "-"

    val displayBudget: String
        get() = team?.displayBudget ?: "100M"

    val displayRankWithTotal: String
        get() = if (globalRank != null && totalMembers > 0) {
            "$globalRank / $totalMembers"
        } else {
            "-"
        }
}

data class RaceReminder(
    val raceName: String,
    val message: String,
    val raceCount: Int = 1
)

data class RaceWithStageInfo(
    val race: Race,
    val totalStages: Int,
    val currentStage: Stage?,
    val nextStage: Stage?
) {
    val isMultiStage: Boolean
        get() = race.type == RaceType.GRAND_TOUR || race.type == RaceType.STAGE_RACE

    val stageProgressText: String
        get() = if (isMultiStage && currentStage != null) {
            "Etapa ${currentStage.stageNumber} de $totalStages"
        } else if (isMultiStage && nextStage != null) {
            "Proxima: Etapa ${nextStage.stageNumber}"
        } else {
            ""
        }
}
