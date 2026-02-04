package com.ciclismo.portugal.data.local.ai

import android.util.Log
import com.ciclismo.portugal.domain.model.AiAction
import com.ciclismo.portugal.domain.model.AiActionResult
import com.ciclismo.portugal.domain.model.AiActionType
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.repository.CyclistRepository
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.repository.RaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes AI-suggested actions after user approval.
 * This is the core of the agentic AI system.
 */
@Singleton
class AiActionExecutor @Inject constructor(
    private val fantasyTeamRepository: FantasyTeamRepository,
    private val cyclistRepository: CyclistRepository,
    private val raceRepository: RaceRepository
) {
    companion object {
        private const val TAG = "AiActionExecutor"
    }

    /**
     * Simple execute method for actions that don't require user/team context.
     * Delegates to executeAction with null user/team.
     */
    suspend fun execute(action: AiAction): AiActionResult {
        return executeAction(action, userId = "", teamId = null)
    }

    /**
     * Execute an action after user approval.
     * Supports full app-wide navigation and Fantasy actions.
     */
    suspend fun executeAction(
        action: AiAction,
        userId: String,
        teamId: String?
    ): AiActionResult {
        Log.d(TAG, "Executing action: ${action.type} - ${action.title}")

        return try {
            when (action.type) {
                // Fantasy Team Management
                AiActionType.BUY_CYCLIST -> executeBuyCyclist(action, teamId)
                AiActionType.SELL_CYCLIST -> executeSellCyclist(action, teamId)
                AiActionType.SET_CAPTAIN -> executeSetCaptain(action, teamId)
                AiActionType.ACTIVATE_CYCLIST -> executeActivateCyclist(action, teamId)
                AiActionType.DEACTIVATE_CYCLIST -> executeDeactivateCyclist(action, teamId)

                // Fantasy Power-ups
                AiActionType.USE_TRIPLE_CAPTAIN -> executeTripleCaptain(action, teamId)
                AiActionType.USE_WILDCARD -> executeWildcard(action, teamId)

                // General Navigation
                AiActionType.NAVIGATE_TO -> executeNavigation(action)
                AiActionType.VIEW_CYCLIST -> executeViewCyclist(action)
                AiActionType.VIEW_RACE -> executeViewRace(action)

                // App-wide Navigation
                AiActionType.VIEW_PROVA -> executeViewProva(action)
                AiActionType.VIEW_NEWS -> AiActionResult.NavigateTo("news")
                AiActionType.VIEW_RANKINGS -> AiActionResult.NavigateTo("rankings")
                AiActionType.VIEW_PROFILE -> AiActionResult.NavigateTo("profile")
                AiActionType.VIEW_VIDEO -> executeViewVideo(action)

                // User Results
                AiActionType.VIEW_MY_RESULTS -> AiActionResult.NavigateTo("results")
                AiActionType.ADD_RESULT -> AiActionResult.NavigateTo("results/add")

                // Information
                AiActionType.SHOW_STANDINGS -> AiActionResult.NavigateTo("fantasy/leagues")
                AiActionType.SHOW_CALENDAR -> AiActionResult.NavigateTo("calendar")
                AiActionType.SHOW_HELP -> executeShowHelp(action)
                AiActionType.SHOW_TUTORIAL -> executeShowTutorial(action)

                // Settings
                AiActionType.ENABLE_NOTIFICATIONS -> executeEnableNotifications(action)
                AiActionType.SET_REMINDER -> executeSetReminder(action)

                // Premium
                AiActionType.VIEW_PREMIUM -> AiActionResult.NavigateTo("premium")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action: ${e.message}", e)
            AiActionResult.Error("Erro ao executar acao: ${e.message}")
        }
    }

    private suspend fun executeBuyCyclist(action: AiAction, teamId: String?): AiActionResult {
        if (teamId == null) {
            return AiActionResult.RequiresAuth("Precisas de criar uma equipa primeiro")
        }

        val cyclist = findCyclist(action.parameters)
            ?: return AiActionResult.Error("Ciclista nao encontrado")

        return fantasyTeamRepository.addCyclistToTeam(teamId, cyclist).fold(
            onSuccess = { AiActionResult.Success("${cyclist.fullName} adicionado a equipa!") },
            onFailure = { AiActionResult.Error(it.message ?: "Erro ao comprar ciclista") }
        )
    }

    private suspend fun executeSellCyclist(action: AiAction, teamId: String?): AiActionResult {
        if (teamId == null) {
            return AiActionResult.RequiresAuth("Precisas de criar uma equipa primeiro")
        }

        val cyclistId = action.parameters["cyclistId"]
            ?: return AiActionResult.Error("ID do ciclista nao especificado")

        return fantasyTeamRepository.removeCyclistFromTeam(teamId, cyclistId).fold(
            onSuccess = { AiActionResult.Success("Ciclista vendido com sucesso!") },
            onFailure = { AiActionResult.Error(it.message ?: "Erro ao vender ciclista") }
        )
    }

    private suspend fun executeSetCaptain(action: AiAction, teamId: String?): AiActionResult {
        if (teamId == null) {
            return AiActionResult.RequiresAuth("Precisas de criar uma equipa primeiro")
        }

        val cyclistId = action.parameters["cyclistId"]
            ?: return AiActionResult.Error("ID do ciclista nao especificado")

        return fantasyTeamRepository.setCaptain(teamId, cyclistId).fold(
            onSuccess = { AiActionResult.Success("Novo capitao definido!") },
            onFailure = { AiActionResult.Error(it.message ?: "Erro ao definir capitao") }
        )
    }

    private suspend fun executeActivateCyclist(action: AiAction, teamId: String?): AiActionResult {
        if (teamId == null) {
            return AiActionResult.RequiresAuth("Precisas de criar uma equipa primeiro")
        }

        val cyclistId = action.parameters["cyclistId"]
            ?: return AiActionResult.Error("ID do ciclista nao especificado")

        return fantasyTeamRepository.setActive(teamId, cyclistId, isActive = true).fold(
            onSuccess = { AiActionResult.Success("Ciclista ativado para a proxima corrida!") },
            onFailure = { AiActionResult.Error(it.message ?: "Erro ao ativar ciclista") }
        )
    }

    private suspend fun executeDeactivateCyclist(action: AiAction, teamId: String?): AiActionResult {
        if (teamId == null) {
            return AiActionResult.RequiresAuth("Precisas de criar uma equipa primeiro")
        }

        val cyclistId = action.parameters["cyclistId"]
            ?: return AiActionResult.Error("ID do ciclista nao especificado")

        return fantasyTeamRepository.setActive(teamId, cyclistId, isActive = false).fold(
            onSuccess = { AiActionResult.Success("Ciclista movido para suplentes") },
            onFailure = { AiActionResult.Error(it.message ?: "Erro ao desativar ciclista") }
        )
    }

    private suspend fun executeTripleCaptain(action: AiAction, teamId: String?): AiActionResult {
        if (teamId == null) {
            return AiActionResult.RequiresAuth("Precisas de criar uma equipa primeiro")
        }

        val raceId = action.parameters["raceId"]
            ?: return AiActionResult.Error("ID da corrida nao especificado")

        return fantasyTeamRepository.activateTripleCaptainForRace(teamId, raceId).fold(
            onSuccess = { AiActionResult.Success("Triple Captain ativado para esta corrida!") },
            onFailure = { AiActionResult.Error(it.message ?: "Erro ao ativar Triple Captain") }
        )
    }

    private suspend fun executeWildcard(action: AiAction, teamId: String?): AiActionResult {
        if (teamId == null) {
            return AiActionResult.RequiresAuth("Precisas de criar uma equipa primeiro")
        }

        val raceId = action.parameters["raceId"]
            ?: return AiActionResult.Error("ID da corrida nao especificado")

        return fantasyTeamRepository.activateWildcardForRace(teamId, raceId).fold(
            onSuccess = { AiActionResult.Success("Wildcard ativado! Transferencias ilimitadas.") },
            onFailure = { AiActionResult.Error(it.message ?: "Erro ao ativar Wildcard") }
        )
    }

    private fun executeNavigation(action: AiAction): AiActionResult {
        val rawRoute = action.parameters["route"]
            ?: action.parameters["destination"]
            ?: action.parameters["screen"]
            ?: action.parameters["target"]
            ?: action.title?.let { extractRouteFromTitle(it) }

        if (rawRoute.isNullOrBlank()) {
            Log.w(TAG, "NAVIGATE_TO action missing route parameter: ${action.parameters}")
            // Default to home if no route specified
            return AiActionResult.NavigateTo("home")
        }

        // Normalize legacy route names to actual NavGraph routes
        val normalizedRoute = normalizeRoute(rawRoute)
        return AiActionResult.NavigateTo(normalizedRoute)
    }

    /**
     * Try to extract route from action title as fallback.
     */
    private fun extractRouteFromTitle(title: String): String? {
        val lowerTitle = title.lowercase()
        return when {
            "mercado" in lowerTitle || "market" in lowerTitle -> "fantasy/market"
            "equipa" in lowerTitle || "team" in lowerTitle -> "fantasy/team"
            "liga" in lowerTitle || "league" in lowerTitle || "ranking" in lowerTitle -> "fantasy/leagues"
            "calendario" in lowerTitle || "calendar" in lowerTitle -> "calendar"
            "noticia" in lowerTitle || "news" in lowerTitle -> "news"
            "perfil" in lowerTitle || "profile" in lowerTitle -> "profile"
            "home" in lowerTitle || "inicio" in lowerTitle -> "home"
            "fantasy" in lowerTitle || "aposta" in lowerTitle -> "apostas"
            else -> null
        }
    }

    /**
     * Normalizes legacy/shorthand route names to actual NavGraph routes.
     */
    private fun normalizeRoute(route: String): String {
        return when (route.lowercase().trim()) {
            // Legacy short names -> actual routes
            "mercado", "market" -> "fantasy/market"
            "equipa", "team", "minha equipa", "my team" -> "fantasy/team"
            "ligas", "leagues", "classificacao", "standings" -> "fantasy/leagues"
            "calendario", "calendar" -> "calendar"
            "noticias", "news" -> "news"
            "provas", "eventos", "home" -> "home"
            "perfil", "profile" -> "profile"
            "apostas", "fantasy", "jogo" -> "apostas"
            "premium" -> "premium"
            "ai", "assistente" -> "ai"
            // Already valid routes pass through
            else -> route
        }
    }

    private suspend fun executeViewCyclist(action: AiAction): AiActionResult {
        // Try cyclistId first, then fall back to cyclistName search
        val cyclistId = action.parameters["cyclistId"]
        if (cyclistId != null && cyclistId.isNotBlank()) {
            return AiActionResult.NavigateTo("ciclista/$cyclistId")
        }

        // Try to find cyclist by name
        val cyclistName = action.parameters["cyclistName"] ?: action.parameters["name"]
        if (cyclistName != null && cyclistName.isNotBlank()) {
            return try {
                val cyclists = cyclistRepository.searchCyclists(cyclistName).first()
                val cyclist = cyclists.firstOrNull()
                if (cyclist != null) {
                    AiActionResult.NavigateTo("ciclista/${cyclist.id}")
                } else {
                    // Suggest navigation to market to search manually
                    AiActionResult.NavigateTo("fantasy/market?search=$cyclistName")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error searching cyclist: ${e.message}")
                AiActionResult.NavigateTo("fantasy/market")
            }
        }

        // If no parameters, navigate to market
        Log.w(TAG, "VIEW_CYCLIST action missing parameters: ${action.parameters}")
        return AiActionResult.NavigateTo("fantasy/market")
    }

    private fun executeViewRace(action: AiAction): AiActionResult {
        val raceId = action.parameters["raceId"]
            ?: return AiActionResult.Error("ID da corrida nao especificado")
        return AiActionResult.NavigateTo("corrida/$raceId")
    }

    private fun executeEnableNotifications(action: AiAction): AiActionResult {
        // Navigate to settings
        return AiActionResult.NavigateTo("definicoes/notificacoes")
    }

    private fun executeSetReminder(action: AiAction): AiActionResult {
        val raceId = action.parameters["raceId"]
        if (raceId != null) {
            return AiActionResult.NavigateTo("corrida/$raceId?reminder=true")
        }
        return AiActionResult.NavigateTo("calendario")
    }

    /**
     * Helper to find a cyclist by ID or name from action parameters.
     */
    private suspend fun findCyclist(parameters: Map<String, String>): Cyclist? {
        // Try cyclistId first
        val cyclistId = parameters["cyclistId"]
        if (cyclistId != null) {
            return cyclistRepository.getCyclistById(cyclistId).first()
        }

        // Try cyclistName
        val cyclistName = parameters["cyclistName"]
        if (cyclistName != null) {
            val cyclists = cyclistRepository.searchCyclists(cyclistName).first()
            return cyclists.firstOrNull()
        }

        return null
    }

    // ========== App-wide Navigation Actions ==========

    /**
     * Navigate to a specific prova (event in Portugal).
     */
    private fun executeViewProva(action: AiAction): AiActionResult {
        val provaId = action.parameters["provaId"]
        if (provaId != null) {
            return try {
                AiActionResult.NavigateTo("details/${provaId.toLong()}")
            } catch (e: Exception) {
                Log.w(TAG, "Invalid provaId: $provaId")
                AiActionResult.NavigateTo("home")
            }
        }
        // Default to home screen (provas list)
        return AiActionResult.NavigateTo("home")
    }

    /**
     * Navigate to video player.
     */
    private fun executeViewVideo(action: AiAction): AiActionResult {
        val videoId = action.parameters["videoId"]
        val title = action.parameters["title"] ?: "Video"
        val channel = action.parameters["channel"] ?: "Ciclismo"

        if (videoId != null) {
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encodedChannel = java.net.URLEncoder.encode(channel, "UTF-8")
            return AiActionResult.NavigateTo("video/$videoId/$encodedTitle/$encodedChannel")
        }
        // Default to home screen where videos are shown
        return AiActionResult.NavigateTo("home")
    }

    /**
     * Show contextual help about a specific topic.
     */
    private fun executeShowHelp(action: AiAction): AiActionResult {
        val topic = action.parameters["topic"]?.lowercase() ?: ""

        // Map topics to relevant screens
        return when {
            "fantasy" in topic || "jogo" in topic -> AiActionResult.NavigateTo("fantasy/rules")
            "equipa" in topic || "team" in topic -> AiActionResult.NavigateTo("fantasy/team")
            "mercado" in topic || "transfer" in topic -> AiActionResult.NavigateTo("fantasy/market")
            "capitao" in topic || "captain" in topic -> AiActionResult.NavigateTo("fantasy/rules")
            "wildcard" in topic || "triple" in topic -> AiActionResult.NavigateTo("fantasy/rules")
            "ranking" in topic || "liga" in topic -> AiActionResult.NavigateTo("fantasy/leagues")
            "prova" in topic || "evento" in topic -> AiActionResult.NavigateTo("home")
            "calendario" in topic || "calendar" in topic -> AiActionResult.NavigateTo("calendar")
            "resultado" in topic || "result" in topic -> AiActionResult.NavigateTo("results")
            "premium" in topic -> AiActionResult.NavigateTo("premium")
            "perfil" in topic || "profile" in topic -> AiActionResult.NavigateTo("profile")
            "strava" in topic -> AiActionResult.NavigateTo("profile")
            else -> AiActionResult.NavigateTo("fantasy/rules") // Default to rules
        }
    }

    /**
     * Show tutorial for a specific feature.
     */
    private fun executeShowTutorial(action: AiAction): AiActionResult {
        val feature = action.parameters["feature"]?.lowercase() ?: ""

        // Map features to relevant screens
        return when {
            "criar equipa" in feature || "create team" in feature -> AiActionResult.NavigateTo("fantasy/create-team")
            "transferencia" in feature || "transfer" in feature -> AiActionResult.NavigateTo("fantasy/market")
            "capitao" in feature || "captain" in feature -> AiActionResult.NavigateTo("fantasy/team")
            "ativar" in feature || "activate" in feature -> AiActionResult.NavigateTo("fantasy/team")
            "liga" in feature || "league" in feature -> AiActionResult.NavigateTo("fantasy/leagues")
            "resultado" in feature || "result" in feature -> AiActionResult.NavigateTo("results/add")
            "prova" in feature || "event" in feature -> AiActionResult.NavigateTo("home")
            else -> AiActionResult.NavigateTo("fantasy/rules")
        }
    }
}
