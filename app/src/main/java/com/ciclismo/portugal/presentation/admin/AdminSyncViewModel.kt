package com.ciclismo.portugal.presentation.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.remote.cycling.CyclingDataSource
import com.ciclismo.portugal.data.remote.firebase.CyclistFirestoreService
import com.ciclismo.portugal.data.remote.firebase.CyclistPhotoStorageService
import com.ciclismo.portugal.data.remote.firebase.RaceFirestoreService
import com.ciclismo.portugal.data.remote.firebase.StageFirestoreService
import com.ciclismo.portugal.data.remote.firebase.SyncStatus
import com.ciclismo.portugal.data.remote.firebase.FantasyTeamFirestoreService
import com.ciclismo.portugal.data.remote.firebase.LeagueFirestoreService
import com.ciclismo.portugal.data.remote.video.CyclingVideosRepository
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.model.RaceResult
import com.ciclismo.portugal.domain.model.RaceType
import com.ciclismo.portugal.domain.model.SeasonConfig
import com.ciclismo.portugal.domain.model.Stage
import com.ciclismo.portugal.domain.model.StageResult
import com.ciclismo.portugal.domain.model.StageType
import com.ciclismo.portugal.domain.model.GcStanding
import com.ciclismo.portugal.domain.repository.ProvaRepository
import com.ciclismo.portugal.domain.repository.RaceRepository
import com.ciclismo.portugal.domain.repository.StageRepository
import com.ciclismo.portugal.data.remote.firebase.FirebaseAuthService
import com.ciclismo.portugal.data.remote.scraper.GenericRaceScraper
import com.ciclismo.portugal.data.remote.scraper.ScrapedRaceData
import com.ciclismo.portugal.domain.usecase.CalculateFantasyPointsUseCase
import com.ciclismo.portugal.domain.usecase.CalculateStageFantasyPointsUseCase
import com.ciclismo.portugal.domain.usecase.SeasonManager
import com.ciclismo.portugal.domain.usecase.SeasonStats
import com.ciclismo.portugal.domain.bots.BotTeamService
import com.ciclismo.portugal.data.local.ai.AiService
import com.ciclismo.portugal.data.local.ai.AiUsageStats
import java.util.UUID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing a team to scrape
 * @param url Full URL of the team page (e.g., "https://www.procyclingstats.com/team/alpecin-premier-tech-2026")
 * @param name Display name of the team
 */
data class TeamConfig(
    val url: String,
    val name: String
)

@HiltViewModel
class AdminSyncViewModel @Inject constructor(
    private val cyclingDataSource: CyclingDataSource,
    private val cyclistFirestoreService: CyclistFirestoreService,
    private val photoStorageService: CyclistPhotoStorageService,
    private val raceRepository: RaceRepository,
    private val raceFirestoreService: RaceFirestoreService,
    private val calculateFantasyPointsUseCase: CalculateFantasyPointsUseCase,
    private val calculateStageFantasyPointsUseCase: CalculateStageFantasyPointsUseCase,
    private val cyclingVideosRepository: CyclingVideosRepository,
    private val seasonManager: SeasonManager,
    private val stageRepository: StageRepository,
    private val stageFirestoreService: StageFirestoreService,
    private val genericRaceScraper: GenericRaceScraper,
    private val provaRepository: ProvaRepository,
    private val firebaseAuthService: FirebaseAuthService,
    private val fantasyTeamFirestoreService: FantasyTeamFirestoreService,
    private val leagueFirestoreService: LeagueFirestoreService,
    private val botTeamService: BotTeamService,
    private val aiService: AiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminSyncUiState>(AdminSyncUiState.Initial)
    val uiState: StateFlow<AdminSyncUiState> = _uiState.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus?>(null)
    val syncStatus: StateFlow<SyncStatus?> = _syncStatus.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _validatedCount = MutableStateFlow(0)
    val validatedCount: StateFlow<Int> = _validatedCount.asStateFlow()

    private val _scrapedCyclists = MutableStateFlow<List<Cyclist>>(emptyList())
    val scrapedCyclists: StateFlow<List<Cyclist>> = _scrapedCyclists.asStateFlow()

    // Teams to scrape from (URL, Name)
    private val _teams = MutableStateFlow<List<TeamConfig>>(emptyList())
    val teams: StateFlow<List<TeamConfig>> = _teams.asStateFlow()

    // Current scraping progress
    private val _scrapingProgress = MutableStateFlow<ScrapingProgress?>(null)
    val scrapingProgress: StateFlow<ScrapingProgress?> = _scrapingProgress.asStateFlow()

    // Photo upload progress
    private val _photoUploadProgress = MutableStateFlow<PhotoUploadProgress?>(null)
    val photoUploadProgress: StateFlow<PhotoUploadProgress?> = _photoUploadProgress.asStateFlow()

    // Race results processing
    private val _processedRace = MutableStateFlow<Race?>(null)
    val processedRace: StateFlow<Race?> = _processedRace.asStateFlow()

    private val _nextRace = MutableStateFlow<Race?>(null)
    val nextRace: StateFlow<Race?> = _nextRace.asStateFlow()

    private val _fantasyPointsResult = MutableStateFlow<FantasyPointsResult?>(null)
    val fantasyPointsResult: StateFlow<FantasyPointsResult?> = _fantasyPointsResult.asStateFlow()

    // Current race to process (auto-detected from calendar or manually selected)
    private val _currentRaceToProcess = MutableStateFlow<Race?>(null)
    val currentRaceToProcess: StateFlow<Race?> = _currentRaceToProcess.asStateFlow()

    // All races for the current season (for admin dropdown selector)
    private val _allSeasonRaces = MutableStateFlow<List<Race>>(emptyList())
    val allSeasonRaces: StateFlow<List<Race>> = _allSeasonRaces.asStateFlow()

    // Season management
    private val _currentSeason = MutableStateFlow(SeasonConfig.CURRENT_SEASON)
    val currentSeason: StateFlow<Int> = _currentSeason.asStateFlow()

    private val _availableSeasons = MutableStateFlow<List<Int>>(listOf(SeasonConfig.CURRENT_SEASON))
    val availableSeasons: StateFlow<List<Int>> = _availableSeasons.asStateFlow()

    private val _seasonStats = MutableStateFlow<SeasonStats?>(null)
    val seasonStats: StateFlow<SeasonStats?> = _seasonStats.asStateFlow()

    // Admin management
    private val _adminsList = MutableStateFlow<List<AdminInfo>>(emptyList())
    val adminsList: StateFlow<List<AdminInfo>> = _adminsList.asStateFlow()

    private val _isLoadingAdmins = MutableStateFlow(false)
    val isLoadingAdmins: StateFlow<Boolean> = _isLoadingAdmins.asStateFlow()

    // Stage processing for Grand Tours
    private val _currentStageNumber = MutableStateFlow(1)
    val currentStageNumber: StateFlow<Int> = _currentStageNumber.asStateFlow()

    private val _currentStageType = MutableStateFlow(StageType.FLAT)
    val currentStageType: StateFlow<StageType> = _currentStageType.asStateFlow()

    private val _stageProcessingResult = MutableStateFlow<StageProcessingResult?>(null)
    val stageProcessingResult: StateFlow<StageProcessingResult?> = _stageProcessingResult.asStateFlow()

    private val _processedStages = MutableStateFlow<List<Int>>(emptyList())
    val processedStages: StateFlow<List<Int>> = _processedStages.asStateFlow()

    private val _currentJerseyHolders = MutableStateFlow<JerseyHoldersInfo?>(null)
    val currentJerseyHolders: StateFlow<JerseyHoldersInfo?> = _currentJerseyHolders.asStateFlow()

    init {
        loadSyncStatus()
        loadNextRaceToProcess()
        loadSeasonData()
        loadAllRacesForSeason()
        loadAdmins()
    }

    /**
     * Load season-related data from SeasonManager
     */
    private fun loadSeasonData() {
        viewModelScope.launch {
            _currentSeason.value = seasonManager.getCurrentSeason()
            _availableSeasons.value = seasonManager.getAvailableSeasons()
            loadSeasonStats()
        }
    }

    /**
     * Load statistics for the current season
     */
    fun loadSeasonStats() {
        viewModelScope.launch {
            val stats = seasonManager.getSeasonStats(_currentSeason.value)
            _seasonStats.value = stats
            android.util.Log.d("AdminSync", "Season ${stats.season} stats: ${stats.cyclistsCount} cyclists, ${stats.racesCount} races, ${stats.teamsCount} teams")
        }
    }

    // ==================== ADMIN MANAGEMENT ====================

    /**
     * Load list of all admins from Firestore
     */
    fun loadAdmins() {
        viewModelScope.launch {
            _isLoadingAdmins.value = true
            try {
                firebaseAuthService.refreshAdminCache()
                val admins = firebaseAuthService.getAllAdmins()
                _adminsList.value = admins.map { data ->
                    AdminInfo(
                        uid = data["uid"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        addedBy = data["addedBy"] as? String ?: "",
                        addedAt = data["addedAt"] as? Long ?: 0L
                    )
                }
                android.util.Log.d("AdminSync", "Loaded ${_adminsList.value.size} admins")
            } catch (e: Exception) {
                android.util.Log.e("AdminSync", "Error loading admins: ${e.message}")
            } finally {
                _isLoadingAdmins.value = false
            }
        }
    }

    /**
     * Add a new admin by email.
     * The user must exist in Firebase Auth.
     */
    fun addAdminByEmail(email: String) {
        viewModelScope.launch {
            try {
                // For now, we'll need the UID. In a real scenario, you'd look up the user by email
                // Since we don't have a direct way to get UID from email without admin SDK,
                // we'll use a simplified approach where admin enters the UID directly
                _uiState.value = AdminSyncUiState.Error("Para adicionar admin, usa o UID do utilizador no Firebase Console")
            } catch (e: Exception) {
                _uiState.value = AdminSyncUiState.Error("Erro: ${e.message}")
            }
        }
    }

    /**
     * Add a new admin by UID and email
     */
    fun addAdmin(uid: String, email: String) {
        viewModelScope.launch {
            try {
                if (uid.isBlank() || email.isBlank()) {
                    _uiState.value = AdminSyncUiState.Error("UID e email sao obrigatorios")
                    return@launch
                }

                firebaseAuthService.addAdmin(uid, email).fold(
                    onSuccess = {
                        loadAdmins() // Refresh list
                        _uiState.value = AdminSyncUiState.UploadComplete(1)
                        android.util.Log.d("AdminSync", "Added admin: $email")
                    },
                    onFailure = { error ->
                        _uiState.value = AdminSyncUiState.Error("Erro ao adicionar admin: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = AdminSyncUiState.Error("Erro: ${e.message}")
            }
        }
    }

    /**
     * Remove an admin
     */
    fun removeAdmin(uid: String) {
        viewModelScope.launch {
            try {
                firebaseAuthService.removeAdmin(uid).fold(
                    onSuccess = {
                        loadAdmins() // Refresh list
                        android.util.Log.d("AdminSync", "Removed admin: $uid")
                    },
                    onFailure = { error ->
                        _uiState.value = AdminSyncUiState.Error("Erro ao remover admin: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = AdminSyncUiState.Error("Erro: ${e.message}")
            }
        }
    }

    /**
     * Load all races for the current season (for admin dropdown selector)
     * Includes both finished and upcoming races, sorted by date
     */
    fun loadAllRacesForSeason() {
        viewModelScope.launch {
            try {
                val result = raceRepository.getRacesForYear(_currentSeason.value)
                result.fold(
                    onSuccess = { races ->
                        // Sort by date descending so most recent races appear first
                        _allSeasonRaces.value = races.sortedByDescending { it.startDate }
                        android.util.Log.d("AdminSync", "Loaded ${races.size} races for season ${_currentSeason.value}")
                    },
                    onFailure = { error ->
                        android.util.Log.e("AdminSync", "Error loading races: ${error.message}")
                        _allSeasonRaces.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("AdminSync", "Exception loading races: ${e.message}")
                _allSeasonRaces.value = emptyList()
            }
        }
    }

    /**
     * Manually select a race to process results for.
     * This allows admin to select any race (including finished ones for reprocessing).
     */
    fun selectRaceToProcess(race: Race) {
        _currentRaceToProcess.value = race
        android.util.Log.d("AdminSync", "Admin selected race: ${race.name} (ID: ${race.id}, finished: ${race.isFinished})")

        // Load stage schedule and processed stages for this race
        loadStageSchedule(race.id)
        loadProcessedStages(race.id)
    }

    /**
     * Switch to a different season
     */
    fun switchSeason(season: Int) {
        viewModelScope.launch {
            seasonManager.setCurrentSeason(season)
            _currentSeason.value = season
            loadSeasonStats()
            loadSyncStatus()
            loadAllRacesForSeason()
            loadNextRaceToProcess()
            android.util.Log.d("AdminSync", "Switched to season $season")
        }
    }

    /**
     * Start a new season (admin action)
     * Creates the season in Firestore and sets it as current
     */
    fun startNewSeason(newSeason: Int) {
        viewModelScope.launch {
            _uiState.value = AdminSyncUiState.Initial
            android.util.Log.d("AdminSync", "Starting new season: $newSeason")

            val result = seasonManager.startNewSeason(newSeason)
            if (result.isSuccess) {
                _currentSeason.value = newSeason
                _availableSeasons.value = seasonManager.getAvailableSeasons()
                loadSeasonStats()
                loadSyncStatus()
                android.util.Log.d("AdminSync", "New season $newSeason started successfully")
            } else {
                _uiState.value = AdminSyncUiState.Error("Erro ao iniciar temporada: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Migrate data from old flat Firestore structure to season-based structure.
     * This copies data from collections like 'cyclists/' to 'seasons/2026/cyclists/'
     */
    fun migrateDataToCurrentSeason() {
        viewModelScope.launch {
            _uiState.value = AdminSyncUiState.Uploading
            android.util.Log.d("AdminSync", "Starting migration to season ${_currentSeason.value}...")

            val result = seasonManager.migrateFromFlatStructure(_currentSeason.value)
            result.fold(
                onSuccess = { stats ->
                    android.util.Log.d("AdminSync", "Migration complete: $stats")
                    _uiState.value = AdminSyncUiState.UploadComplete(stats.totalMigrated)
                    loadSeasonStats()
                    loadSyncStatus()
                },
                onFailure = { error ->
                    android.util.Log.e("AdminSync", "Migration failed: ${error.message}")
                    _uiState.value = AdminSyncUiState.Error("Erro na migracao: ${error.message}")
                }
            )
        }
    }

    /**
     * Load the next unfinished race from the calendar.
     * This race will be used when applying race results.
     */
    private fun loadNextRaceToProcess() {
        viewModelScope.launch {
            try {
                val upcomingRaces = raceRepository.getUpcomingRaces().first()
                val activeRaces = raceRepository.getActiveRaces().first()
                val allRaces = (activeRaces + upcomingRaces).distinctBy { it.id }

                // Find the next race that is not finished, sorted by start date
                val nextRace = allRaces
                    .filter { !it.isFinished }
                    .minByOrNull { it.startDate }

                _currentRaceToProcess.value = nextRace

                if (nextRace != null) {
                    android.util.Log.d("AdminSync", "Next race to process: ${nextRace.name} (ID: ${nextRace.id})")
                } else {
                    android.util.Log.w("AdminSync", "No upcoming races found in calendar")
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminSync", "Error loading next race: ${e.message}")
            }
        }
    }

    /**
     * Refresh the next race to process after applying results
     */
    fun refreshNextRace() {
        loadNextRaceToProcess()
    }

    // ==================== STAGE PROCESSING (GRAND TOURS) ====================

    /**
     * Set the current stage number for processing
     */
    fun setStageNumber(stageNumber: Int) {
        _currentStageNumber.value = stageNumber
    }

    /**
     * Set the stage type for processing
     */
    fun setStageType(stageType: StageType) {
        _currentStageType.value = stageType
    }

    /**
     * Load processed stages for the current race
     */
    fun loadProcessedStages(raceId: String) {
        viewModelScope.launch {
            try {
                val stages = stageRepository.getProcessedStages(raceId)
                _processedStages.value = stages
                android.util.Log.d("StageAdmin", "Loaded ${stages.size} processed stages for race $raceId")

                // Auto-set next stage number
                val nextStage = (stages.maxOrNull() ?: 0) + 1
                _currentStageNumber.value = nextStage

                // Load current jersey holders
                loadJerseyHolders(raceId)
            } catch (e: Exception) {
                android.util.Log.e("StageAdmin", "Error loading processed stages: ${e.message}")
            }
        }
    }

    /**
     * Load current jersey holders for a race
     */
    private fun loadJerseyHolders(raceId: String) {
        viewModelScope.launch {
            try {
                val jerseys = stageRepository.getJerseyHolders(raceId)
                val cyclistsResult = cyclistFirestoreService.getValidatedCyclistsOnce()
                val cyclists = cyclistsResult.getOrNull() ?: emptyList()
                val cyclistMap = cyclists.associateBy { it.id }

                _currentJerseyHolders.value = JerseyHoldersInfo(
                    gcLeaderName = jerseys.gcLeaderId?.let { cyclistMap[it]?.fullName },
                    gcLeaderId = jerseys.gcLeaderId,
                    pointsLeaderName = jerseys.pointsLeaderId?.let { cyclistMap[it]?.fullName },
                    pointsLeaderId = jerseys.pointsLeaderId,
                    mountainsLeaderName = jerseys.mountainsLeaderId?.let { cyclistMap[it]?.fullName },
                    mountainsLeaderId = jerseys.mountainsLeaderId,
                    youngLeaderName = jerseys.youngLeaderId?.let { cyclistMap[it]?.fullName },
                    youngLeaderId = jerseys.youngLeaderId
                )
            } catch (e: Exception) {
                android.util.Log.e("StageAdmin", "Error loading jersey holders: ${e.message}")
            }
        }
    }

    /**
     * Process stage results for a Grand Tour.
     * Creates StageResult entries in Firebase and updates GC standings.
     */
    fun applyStageResults(results: List<ParsedStageResult>) {
        viewModelScope.launch {
            val race = _currentRaceToProcess.value
            if (race == null) {
                _uiState.value = AdminSyncUiState.Error("Nenhuma corrida selecionada")
                return@launch
            }

            if (results.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error("Nenhum resultado para aplicar")
                return@launch
            }

            _uiState.value = AdminSyncUiState.Uploading
            _stageProcessingResult.value = null

            val stageNumber = _currentStageNumber.value
            val stageType = _currentStageType.value
            val season = _currentSeason.value
            val timestamp = System.currentTimeMillis()

            android.util.Log.d("StageAdmin", "Processing stage $stageNumber (${stageType.displayNamePt}) for ${race.name}")

            // Get all cyclists from Firestore for matching
            val cyclistsResult = cyclistFirestoreService.getValidatedCyclistsOnce()
            if (cyclistsResult.isFailure) {
                _uiState.value = AdminSyncUiState.Error("Erro ao buscar ciclistas: ${cyclistsResult.exceptionOrNull()?.message}")
                return@launch
            }

            val cyclists = cyclistsResult.getOrNull() ?: emptyList()
            android.util.Log.d("StageAdmin", "Loaded ${cyclists.size} cyclists from Firestore")

            val stageResults = mutableListOf<StageResult>()
            var matchedCount = 0
            var totalPoints = 0

            // Jersey holders from this stage
            var gcLeaderName: String? = null
            var pointsLeaderName: String? = null
            var mountainsLeaderName: String? = null
            var youngLeaderName: String? = null

            // Match cyclists and create StageResult objects
            for (result in results) {
                val matchedCyclist = findCyclistByName(result.riderName, cyclists)

                if (matchedCyclist != null) {
                    matchedCount++

                    // Calculate points based on position and stage type
                    val positionPoints = StageResult.calculatePoints(result.position, stageType)
                    val jerseyBonus = StageResult.calculateJerseyBonus(
                        result.isGcLeader, result.isPointsLeader,
                        result.isMountainsLeader, result.isYoungLeader
                    )

                    val stageResult = StageResult(
                        id = "${race.id}_stage${stageNumber}_${matchedCyclist.id}",
                        raceId = race.id,
                        stageNumber = stageNumber,
                        stageType = stageType,
                        cyclistId = matchedCyclist.id,
                        position = result.position,
                        points = positionPoints,
                        jerseyBonus = jerseyBonus,
                        isGcLeader = result.isGcLeader,
                        isMountainsLeader = result.isMountainsLeader,
                        isPointsLeader = result.isPointsLeader,
                        isYoungLeader = result.isYoungLeader,
                        time = result.time,
                        status = result.status,
                        timestamp = timestamp,
                        season = season
                    )

                    stageResults.add(stageResult)
                    totalPoints += stageResult.totalPoints

                    // Track jersey holders
                    if (result.isGcLeader) gcLeaderName = matchedCyclist.fullName
                    if (result.isPointsLeader) pointsLeaderName = matchedCyclist.fullName
                    if (result.isMountainsLeader) mountainsLeaderName = matchedCyclist.fullName
                    if (result.isYoungLeader) youngLeaderName = matchedCyclist.fullName

                    android.util.Log.d("StageAdmin", "Matched ${matchedCyclist.fullName}: pos=${result.position}, pts=$positionPoints, jersey=$jerseyBonus")
                } else {
                    android.util.Log.w("StageAdmin", "No match for rider: ${result.riderName}")
                }
            }

            // Save stage results to Firebase and local DB
            if (stageResults.isNotEmpty()) {
                val saveResult = stageRepository.saveStageResults(stageResults)
                saveResult.fold(
                    onSuccess = { count ->
                        android.util.Log.d("StageAdmin", "Saved $count stage results")
                    },
                    onFailure = { error ->
                        android.util.Log.e("StageAdmin", "Error saving stage results: ${error.message}")
                    }
                )

                // Calculate and apply fantasy points for all teams
                val fantasyResult = calculateStageFantasyPointsUseCase.processStage(
                    raceId = race.id,
                    stageNumber = stageNumber,
                    stageResults = stageResults
                )
                fantasyResult.fold(
                    onSuccess = { pointsResult ->
                        android.util.Log.d("StageAdmin", "Fantasy points calculated: ${pointsResult.teamsProcessed} teams, ${pointsResult.totalPointsAwarded} total pts")
                    },
                    onFailure = { error ->
                        android.util.Log.e("StageAdmin", "Error calculating fantasy points: ${error.message}")
                    }
                )

                // Update race current stage in Firebase
                stageRepository.updateRaceCurrentStage(race.id, stageNumber, season)

                // Update processed stages list
                val newProcessedStages = (_processedStages.value + stageNumber).distinct().sorted()
                _processedStages.value = newProcessedStages

                // Auto-increment to next stage
                _currentStageNumber.value = stageNumber + 1

                // Reload jersey holders
                loadJerseyHolders(race.id)

                _stageProcessingResult.value = StageProcessingResult(
                    stageNumber = stageNumber,
                    stageType = stageType,
                    resultsCount = stageResults.size,
                    pointsAwarded = totalPoints,
                    gcLeader = gcLeaderName,
                    pointsLeader = pointsLeaderName,
                    mountainsLeader = mountainsLeaderName,
                    youngLeader = youngLeaderName
                )

                _uiState.value = AdminSyncUiState.UploadComplete(stageResults.size)
                android.util.Log.d("StageAdmin", "Stage $stageNumber processed: ${stageResults.size} results, $totalPoints points")
            } else {
                _uiState.value = AdminSyncUiState.Error(
                    "Nenhum ciclista correspondido. Matched: $matchedCount/${results.size}"
                )
            }
        }
    }

    /**
     * Mark the race as completed after processing the final stage.
     * Applies final GC bonus points to fantasy teams.
     */
    fun finalizeGrandTour() {
        viewModelScope.launch {
            val race = _currentRaceToProcess.value
            if (race == null) {
                _uiState.value = AdminSyncUiState.Error("Nenhuma corrida selecionada")
                return@launch
            }

            _uiState.value = AdminSyncUiState.Uploading
            val season = _currentSeason.value

            android.util.Log.d("StageAdmin", "Finalizing Grand Tour: ${race.name}")

            // Apply final GC bonus points to fantasy teams
            val gcBonusResult = calculateStageFantasyPointsUseCase.applyFinalGcBonus(race.id)
            gcBonusResult.fold(
                onSuccess = { result ->
                    android.util.Log.d("StageAdmin", "Final GC bonus applied: ${result.teamsProcessed} teams, ${result.totalBonusAwarded} pts")

                    _uiState.value = AdminSyncUiState.UploadComplete(result.teamsProcessed)

                    // Clear current race and load next
                    _currentRaceToProcess.value = null
                    _processedStages.value = emptyList()
                    _currentJerseyHolders.value = null
                    _stageProcessingResult.value = null
                    _stageSchedule.value = emptyList()
                    loadNextRaceToProcess()
                },
                onFailure = { error ->
                    _uiState.value = AdminSyncUiState.Error("Erro ao finalizar: ${error.message}")
                }
            )
        }
    }

    /**
     * Parse stage results from text input.
     * Expected format per line: "position, name, team, time, [jerseys]"
     * Jerseys: GC, KOM/MTN, PTS, YNG
     */
    fun parseStageResultsText(text: String): List<ParsedStageResult> {
        val results = mutableListOf<ParsedStageResult>()
        val lines = text.lines().filter { it.isNotBlank() }

        for (line in lines) {
            try {
                val parts = line.split(",", "\t").map { it.trim() }
                if (parts.size < 3) continue

                val positionStr = parts[0]
                val position = positionStr.filter { it.isDigit() }.toIntOrNull()
                val status = if (position == null) positionStr.uppercase() else ""

                val name = parts[1]
                val team = parts.getOrElse(2) { "" }
                val time = parts.getOrElse(3) { "" }

                // Check for jersey indicators in remaining parts
                val extras = parts.drop(4).joinToString(" ").uppercase()
                val isGcLeader = extras.contains("GC") || extras.contains("YELLOW") || extras.contains("AMARELA")
                val isMountainsLeader = extras.contains("KOM") || extras.contains("MTN") || extras.contains("MONTANHA")
                val isPointsLeader = extras.contains("PTS") || extras.contains("POINTS") || extras.contains("VERDE")
                val isYoungLeader = extras.contains("YNG") || extras.contains("YOUNG") || extras.contains("BRANCA")

                results.add(
                    ParsedStageResult(
                        position = position,
                        riderName = name,
                        teamName = team,
                        time = time,
                        points = 0, // Will be calculated based on position
                        status = status,
                        isGcLeader = isGcLeader,
                        isMountainsLeader = isMountainsLeader,
                        isPointsLeader = isPointsLeader,
                        isYoungLeader = isYoungLeader
                    )
                )
            } catch (e: Exception) {
                android.util.Log.w("StageAdmin", "Failed to parse line: $line")
            }
        }

        android.util.Log.d("StageAdmin", "Parsed ${results.size} stage results")
        return results
    }

    // ==================== STAGE SCHEDULE IMPORT ====================

    // Stage schedule state
    private val _stageSchedule = MutableStateFlow<List<Stage>>(emptyList())
    val stageSchedule: StateFlow<List<Stage>> = _stageSchedule.asStateFlow()

    private val _stageScheduleImportResult = MutableStateFlow<StageScheduleImportResult?>(null)
    val stageScheduleImportResult: StateFlow<StageScheduleImportResult?> = _stageScheduleImportResult.asStateFlow()

    /**
     * Parse stage schedule from pasted text.
     *
     * Expected format (from PCS or similar):
     * Date    Day        Stage                                          KM
     * 16/02   Monday     Stage 1 | Madinat Zayed - Liwa Palace          144
     * 17/02   Tuesday    Stage 2 (ITT) | Al Hudayriyat - Al Hudayriyat  12.2
     * ...
     * 23/02   Monday     Restday
     * 24/02   Tuesday    Stage 6 | Al-Ain - Jebel Hafeet               130
     *
     * Notes:
     * - "Restday" indicates a rest day (no stage)
     * - Stage type is detected from parentheses: (ITT), (TTT)
     * - Locations are extracted from "Start - Finish" format
     */
    fun parseStageScheduleText(text: String, raceId: String): List<Stage> {
        val stages = mutableListOf<Stage>()
        val lines = text.lines().filter { it.isNotBlank() }

        // Skip header line if present (check for actual header patterns, not day names like Monday)
        val firstLine = lines.firstOrNull() ?: ""
        val isHeaderLine = (firstLine.contains("Date", ignoreCase = true) ||
            firstLine.contains(Regex("\\bDay\\b", RegexOption.IGNORE_CASE))) &&
            !firstLine.contains("Stage", ignoreCase = true) &&
            !firstLine.contains("Etapa", ignoreCase = true)
        val dataLines = if (isHeaderLine) lines.drop(1) else lines

        var stageNumber = 0
        var lastStageIndex = -1

        for (line in dataLines) {
            try {
                // Split by tabs or multiple spaces
                val parts = line.split(Regex("\\t+|\\s{2,}")).map { it.trim() }.filter { it.isNotEmpty() }

                if (parts.size < 2) continue

                // Check for rest day
                val isRestDay = line.contains("Restday", ignoreCase = true) ||
                                line.contains("Rest day", ignoreCase = true) ||
                                line.contains("Dia de Descanso", ignoreCase = true)

                if (isRestDay) {
                    // Mark previous stage as having rest day after
                    if (lastStageIndex >= 0 && lastStageIndex < stages.size) {
                        stages[lastStageIndex] = stages[lastStageIndex].copy(isRestDayAfter = true)
                    }
                    continue
                }

                // Extract date (first column, format: DD/MM)
                val dateString = parts.getOrNull(0) ?: ""

                // Extract day of week (second column)
                val dayOfWeek = parts.getOrNull(1) ?: ""

                // Find the stage info (contains "Stage" or "Etapa")
                val stageInfo = parts.drop(2).joinToString(" ")

                // Skip if no stage info
                if (stageInfo.isBlank() || (!stageInfo.contains("Stage", ignoreCase = true) &&
                    !stageInfo.contains("Etapa", ignoreCase = true) &&
                    !stageInfo.contains("Prologue", ignoreCase = true) &&
                    !stageInfo.contains("Prologo", ignoreCase = true))) {
                    continue
                }

                stageNumber++

                // Extract stage type from parentheses: (ITT), (TTT)
                val stageType = when {
                    stageInfo.contains("(ITT)", ignoreCase = true) -> StageType.ITT
                    stageInfo.contains("(TTT)", ignoreCase = true) -> StageType.TTT
                    stageInfo.contains("Prologue", ignoreCase = true) ||
                        stageInfo.contains("Prologo", ignoreCase = true) -> StageType.PROLOGUE
                    else -> StageType.FLAT // Default, admin can change later
                }

                // Extract distance from the last part if it's a number
                val distanceStr = parts.lastOrNull()?.replace(",", ".") ?: ""
                val distance = distanceStr.toDoubleOrNull()

                // Extract stage name and locations
                // Format: "Stage 1 | Start - Finish" or "Stage 1: Start - Finish"
                val nameParts = stageInfo.split("|", ":")
                val stageName = nameParts.firstOrNull()?.trim()
                    ?.replace(Regex("\\(ITT\\)|\\(TTT\\)", RegexOption.IGNORE_CASE), "")
                    ?.trim() ?: "Etapa $stageNumber"

                // Try to extract start and finish locations
                var startLocation = ""
                var finishLocation = ""
                if (nameParts.size > 1) {
                    val locationPart = nameParts.drop(1).joinToString(" ").trim()
                    val locations = locationPart.split(" - ", " – ", " — ")
                    startLocation = locations.getOrNull(0)?.trim() ?: ""
                    finishLocation = locations.getOrNull(1)?.trim()
                        ?.replace(Regex("\\d+\\.?\\d*$"), "")?.trim() ?: "" // Remove trailing distance
                }

                val stage = Stage(
                    id = "${raceId}_stage_$stageNumber",
                    raceId = raceId,
                    stageNumber = stageNumber,
                    stageType = stageType,
                    name = stageName,
                    distance = distance,
                    startLocation = startLocation,
                    finishLocation = finishLocation,
                    dateString = dateString,
                    dayOfWeek = dayOfWeek,
                    season = _currentSeason.value
                )

                stages.add(stage)
                lastStageIndex = stages.size - 1

                android.util.Log.d("StageAdmin", "Parsed stage: $stageName (${stageType.name}), ${distance}km")

            } catch (e: Exception) {
                android.util.Log.w("StageAdmin", "Failed to parse stage line: $line - ${e.message}")
            }
        }

        android.util.Log.d("StageAdmin", "Parsed ${stages.size} stages from text")
        _stageSchedule.value = stages
        return stages
    }

    /**
     * Import parsed stages to Firebase.
     */
    fun importStageSchedule(stages: List<Stage>) {
        viewModelScope.launch {
            if (stages.isEmpty()) {
                _stageScheduleImportResult.value = StageScheduleImportResult(
                    success = false,
                    message = "Nenhuma etapa para importar"
                )
                return@launch
            }

            val result = stageFirestoreService.saveStageSchedule(stages)

            result.fold(
                onSuccess = { count ->
                    _stageScheduleImportResult.value = StageScheduleImportResult(
                        success = true,
                        message = "Importadas $count etapas com sucesso",
                        stageCount = count
                    )
                    android.util.Log.d("StageAdmin", "Successfully imported $count stages")
                },
                onFailure = { error ->
                    _stageScheduleImportResult.value = StageScheduleImportResult(
                        success = false,
                        message = "Erro ao importar: ${error.message}"
                    )
                    android.util.Log.e("StageAdmin", "Failed to import stages: ${error.message}")
                }
            )
        }
    }

    /**
     * Load stage schedule for a race from Firebase.
     */
    fun loadStageSchedule(raceId: String) {
        viewModelScope.launch {
            val stages = stageFirestoreService.getStageSchedule(raceId, _currentSeason.value)
            _stageSchedule.value = stages
            android.util.Log.d("StageAdmin", "Loaded ${stages.size} stages for race $raceId")
        }
    }

    /**
     * Clear stage schedule import result.
     */
    fun clearStageScheduleImportResult() {
        _stageScheduleImportResult.value = null
    }

    // ==================== END STAGE PROCESSING ====================

    fun loadSyncStatus() {
        val season = _currentSeason.value
        // Use callbacks to avoid blocking
        cyclistFirestoreService.getSyncStatus(season) { status ->
            _syncStatus.value = status
        }
        cyclistFirestoreService.getPendingCyclistsCount(season) { count ->
            _pendingCount.value = count
        }
        cyclistFirestoreService.getValidatedCyclistsCount(season) { count ->
            _validatedCount.value = count
        }
    }

    /**
     * Upload photos for VALIDATED cyclists in Firebase
     * Fetches cyclists from Firestore, matches photos by filename,
     * uploads to Storage and updates Firestore with photo URLs
     *
     * @param photoUris List of photo URIs selected by user
     * @param context Android context for file operations
     */
    fun uploadPhotosForFirebaseCyclists(
        photoUris: List<Uri>,
        context: Context
    ) {
        viewModelScope.launch {
            if (photoUris.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error("Nenhuma foto selecionada")
                return@launch
            }

            _uiState.value = AdminSyncUiState.UploadingPhotos
            _photoUploadProgress.value = PhotoUploadProgress(0, photoUris.size, "A buscar ciclistas do Firebase...")

            // Fetch validated cyclists from Firebase
            val cyclistsResult = cyclistFirestoreService.getValidatedCyclistsOnce()
            if (cyclistsResult.isFailure) {
                _uiState.value = AdminSyncUiState.Error("Erro ao buscar ciclistas: ${cyclistsResult.exceptionOrNull()?.message}")
                return@launch
            }

            val firebaseCyclists = cyclistsResult.getOrNull() ?: emptyList()
            if (firebaseCyclists.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error("Nenhum ciclista validado no Firebase. Valida os ciclistas primeiro.")
                return@launch
            }

            android.util.Log.d("PhotoUpload", "=== FIREBASE CYCLISTS (${firebaseCyclists.size}) ===")
            firebaseCyclists.take(5).forEach { cyclist ->
                android.util.Log.d("PhotoUpload", "  ${cyclist.fullName} (id=${cyclist.id})")
            }

            var matchedCount = 0
            var uploadedCount = 0

            for ((index, uri) in photoUris.withIndex()) {
                val filename = getFilenameFromUri(context, uri) ?: "photo_${index}"

                android.util.Log.d("PhotoUpload", "=== PHOTO ${index + 1}/${photoUris.size}: '$filename' ===")

                // Extract name from filename
                var searchName = filename
                    .substringBeforeLast(".")
                    .lowercase()
                    .replace("_", "-")
                    .replace(" ", "-")
                    .trim()

                // Remove year suffix
                searchName = searchName.replace(Regex("-20\\d{2}$"), "")

                android.util.Log.d("PhotoUpload", "  searchName: '$searchName'")

                _photoUploadProgress.value = PhotoUploadProgress(
                    uploaded = uploadedCount,
                    total = photoUris.size,
                    currentName = filename
                )

                // Match with Firebase cyclist
                val matchedCyclist = findCyclistByPhotoName(searchName, firebaseCyclists)

                if (matchedCyclist != null) {
                    matchedCount++
                    android.util.Log.d("PhotoUpload", "  MATCHED: ${matchedCyclist.fullName}")

                    // Upload photo to Storage
                    val uploadResult = photoStorageService.uploadCyclistPhoto(
                        cyclistId = matchedCyclist.id,
                        imageUri = uri,
                        context = context
                    )

                    uploadResult.fold(
                        onSuccess = { downloadUrl ->
                            // Update Firestore with photo URL
                            val updateResult = cyclistFirestoreService.updateCyclistPhotoUrl(
                                cyclistId = matchedCyclist.id,
                                photoUrl = downloadUrl
                            )
                            if (updateResult.isSuccess) {
                                uploadedCount++
                                android.util.Log.d("PhotoUpload", "  SUCCESS: Photo uploaded and Firestore updated")
                            } else {
                                android.util.Log.e("PhotoUpload", "  Photo uploaded but Firestore update failed")
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("PhotoUpload", "  FAILED: ${error.message}")
                        }
                    )
                } else {
                    android.util.Log.w("PhotoUpload", "  NO MATCH for '$searchName'")
                }
            }

            _photoUploadProgress.value = null

            if (uploadedCount > 0) {
                _uiState.value = AdminSyncUiState.PhotoUploadComplete(uploadedCount, matchedCount, photoUris.size)
                // Refresh counts
                loadSyncStatus()
            } else {
                _uiState.value = AdminSyncUiState.Error(
                    "Nenhuma foto correspondeu a ciclistas. Verifica o formato do nome: apelido-nome-2026.jpg"
                )
            }
        }
    }

    /**
     * Upload photos for cyclists in LOCAL memory (before Firebase upload)
     *
     * @param photoUris List of photo URIs selected by user
     * @param context Android context for file operations
     */
    fun uploadPhotosForCyclists(
        photoUris: List<Uri>,
        context: Context
    ) {
        viewModelScope.launch {
            if (photoUris.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error("Nenhuma foto selecionada")
                return@launch
            }

            val currentCyclists = _scrapedCyclists.value
            if (currentCyclists.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error("Carrega ciclistas primeiro antes de adicionar fotos")
                return@launch
            }

            _uiState.value = AdminSyncUiState.UploadingPhotos
            _photoUploadProgress.value = PhotoUploadProgress(0, photoUris.size, "A iniciar...")

            // Log all cyclists for debugging
            android.util.Log.d("PhotoUpload", "=== CYCLISTS IN MEMORY (${currentCyclists.size}) ===")
            currentCyclists.take(10).forEach { cyclist ->
                android.util.Log.d("PhotoUpload", "  Cyclist: firstName='${cyclist.firstName}', lastName='${cyclist.lastName}', fullName='${cyclist.fullName}'")
            }
            if (currentCyclists.size > 10) {
                android.util.Log.d("PhotoUpload", "  ... and ${currentCyclists.size - 10} more")
            }

            var matchedCount = 0
            var uploadedCount = 0

            for ((index, uri) in photoUris.withIndex()) {
                // Get filename from URI
                val filename = getFilenameFromUri(context, uri) ?: "photo_${index}"

                android.util.Log.d("PhotoUpload", "=== PHOTO ${index + 1}/${photoUris.size}: '$filename' ===")

                // Extract name from filename (remove extension and year suffix)
                // Format expected: lastname-firstname-2026.jpg
                var searchName = filename
                    .substringBeforeLast(".")  // Remove .jpg
                    .lowercase()
                    .replace("_", "-")
                    .replace(" ", "-")
                    .trim()

                // Remove year suffix if present (e.g., "-2026", "-2025", "-2024")
                searchName = searchName.replace(Regex("-20\\d{2}$"), "")

                android.util.Log.d("PhotoUpload", "  searchName: '$searchName'")

                _photoUploadProgress.value = PhotoUploadProgress(
                    uploaded = uploadedCount,
                    total = photoUris.size,
                    currentName = filename
                )

                // Try to match with a cyclist
                val matchedCyclist = findCyclistByPhotoName(searchName, currentCyclists)

                if (matchedCyclist != null) {
                    matchedCount++
                    android.util.Log.d("PhotoUpload", "Matched '$filename' to ${matchedCyclist.fullName}")

                    // Upload to Firebase Storage
                    val result = photoStorageService.uploadCyclistPhoto(
                        cyclistId = matchedCyclist.id,
                        imageUri = uri,
                        context = context
                    )

                    result.fold(
                        onSuccess = { downloadUrl ->
                            // Update cyclist with photo URL
                            _scrapedCyclists.value = _scrapedCyclists.value.map { cyclist ->
                                if (cyclist.id == matchedCyclist.id) {
                                    cyclist.copy(photoUrl = downloadUrl)
                                } else {
                                    cyclist
                                }
                            }
                            uploadedCount++
                            android.util.Log.d("PhotoUpload", "Uploaded photo for ${matchedCyclist.fullName}: $downloadUrl")
                        },
                        onFailure = { error ->
                            android.util.Log.e("PhotoUpload", "Failed to upload for ${matchedCyclist.fullName}: ${error.message}")
                        }
                    )
                } else {
                    android.util.Log.w("PhotoUpload", "No match found for: '$filename' (searchName=$searchName)")
                }
            }

            _photoUploadProgress.value = null

            if (uploadedCount > 0) {
                _uiState.value = AdminSyncUiState.PhotoUploadComplete(uploadedCount, matchedCount, photoUris.size)
            } else {
                _uiState.value = AdminSyncUiState.Error(
                    "Nenhuma foto correspondeu a ciclistas. " +
                    "Verifica se o nome do ciclista esta nas propriedades da foto (titulo/descricao) " +
                    "ou no nome do ficheiro (ex: tadej-pogacar.jpg)"
                )
            }
        }
    }

    /**
     * Extract cyclist name from image EXIF metadata
     * Checks: Title, Description, User Comment, Artist, Subject
     */
    private fun getNameFromImageMetadata(context: Context, uri: Uri): String? {
        android.util.Log.d("PhotoMetadata", "=== Reading metadata for URI: $uri ===")

        // First try MediaStore (more reliable for Windows Title property)
        val mediaStoreResult = tryGetNameFromMediaStore(context, uri)
        if (mediaStoreResult != null) {
            return mediaStoreResult
        }

        // Then try EXIF
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = androidx.exifinterface.media.ExifInterface(inputStream)

                // Try different EXIF tags where the name might be stored
                val possibleTags = listOf(
                    androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION,
                    androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT,
                    androidx.exifinterface.media.ExifInterface.TAG_ARTIST,
                    androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT,
                    androidx.exifinterface.media.ExifInterface.TAG_MAKER_NOTE
                )

                for (tag in possibleTags) {
                    val value = exif.getAttribute(tag)
                    android.util.Log.d("PhotoMetadata", "  EXIF $tag = '$value'")
                    if (!value.isNullOrBlank() && value.length > 2) {
                        android.util.Log.d("PhotoMetadata", "  -> Using EXIF $tag: $value")
                        return@use value.trim()
                    }
                }

                android.util.Log.d("PhotoMetadata", "  No EXIF metadata found")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("PhotoMetadata", "Error reading EXIF: ${e.message}")
            null
        }
    }

    /**
     * Get name from MediaStore metadata (more reliable for Windows Title property)
     */
    private fun tryGetNameFromMediaStore(context: Context, uri: Uri): String? {
        return try {
            // Query with all possible columns
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // Log all available columns for debugging
                    android.util.Log.d("PhotoMetadata", "  MediaStore columns: ${cursor.columnNames.joinToString()}")

                    // Try TITLE first (this is what Windows "Title" maps to)
                    val titleIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.TITLE)
                    if (titleIndex >= 0) {
                        val title = cursor.getString(titleIndex)
                        android.util.Log.d("PhotoMetadata", "  MediaStore TITLE = '$title'")
                        if (!title.isNullOrBlank() && !title.contains(".") && title.length > 2) {
                            android.util.Log.d("PhotoMetadata", "  -> Using MediaStore TITLE: $title")
                            return@use title.trim()
                        }
                    }

                    // Try DESCRIPTION
                    val descIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DESCRIPTION)
                    if (descIndex >= 0) {
                        val desc = cursor.getString(descIndex)
                        android.util.Log.d("PhotoMetadata", "  MediaStore DESCRIPTION = '$desc'")
                        if (!desc.isNullOrBlank() && desc.length > 2) {
                            android.util.Log.d("PhotoMetadata", "  -> Using MediaStore DESCRIPTION: $desc")
                            return@use desc.trim()
                        }
                    }

                    // Log DISPLAY_NAME for reference
                    val displayNameIndex = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DISPLAY_NAME)
                    if (displayNameIndex >= 0) {
                        val displayName = cursor.getString(displayNameIndex)
                        android.util.Log.d("PhotoMetadata", "  MediaStore DISPLAY_NAME = '$displayName'")
                    }
                }
                android.util.Log.d("PhotoMetadata", "  No usable MediaStore metadata found")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("PhotoMetadata", "Error reading MediaStore: ${e.message}")
            null
        }
    }

    /**
     * Find a cyclist that matches the photo filename
     * Tries multiple matching strategies
     * Primary format expected: ultimonome-primeironome (lastName-firstName)
     */
    private fun findCyclistByPhotoName(photoName: String, cyclists: List<Cyclist>): Cyclist? {
        // Normalize the search name (remove accents, lowercase)
        val normalizedPhotoName = normalizeForMatching(photoName)

        android.util.Log.d("PhotoMatch", "Looking for: '$normalizedPhotoName'")

        // Log first 3 cyclists for debugging
        cyclists.take(3).forEach { c ->
            val lastFirst = normalizeForMatching("${c.lastName}-${c.firstName}")
            val firstLast = normalizeForMatching("${c.firstName}-${c.lastName}")
            android.util.Log.d("PhotoMatch", "  Cyclist '${c.fullName}': lastFirst='$lastFirst', firstLast='$firstLast'")
        }

        // Strategy 1: lastName-firstName match (PRIMARY - user's format)
        cyclists.find {
            val expected = normalizeForMatching("${it.lastName}-${it.firstName}")
            expected == normalizedPhotoName
        }?.let {
            android.util.Log.d("PhotoMatch", "  MATCH (lastName-firstName): ${it.fullName}")
            return it
        }

        // Strategy 2: firstName-lastName match
        cyclists.find {
            val expected = normalizeForMatching("${it.firstName}-${it.lastName}")
            expected == normalizedPhotoName
        }?.let {
            android.util.Log.d("PhotoMatch", "  MATCH (firstName-lastName): ${it.fullName}")
            return it
        }

        // Strategy 3: fullName without spaces
        cyclists.find {
            normalizeForMatching(it.fullName) == normalizedPhotoName
        }?.let {
            android.util.Log.d("PhotoMatch", "  MATCH (fullName): ${it.fullName}")
            return it
        }

        // Strategy 4: Partial match - both parts of name present
        cyclists.find {
            val firstName = normalizeForMatching(it.firstName)
            val lastName = normalizeForMatching(it.lastName)
            firstName.length > 2 && lastName.length > 2 &&
            normalizedPhotoName.contains(firstName) && normalizedPhotoName.contains(lastName)
        }?.let {
            android.util.Log.d("PhotoMatch", "  MATCH (partial): ${it.fullName}")
            return it
        }

        // Strategy 5: Last name only match (if unique and lastName > 3 chars)
        val lastNameMatches = cyclists.filter {
            val lastName = normalizeForMatching(it.lastName)
            lastName.length > 3 && normalizedPhotoName.contains(lastName)
        }
        if (lastNameMatches.size == 1) {
            android.util.Log.d("PhotoMatch", "  MATCH (unique lastName): ${lastNameMatches[0].fullName}")
            return lastNameMatches[0]
        }

        android.util.Log.w("PhotoMatch", "  NO MATCH for '$normalizedPhotoName'")
        return null
    }

    /**
     * Normalize a string for matching: lowercase, remove accents, replace spaces with hyphens
     */
    private fun normalizeForMatching(text: String): String {
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
            .lowercase()
            .replace(" ", "-")
            .replace("_", "-")
            .trim()
    }

    /**
     * Get filename from a content URI
     */
    private fun getFilenameFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } ?: uri.lastPathSegment
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    /**
     * Add a team to the scraping list
     * @param url Full URL of the team page
     * @param name Display name of the team
     */
    fun addTeam(url: String, name: String) {
        val cleanUrl = url.trim()
        val cleanName = name.trim()

        if (cleanUrl.isBlank() || cleanName.isBlank()) return

        // Validate URL contains procyclingstats
        if (!cleanUrl.contains("procyclingstats.com")) {
            _uiState.value = AdminSyncUiState.Error("URL deve ser de procyclingstats.com")
            return
        }

        val currentTeams = _teams.value.toMutableList()
        val newTeam = TeamConfig(cleanUrl, cleanName)

        // Check for duplicates
        if (currentTeams.any { it.url == cleanUrl || it.name.equals(cleanName, ignoreCase = true) }) {
            _uiState.value = AdminSyncUiState.Error("Equipa já existe na lista")
            return
        }

        currentTeams.add(newTeam)
        _teams.value = currentTeams
        _uiState.value = AdminSyncUiState.Initial
    }

    /**
     * Remove a team from the scraping list
     */
    fun removeTeam(url: String) {
        _teams.value = _teams.value.filter { it.url != url }
    }

    /**
     * Clear all teams from the list
     */
    fun clearAllTeams() {
        _teams.value = emptyList()
    }

    /**
     * Scrape cyclists from a single team
     */
    fun scrapeTeam(teamConfig: TeamConfig) {
        viewModelScope.launch {
            _uiState.value = AdminSyncUiState.Scraping
            _scrapingProgress.value = ScrapingProgress(
                currentTeam = teamConfig.name,
                currentIndex = 0,
                totalTeams = 1
            )

            cyclingDataSource.getCyclistsFromTeamUrl(teamConfig.url, teamConfig.name).fold(
                onSuccess = { dtos ->
                    val newCyclists = dtos.map { dto -> convertDtoToCyclist(dto) }

                    // Add to existing list (avoiding duplicates)
                    val currentCyclists = _scrapedCyclists.value.toMutableList()
                    val existingIds = currentCyclists.map { it.id }.toSet()
                    val uniqueNewCyclists = newCyclists.filter { it.id !in existingIds }

                    currentCyclists.addAll(uniqueNewCyclists)
                    _scrapedCyclists.value = currentCyclists

                    _uiState.value = AdminSyncUiState.ScrapingComplete(uniqueNewCyclists.size)
                    _scrapingProgress.value = null
                },
                onFailure = { error ->
                    _uiState.value = AdminSyncUiState.Error(
                        "Erro ao fazer scraping de ${teamConfig.name}: ${error.message}"
                    )
                    _scrapingProgress.value = null
                }
            )
        }
    }

    /**
     * Scrape all teams in the list sequentially
     */
    fun scrapeAllTeams() {
        viewModelScope.launch {
            val teamsToScrape = _teams.value
            if (teamsToScrape.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error("Nenhuma equipa definida. Adiciona pelo menos uma equipa.")
                return@launch
            }

            _uiState.value = AdminSyncUiState.Scraping
            val allNewCyclists = mutableListOf<Cyclist>()

            teamsToScrape.forEachIndexed { index, teamConfig ->
                _scrapingProgress.value = ScrapingProgress(
                    currentTeam = teamConfig.name,
                    currentIndex = index + 1,
                    totalTeams = teamsToScrape.size
                )

                cyclingDataSource.getCyclistsFromTeamUrl(teamConfig.url, teamConfig.name).fold(
                    onSuccess = { dtos ->
                        val cyclists = dtos.map { dto -> convertDtoToCyclist(dto) }
                        allNewCyclists.addAll(cyclists)
                    },
                    onFailure = { error ->
                        // Log error but continue with other teams
                        android.util.Log.e("AdminSync", "Failed to scrape ${teamConfig.name}: ${error.message}")
                    }
                )
            }

            // Add to existing list (avoiding duplicates)
            val currentCyclists = _scrapedCyclists.value.toMutableList()
            val existingIds = currentCyclists.map { it.id }.toSet()
            val uniqueNewCyclists = allNewCyclists.filter { it.id !in existingIds }

            currentCyclists.addAll(uniqueNewCyclists)
            _scrapedCyclists.value = currentCyclists

            _uiState.value = AdminSyncUiState.ScrapingComplete(uniqueNewCyclists.size)
            _scrapingProgress.value = null
        }
    }

    /**
     * Convert CyclistDto to Cyclist domain model with calculated price
     */
    private fun convertDtoToCyclist(dto: com.ciclismo.portugal.data.remote.cycling.CyclistDto): Cyclist {
        // Convert speciality to category (6 categorias: Climber, Hills, TT, Sprint, GC, Oneday)
        val category = when (dto.speciality?.lowercase()) {
            "climber", "climbing", "mountains" -> CyclistCategory.CLIMBER
            "hills", "puncheur", "puncher" -> CyclistCategory.HILLS
            "tt", "time trial", "timetrial", "itt" -> CyclistCategory.TT
            "sprint", "sprinter" -> CyclistCategory.SPRINT
            "gc", "general classification", "stage races", "all-rounder" -> CyclistCategory.GC
            "oneday", "one day", "classics", "classic", "one day races", "cobbles" -> CyclistCategory.ONEDAY
            else -> CyclistCategory.GC // Default to GC
        }

        // Calculate price based on UCI ranking
        val basePrice = when {
            dto.uciRanking != null && dto.uciRanking <= 10 -> 15.0
            dto.uciRanking != null && dto.uciRanking <= 25 -> 12.0
            dto.uciRanking != null && dto.uciRanking <= 50 -> 10.0
            dto.uciRanking != null && dto.uciRanking <= 100 -> 8.0
            dto.uciRanking != null && dto.uciRanking <= 200 -> 6.5
            dto.uciRanking != null && dto.uciRanking <= 500 -> 5.5
            dto.points > 2000 -> 7.0
            dto.points > 1000 -> 5.5
            dto.points > 500 -> 4.5
            else -> 4.0
        }

        return Cyclist(
            id = dto.id,
            firstName = dto.firstName,
            lastName = dto.lastName,
            fullName = "${dto.firstName} ${dto.lastName}",
            teamId = dto.teamId,
            teamName = dto.teamName,
            nationality = dto.nationality,
            photoUrl = dto.photoUrl,
            category = category,
            price = basePrice,
            totalPoints = dto.points,
            form = 0.0,
            popularity = 0.0,
            age = dto.age,
            uciRanking = dto.uciRanking,
            speciality = dto.speciality,
            profileUrl = dto.profileUrl
        )
    }

    // Upload progress tracking
    private val _uploadProgress = MutableStateFlow<UploadProgress?>(null)
    val uploadProgress: StateFlow<UploadProgress?> = _uploadProgress.asStateFlow()

    /**
     * Upload scraped cyclists to Firestore
     * Deletes all existing cyclists first, then uploads new ones in batches
     * Removes cyclists from list as they are uploaded for visual feedback
     */
    fun uploadToFirestore() {
        viewModelScope.launch {
            val cyclists = _scrapedCyclists.value
            if (cyclists.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error("Nenhum ciclista para enviar. Faz scraping primeiro.")
                return@launch
            }

            _uiState.value = AdminSyncUiState.Uploading

            // Show initial progress
            _uploadProgress.value = UploadProgress(
                uploaded = 0,
                total = cyclists.size,
                currentName = "A preparar upload...",
                batchNumber = 0,
                totalBatches = 0
            )

            // Skip delete - Firestore set() will overwrite existing documents
            // Delete can be done manually via "Apagar Todos" button if needed
            android.util.Log.d("AdminSync", "Skipping delete - will overwrite existing docs")

            // Calculate prices before uploading
            val cyclistsWithPrices = calculatePricesForCyclists(cyclists)
            _scrapedCyclists.value = cyclistsWithPrices

            val totalCount = cyclistsWithPrices.size
            var uploadedCount = 0
            val batchSize = 5 // Upload 5 cyclists at a time for better progress visibility

            // Upload in batches and remove from list as we go
            val batches = cyclistsWithPrices.chunked(batchSize)

            android.util.Log.d("AdminSync", "Starting upload of $totalCount cyclists in ${batches.size} batches")

            val totalBatches = batches.size

            for ((index, batch) in batches.withIndex()) {
                val batchNum = index + 1

                // Update progress BEFORE upload
                _uploadProgress.value = UploadProgress(
                    uploaded = uploadedCount,
                    total = totalCount,
                    currentName = batch.firstOrNull()?.fullName ?: "",
                    batchNumber = batchNum,
                    totalBatches = totalBatches
                )

                android.util.Log.d("AdminSync", "Batch $batchNum/$totalBatches - Progress: $uploadedCount/$totalCount")

                // Give Compose time to recompose with the new progress
                kotlinx.coroutines.delay(100)

                // Upload batch on IO thread with timeout
                val isLastBatch = index == batches.size - 1
                android.util.Log.d("AdminSync", "Starting Firestore upload for batch $batchNum...")

                val result = try {
                    kotlinx.coroutines.withTimeout(30000) { // 30 second timeout per batch
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            cyclistFirestoreService.uploadCyclists(batch, updateStatus = isLastBatch)
                        }
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    android.util.Log.e("AdminSync", "Batch $batchNum timed out!")
                    Result.failure<Int>(Exception("Upload timeout - batch $batchNum demorou mais de 30s"))
                }

                android.util.Log.d("AdminSync", "Firestore upload for batch $batchNum completed: ${result.isSuccess}")

                if (result.isSuccess) {
                    uploadedCount += batch.size

                    // Remove uploaded cyclists from the list immediately
                    val uploadedIds = batch.map { it.id }.toSet()
                    _scrapedCyclists.value = _scrapedCyclists.value.filter { it.id !in uploadedIds }

                    // Update progress AFTER successful upload
                    _uploadProgress.value = UploadProgress(
                        uploaded = uploadedCount,
                        total = totalCount,
                        currentName = if (index < batches.size - 1) batches[index + 1].firstOrNull()?.fullName ?: "" else "Concluido!",
                        batchNumber = batchNum,
                        totalBatches = totalBatches
                    )

                    android.util.Log.d("AdminSync", "Batch $batchNum complete - Uploaded: $uploadedCount")

                    // Small delay to let UI update
                    kotlinx.coroutines.delay(50)
                } else {
                    _uiState.value = AdminSyncUiState.Error(
                        "Erro no upload batch ${index + 1}: ${result.exceptionOrNull()?.message}"
                    )
                    _uploadProgress.value = null
                    return@launch
                }
            }

            _uploadProgress.value = null
            _uiState.value = AdminSyncUiState.UploadComplete(totalCount)
            loadSyncStatus()
        }
    }

    /**
     * Calculate prices for all cyclists based on their category and UCI ranking
     * Rules:
     * - #1 overall UCI ranking = 22M
     * - Top 2% of each category = 20M
     * - Bottom 5% of each category = 1M
     * - Proportional distribution for the rest (between 1M and 20M)
     */
    private fun calculatePricesForCyclists(cyclists: List<Cyclist>): List<Cyclist> {
        if (cyclists.isEmpty()) return cyclists

        // Find the #1 ranked cyclist overall
        val topRankedCyclist = cyclists.filter { it.uciRanking != null && it.uciRanking > 0 }
            .minByOrNull { it.uciRanking!! }

        // Group cyclists by category
        val cyclistsByCategory = cyclists.groupBy { it.category }

        val result = mutableListOf<Cyclist>()

        for ((category, categoryCyclists) in cyclistsByCategory) {
            // Sort by UCI ranking within category (null/0 rankings go to the end)
            val sortedCyclists = categoryCyclists.sortedBy {
                it.uciRanking?.takeIf { r -> r > 0 } ?: Int.MAX_VALUE
            }

            val total = sortedCyclists.size
            val top2PercentCount = maxOf(1, (total * 0.02).toInt())
            val bottom5PercentCount = maxOf(1, (total * 0.05).toInt())
            val middleCount = total - top2PercentCount - bottom5PercentCount

            for ((index, cyclist) in sortedCyclists.withIndex()) {
                val price = when {
                    // #1 overall ranked cyclist gets 22M
                    topRankedCyclist != null && cyclist.id == topRankedCyclist.id -> 22.0

                    // Top 2% get 20M
                    index < top2PercentCount -> 20.0

                    // Bottom 5% get 1M
                    index >= (total - bottom5PercentCount) -> 1.0

                    // Middle cyclists get proportional value
                    else -> {
                        val middleIndex = index - top2PercentCount
                        // Linear interpolation from 19M (just below top) to 2M (just above bottom)
                        val ratio = if (middleCount > 1) {
                            middleIndex.toDouble() / (middleCount - 1)
                        } else {
                            0.5
                        }
                        // Price goes from 19M to 2M as index increases
                        19.0 - (ratio * 17.0)
                    }
                }

                // Round to 1 decimal place
                val roundedPrice = (price * 10).toInt() / 10.0

                result.add(cyclist.copy(price = roundedPrice))
            }
        }

        return result
    }

    /**
     * Validate all pending cyclists
     */
    fun validateAllCyclists() {
        viewModelScope.launch {
            _uiState.value = AdminSyncUiState.Validating

            cyclistFirestoreService.validateAllCyclists().fold(
                onSuccess = { count ->
                    _uiState.value = AdminSyncUiState.ValidationComplete(count)
                    // Wait for fire-and-forget operations to complete, then refresh
                    kotlinx.coroutines.delay(2000)
                    loadSyncStatus()
                },
                onFailure = { error ->
                    _uiState.value = AdminSyncUiState.Error(
                        error.message ?: "Erro ao validar ciclistas"
                    )
                }
            )
        }
    }

    /**
     * Delete all cyclists from Firestore
     */
    fun deleteAllCyclists() {
        viewModelScope.launch {
            _uiState.value = AdminSyncUiState.Deleting

            cyclistFirestoreService.deleteAllCyclists().fold(
                onSuccess = {
                    _uiState.value = AdminSyncUiState.DeleteComplete
                    // Wait for fire-and-forget operations to complete, then refresh
                    kotlinx.coroutines.delay(2000)
                    loadSyncStatus()
                },
                onFailure = { error ->
                    _uiState.value = AdminSyncUiState.Error(
                        error.message ?: "Erro ao apagar ciclistas"
                    )
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = AdminSyncUiState.Initial
    }

    // Fantasy team reset state
    private val _teamResetState = MutableStateFlow<TeamResetState>(TeamResetState.Idle)
    val teamResetState: StateFlow<TeamResetState> = _teamResetState.asStateFlow()

    /**
     * Delete all fantasy teams for a season.
     * Users will need to create a new team starting with 100M budget.
     * Also clears league members for the season.
     */
    fun deleteAllFantasyTeams(season: Int = SeasonConfig.CURRENT_SEASON) {
        viewModelScope.launch {
            try {
                _teamResetState.value = TeamResetState.Deleting

                // 1. Delete all fantasy teams
                val teamsResult = fantasyTeamFirestoreService.deleteAllTeamsForSeason(season)
                val teamsDeleted = teamsResult.getOrDefault(0)

                // 2. Clear league members for the season
                val leagueResult = leagueFirestoreService.clearAllMembersForSeason(season)
                val membersCleared = leagueResult.getOrDefault(0)

                _teamResetState.value = TeamResetState.Success(
                    teamsDeleted = teamsDeleted,
                    membersCleared = membersCleared
                )

                android.util.Log.d("AdminSync", "Reset complete: $teamsDeleted teams deleted, $membersCleared league members cleared")
            } catch (e: Exception) {
                android.util.Log.e("AdminSync", "Error resetting teams: ${e.message}")
                _teamResetState.value = TeamResetState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun dismissTeamResetState() {
        _teamResetState.value = TeamResetState.Idle
    }

    /**
     * Clear local image cache (Coil disk and memory cache) AND Firebase Storage photos
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearImageCache(context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = AdminSyncUiState.Deleting

                // Clear Coil memory cache
                coil.Coil.imageLoader(context).memoryCache?.clear()
                android.util.Log.d("AdminSync", "Coil memory cache cleared")

                // Clear Coil disk cache
                coil.Coil.imageLoader(context).diskCache?.clear()
                android.util.Log.d("AdminSync", "Coil disk cache cleared")

                // Clear Firebase Storage photos
                val storageResult = photoStorageService.deleteAllPhotos()
                storageResult.fold(
                    onSuccess = { count ->
                        android.util.Log.d("AdminSync", "Deleted $count photos from Firebase Storage")
                    },
                    onFailure = { error ->
                        android.util.Log.e("AdminSync", "Error deleting Firebase photos: ${error.message}")
                    }
                )

                // Clear photoUrl from all cyclists in Firestore
                val firestoreResult = cyclistFirestoreService.clearAllPhotoUrls()
                firestoreResult.fold(
                    onSuccess = { count ->
                        android.util.Log.d("AdminSync", "Cleared photoUrl from $count cyclists in Firestore")
                    },
                    onFailure = { error ->
                        android.util.Log.e("AdminSync", "Error clearing Firestore photoUrls: ${error.message}")
                    }
                )

                _uiState.value = AdminSyncUiState.DeleteComplete
                loadSyncStatus() // Refresh counts
                android.util.Log.d("AdminSync", "All image caches cleared successfully")
            } catch (e: Exception) {
                _uiState.value = AdminSyncUiState.Error("Erro ao limpar cache: ${e.message}")
                android.util.Log.e("AdminSync", "Error clearing cache: ${e.message}")
            }
        }
    }

    /**
     * Update a cyclist in the scraped list (before upload)
     */
    fun updateCyclist(updatedCyclist: Cyclist) {
        _scrapedCyclists.value = _scrapedCyclists.value.map { cyclist ->
            if (cyclist.id == updatedCyclist.id) updatedCyclist else cyclist
        }
    }

    // ========== CYCLIST SEARCH AND EDIT (Firebase) ==========

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Cyclist>>(emptyList())
    val searchResults: StateFlow<List<Cyclist>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    /**
     * Update search query and trigger search
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            searchCyclists(query)
        } else {
            _searchResults.value = emptyList()
        }
    }

    /**
     * Search cyclists in Firebase by name
     */
    fun searchCyclists(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            android.util.Log.d("AdminSync", "Searching cyclists: '$query' (season: ${_currentSeason.value})")
            val result = cyclistFirestoreService.searchCyclistsByName(query, _currentSeason.value)
            result.fold(
                onSuccess = { cyclists ->
                    android.util.Log.d("AdminSync", "Search returned ${cyclists.size} cyclists")
                    cyclists.forEach { cyclist ->
                        android.util.Log.d("AdminSync", "  - ${cyclist.fullName}: ${cyclist.price}M (${cyclist.id})")
                    }
                    _searchResults.value = cyclists
                },
                onFailure = { error ->
                    android.util.Log.e("AdminSync", "Search failed: ${error.message}")
                    _searchResults.value = emptyList()
                }
            )
            _isSearching.value = false
        }
    }

    /**
     * Update cyclist in Firebase (full details)
     */
    fun updateCyclistInFirebase(cyclist: Cyclist) {
        viewModelScope.launch {
            android.util.Log.d("AdminSync", "========== UPDATING CYCLIST ==========")
            android.util.Log.d("AdminSync", "Name: ${cyclist.fullName}")
            android.util.Log.d("AdminSync", "ID: ${cyclist.id}")
            android.util.Log.d("AdminSync", "Price: ${cyclist.price}M")
            android.util.Log.d("AdminSync", "Season: ${_currentSeason.value}")

            _uiState.value = AdminSyncUiState.Uploading
            val result = cyclistFirestoreService.updateCyclist(cyclist, _currentSeason.value)
            result.fold(
                onSuccess = {
                    android.util.Log.d("AdminSync", "SUCCESS: Cyclist updated in Firestore")
                    _uiState.value = AdminSyncUiState.UploadComplete(1)

                    // Small delay to ensure Firestore has propagated the change
                    kotlinx.coroutines.delay(500)

                    // Refresh search results to show updated data
                    if (_searchQuery.value.isNotEmpty()) {
                        android.util.Log.d("AdminSync", "Refreshing search results...")
                        searchCyclists(_searchQuery.value)
                    }
                    // Refresh counts
                    loadSyncStatus()
                    android.util.Log.d("AdminSync", "========================================")
                },
                onFailure = { error ->
                    android.util.Log.e("AdminSync", "FAILED: ${error.message}")
                    android.util.Log.e("AdminSync", "========================================")
                    _uiState.value = AdminSyncUiState.Error("Erro ao atualizar ciclista: ${error.message}")
                }
            )
        }
    }

    /**
     * Disable a cyclist (injury, dropout, suspension, etc)
     * This makes the cyclist unavailable in the market
     */
    fun disableCyclist(cyclistId: String, reason: String) {
        viewModelScope.launch {
            val result = cyclistFirestoreService.updateCyclistAvailability(
                cyclistId = cyclistId,
                isDisabled = true,
                reason = reason,
                season = _currentSeason.value
            )
            result.fold(
                onSuccess = {
                    android.util.Log.d("AdminSync", "Cyclist $cyclistId disabled: $reason")
                    // Refresh search results to show updated status
                    if (_searchQuery.value.isNotEmpty()) {
                        searchCyclists(_searchQuery.value)
                    }
                },
                onFailure = { error ->
                    _uiState.value = AdminSyncUiState.Error("Erro ao desativar ciclista: ${error.message}")
                }
            )
        }
    }

    /**
     * Enable a previously disabled cyclist
     * This makes the cyclist available again in the market
     */
    fun enableCyclist(cyclistId: String) {
        viewModelScope.launch {
            val result = cyclistFirestoreService.updateCyclistAvailability(
                cyclistId = cyclistId,
                isDisabled = false,
                reason = null,
                season = _currentSeason.value
            )
            result.fold(
                onSuccess = {
                    android.util.Log.d("AdminSync", "Cyclist $cyclistId enabled")
                    // Refresh search results to show updated status
                    if (_searchQuery.value.isNotEmpty()) {
                        searchCyclists(_searchQuery.value)
                    }
                },
                onFailure = { error ->
                    _uiState.value = AdminSyncUiState.Error("Erro ao ativar ciclista: ${error.message}")
                }
            )
        }
    }

    /**
     * Toggle cyclist availability (admin function)
     * @param cyclistId The cyclist's ID
     * @param isDisabled Whether to disable the cyclist
     * @param reason The reason for disabling (only used when disabling)
     */
    fun toggleCyclistAvailability(cyclistId: String, isDisabled: Boolean, reason: String?) {
        if (isDisabled) {
            disableCyclist(cyclistId, reason ?: "Indisponível")
        } else {
            enableCyclist(cyclistId)
        }
    }

    /**
     * Clear search results
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    /**
     * Delete a cyclist from the scraped list
     */
    fun removeCyclist(cyclistId: String) {
        _scrapedCyclists.value = _scrapedCyclists.value.filter { it.id != cyclistId }
    }

    /**
     * Clear all scraped cyclists
     */
    fun clearScrapedCyclists() {
        _scrapedCyclists.value = emptyList()
    }

    /**
     * Add a cyclist manually (without scraping)
     * Recalculates prices for all cyclists to maintain proper ranking
     */
    fun addCyclistManually(cyclist: Cyclist) {
        val currentCyclists = _scrapedCyclists.value.toMutableList()

        // Check for duplicates by ID
        if (currentCyclists.any { it.id == cyclist.id }) {
            _uiState.value = AdminSyncUiState.Error("Ciclista com este ID ja existe")
            return
        }

        currentCyclists.add(cyclist)

        // Recalculate prices for all cyclists
        val cyclistsWithPrices = calculatePricesForCyclists(currentCyclists)
        _scrapedCyclists.value = cyclistsWithPrices
        _uiState.value = AdminSyncUiState.ScrapingComplete(1)
    }

    /**
     * Add multiple cyclists from CSV import
     * Calculates prices automatically based on category rankings
     */
    fun addCyclistsFromCsv(cyclists: List<Cyclist>) {
        val currentCyclists = _scrapedCyclists.value.toMutableList()
        val existingNames = currentCyclists.map { it.fullName.lowercase() }.toSet()

        // Filter out duplicates by name
        val uniqueNewCyclists = cyclists.filter { it.fullName.lowercase() !in existingNames }

        currentCyclists.addAll(uniqueNewCyclists)

        // Calculate prices for all cyclists (including existing ones for proper ranking)
        val cyclistsWithPrices = calculatePricesForCyclists(currentCyclists)
        _scrapedCyclists.value = cyclistsWithPrices

        val duplicatesCount = cyclists.size - uniqueNewCyclists.size
        val message = if (duplicatesCount > 0) {
            "${uniqueNewCyclists.size} ciclistas importados ($duplicatesCount duplicados ignorados)"
        } else {
            "${uniqueNewCyclists.size} ciclistas importados"
        }

        _uiState.value = AdminSyncUiState.ScrapingComplete(uniqueNewCyclists.size)
    }

    /**
     * Show error message
     */
    fun showError(message: String) {
        _uiState.value = AdminSyncUiState.Error(message)
    }

    /**
     * Scrape cyclists from a list of individual URLs
     * @param urls List of ProCyclingStats cyclist URLs
     * @param teamName Optional team name to associate with all cyclists
     */
    fun scrapeFromUrls(urls: List<String>, teamName: String = "") {
        viewModelScope.launch {
            if (urls.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error("Nenhum URL fornecido")
                return@launch
            }

            _uiState.value = AdminSyncUiState.Scraping
            val newCyclists = mutableListOf<Cyclist>()
            var successCount = 0
            var errorCount = 0

            urls.forEachIndexed { index, url ->
                _scrapingProgress.value = ScrapingProgress(
                    currentTeam = "Ciclista ${index + 1}/${urls.size}",
                    currentIndex = index + 1,
                    totalTeams = urls.size
                )

                // Small delay between requests to avoid rate limiting
                if (index > 0) {
                    kotlinx.coroutines.delay(500)
                }

                cyclingDataSource.getCyclistFromUrl(url, teamName).fold(
                    onSuccess = { dto ->
                        val cyclist = convertDtoToCyclist(dto)
                        newCyclists.add(cyclist)
                        successCount++
                    },
                    onFailure = { error ->
                        android.util.Log.e("AdminSync", "Failed to scrape $url: ${error.message}")
                        errorCount++
                    }
                )
            }

            // Add to existing list (avoiding duplicates)
            val currentCyclists = _scrapedCyclists.value.toMutableList()
            val existingNames = currentCyclists.map { it.fullName.lowercase() }.toSet()
            val uniqueNewCyclists = newCyclists.filter { it.fullName.lowercase() !in existingNames }

            currentCyclists.addAll(uniqueNewCyclists)
            _scrapedCyclists.value = currentCyclists

            _scrapingProgress.value = null

            if (successCount > 0) {
                val message = if (errorCount > 0) {
                    "$successCount ciclistas obtidos ($errorCount falharam)"
                } else {
                    "$successCount ciclistas obtidos"
                }
                _uiState.value = AdminSyncUiState.ScrapingComplete(successCount)
            } else {
                _uiState.value = AdminSyncUiState.Error("Nenhum ciclista obtido. Site pode estar a bloquear ($errorCount erros)")
            }
        }
    }

    /**
     * Upload WorldTour races for the current year to Firestore
     * This seeds the database with real UCI WorldTour races
     */
    fun uploadWorldTourRaces() {
        viewModelScope.launch {
            _uiState.value = AdminSyncUiState.Uploading

            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val races = generateWorldTourRaces(currentYear)

            android.util.Log.d("AdminSync", "Uploading ${races.size} WorldTour races for $currentYear...")

            raceRepository.uploadRaces(races).fold(
                onSuccess = { count ->
                    android.util.Log.d("AdminSync", "Uploaded $count WorldTour races")
                    _uiState.value = AdminSyncUiState.UploadComplete(count)
                },
                onFailure = { error ->
                    android.util.Log.e("AdminSync", "Error uploading races: ${error.message}")
                    _uiState.value = AdminSyncUiState.Error("Erro ao carregar corridas: ${error.message}")
                }
            )
        }
    }

    /**
     * Import races from CSV and upload to Firestore
     * CSV format: datainicio,datafim,ano,nome,url
     * - datainicio/datafim: dd/MM/yyyy or yyyy-MM-dd
     * - datafim can be empty for one-day races
     * - ano: year (used to validate/generate ID)
     * - url: optional link to race info
     *
     * @param csvLines List of CSV lines (first line can be header)
     */
    fun importRacesFromCsv(csvLines: List<String>) {
        viewModelScope.launch {
            if (csvLines.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error("Ficheiro CSV vazio")
                return@launch
            }

            _uiState.value = AdminSyncUiState.Uploading

            val races = mutableListOf<Race>()
            var errorCount = 0

            // Skip header if present
            val startIndex = if (csvLines.first().lowercase().contains("nome") ||
                                 csvLines.first().lowercase().contains("datainicio")) 1 else 0

            for (i in startIndex until csvLines.size) {
                val line = csvLines[i].trim()
                if (line.isBlank()) continue

                try {
                    val race = parseRaceCsvLine(line)
                    if (race != null) {
                        races.add(race)
                    } else {
                        errorCount++
                        android.util.Log.w("AdminSync", "Failed to parse line $i: $line")
                    }
                } catch (e: Exception) {
                    errorCount++
                    android.util.Log.e("AdminSync", "Error parsing line $i: ${e.message}")
                }
            }

            if (races.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error(
                    "Nenhuma corrida encontrada no CSV. Verifica o formato:\ndatainicio,datafim,ano,nome,url"
                )
                return@launch
            }

            android.util.Log.d("AdminSync", "Parsed ${races.size} races from CSV ($errorCount errors)")

            raceRepository.uploadRaces(races).fold(
                onSuccess = { count ->
                    val message = if (errorCount > 0) {
                        "$count corridas carregadas ($errorCount linhas com erro)"
                    } else {
                        "$count corridas carregadas"
                    }
                    android.util.Log.d("AdminSync", message)
                    _uiState.value = AdminSyncUiState.UploadComplete(count)
                },
                onFailure = { error ->
                    android.util.Log.e("AdminSync", "Error uploading races: ${error.message}")
                    _uiState.value = AdminSyncUiState.Error("Erro ao carregar corridas: ${error.message}")
                }
            )
        }
    }

    /**
     * Parse a single CSV line into a Race object
     * Format: datainicio,datafim,ano,nome,url
     * Date format: dd/MM (year comes from 'ano' column)
     */
    private fun parseRaceCsvLine(line: String): Race? {
        // Split by comma, but handle quoted values
        val parts = parseCsvLineWithQuotes(line)

        if (parts.size < 4) {
            android.util.Log.w("AdminSync", "Line has ${parts.size} columns, expected at least 4: $line")
            return null
        }

        val startDateStr = parts[0].trim()
        val endDateStr = parts.getOrNull(1)?.trim() ?: ""
        val yearStr = parts.getOrNull(2)?.trim() ?: ""
        val name = parts.getOrNull(3)?.trim() ?: ""
        val url = parts.getOrNull(4)?.trim() ?: ""

        if (name.isBlank() || startDateStr.isBlank()) {
            return null
        }

        // Get year first (needed for date parsing)
        val year = yearStr.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

        // Parse dates using the year from 'ano' column
        val startDate = parseDateWithYear(startDateStr, year) ?: return null
        val endDate = if (endDateStr.isNotBlank()) parseDateWithYear(endDateStr, year) else null

        // Generate ID from name and year
        val id = generateRaceId(name, year)

        // Determine race type based on duration
        val type = determineRaceType(name, startDate, endDate)

        // Calculate stages if multi-day race
        val stages = if (endDate != null && endDate > startDate) {
            val days = ((endDate - startDate) / (1000 * 60 * 60 * 24)).toInt() + 1
            days.coerceIn(1, 21)
        } else {
            1
        }

        // Determine category based on race name
        val category = determineCategoryFromName(name)

        // Try to extract country from name or URL
        val country = extractCountryFromName(name)

        // Races that haven't ended yet should be active for notifications
        val now = System.currentTimeMillis()
        val raceEndDate = endDate ?: startDate
        val shouldBeActive = raceEndDate >= now

        return Race(
            id = id,
            name = name,
            type = type,
            startDate = startDate,
            endDate = endDate,
            stages = stages,
            country = country,
            category = category,
            isActive = shouldBeActive,
            profileUrl = url.ifBlank { null }
        )
    }

    /**
     * Generate a URL-safe ID from race name and year
     */
    private fun generateRaceId(name: String, year: Int): String {
        val normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return "$normalized-$year"
    }

    /**
     * Determine race type from name and dates
     */
    private fun determineRaceType(name: String, startDate: Long, endDate: Long?): RaceType {
        val nameLower = name.lowercase()

        // Grand Tours
        if (nameLower.contains("tour de france") ||
            nameLower.contains("giro") ||
            nameLower.contains("vuelta a espana") ||
            nameLower.contains("vuelta a españa")) {
            return RaceType.GRAND_TOUR
        }

        // One-day races (classics)
        if (endDate == null || endDate == startDate) {
            return RaceType.ONE_DAY
        }

        // Check duration for multi-day
        val days = ((endDate - startDate) / (1000 * 60 * 60 * 24)).toInt()
        return when {
            days >= 18 -> RaceType.GRAND_TOUR
            days > 1 -> RaceType.STAGE_RACE
            else -> RaceType.ONE_DAY
        }
    }

    /**
     * Determine category from race name
     */
    private fun determineCategoryFromName(name: String): String {
        val nameLower = name.lowercase()
        return when {
            nameLower.contains("tour de france") ||
            nameLower.contains("giro") ||
            nameLower.contains("vuelta a espana") ||
            nameLower.contains("vuelta a españa") -> "GT"

            nameLower.contains("volta a portugal") ||
            nameLower.contains("volta portugal") -> "2.1"

            nameLower.contains("paris-nice") ||
            nameLower.contains("tirreno") ||
            nameLower.contains("dauphine") ||
            nameLower.contains("suisse") ||
            nameLower.contains("romandie") ||
            nameLower.contains("catalunya") ||
            nameLower.contains("pais vasco") ||
            nameLower.contains("paris-roubaix") ||
            nameLower.contains("milano-sanremo") ||
            nameLower.contains("ronde") ||
            nameLower.contains("liege") ||
            nameLower.contains("lombardia") -> "WT"

            else -> "WT"
        }
    }

    /**
     * Extract country from race name
     */
    private fun extractCountryFromName(name: String): String {
        val nameLower = name.lowercase()
        return when {
            nameLower.contains("portugal") -> "Portugal"
            nameLower.contains("france") || nameLower.contains("paris") -> "Franca"
            nameLower.contains("italia") || nameLower.contains("giro") ||
            nameLower.contains("sanremo") || nameLower.contains("lombardia") -> "Italia"
            nameLower.contains("espana") || nameLower.contains("españa") ||
            nameLower.contains("catalunya") || nameLower.contains("pais vasco") -> "Espanha"
            nameLower.contains("belgica") || nameLower.contains("ronde") ||
            nameLower.contains("liege") || nameLower.contains("vlaanderen") -> "Belgica"
            nameLower.contains("suisse") -> "Suica"
            nameLower.contains("romandie") -> "Suica"
            else -> ""
        }
    }

    /**
     * Parse a CSV line handling quoted values
     */
    private fun parseCsvLineWithQuotes(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())

        return result
    }

    /**
     * Parse date string with external year
     * Supports: dd.MM (primary), dd/MM, dd-MM formats
     * Year is provided from 'ano' column
     */
    private fun parseDateWithYear(dateStr: String, year: Int): Long? {
        // Try to extract day and month from various formats
        val cleanDate = dateStr.trim()

        // Split by common separators: . / -
        val parts = cleanDate.split(".", "/", "-")

        if (parts.size >= 2) {
            val day = parts[0].trim().toIntOrNull()
            val month = parts[1].trim().toIntOrNull()

            if (day != null && month != null && day in 1..31 && month in 1..12) {
                val cal = Calendar.getInstance()
                cal.set(year, month - 1, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
        }

        android.util.Log.w("AdminSync", "Could not parse date: $dateStr (year=$year)")
        return null
    }

    /**
     * Parse date string to timestamp (with full year in date string)
     * Supports: dd/MM/yyyy, yyyy-MM-dd, dd-MM-yyyy, dd.MM.yyyy
     */
    private fun parseDate(dateStr: String): Long? {
        val formats = listOf(
            java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()),
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()),
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()),
            java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
        )

        for (format in formats) {
            try {
                format.isLenient = false
                val date = format.parse(dateStr)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // Try next format
            }
        }

        android.util.Log.w("AdminSync", "Could not parse date: $dateStr")
        return null
    }

    // Race results state
    private val _parsedResults = MutableStateFlow<List<ParsedRaceResult>>(emptyList())
    val parsedResults: StateFlow<List<ParsedRaceResult>> = _parsedResults.asStateFlow()

    /**
     * Parse race results from pasted text
     * Format: Rnk\tRider\tTeam\tUCI\tPnt\tTime
     * Example:
     * 1	 Andresen Tobias Lund
     * Decathlon CMA CGM Team	400	225	4:15:25
     */
    fun parseRaceResults(text: String): List<ParsedRaceResult> {
        val results = mutableListOf<ParsedRaceResult>()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

        android.util.Log.d("RaceResults", "Parsing ${lines.size} lines")

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            // Skip header line
            if (line.startsWith("Rnk") || line.startsWith("Rank")) {
                i++
                continue
            }

            // Check if line starts with a rank number or DNF/DNS
            val rankMatch = Regex("^(\\d+|DNF|DNS)\\s+(.+)$").find(line)

            if (rankMatch != null) {
                val rankStr = rankMatch.groupValues[1]
                val restOfLine = rankMatch.groupValues[2].trim()

                // Parse rank (DNF/DNS = -1)
                val rank = rankStr.toIntOrNull() ?: -1
                val status = if (rank == -1) rankStr else ""

                // The rider name might be on same line or next line might have team
                // Format can be: "Andresen Tobias Lund" followed by team line
                // Or: "Andresen Tobias Lund\nDecathlon CMA CGM Team\t400\t225\t4:15:25"

                var riderName = ""
                var teamName = ""
                var uciPoints = 0
                var fantasyPoints = 0
                var time = ""

                // Check if rest of line contains tabs (meaning data is on same line)
                val parts = restOfLine.split("\t").map { it.trim() }

                if (parts.size >= 4) {
                    // All data on same line: Name\tTeam\tUCI\tPnt\tTime
                    riderName = cleanRiderName(parts[0])
                    teamName = parts.getOrNull(1) ?: ""
                    uciPoints = parts.getOrNull(2)?.toIntOrNull() ?: 0
                    fantasyPoints = parts.getOrNull(3)?.toIntOrNull() ?: 0
                    time = parts.getOrNull(4) ?: ""
                } else {
                    // Rider name on this line, team/points on next line
                    riderName = cleanRiderName(restOfLine)

                    // Look at next line for team and points
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        // Check if next line looks like team + points (doesn't start with number)
                        if (!nextLine.matches(Regex("^\\d+\\s+.*")) && !nextLine.startsWith("DNF") && !nextLine.startsWith("DNS")) {
                            val nextParts = nextLine.split("\t").map { it.trim() }
                            teamName = nextParts.getOrNull(0) ?: ""
                            uciPoints = nextParts.getOrNull(1)?.toIntOrNull() ?: 0
                            fantasyPoints = nextParts.getOrNull(2)?.toIntOrNull() ?: 0
                            time = nextParts.getOrNull(3) ?: ""
                            i++ // Skip the team line
                        }
                    }
                }

                if (riderName.isNotBlank()) {
                    // DNF/DNS = 0 points (no penalty, just no points earned)
                    val finalFantasyPoints = when (status.uppercase()) {
                        "DNF", "DNS" -> 0
                        "DSQ" -> -20  // DSQ_PENALTY - only disqualification has penalty
                        else -> fantasyPoints
                    }

                    results.add(ParsedRaceResult(
                        rank = rank,
                        riderName = riderName,
                        teamName = teamName,
                        uciPoints = uciPoints,
                        fantasyPoints = finalFantasyPoints,
                        time = time,
                        status = status
                    ))
                    android.util.Log.d("RaceResults", "Parsed: #$rank $riderName ($teamName) - UCI: $uciPoints, Fantasy: $finalFantasyPoints, Status: $status")
                }
            }

            i++
        }

        _parsedResults.value = results
        android.util.Log.d("RaceResults", "Total parsed: ${results.size} results")
        return results
    }

    /**
     * Clean rider name - remove flags and extra whitespace
     */
    private fun cleanRiderName(name: String): String {
        return name
            .replace(Regex("^\\s*[A-Z]{3}\\s+"), "") // Remove country code at start
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
    }

    /**
     * Apply race results to update cyclist points in Firestore.
     * Uses the auto-detected current race if available, otherwise searches by name.
     * Also calculates fantasy points for teams and closes the race.
     * @param raceName Name of the race (optional, used for logging if no auto-detected race)
     * @param results List of parsed race results
     */
    fun applyRaceResults(raceName: String = "", results: List<ParsedRaceResult>) {
        viewModelScope.launch {
            if (results.isEmpty()) {
                _uiState.value = AdminSyncUiState.Error("Nenhum resultado para aplicar")
                return@launch
            }

            _uiState.value = AdminSyncUiState.Uploading
            _processedRace.value = null
            _nextRace.value = null
            _fantasyPointsResult.value = null

            // 1. Use the auto-detected current race, or search by name as fallback
            val matchedRace: Race? = _currentRaceToProcess.value?.let { currentRace ->
                if (!currentRace.isFinished) {
                    android.util.Log.d("RaceResults", "Using auto-detected race: ${currentRace.name} (ID: ${currentRace.id})")
                    currentRace
                } else {
                    android.util.Log.w("RaceResults", "Auto-detected race already finished, searching by name...")
                    null
                }
            } ?: run {
                // Fallback: search by name
                if (raceName.isBlank()) {
                    _uiState.value = AdminSyncUiState.Error("Nenhuma corrida detetada. Sincroniza as corridas primeiro.")
                    return@launch
                }
                val upcomingRaces = raceRepository.getUpcomingRaces().first()
                val activeRaces = raceRepository.getActiveRaces().first()
                val allRaces = (activeRaces + upcomingRaces).distinctBy { it.id }

                allRaces.find { race ->
                    race.name.equals(raceName, ignoreCase = true) ||
                    race.name.contains(raceName, ignoreCase = true) ||
                    raceName.contains(race.name, ignoreCase = true)
                }
            }

            if (matchedRace == null) {
                android.util.Log.w("RaceResults", "No race found to process")
                _uiState.value = AdminSyncUiState.Error("Corrida nao encontrada. Sincroniza as corridas primeiro.")
                return@launch
            }

            android.util.Log.d("RaceResults", "Processing race: ${matchedRace.name} (ID: ${matchedRace.id})")
            _processedRace.value = matchedRace

            // 2. Get all cyclists from Firestore
            val cyclistsResult = cyclistFirestoreService.getValidatedCyclistsOnce()
            if (cyclistsResult.isFailure) {
                _uiState.value = AdminSyncUiState.Error("Erro ao buscar ciclistas: ${cyclistsResult.exceptionOrNull()?.message}")
                return@launch
            }

            val cyclists = cyclistsResult.getOrNull() ?: emptyList()
            android.util.Log.d("RaceResults", "Loaded ${cyclists.size} cyclists from Firestore")

            var matchedCount = 0
            var updatedCount = 0
            val domainRaceResults = mutableListOf<RaceResult>()
            val timestamp = System.currentTimeMillis()

            // 3. Match cyclists and create domain RaceResult objects
            for (result in results) {
                val matchedCyclist = findCyclistByName(result.riderName, cyclists)

                if (matchedCyclist != null) {
                    matchedCount++

                    val newTotalPoints = matchedCyclist.totalPoints + result.fantasyPoints

                    // Update cyclist's total points in Firestore
                    val updateResult = cyclistFirestoreService.updateCyclistPoints(
                        cyclistId = matchedCyclist.id,
                        newPoints = newTotalPoints,
                        racePoints = result.fantasyPoints,
                        raceName = matchedRace.name
                    )

                    if (updateResult.isSuccess) {
                        updatedCount++
                        android.util.Log.d("RaceResults", "Updated ${matchedCyclist.fullName}: +${result.fantasyPoints} points (total: $newTotalPoints)")

                        // Create domain RaceResult for Firestore (needed for fantasy points calculation)
                        // DNF/DNS/DSQ have position null (rank = -1 in parsed results)
                        val finalPosition = if (result.rank > 0) result.rank else null

                        domainRaceResults.add(
                            RaceResult(
                                id = UUID.randomUUID().toString(),
                                raceId = matchedRace.id,
                                cyclistId = matchedCyclist.id,
                                stageNumber = null, // One-day race
                                position = finalPosition,
                                points = result.fantasyPoints,
                                bonusPoints = 0,
                                isGcLeader = false,
                                isMountainsLeader = false,
                                isPointsLeader = false,
                                isYoungLeader = false,
                                timestamp = timestamp,
                                status = result.status // DNF, DNS, DSQ or empty
                            )
                        )
                    } else {
                        android.util.Log.e("RaceResults", "Failed to update ${matchedCyclist.fullName}: ${updateResult.exceptionOrNull()?.message}")
                    }
                } else {
                    android.util.Log.w("RaceResults", "No match for rider: ${result.riderName}")
                }
            }

            // 4. Upload race results to Firestore (needed for fantasy points calculation)
            if (domainRaceResults.isNotEmpty()) {
                android.util.Log.d("RaceResults", "Uploading ${domainRaceResults.size} race results to Firestore...")
                val uploadResult = raceFirestoreService.uploadRaceResults(domainRaceResults)
                uploadResult.fold(
                    onSuccess = { count ->
                        android.util.Log.d("RaceResults", "Uploaded $count race results to Firestore")
                    },
                    onFailure = { error ->
                        android.util.Log.e("RaceResults", "Failed to upload race results: ${error.message}")
                    }
                )
            }

            // 5. Calculate fantasy points for teams and close the race
            // Always try to calculate - forceReprocess will handle reprocessing if needed
            android.util.Log.d("RaceResults", "Calculating fantasy points for race: ${matchedRace.id} (wasFinished=${matchedRace.isFinished})")

            val fantasyResult = calculateFantasyPointsUseCase(
                raceId = matchedRace.id,
                forceReprocess = true // Admin always forces reprocess
            )
            fantasyResult.fold(
                onSuccess = { teamPoints ->
                    val teamsWithPoints = teamPoints.filter { it.value > 0 }.size
                    _fantasyPointsResult.value = FantasyPointsResult(
                        teamsUpdated = teamsWithPoints,
                        totalPointsAwarded = teamPoints.values.sum()
                    )
                    android.util.Log.d("RaceResults", "Fantasy points applied: $teamsWithPoints teams, ${teamPoints.values.sum()} total points")
                },
                onFailure = { error ->
                    android.util.Log.e("RaceResults", "Error calculating fantasy points: ${error.message}")
                }
            )

            // 5. Find the next race and update current race to process
            val refreshedRaces = raceRepository.getUpcomingRaces().first()
            val nextUpcomingRace = refreshedRaces
                .filter { !it.isFinished && it.id != matchedRace?.id }
                .minByOrNull { it.startDate }

            if (nextUpcomingRace != null) {
                _nextRace.value = nextUpcomingRace
                _currentRaceToProcess.value = nextUpcomingRace
                android.util.Log.d("RaceResults", "Next race: ${nextUpcomingRace.name} (${nextUpcomingRace.formattedStartDate})")
            } else {
                _currentRaceToProcess.value = null
            }

            _parsedResults.value = emptyList()

            if (updatedCount > 0 || matchedRace != null) {
                val message = buildString {
                    append("$updatedCount ciclistas atualizados")
                    if (matchedRace != null) {
                        append(". Corrida '${matchedRace.name}' encerrada")
                    }
                }
                android.util.Log.d("RaceResults", message)
                _uiState.value = AdminSyncUiState.UploadComplete(updatedCount)
                loadSyncStatus()
            } else {
                _uiState.value = AdminSyncUiState.Error(
                    "Nenhum ciclista atualizado. Matched: $matchedCount/${results.size}"
                )
            }
        }
    }

    /**
     * Find cyclist by name (fuzzy matching)
     */
    private fun findCyclistByName(riderName: String, cyclists: List<Cyclist>): Cyclist? {
        val normalizedName = normalizeForMatching(riderName)

        // Strategy 1: Exact full name match
        cyclists.find {
            normalizeForMatching(it.fullName) == normalizedName
        }?.let { return it }

        // Strategy 2: lastName firstName match
        cyclists.find {
            normalizeForMatching("${it.lastName} ${it.firstName}") == normalizedName
        }?.let { return it }

        // Strategy 3: Partial match (both parts present)
        cyclists.find {
            val firstName = normalizeForMatching(it.firstName)
            val lastName = normalizeForMatching(it.lastName)
            firstName.length > 2 && lastName.length > 2 &&
            normalizedName.contains(firstName) && normalizedName.contains(lastName)
        }?.let { return it }

        // Strategy 4: Last name only (if unique)
        val lastNameMatches = cyclists.filter {
            val lastName = normalizeForMatching(it.lastName)
            lastName.length > 3 && normalizedName.contains(lastName)
        }
        if (lastNameMatches.size == 1) {
            return lastNameMatches[0]
        }

        return null
    }

    /**
     * Clear parsed race results
     */
    fun clearRaceResults() {
        _parsedResults.value = emptyList()
    }

    /**
     * Generate WorldTour races for a given year
     * The dates are approximate based on typical UCI calendar patterns
     * @param year The year to generate races for
     */
    private fun generateWorldTourRaces(year: Int): List<Race> {
        val races = mutableListOf<Race>()
        val now = System.currentTimeMillis()

        // Helper function to create timestamp
        fun createDate(y: Int, month: Int, day: Int): Long {
            val cal = Calendar.getInstance()
            cal.set(y, month - 1, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        // Helper to check if race should be active (hasn't ended yet)
        fun isRaceActive(endDate: Long?): Boolean = (endDate ?: now) >= now

        // Grand Tours (dates are approximate, typically same period each year)
        val giroEnd = createDate(year, 6, 1)
        races.add(Race(
            id = "giro-$year",
            name = "Giro d'Italia",
            type = RaceType.GRAND_TOUR,
            startDate = createDate(year, 5, 9),
            endDate = giroEnd,
            stages = 21,
            country = "Italia",
            category = "GT",
            isActive = isRaceActive(giroEnd)
        ))

        val tourEnd = createDate(year, 7, 27)
        races.add(Race(
            id = "tour-$year",
            name = "Tour de France",
            type = RaceType.GRAND_TOUR,
            startDate = createDate(year, 7, 5),
            endDate = tourEnd,
            stages = 21,
            country = "Franca",
            category = "GT",
            isActive = isRaceActive(tourEnd)
        ))

        val vueltaEnd = createDate(year, 9, 14)
        races.add(Race(
            id = "vuelta-$year",
            name = "La Vuelta a Espana",
            type = RaceType.GRAND_TOUR,
            startDate = createDate(year, 8, 23),
            endDate = vueltaEnd,
            stages = 21,
            country = "Espanha",
            category = "GT",
            isActive = isRaceActive(vueltaEnd)
        ))

        // Spring Classics (Monument races) - one-day races use startDate as endDate
        val milanoDate = createDate(year, 3, 22)
        races.add(Race(
            id = "milano-sanremo-$year",
            name = "Milano-Sanremo",
            type = RaceType.ONE_DAY,
            startDate = milanoDate,
            endDate = null,
            stages = 1,
            country = "Italia",
            category = "WT",
            isActive = isRaceActive(milanoDate)
        ))

        val rondeDate = createDate(year, 4, 6)
        races.add(Race(
            id = "ronde-$year",
            name = "Ronde van Vlaanderen",
            type = RaceType.ONE_DAY,
            startDate = rondeDate,
            endDate = null,
            stages = 1,
            country = "Belgica",
            category = "WT",
            isActive = isRaceActive(rondeDate)
        ))

        val roubaixDate = createDate(year, 4, 13)
        races.add(Race(
            id = "paris-roubaix-$year",
            name = "Paris-Roubaix",
            type = RaceType.ONE_DAY,
            startDate = roubaixDate,
            endDate = null,
            stages = 1,
            country = "Franca",
            category = "WT",
            isActive = isRaceActive(roubaixDate)
        ))

        val liegeDate = createDate(year, 4, 27)
        races.add(Race(
            id = "liege-$year",
            name = "Liege-Bastogne-Liege",
            type = RaceType.ONE_DAY,
            startDate = liegeDate,
            endDate = null,
            stages = 1,
            country = "Belgica",
            category = "WT",
            isActive = isRaceActive(liegeDate)
        ))

        // Stage Races
        // UAE Tour (February - first big stage race of the season)
        val uaeEnd = createDate(year, 2, 23)
        races.add(Race(
            id = "uae-tour-$year",
            name = "UAE Tour",
            type = RaceType.STAGE_RACE,
            startDate = createDate(year, 2, 17),
            endDate = uaeEnd,
            stages = 7,
            country = "Emirados Arabes Unidos",
            category = "WT",
            isActive = isRaceActive(uaeEnd)
        ))

        val tirrenoEnd = createDate(year, 3, 16)
        races.add(Race(
            id = "tirreno-$year",
            name = "Tirreno-Adriatico",
            type = RaceType.STAGE_RACE,
            startDate = createDate(year, 3, 10),
            endDate = tirrenoEnd,
            stages = 7,
            country = "Italia",
            category = "WT",
            isActive = isRaceActive(tirrenoEnd)
        ))

        val parisNiceEnd = createDate(year, 3, 16)
        races.add(Race(
            id = "paris-nice-$year",
            name = "Paris-Nice",
            type = RaceType.STAGE_RACE,
            startDate = createDate(year, 3, 9),
            endDate = parisNiceEnd,
            stages = 8,
            country = "Franca",
            category = "WT",
            isActive = isRaceActive(parisNiceEnd)
        ))

        val catalunyaEnd = createDate(year, 3, 30)
        races.add(Race(
            id = "volta-catalunya-$year",
            name = "Volta a Catalunya",
            type = RaceType.STAGE_RACE,
            startDate = createDate(year, 3, 24),
            endDate = catalunyaEnd,
            stages = 7,
            country = "Espanha",
            category = "WT",
            isActive = isRaceActive(catalunyaEnd)
        ))

        val suisseEnd = createDate(year, 6, 15)
        races.add(Race(
            id = "tour-suisse-$year",
            name = "Tour de Suisse",
            type = RaceType.STAGE_RACE,
            startDate = createDate(year, 6, 8),
            endDate = suisseEnd,
            stages = 8,
            country = "Suica",
            category = "WT",
            isActive = isRaceActive(suisseEnd)
        ))

        val dauphineEnd = createDate(year, 6, 15)
        races.add(Race(
            id = "dauphine-$year",
            name = "Criterium du Dauphine",
            type = RaceType.STAGE_RACE,
            startDate = createDate(year, 6, 8),
            endDate = dauphineEnd,
            stages = 8,
            country = "Franca",
            category = "WT",
            isActive = isRaceActive(dauphineEnd)
        ))

        // Fall Classics
        val lombardiaDate = createDate(year, 10, 11)
        races.add(Race(
            id = "lombardia-$year",
            name = "Il Lombardia",
            type = RaceType.ONE_DAY,
            startDate = lombardiaDate,
            endDate = null,
            stages = 1,
            country = "Italia",
            category = "WT",
            isActive = isRaceActive(lombardiaDate)
        ))

        // Volta a Portugal (special for Portuguese app)
        val voltaEnd = createDate(year, 8, 17)
        races.add(Race(
            id = "volta-portugal-$year",
            name = "Volta a Portugal",
            type = RaceType.STAGE_RACE,
            startDate = createDate(year, 8, 6),
            endDate = voltaEnd,
            stages = 10,
            country = "Portugal",
            category = "2.1",
            isActive = isRaceActive(voltaEnd)
        ))

        return races
    }

    /**
     * Generate stages for UAE Tour
     * Can be called after uploading WorldTour races to also upload stages
     */
    fun uploadUaeTourStages(year: Int = Calendar.getInstance().get(Calendar.YEAR)) {
        viewModelScope.launch {
            _uiState.value = AdminSyncUiState.Uploading

            val raceId = "uae-tour-$year"
            val stages = generateUaeTourStages(year, raceId)

            android.util.Log.d("AdminSync", "Uploading ${stages.size} UAE Tour stages for $year...")

            val result = stageFirestoreService.saveStageSchedule(stages)
            result.fold(
                onSuccess = { count ->
                    android.util.Log.d("AdminSync", "Uploaded $count UAE Tour stages")
                    _uiState.value = AdminSyncUiState.UploadComplete(count)
                },
                onFailure = { error ->
                    android.util.Log.e("AdminSync", "Error uploading UAE Tour stages: ${error.message}")
                    _uiState.value = AdminSyncUiState.Error("Erro ao carregar etapas UAE Tour: ${error.message}")
                }
            )
        }
    }

    /**
     * Generate typical UAE Tour stage schedule
     * Based on typical race format: 7 stages including 1 ITT
     */
    private fun generateUaeTourStages(year: Int, raceId: String): List<Stage> {
        val stages = mutableListOf<Stage>()
        val season = _currentSeason.value

        // Helper to create date
        fun createDate(y: Int, month: Int, day: Int): Long {
            val cal = Calendar.getInstance()
            cal.set(y, month - 1, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        // UAE Tour 2026 typical schedule (Feb 17-23)
        val uaeStages = listOf(
            Triple(1, StageType.ITT, "Al Dhafra - Al Dhafra" to 9.0),
            Triple(2, StageType.FLAT, "Al Dhafra - Abu Dhabi" to 150.0),
            Triple(3, StageType.HILLY, "Abu Dhabi - Jebel Hafeet" to 170.0),
            Triple(4, StageType.FLAT, "Dubai - Dubai" to 140.0),
            Triple(5, StageType.HILLY, "Sharjah - Khor Fakkan" to 175.0),
            Triple(6, StageType.FLAT, "Ajman - Fujairah" to 165.0),
            Triple(7, StageType.HILLY, "Al Ain - Jebel Hafeet" to 125.0)
        )

        val baseDate = createDate(year, 2, 17) // February 17
        val dayNames = listOf("Segunda", "Terca", "Quarta", "Quinta", "Sexta", "Sabado", "Domingo")

        uaeStages.forEachIndexed { index, (stageNum, stageType, routeInfo) ->
            val (route, distance) = routeInfo
            val locations = route.split(" - ")
            val stageDate = baseDate + (index * 24 * 60 * 60 * 1000L)
            val cal = Calendar.getInstance().apply { timeInMillis = stageDate }
            val dayOfWeek = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val dateStr = String.format("%02d/%02d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)

            stages.add(
                Stage(
                    id = "${raceId}_stage_$stageNum",
                    raceId = raceId,
                    stageNumber = stageNum,
                    stageType = stageType,
                    name = "Etapa $stageNum",
                    distance = distance,
                    startLocation = locations.getOrNull(0)?.trim() ?: "",
                    finishLocation = locations.getOrNull(1)?.trim() ?: "",
                    date = stageDate,
                    dateString = dateStr,
                    dayOfWeek = dayOfWeek,
                    isRestDayAfter = false,
                    isProcessed = false,
                    season = season
                )
            )
        }

        return stages
    }

    // ========== VIDEO SYNC ==========

    private val _videoSyncState = MutableStateFlow<VideoSyncState>(VideoSyncState.Idle)
    val videoSyncState: StateFlow<VideoSyncState> = _videoSyncState.asStateFlow()

    /**
     * Force sync videos to Firestore immediately
     */
    fun forceVideoSync() {
        viewModelScope.launch {
            _videoSyncState.value = VideoSyncState.Syncing
            android.util.Log.d("AdminSync", "Starting manual video sync...")

            try {
                val result = cyclingVideosRepository.syncVideosToFirestore()
                result.fold(
                    onSuccess = { count ->
                        android.util.Log.d("AdminSync", "Video sync complete: $count videos")
                        _videoSyncState.value = VideoSyncState.Success(count)
                    },
                    onFailure = { error ->
                        android.util.Log.e("AdminSync", "Video sync failed: ${error.message}")
                        _videoSyncState.value = VideoSyncState.Error(error.message ?: "Erro desconhecido")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("AdminSync", "Video sync exception: ${e.message}")
                _videoSyncState.value = VideoSyncState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun resetVideoSyncState() {
        _videoSyncState.value = VideoSyncState.Idle
    }

    // =====================================================
    // WEBSITE RACE SCRAPING - Add races from any URL
    // =====================================================

    private val _scrapedRaceData = MutableStateFlow<ScrapedRaceData?>(null)
    val scrapedRaceData: StateFlow<ScrapedRaceData?> = _scrapedRaceData.asStateFlow()

    private val _websiteScrapingState = MutableStateFlow<WebsiteScrapingState>(WebsiteScrapingState.Idle)
    val websiteScrapingState: StateFlow<WebsiteScrapingState> = _websiteScrapingState.asStateFlow()

    /**
     * Scrape race data from a website URL.
     * The result can be reviewed and edited by admin before saving.
     */
    fun scrapeRaceFromWebsite(url: String) {
        viewModelScope.launch {
            if (url.isBlank()) {
                _websiteScrapingState.value = WebsiteScrapingState.Error("URL inválido")
                return@launch
            }

            _websiteScrapingState.value = WebsiteScrapingState.Scraping

            genericRaceScraper.scrapeFromUrl(url).fold(
                onSuccess = { data ->
                    _scrapedRaceData.value = data
                    _websiteScrapingState.value = WebsiteScrapingState.Success(data)
                },
                onFailure = { error ->
                    _websiteScrapingState.value = WebsiteScrapingState.Error(
                        error.message ?: "Erro ao extrair dados do website"
                    )
                }
            )
        }
    }

    /**
     * Update scraped race data fields (admin can edit before saving)
     */
    fun updateScrapedRaceData(
        nome: String? = null,
        data: Long? = null,
        local: String? = null,
        tipo: String? = null,
        distancias: String? = null,
        preco: String? = null,
        descricao: String? = null
    ) {
        val current = _scrapedRaceData.value ?: return
        _scrapedRaceData.value = current.copy(
            nome = nome ?: current.nome,
            data = data ?: current.data,
            local = local ?: current.local,
            tipo = tipo ?: current.tipo,
            distancias = distancias ?: current.distancias,
            preco = preco ?: current.preco,
            descricao = descricao ?: current.descricao
        )
    }

    /**
     * Save the scraped race to the database
     */
    fun saveScrapedRace() {
        viewModelScope.launch {
            val data = _scrapedRaceData.value
            if (data == null) {
                _websiteScrapingState.value = WebsiteScrapingState.Error("Nenhuma prova para guardar")
                return@launch
            }

            if (data.nome.isBlank()) {
                _websiteScrapingState.value = WebsiteScrapingState.Error("Nome da prova é obrigatório")
                return@launch
            }

            if (data.data == 0L) {
                _websiteScrapingState.value = WebsiteScrapingState.Error("Data da prova é obrigatória")
                return@launch
            }

            _websiteScrapingState.value = WebsiteScrapingState.Saving

            try {
                val prova = data.toDomain()
                provaRepository.insertProva(prova)
                _websiteScrapingState.value = WebsiteScrapingState.Saved(data.nome)
                _scrapedRaceData.value = null
            } catch (e: Exception) {
                _websiteScrapingState.value = WebsiteScrapingState.Error(
                    "Erro ao guardar: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear scraped race data and reset state
     */
    fun clearScrapedRace() {
        _scrapedRaceData.value = null
        _websiteScrapingState.value = WebsiteScrapingState.Idle
    }

    // ========== Bot Teams Generation ==========

    private val _botTeamState = MutableStateFlow<BotTeamState>(BotTeamState.Idle)
    val botTeamState: StateFlow<BotTeamState> = _botTeamState.asStateFlow()

    private val _botTeamCount = MutableStateFlow(0)
    val botTeamCount: StateFlow<Int> = _botTeamCount.asStateFlow()

    init {
        // Load initial bot team count
        viewModelScope.launch {
            _botTeamCount.value = botTeamService.getBotTeamCount()
        }
    }

    /**
     * Generate 236 bot teams for competition
     */
    fun generateBotTeams() {
        viewModelScope.launch {
            _botTeamState.value = BotTeamState.Generating(0, "A iniciar...")

            botTeamService.generateBotTeams { progress, message ->
                _botTeamState.value = BotTeamState.Generating(progress, message)
            }.onSuccess { count ->
                _botTeamState.value = BotTeamState.Success(count)
                _botTeamCount.value = botTeamService.getBotTeamCount()
            }.onFailure { error ->
                _botTeamState.value = BotTeamState.Error(error.message ?: "Erro desconhecido")
            }
        }
    }

    /**
     * Delete all bot teams
     */
    fun deleteBotTeams() {
        viewModelScope.launch {
            _botTeamState.value = BotTeamState.Deleting

            botTeamService.deleteBotTeams()
                .onSuccess { count ->
                    _botTeamState.value = BotTeamState.Deleted(count)
                    _botTeamCount.value = 0
                }
                .onFailure { error ->
                    _botTeamState.value = BotTeamState.Error(error.message ?: "Erro ao apagar bots")
                }
        }
    }

    /**
     * Refresh bot team count
     */
    fun refreshBotTeamCount() {
        viewModelScope.launch {
            _botTeamCount.value = botTeamService.getBotTeamCount()
        }
    }

    /**
     * Reset bot state to idle
     */
    fun resetBotState() {
        _botTeamState.value = BotTeamState.Idle
    }

    // ========== AI Quota Management ==========

    private val _aiUsageStats = MutableStateFlow<AiUsageStats?>(null)
    val aiUsageStats: StateFlow<AiUsageStats?> = _aiUsageStats.asStateFlow()

    private val _aiQuotaResetState = MutableStateFlow<AiQuotaResetState>(AiQuotaResetState.Idle)
    val aiQuotaResetState: StateFlow<AiQuotaResetState> = _aiQuotaResetState.asStateFlow()

    /**
     * Load AI usage stats
     */
    fun loadAiUsageStats() {
        viewModelScope.launch {
            try {
                _aiUsageStats.value = aiService.getUsageStats()
            } catch (e: Exception) {
                android.util.Log.e("AdminSyncViewModel", "Error loading AI stats: ${e.message}")
            }
        }
    }

    /**
     * Reset AI daily quota (for debugging)
     */
    fun resetAiQuota() {
        viewModelScope.launch {
            try {
                _aiQuotaResetState.value = AiQuotaResetState.Resetting
                aiService.resetQuota()
                _aiUsageStats.value = aiService.getUsageStats()
                _aiQuotaResetState.value = AiQuotaResetState.Success
            } catch (e: Exception) {
                _aiQuotaResetState.value = AiQuotaResetState.Error(e.message ?: "Erro ao repor quota")
            }
        }
    }

    /**
     * Reset AI quota state to idle
     */
    fun resetAiQuotaState() {
        _aiQuotaResetState.value = AiQuotaResetState.Idle
    }

    // ========== Fake Teams Management ==========

    private val _fakeTeamsState = MutableStateFlow<FakeTeamsState>(FakeTeamsState.Idle)
    val fakeTeamsState: StateFlow<FakeTeamsState> = _fakeTeamsState.asStateFlow()

    /**
     * Add fake teams to the global Liga Portugal league for testing
     */
    fun addFakeTeamsToGlobalLeague(count: Int = 236) {
        viewModelScope.launch {
            try {
                _fakeTeamsState.value = FakeTeamsState.Processing("A criar $count equipas de teste...")

                val result = leagueFirestoreService.addFakeTeamsToGlobalLeague(count)

                result.fold(
                    onSuccess = { addedCount ->
                        _fakeTeamsState.value = FakeTeamsState.Success("$addedCount equipas de teste criadas!")
                    },
                    onFailure = { e ->
                        _fakeTeamsState.value = FakeTeamsState.Error(e.message ?: "Erro ao criar equipas")
                    }
                )
            } catch (e: Exception) {
                _fakeTeamsState.value = FakeTeamsState.Error(e.message ?: "Erro ao criar equipas")
            }
        }
    }

    /**
     * Remove all fake teams from the global Liga Portugal league
     */
    fun removeFakeTeamsFromGlobalLeague() {
        viewModelScope.launch {
            try {
                _fakeTeamsState.value = FakeTeamsState.Processing("A remover equipas de teste...")

                val result = leagueFirestoreService.removeFakeTeamsFromGlobalLeague()

                result.fold(
                    onSuccess = { removedCount ->
                        _fakeTeamsState.value = FakeTeamsState.Success("$removedCount equipas de teste removidas!")
                    },
                    onFailure = { e ->
                        _fakeTeamsState.value = FakeTeamsState.Error(e.message ?: "Erro ao remover equipas")
                    }
                )
            } catch (e: Exception) {
                _fakeTeamsState.value = FakeTeamsState.Error(e.message ?: "Erro ao remover equipas")
            }
        }
    }

    /**
     * Reset fake teams state to idle
     */
    fun resetFakeTeamsState() {
        _fakeTeamsState.value = FakeTeamsState.Idle
    }
}

sealed class FakeTeamsState {
    object Idle : FakeTeamsState()
    data class Processing(val message: String) : FakeTeamsState()
    data class Success(val message: String) : FakeTeamsState()
    data class Error(val message: String) : FakeTeamsState()
}

sealed class VideoSyncState {
    object Idle : VideoSyncState()
    object Syncing : VideoSyncState()
    data class Success(val count: Int) : VideoSyncState()
    data class Error(val message: String) : VideoSyncState()
}

sealed class TeamResetState {
    object Idle : TeamResetState()
    object Deleting : TeamResetState()
    data class Success(val teamsDeleted: Int, val membersCleared: Int) : TeamResetState()
    data class Error(val message: String) : TeamResetState()
}

sealed class BotTeamState {
    object Idle : BotTeamState()
    data class Generating(val progress: Int, val message: String) : BotTeamState()
    object Deleting : BotTeamState()
    data class Success(val count: Int) : BotTeamState()
    data class Deleted(val count: Int) : BotTeamState()
    data class Error(val message: String) : BotTeamState()
}

sealed class AiQuotaResetState {
    object Idle : AiQuotaResetState()
    object Resetting : AiQuotaResetState()
    object Success : AiQuotaResetState()
    data class Error(val message: String) : AiQuotaResetState()
}

sealed class WebsiteScrapingState {
    object Idle : WebsiteScrapingState()
    object Scraping : WebsiteScrapingState()
    data class Success(val data: ScrapedRaceData) : WebsiteScrapingState()
    object Saving : WebsiteScrapingState()
    data class Saved(val raceName: String) : WebsiteScrapingState()
    data class Error(val message: String) : WebsiteScrapingState()
}

data class ScrapingProgress(
    val currentTeam: String,
    val currentIndex: Int,
    val totalTeams: Int
)

sealed class AdminSyncUiState {
    object Initial : AdminSyncUiState()
    object Scraping : AdminSyncUiState()
    data class ScrapingComplete(val count: Int) : AdminSyncUiState()
    object Uploading : AdminSyncUiState()
    data class UploadComplete(val count: Int) : AdminSyncUiState()
    object UploadingPhotos : AdminSyncUiState()
    data class PhotoUploadComplete(val uploaded: Int, val matched: Int, val total: Int) : AdminSyncUiState()
    object Validating : AdminSyncUiState()
    data class ValidationComplete(val count: Int) : AdminSyncUiState()
    object Deleting : AdminSyncUiState()
    object DeleteComplete : AdminSyncUiState()
    data class Error(val message: String) : AdminSyncUiState()
}

data class UploadProgress(
    val uploaded: Int,
    val total: Int,
    val currentName: String,
    val batchNumber: Int = 0,
    val totalBatches: Int = 0
) {
    val percentage: Float get() = if (total > 0) uploaded.toFloat() / total else 0f
    val displayText: String get() = "$uploaded / $total"
    val batchText: String get() = if (totalBatches > 0) "Batch $batchNumber/$totalBatches" else ""
}

data class PhotoUploadProgress(
    val uploaded: Int,
    val total: Int,
    val currentName: String
) {
    val percentage: Float get() = if (total > 0) uploaded.toFloat() / total else 0f
    val displayText: String get() = "$uploaded / $total"
}

/**
 * Represents a parsed race result from admin input (not the same as domain RaceResult)
 */
data class ParsedRaceResult(
    val rank: Int,
    val riderName: String,
    val teamName: String,
    val uciPoints: Int,
    val fantasyPoints: Int,
    val time: String,
    val status: String = "" // DNF, DNS, etc
)

/**
 * Result of fantasy points calculation after applying race results
 */
data class FantasyPointsResult(
    val teamsUpdated: Int,
    val totalPointsAwarded: Int
)

/**
 * Result of stage processing
 */
data class StageProcessingResult(
    val stageNumber: Int,
    val stageType: StageType,
    val resultsCount: Int,
    val pointsAwarded: Int,
    val gcLeader: String?,
    val pointsLeader: String?,
    val mountainsLeader: String?,
    val youngLeader: String?
)

/**
 * Current jersey holders information
 */
data class JerseyHoldersInfo(
    val gcLeaderName: String?,
    val gcLeaderId: String?,
    val pointsLeaderName: String?,
    val pointsLeaderId: String?,
    val mountainsLeaderName: String?,
    val mountainsLeaderId: String?,
    val youngLeaderName: String?,
    val youngLeaderId: String?
)

/**
 * Parsed stage result from admin input
 */
data class ParsedStageResult(
    val position: Int?,
    val riderName: String,
    val teamName: String,
    val time: String,
    val points: Int,
    val status: String = "", // DNF, DNS, DSQ, OTL
    val isGcLeader: Boolean = false,
    val isMountainsLeader: Boolean = false,
    val isPointsLeader: Boolean = false,
    val isYoungLeader: Boolean = false
)

/**
 * Result of stage schedule import
 */
data class StageScheduleImportResult(
    val success: Boolean,
    val message: String,
    val stageCount: Int = 0
)

/**
 * Admin user information
 */
data class AdminInfo(
    val uid: String,
    val email: String,
    val addedBy: String,
    val addedAt: Long
)
