package com.ciclismo.portugal.presentation.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ciclismo.portugal.data.local.ai.AiActionExecutor
import com.ciclismo.portugal.data.local.ai.AiCapabilities
import com.ciclismo.portugal.data.local.ai.AiCapabilityChecker
import com.ciclismo.portugal.data.local.ai.AiMode
import com.ciclismo.portugal.data.local.ai.AiPromptTemplates
import com.ciclismo.portugal.data.local.ai.AiService
import com.ciclismo.portugal.data.remote.firebase.AuthService
import com.ciclismo.portugal.domain.model.AiAction
import com.ciclismo.portugal.domain.model.AiActionResult
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.Race
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.repository.RaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val aiService: AiService,
    private val capabilityChecker: AiCapabilityChecker,
    private val fantasyTeamRepository: FantasyTeamRepository,
    private val raceRepository: RaceRepository,
    private val authService: AuthService,
    private val actionExecutor: AiActionExecutor
) : ViewModel() {

    companion object {
        private const val TAG = "AiAssistantVM"
    }

    private val _uiState = MutableStateFlow<AiAssistantUiState>(AiAssistantUiState.Idle)
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _capabilities = MutableStateFlow<AiCapabilities?>(null)
    val capabilities: StateFlow<AiCapabilities?> = _capabilities.asStateFlow()

    private val _quickSuggestions = MutableStateFlow<List<String>>(emptyList())
    val quickSuggestions: StateFlow<List<String>> = _quickSuggestions.asStateFlow()

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    // Pending actions from AI suggestions
    private val _pendingActions = MutableStateFlow<List<AiAction>>(emptyList())
    val pendingActions: StateFlow<List<AiAction>> = _pendingActions.asStateFlow()

    // Navigation events from action execution
    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    // Current user/team context
    private var currentUserId: String? = null
    private var currentTeamId: String? = null
    private var currentTeam: FantasyTeam? = null
    private var currentScreen: String = "Home"

    // Track if user context is being loaded to avoid false "need to sign in" errors
    private val _isContextLoading = MutableStateFlow(true)
    val isContextLoading: StateFlow<Boolean> = _isContextLoading.asStateFlow()

    init {
        loadCapabilities()
        loadQuickSuggestions()
        initializeAi()
        loadUserContext()
    }

    private fun loadUserContext() {
        viewModelScope.launch {
            try {
                _isContextLoading.value = true
                Log.d(TAG, "Loading user context...")

                authService.getCurrentUser()?.let { user ->
                    Log.d(TAG, "User found: ${user.id}, email: ${user.email}")
                    currentUserId = user.id
                    fantasyTeamRepository.getTeamByUserId(user.id).first()?.let { team ->
                        currentTeamId = team.id
                        currentTeam = team
                        Log.d(TAG, "Team loaded: ${team.teamName}")
                    } ?: run {
                        Log.w(TAG, "No team found for user ${user.id}")
                    }
                } ?: run {
                    Log.w(TAG, "No authenticated user found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user context: ${e.message}", e)
            } finally {
                _isContextLoading.value = false
                Log.d(TAG, "User context loaded. UserId: $currentUserId, TeamId: $currentTeamId")
            }
        }
    }

    private fun loadCapabilities() {
        _capabilities.value = capabilityChecker.getAiCapabilities()
    }

    private fun loadQuickSuggestions() {
        _quickSuggestions.value = aiService.getQuickSuggestions()
    }

    private fun initializeAi() {
        viewModelScope.launch {
            aiService.initialize()
        }
    }

    /**
     * Envia uma mensagem para o assistente.
     * Usa chatWithActions para suportar acoes executaveis.
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            // Wait for context to load before processing
            if (_isContextLoading.value) {
                Log.d(TAG, "Context still loading, waiting...")
                addSystemMessage("A carregar contexto do utilizador, aguarda um momento...")
                return@launch
            }

            // Adiciona mensagem do utilizador
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = message,
                isUser = true,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = _messages.value + userMessage

            // Clear previous pending actions
            _pendingActions.value = emptyList()

            // Mostra estado de loading
            _uiState.value = AiAssistantUiState.Loading

            // Build user context for better AI responses
            val userContext = buildUserContext()
            val conversationContext = buildConversationContext()

            // Gera resposta com suporte a acoes
            aiService.chatWithActions(message, conversationContext, userContext)
                .onSuccess { response ->
                    Log.d(TAG, "========== AI RESPONSE RECEIVED ==========")
                    Log.d(TAG, "Message length: ${response.message.length} chars")
                    Log.d(TAG, "Actions count: ${response.actions.size}")

                    response.actions.forEachIndexed { index, action ->
                        Log.d(TAG, "Action $index: ${action.type} - ${action.title}")
                    }

                    val aiMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = response.message,
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        actions = response.actions
                    )
                    _messages.value = _messages.value + aiMessage

                    // Set pending actions for user approval
                    if (response.actions.isNotEmpty()) {
                        _pendingActions.value = response.actions
                        Log.d(TAG, "✅ Set ${response.actions.size} pending actions for user approval")
                    } else {
                        Log.w(TAG, "⚠️ No actions in AI response - buttons won't appear!")
                    }

                    _uiState.value = AiAssistantUiState.Idle
                }
                .onFailure { error ->
                    val errorMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = "Desculpa, ocorreu um erro: ${error.message}",
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        isError = true
                    )
                    _messages.value = _messages.value + errorMessage
                    _uiState.value = AiAssistantUiState.Error(error.message ?: "Erro desconhecido")
                }
        }
    }

    /**
     * Execute an AI-suggested action after user approval.
     */
    fun executeAction(action: AiAction) {
        viewModelScope.launch {
            // Wait for context to load before checking auth
            if (_isContextLoading.value) {
                Log.d(TAG, "Context still loading, cannot execute action yet")
                addSystemMessage("A carregar contexto do utilizador, aguarda um momento...")
                return@launch
            }

            val userId = currentUserId
            if (userId == null) {
                addSystemMessage("Precisas de fazer login para executar esta acao")
                return@launch
            }

            _uiState.value = AiAssistantUiState.Loading
            Log.d(TAG, "Executing action: ${action.type} - ${action.title}")

            val result = actionExecutor.executeAction(action, userId, currentTeamId)

            when (result) {
                is AiActionResult.Success -> {
                    addSystemMessage("✅ ${result.message}")
                    // Remove executed action from pending
                    _pendingActions.value = _pendingActions.value.filter { it.id != action.id }
                    // Refresh team context
                    loadUserContext()
                }
                is AiActionResult.Error -> {
                    addSystemMessage("❌ ${result.message}")
                }
                is AiActionResult.RequiresAuth -> {
                    addSystemMessage("🔐 ${result.message}")
                }
                is AiActionResult.NavigateTo -> {
                    _navigationEvent.emit(result.route)
                    // Remove action from pending
                    _pendingActions.value = _pendingActions.value.filter { it.id != action.id }
                }
            }

            _uiState.value = AiAssistantUiState.Idle
        }
    }

    /**
     * Dismiss a pending action (user rejected it).
     */
    fun dismissAction(action: AiAction) {
        _pendingActions.value = _pendingActions.value.filter { it.id != action.id }
        addSystemMessage("Acao \"${action.title}\" ignorada")
    }

    /**
     * Clear all pending actions.
     */
    fun clearPendingActions() {
        _pendingActions.value = emptyList()
    }

    /**
     * Build user context for AI personalization.
     */
    private suspend fun buildUserContext(): AiPromptTemplates.UserContext? {
        val team = currentTeam ?: return null

        // Get team cyclists for captain info
        val cyclistsWithDetails = fantasyTeamRepository.getTeamCyclistsWithDetails(team.id).first()
        val captain = cyclistsWithDetails.find { it.first.isCaptain }?.second
        val activeCyclists = cyclistsWithDetails.filter { it.first.isActive }

        // Get next race
        val nextRace = try {
            raceRepository.getUpcomingRaces().first().minByOrNull { it.startDate }
        } catch (e: Exception) {
            null
        }

        return AiPromptTemplates.UserContext(
            teamName = team.teamName,
            budget = team.budget,
            totalPoints = team.totalPoints,
            activeCyclistCount = activeCyclists.size,
            captainName = captain?.fullName,
            nextRaceName = nextRace?.name,
            nextRaceId = nextRace?.id
        )
    }

    private fun addSystemMessage(content: String) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isSystemMessage = true
        )
        _messages.value = _messages.value + message
    }

    /**
     * Analisa a equipa do utilizador.
     */
    fun analyzeTeam() {
        viewModelScope.launch {
            val user = authService.getCurrentUser() ?: return@launch
            val team = fantasyTeamRepository.getTeamByUserId(user.id).first() ?: return@launch

            _uiState.value = AiAssistantUiState.Loading

            // Obter ciclistas da equipa
            val cyclistsWithDetails = fantasyTeamRepository.getTeamCyclistsWithDetails(team.id).first()
            val cyclists = cyclistsWithDetails.map { it.second }
            val activeCyclists = cyclistsWithDetails.filter { it.first.isActive }.map { it.second }
            val captain = cyclistsWithDetails.find { it.first.isCaptain }?.second

            aiService.getTeamAnalysis(team, cyclists, activeCyclists, captain)
                .onSuccess { response ->
                    addAiMessage(response)
                    _uiState.value = AiAssistantUiState.Idle
                }
                .onFailure { error ->
                    addErrorMessage(error.message ?: "Erro ao analisar equipa")
                    _uiState.value = AiAssistantUiState.Error(error.message ?: "Erro")
                }
        }
    }

    /**
     * Obtem recomendacoes de transferencias.
     */
    fun getTransferRecommendations() {
        viewModelScope.launch {
            val user = authService.getCurrentUser() ?: return@launch
            val team = fantasyTeamRepository.getTeamByUserId(user.id).first() ?: return@launch

            _uiState.value = AiAssistantUiState.Loading

            // Obter dados
            val cyclistsWithDetails = fantasyTeamRepository.getTeamCyclistsWithDetails(team.id).first()
            val teamCyclists = cyclistsWithDetails.map { it.second }

            // Obter proxima corrida
            val nextRace = raceRepository.getUpcomingRaces().first().minByOrNull { it.startDate }

            // TODO: Obter ciclistas disponiveis do mercado
            val availableCyclists = emptyList<Cyclist>()

            aiService.getTransferRecommendations(
                team = team,
                teamCyclists = teamCyclists,
                availableCyclists = availableCyclists,
                nextRace = nextRace,
                budget = team.budget
            )
                .onSuccess { response ->
                    addAiMessage(response)
                    _uiState.value = AiAssistantUiState.Idle
                }
                .onFailure { error ->
                    addErrorMessage(error.message ?: "Erro ao obter recomendacoes")
                    _uiState.value = AiAssistantUiState.Error(error.message ?: "Erro")
                }
        }
    }

    /**
     * Obtem ajuda de navegacao.
     */
    fun getNavigationHelp(question: String) {
        viewModelScope.launch {
            _uiState.value = AiAssistantUiState.Loading

            aiService.getNavigationHelp(currentScreen, question)
                .onSuccess { response ->
                    addAiMessage(response)
                    _uiState.value = AiAssistantUiState.Idle
                }
                .onFailure { error ->
                    addErrorMessage(error.message ?: "Erro ao obter ajuda")
                    _uiState.value = AiAssistantUiState.Error(error.message ?: "Erro")
                }
        }
    }

    /**
     * Define o ecra atual para contexto.
     */
    fun setCurrentScreen(screen: String) {
        currentScreen = screen
    }

    /**
     * Expande/colapsa o chat.
     */
    fun toggleExpanded() {
        _isExpanded.value = !_isExpanded.value
    }

    /**
     * Fecha o chat.
     */
    fun collapse() {
        _isExpanded.value = false
    }

    /**
     * Limpa o historico de mensagens.
     */
    fun clearHistory() {
        _messages.value = emptyList()
    }

    /**
     * Define o modo de AI.
     */
    fun setAiMode(mode: AiMode) {
        aiService.setMode(mode)
        loadCapabilities()
    }

    // ========== Private Helpers ==========

    private fun buildConversationContext(): String {
        val recentMessages = _messages.value.takeLast(6)
        return recentMessages.joinToString("\n") { msg ->
            val prefix = if (msg.isUser) "Utilizador" else "Assistente"
            "$prefix: ${msg.content}"
        }
    }

    private fun addAiMessage(content: String) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            isUser = false,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + message
    }

    private fun addErrorMessage(error: String) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = "Erro: $error",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isError = true
        )
        _messages.value = _messages.value + message
    }
}

sealed class AiAssistantUiState {
    object Idle : AiAssistantUiState()
    object Loading : AiAssistantUiState()
    data class Error(val message: String) : AiAssistantUiState()
}

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val isError: Boolean = false,
    val isSystemMessage: Boolean = false,
    val actions: List<AiAction> = emptyList()
)
