package com.ciclismo.portugal.data.local.ai

import android.util.Log
import com.ciclismo.portugal.domain.model.ActionPriority
import com.ciclismo.portugal.domain.model.AiAction
import com.ciclismo.portugal.domain.model.AiActionType
import com.ciclismo.portugal.domain.model.AiResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses AI responses to extract message and suggested actions.
 * Supports both structured JSON responses and plain text with action detection.
 */
@Singleton
class AiResponseParser @Inject constructor() {

    companion object {
        private const val TAG = "AiResponseParser"

        // JSON markers in response
        private const val JSON_START = "```json"
        private const val JSON_END = "```"
        private const val ACTIONS_KEY = "actions"
        private const val MESSAGE_KEY = "message"
    }

    private val gson = Gson()

    /**
     * Parse AI response text to extract message and actions.
     */
    fun parseResponse(rawResponse: String): AiResponse {
        Log.d(TAG, "parseResponse: Parsing response (${rawResponse.length} chars)")
        return try {
            // First, try to extract JSON block if present
            val jsonContent = extractJsonBlock(rawResponse)

            var result = if (jsonContent != null) {
                Log.d(TAG, "parseResponse: Found JSON block, parsing...")
                Log.d(TAG, "parseResponse: JSON content: ${jsonContent.take(200)}...")
                parseJsonResponse(jsonContent, rawResponse)
            } else {
                // Fall back to pattern matching for action detection
                Log.d(TAG, "parseResponse: No JSON block found, using pattern detection...")
                parseTextResponse(rawResponse)
            }

            // IMPORTANT: If JSON parsing found no actions, also try pattern detection
            if (result.actions.isEmpty()) {
                Log.d(TAG, "parseResponse: No actions from initial parse, trying pattern detection on message...")
                val patternActions = mutableListOf<AiAction>()

                // Run pattern detection on the message text
                detectTransferSuggestions(result.message, patternActions)
                detectCaptainSuggestions(result.message, patternActions)
                detectWildcardSuggestions(result.message, patternActions)
                detectNavigationSuggestions(result.message, patternActions)
                detectHelpSuggestions(result.message, patternActions)

                // If still no actions, use fallback
                if (patternActions.isEmpty()) {
                    Log.d(TAG, "parseResponse: Pattern detection found no actions, using fallback...")
                    addFallbackActions(result.message, patternActions)
                }

                if (patternActions.isNotEmpty()) {
                    Log.d(TAG, "parseResponse: Pattern detection found ${patternActions.size} actions")
                    result = AiResponse(message = result.message, actions = patternActions)
                }
            }

            // CRITICAL: Guarantee at least one action ALWAYS exists
            if (result.actions.isEmpty()) {
                Log.w(TAG, "⚠️ CRITICAL: No actions after all parsing! Injecting guaranteed action...")
                result = AiResponse(
                    message = result.message,
                    actions = listOf(
                        AiAction(
                            id = UUID.randomUUID().toString(),
                            type = AiActionType.NAVIGATE_TO,
                            title = "Explorar App",
                            description = "Ver funcionalidades",
                            parameters = mapOf("route" to "apostas"),
                            priority = ActionPriority.LOW
                        )
                    )
                )
            }

            Log.d(TAG, "parseResponse: ✅ Final result has ${result.actions.size} actions")
            if (result.actions.isNotEmpty()) {
                result.actions.forEach { action ->
                    Log.d(TAG, "parseResponse: Action - ${action.type}: ${action.title}")
                }
            } else {
                Log.e(TAG, "parseResponse: ❌❌❌ STILL NO ACTIONS! This should never happen!")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}", e)
            // Even on error, provide a fallback action
            AiResponse(
                message = rawResponse.ifBlank { "Desculpa, ocorreu um erro ao processar a resposta." },
                actions = listOf(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.SHOW_HELP,
                        title = "Ajuda",
                        description = "Precisas de ajuda?",
                        parameters = mapOf("topic" to "general"),
                        priority = ActionPriority.LOW
                    )
                )
            )
        }
    }

    /**
     * Extract JSON block from response (between ```json and ```)
     */
    private fun extractJsonBlock(text: String): String? {
        val startIndex = text.indexOf(JSON_START)
        if (startIndex == -1) return null

        val jsonStart = startIndex + JSON_START.length
        val endIndex = text.indexOf(JSON_END, jsonStart)
        if (endIndex == -1) return null

        return text.substring(jsonStart, endIndex).trim()
    }

    /**
     * Parse structured JSON response.
     */
    private fun parseJsonResponse(json: String, originalText: String): AiResponse {
        return try {
            val jsonObject = JsonParser.parseString(json).asJsonObject

            val message = if (jsonObject.has(MESSAGE_KEY)) {
                jsonObject.get(MESSAGE_KEY).asString
            } else {
                // Extract message from text before JSON
                val jsonBlockStart = originalText.indexOf(JSON_START)
                if (jsonBlockStart > 0) {
                    originalText.substring(0, jsonBlockStart).trim()
                } else {
                    originalText.replace(Regex("```json.*```", RegexOption.DOT_MATCHES_ALL), "").trim()
                }
            }

            val actions = if (jsonObject.has(ACTIONS_KEY)) {
                parseActionsArray(jsonObject.getAsJsonArray(ACTIONS_KEY))
            } else {
                emptyList()
            }

            AiResponse(message = message, actions = actions)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}")
            AiResponse(message = originalText, actions = emptyList())
        }
    }

    /**
     * Parse actions array from JSON.
     */
    private fun parseActionsArray(jsonArray: com.google.gson.JsonArray): List<AiAction> {
        return jsonArray.mapNotNull { element ->
            try {
                val obj = element.asJsonObject

                val type = parseActionType(obj.get("type")?.asString ?: "")
                    ?: return@mapNotNull null

                val parameters = if (obj.has("parameters")) {
                    obj.getAsJsonObject("parameters").entrySet()
                        .associate { it.key to it.value.asString }
                } else {
                    emptyMap()
                }

                AiAction(
                    id = obj.get("id")?.asString ?: UUID.randomUUID().toString(),
                    type = type,
                    title = obj.get("title")?.asString ?: getDefaultTitle(type),
                    description = obj.get("description")?.asString ?: "",
                    parameters = parameters,
                    priority = parsePriority(obj.get("priority")?.asString),
                    requiresConfirmation = obj.get("requiresConfirmation")?.asBoolean ?: true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing action: ${e.message}")
                null
            }
        }
    }

    /**
     * Parse action type from string.
     * Supports all action types for full app-wide tutoring.
     */
    private fun parseActionType(typeStr: String): AiActionType? {
        return try {
            when (typeStr.lowercase().replace("_", "").replace("-", "")) {
                // Fantasy actions
                "buycyclist", "buy" -> AiActionType.BUY_CYCLIST
                "sellcyclist", "sell" -> AiActionType.SELL_CYCLIST
                "setcaptain", "captain" -> AiActionType.SET_CAPTAIN
                "activatecyclist", "activate" -> AiActionType.ACTIVATE_CYCLIST
                "deactivatecyclist", "deactivate" -> AiActionType.DEACTIVATE_CYCLIST
                "usetriplecaptain", "triplecaptain", "3x" -> AiActionType.USE_TRIPLE_CAPTAIN
                "usewildcard", "wildcard" -> AiActionType.USE_WILDCARD

                // Navigation
                "navigateto", "navigate", "goto" -> AiActionType.NAVIGATE_TO
                "viewcyclist" -> AiActionType.VIEW_CYCLIST
                "viewrace" -> AiActionType.VIEW_RACE

                // App-wide navigation
                "viewprova", "prova" -> AiActionType.VIEW_PROVA
                "viewnews" -> AiActionType.VIEW_NEWS
                "viewrankings" -> AiActionType.VIEW_RANKINGS
                "viewprofile" -> AiActionType.VIEW_PROFILE
                "viewvideo" -> AiActionType.VIEW_VIDEO

                // User results
                "viewmyresults", "myresults", "results" -> AiActionType.VIEW_MY_RESULTS
                "addresult" -> AiActionType.ADD_RESULT

                // Information
                "showstandings", "standings" -> AiActionType.SHOW_STANDINGS
                "showcalendar", "calendar" -> AiActionType.SHOW_CALENDAR
                "showhelp", "help" -> AiActionType.SHOW_HELP
                "showtutorial", "tutorial" -> AiActionType.SHOW_TUTORIAL

                // Settings
                "enablenotifications", "notifications" -> AiActionType.ENABLE_NOTIFICATIONS
                "setreminder", "reminder" -> AiActionType.SET_REMINDER

                // Premium
                "viewpremium", "premium" -> AiActionType.VIEW_PREMIUM

                else -> AiActionType.entries.find { it.name.equals(typeStr, ignoreCase = true) }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse priority from string.
     */
    private fun parsePriority(priorityStr: String?): ActionPriority {
        return when (priorityStr?.lowercase()) {
            "low" -> ActionPriority.LOW
            "high" -> ActionPriority.HIGH
            "urgent" -> ActionPriority.URGENT
            else -> ActionPriority.NORMAL
        }
    }

    /**
     * Get default title for action type.
     * Supports all action types for full app-wide tutoring.
     */
    private fun getDefaultTitle(type: AiActionType): String {
        return when (type) {
            // Fantasy actions
            AiActionType.BUY_CYCLIST -> "Comprar ciclista"
            AiActionType.SELL_CYCLIST -> "Vender ciclista"
            AiActionType.SET_CAPTAIN -> "Definir capitao"
            AiActionType.ACTIVATE_CYCLIST -> "Ativar ciclista"
            AiActionType.DEACTIVATE_CYCLIST -> "Desativar ciclista"
            AiActionType.USE_TRIPLE_CAPTAIN -> "Ativar Triple Captain"
            AiActionType.USE_WILDCARD -> "Ativar Wildcard"

            // Navigation
            AiActionType.NAVIGATE_TO -> "Ir para"
            AiActionType.VIEW_CYCLIST -> "Ver ciclista"
            AiActionType.VIEW_RACE -> "Ver corrida"

            // App-wide navigation
            AiActionType.VIEW_PROVA -> "Ver prova"
            AiActionType.VIEW_NEWS -> "Ver noticias"
            AiActionType.VIEW_RANKINGS -> "Ver rankings"
            AiActionType.VIEW_PROFILE -> "Ver perfil"
            AiActionType.VIEW_VIDEO -> "Ver video"

            // User results
            AiActionType.VIEW_MY_RESULTS -> "Ver meus resultados"
            AiActionType.ADD_RESULT -> "Adicionar resultado"

            // Information
            AiActionType.SHOW_STANDINGS -> "Ver classificacao"
            AiActionType.SHOW_CALENDAR -> "Ver calendario"
            AiActionType.SHOW_HELP -> "Ver ajuda"
            AiActionType.SHOW_TUTORIAL -> "Ver tutorial"

            // Settings
            AiActionType.ENABLE_NOTIFICATIONS -> "Ativar notificacoes"
            AiActionType.SET_REMINDER -> "Criar lembrete"

            // Premium
            AiActionType.VIEW_PREMIUM -> "Ver Premium"
        }
    }

    /**
     * Parse plain text response with pattern-based action detection.
     * Fallback when AI doesn't return JSON.
     * Enhanced for full app-wide tutoring.
     */
    private fun parseTextResponse(text: String): AiResponse {
        val actions = mutableListOf<AiAction>()

        Log.d(TAG, "parseTextResponse: Analyzing text for patterns...")

        // PRIORITY 1: Simple keyword-based action injection (most reliable)
        addSimpleKeywordActions(text, actions)
        Log.d(TAG, "parseTextResponse: After simple keyword detection: ${actions.size} actions")

        // PRIORITY 2: Detect transfer recommendations (buy/sell patterns)
        detectTransferSuggestions(text, actions)
        Log.d(TAG, "parseTextResponse: After transfer detection: ${actions.size} actions")

        // PRIORITY 3: Detect captain suggestions
        detectCaptainSuggestions(text, actions)
        Log.d(TAG, "parseTextResponse: After captain detection: ${actions.size} actions")

        // PRIORITY 4: Detect wildcard suggestions
        detectWildcardSuggestions(text, actions)
        Log.d(TAG, "parseTextResponse: After wildcard detection: ${actions.size} actions")

        // PRIORITY 5: Detect navigation suggestions
        detectNavigationSuggestions(text, actions)
        Log.d(TAG, "parseTextResponse: After navigation detection: ${actions.size} actions")

        // PRIORITY 6: Detect help/tutorial suggestions
        detectHelpSuggestions(text, actions)
        Log.d(TAG, "parseTextResponse: After help detection: ${actions.size} actions")

        // FALLBACK: If no actions found, add contextual navigation based on topic
        if (actions.isEmpty()) {
            Log.d(TAG, "parseTextResponse: No actions found, applying fallback...")
            addFallbackActions(text, actions)
        }

        Log.d(TAG, "parseTextResponse: Final action count: ${actions.size}")
        return AiResponse(message = text, actions = actions)
    }

    /**
     * Simple keyword-to-action mapper for guaranteed action generation.
     * This is the most aggressive detection - even a single keyword triggers an action.
     */
    private fun addSimpleKeywordActions(text: String, actions: MutableList<AiAction>) {
        val textLower = text.lowercase()
        val addedActions = mutableSetOf<String>()

        // Market keywords -> Navigate to Market
        if ((textLower.contains("mercado") || textLower.contains("market") ||
             textLower.contains("comprar") || textLower.contains("vender") ||
             textLower.contains("transferencia") || textLower.contains("transfer")) &&
            !addedActions.contains("market")) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ir ao Mercado",
                    description = "Ver ciclistas disponiveis",
                    parameters = mapOf("route" to "fantasy/market"),
                    priority = ActionPriority.NORMAL
                )
            )
            addedActions.add("market")
            Log.d(TAG, "addSimpleKeywordActions: ✅ Added Market action (keyword: mercado/comprar/vender)")
        }

        // Team keywords -> Navigate to Team
        if ((textLower.contains("minha equipa") || textLower.contains("my team") ||
             textLower.contains("equipa fantasy") || textLower.contains("team fantasy") ||
             textLower.contains("gerir equipa") || textLower.contains("manage team")) &&
            !addedActions.contains("team")) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ver Equipa",
                    description = "Gerir a tua equipa Fantasy",
                    parameters = mapOf("route" to "fantasy/team"),
                    priority = ActionPriority.NORMAL
                )
            )
            addedActions.add("team")
            Log.d(TAG, "addSimpleKeywordActions: ✅ Added Team action (keyword: minha equipa)")
        }

        // Captain keywords -> Navigate to Team OR Set Captain
        if ((textLower.contains("capitao") || textLower.contains("captain") ||
             textLower.contains("capitão")) && !addedActions.contains("captain")) {
            // If asking "how" or "what", show help. Otherwise navigate to team.
            if (textLower.contains("como") || textLower.contains("how") ||
                textLower.contains("o que e") || textLower.contains("what is")) {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.SHOW_HELP,
                        title = "Ajuda sobre Capitao",
                        description = "Como funciona o capitao",
                        parameters = mapOf("topic" to "captain"),
                        priority = ActionPriority.NORMAL
                    )
                )
            } else {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.NAVIGATE_TO,
                        title = "Ver Equipa",
                        description = "Definir capitao",
                        parameters = mapOf("route" to "fantasy/team"),
                        priority = ActionPriority.NORMAL
                    )
                )
            }
            addedActions.add("captain")
            Log.d(TAG, "addSimpleKeywordActions: ✅ Added Captain action")
        }

        // Calendar keywords -> Navigate to Calendar
        if ((textLower.contains("calendario") || textLower.contains("calendar") ||
             textLower.contains("corrida") || textLower.contains("race") ||
             textLower.contains("proxima corrida") || textLower.contains("next race")) &&
            !addedActions.contains("calendar")) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ver Calendario",
                    description = "Calendario de corridas",
                    parameters = mapOf("route" to "calendar"),
                    priority = ActionPriority.LOW
                )
            )
            addedActions.add("calendar")
            Log.d(TAG, "addSimpleKeywordActions: ✅ Added Calendar action")
        }

        // Wildcard keywords -> Navigate to Team or Show Help
        if ((textLower.contains("wildcard") || textLower.contains("transferencias ilimitadas")) &&
            !addedActions.contains("wildcard")) {
            if (textLower.contains("como") || textLower.contains("how") ||
                textLower.contains("usar") || textLower.contains("use")) {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.SHOW_HELP,
                        title = "Ajuda sobre Wildcard",
                        description = "Como usar wildcard",
                        parameters = mapOf("topic" to "wildcard"),
                        priority = ActionPriority.NORMAL
                    )
                )
            } else {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.NAVIGATE_TO,
                        title = "Ver Equipa",
                        description = "Gerir wildcards",
                        parameters = mapOf("route" to "fantasy/team"),
                        priority = ActionPriority.NORMAL
                    )
                )
            }
            addedActions.add("wildcard")
            Log.d(TAG, "addSimpleKeywordActions: ✅ Added Wildcard action")
        }

        // Fantasy hub keywords
        if ((textLower.contains("fantasy hub") || textLower.contains("jogo fantasy") ||
             textLower.contains("fantasy game") || textLower.contains("apostas")) &&
            !addedActions.contains("fantasy")) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Fantasy Hub",
                    description = "Ir para Fantasy",
                    parameters = mapOf("route" to "apostas"),
                    priority = ActionPriority.NORMAL
                )
            )
            addedActions.add("fantasy")
            Log.d(TAG, "addSimpleKeywordActions: ✅ Added Fantasy Hub action")
        }

        // Leagues/Rankings keywords
        if ((textLower.contains("liga") || textLower.contains("league") ||
             textLower.contains("ranking") || textLower.contains("classificacao")) &&
            !addedActions.contains("leagues")) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ver Ligas",
                    description = "Rankings e classificacoes",
                    parameters = mapOf("route" to "fantasy/leagues"),
                    priority = ActionPriority.LOW
                )
            )
            addedActions.add("leagues")
            Log.d(TAG, "addSimpleKeywordActions: ✅ Added Leagues action")
        }

        // News keywords
        if ((textLower.contains("noticia") || textLower.contains("news") ||
             textLower.contains("artigo") || textLower.contains("article")) &&
            !addedActions.contains("news")) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ver Noticias",
                    description = "Ultimas noticias",
                    parameters = mapOf("route" to "news"),
                    priority = ActionPriority.LOW
                )
            )
            addedActions.add("news")
            Log.d(TAG, "addSimpleKeywordActions: ✅ Added News action")
        }
    }

    /**
     * Fallback to add at least one relevant action when no other patterns matched.
     * This ensures users always see actionable suggestions.
     */
    private fun addFallbackActions(text: String, actions: MutableList<AiAction>) {
        val textLower = text.lowercase()

        // Fantasy-related topics
        if (textLower.contains("fantasy") || textLower.contains("jogo") ||
            textLower.contains("equipa") || textLower.contains("ciclista") ||
            textLower.contains("pontos") || textLower.contains("transferencia")) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Explorar Fantasy",
                    description = "Aceder ao hub do Fantasy",
                    parameters = mapOf("route" to "apostas"),
                    priority = ActionPriority.NORMAL
                )
            )
            Log.d(TAG, "addFallbackActions: Added Fantasy hub action")
        }

        // Market/transfer topics
        if (textLower.contains("mercado") || textLower.contains("comprar") ||
            textLower.contains("vender") || textLower.contains("preco") ||
            textLower.contains("transferir") || textLower.contains("budget") ||
            textLower.contains("orcamento")) {
            if (actions.none { it.parameters["route"] == "fantasy/market" }) {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.NAVIGATE_TO,
                        title = "Ver Mercado",
                        description = "Mercado de ciclistas",
                        parameters = mapOf("route" to "fantasy/market"),
                        priority = ActionPriority.NORMAL
                    )
                )
                Log.d(TAG, "addFallbackActions: Added Market action")
            }
        }

        // Team topics
        if (textLower.contains("equipa") || textLower.contains("capitao") ||
            textLower.contains("ativo") || textLower.contains("formacao") ||
            textLower.contains("minha equipa")) {
            if (actions.none { it.parameters["route"] == "fantasy/team" }) {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.NAVIGATE_TO,
                        title = "Ver Equipa",
                        description = "Gerir a tua equipa",
                        parameters = mapOf("route" to "fantasy/team"),
                        priority = ActionPriority.NORMAL
                    )
                )
                Log.d(TAG, "addFallbackActions: Added Team action")
            }
        }

        // Calendar/race topics
        if (textLower.contains("corrida") || textLower.contains("calendario") ||
            textLower.contains("etapa") || textLower.contains("worldtour") ||
            textLower.contains("tour") || textLower.contains("giro") ||
            textLower.contains("vuelta")) {
            if (actions.none { it.parameters["route"] == "calendar" }) {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.NAVIGATE_TO,
                        title = "Ver Calendario",
                        description = "Calendario de corridas",
                        parameters = mapOf("route" to "calendar"),
                        priority = ActionPriority.LOW
                    )
                )
                Log.d(TAG, "addFallbackActions: Added Calendar action")
            }
        }

        // News topics
        if (textLower.contains("noticia") || textLower.contains("artigo") ||
            textLower.contains("informacao")) {
            if (actions.none { it.parameters["route"] == "news" }) {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.NAVIGATE_TO,
                        title = "Ver Noticias",
                        description = "Noticias de ciclismo",
                        parameters = mapOf("route" to "news"),
                        priority = ActionPriority.LOW
                    )
                )
                Log.d(TAG, "addFallbackActions: Added News action")
            }
        }

        // Leagues topics
        if (textLower.contains("liga") || textLower.contains("ranking") ||
            textLower.contains("classificacao") || textLower.contains("posicao")) {
            if (actions.none { it.parameters["route"] == "fantasy/leagues" }) {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.NAVIGATE_TO,
                        title = "Ver Ligas",
                        description = "Rankings e ligas",
                        parameters = mapOf("route" to "fantasy/leagues"),
                        priority = ActionPriority.LOW
                    )
                )
                Log.d(TAG, "addFallbackActions: Added Leagues action")
            }
        }

        // Rules topics
        if (textLower.contains("regra") || textLower.contains("como funciona") ||
            textLower.contains("explicacao") || textLower.contains("tutorial")) {
            if (actions.none { it.parameters["route"] == "fantasy/rules" }) {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.NAVIGATE_TO,
                        title = "Ver Regras",
                        description = "Regras do Fantasy",
                        parameters = mapOf("route" to "fantasy/rules"),
                        priority = ActionPriority.LOW
                    )
                )
                Log.d(TAG, "addFallbackActions: Added Rules action")
            }
        }

        // If still no actions and response is about the app, add general help
        if (actions.isEmpty() && (textLower.contains("app") || textLower.contains("aplicacao") ||
            textLower.contains("posso") || textLower.contains("funcionalidade"))) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Explorar App",
                    description = "Ver todas as funcionalidades",
                    parameters = mapOf("route" to "apostas"),
                    priority = ActionPriority.LOW
                )
            )
            Log.d(TAG, "addFallbackActions: Added general explore action")
        }

        // ULTIMATE FALLBACK: If we still have no actions at all, ALWAYS add at least one
        // This ensures the user always sees actionable suggestions
        if (actions.isEmpty()) {
            Log.d(TAG, "addFallbackActions: No actions matched, adding ultimate fallback")
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ver Fantasy",
                    description = "Explorar o jogo Fantasy",
                    parameters = mapOf("route" to "apostas"),
                    priority = ActionPriority.LOW
                )
            )
        }
    }

    private fun detectTransferSuggestions(text: String, actions: MutableList<AiAction>) {
        // Detect "comprar/buy" patterns - various formats
        val buyPatterns = listOf(
            Regex("(?:comprar|buy|adiciona|recruta|contratar)\\s+[\"']?([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s]{2,25})[\"']?", RegexOption.IGNORE_CASE),
            Regex("(?:recomendo|sugiro)\\s+(?:o\\s+)?([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s]{2,25})\\s+(?:para|como)", RegexOption.IGNORE_CASE),
            Regex("([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s]{2,25})\\s+(?:e|seria)\\s+(?:uma\\s+)?(?:boa|excelente|otima)\\s+(?:compra|opcao|escolha)", RegexOption.IGNORE_CASE)
        )

        val foundBuyCyclists = mutableSetOf<String>()

        buyPatterns.forEach { pattern ->
            pattern.findAll(text).forEach { match ->
                val cyclistName = match.groupValues[1].trim()
                    .replace(Regex("\\s+"), " ") // Normalize spaces
                    .takeWhile { it.isLetter() || it.isWhitespace() || it in "ÀÁÂÃÄÅàáâãäåÈÉÊËèéêëÌÍÎÏìíîïÒÓÔÕÖòóôõöÙÚÛÜùúûüÇçÑñ" }
                    .trim()

                if (cyclistName.length > 3 && !foundBuyCyclists.contains(cyclistName.lowercase()) &&
                    !isCommonWord(cyclistName)) {
                    actions.add(
                        AiAction(
                            id = UUID.randomUUID().toString(),
                            type = AiActionType.BUY_CYCLIST,
                            title = "Comprar $cyclistName",
                            description = "Adicionar $cyclistName a equipa",
                            parameters = mapOf("cyclistName" to cyclistName),
                            priority = ActionPriority.NORMAL
                        )
                    )
                    foundBuyCyclists.add(cyclistName.lowercase())
                }
            }
        }

        // Detect "vender/sell" patterns - various formats
        val sellPatterns = listOf(
            Regex("(?:vender|sell|remover|dispensar|libertar)\\s+[\"']?([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s]{2,25})[\"']?", RegexOption.IGNORE_CASE),
            Regex("([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s]{2,25})\\s+(?:pode|deveria|devia)\\s+(?:ser\\s+)?(?:vendido|dispensado|removido)", RegexOption.IGNORE_CASE)
        )

        val foundSellCyclists = mutableSetOf<String>()

        sellPatterns.forEach { pattern ->
            pattern.findAll(text).forEach { match ->
                val cyclistName = match.groupValues[1].trim()
                    .replace(Regex("\\s+"), " ")
                    .takeWhile { it.isLetter() || it.isWhitespace() || it in "ÀÁÂÃÄÅàáâãäåÈÉÊËèéêëÌÍÎÏìíîïÒÓÔÕÖòóôõöÙÚÛÜùúûüÇçÑñ" }
                    .trim()

                if (cyclistName.length > 3 && !foundSellCyclists.contains(cyclistName.lowercase()) &&
                    !isCommonWord(cyclistName)) {
                    actions.add(
                        AiAction(
                            id = UUID.randomUUID().toString(),
                            type = AiActionType.SELL_CYCLIST,
                            title = "Vender $cyclistName",
                            description = "Remover $cyclistName da equipa",
                            parameters = mapOf("cyclistName" to cyclistName),
                            priority = ActionPriority.NORMAL
                        )
                    )
                    foundSellCyclists.add(cyclistName.lowercase())
                }
            }
        }
    }

    /**
     * Check if a detected name is actually a common word that shouldn't be treated as a cyclist name.
     */
    private fun isCommonWord(name: String): Boolean {
        val commonWords = setOf(
            "equipa", "ciclista", "ciclistas", "corrida", "corridas", "etapa", "etapas",
            "capitao", "pontos", "preco", "forma", "mercado", "ligas", "liga",
            "opcao", "escolha", "compra", "venda", "transferencia", "orcamento",
            "esta", "este", "essa", "esse", "uma", "uns", "umas", "que", "para",
            "como", "com", "sem", "por", "mais", "menos", "muito", "pouco"
        )
        return commonWords.contains(name.lowercase())
    }

    private fun detectCaptainSuggestions(text: String, actions: MutableList<AiAction>) {
        val captainPatterns = listOf(
            // "capitao deve ser X", "recomendo X como capitao"
            Regex("(?:capitao|captain).*?(?:deve ser|sugiro|recomendo|seria|pode ser)\\s+[\"']?([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s]{2,25})[\"']?", RegexOption.IGNORE_CASE),
            // "define X como capitao", "coloca X como capitao"
            Regex("(?:define|coloca|mete|poe)\\s+[\"']?([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s]{2,25})[\"']?\\s+como\\s+capitao", RegexOption.IGNORE_CASE),
            // "X e um bom capitao", "X seria um bom capitao"
            Regex("([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s]{2,25})\\s+(?:e|seria)\\s+(?:um\\s+)?(?:bom|excelente|otimo)\\s+capitao", RegexOption.IGNORE_CASE),
            // "sugiro X para capitao"
            Regex("(?:sugiro|recomendo)\\s+[\"']?([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s]{2,25})[\"']?\\s+(?:para|como)\\s+capitao", RegexOption.IGNORE_CASE)
        )

        // Only add one captain suggestion
        for (pattern in captainPatterns) {
            pattern.find(text)?.let { match ->
                val cyclistName = match.groupValues[1].trim()
                    .replace(Regex("\\s+"), " ")
                    .takeWhile { it.isLetter() || it.isWhitespace() || it in "ÀÁÂÃÄÅàáâãäåÈÉÊËèéêëÌÍÎÏìíîïÒÓÔÕÖòóôõöÙÚÛÜùúûüÇçÑñ" }
                    .trim()

                if (cyclistName.length > 3 && !isCommonWord(cyclistName)) {
                    actions.add(
                        AiAction(
                            id = UUID.randomUUID().toString(),
                            type = AiActionType.SET_CAPTAIN,
                            title = "Definir $cyclistName como capitao",
                            description = "O capitao ganha pontos duplos",
                            parameters = mapOf("cyclistName" to cyclistName),
                            priority = ActionPriority.HIGH
                        )
                    )
                    return // Only add one captain suggestion
                }
            }
        }
    }

    private fun detectWildcardSuggestions(text: String, actions: MutableList<AiAction>) {
        // Detect Triple Captain suggestions
        if (text.contains("triple captain", ignoreCase = true) &&
            (text.contains("usar", ignoreCase = true) || text.contains("ativar", ignoreCase = true))) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.USE_TRIPLE_CAPTAIN,
                    title = "Ativar Triple Captain",
                    description = "Capitao ganha pontos triplos nesta corrida",
                    priority = ActionPriority.HIGH
                )
            )
        }

        // Detect Wildcard suggestions
        if (text.contains("wildcard", ignoreCase = true) &&
            (text.contains("usar", ignoreCase = true) || text.contains("ativar", ignoreCase = true))) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.USE_WILDCARD,
                    title = "Ativar Wildcard",
                    description = "Transferencias ilimitadas sem penalizacao",
                    priority = ActionPriority.HIGH
                )
            )
        }
    }

    private fun detectNavigationSuggestions(text: String, actions: MutableList<AiAction>) {
        // Map keywords to actual navigation routes (matching NavGraph.kt routes)
        val screenMappings = mapOf(
            // Fantasy screens
            "mercado" to "fantasy/market",
            "market" to "fantasy/market",
            "minha equipa" to "fantasy/team",
            "my team" to "fantasy/team",
            "equipa fantasy" to "fantasy/team",
            "ligas" to "fantasy/leagues",
            "leagues" to "fantasy/leagues",
            "classificacao" to "fantasy/leagues",
            "standings" to "fantasy/leagues",
            "regras" to "fantasy/rules",
            "rules" to "fantasy/rules",
            "criar equipa" to "fantasy/create-team",

            // Main screens
            "calendario" to "calendar",
            "calendar" to "calendar",
            "noticias" to "news",
            "news" to "news",
            "provas" to "home",
            "eventos" to "home",
            "home" to "home",
            "apostas" to "apostas",
            "fantasy" to "apostas",
            "jogo" to "apostas",

            // Profile & User
            "perfil" to "profile",
            "profile" to "profile",
            "resultados" to "results",
            "meus resultados" to "results",
            "my results" to "results",
            "adicionar resultado" to "results/add",

            // Rankings
            "rankings" to "rankings",
            "ranking" to "rankings",
            "liga portugal" to "rankings",

            // Premium & AI
            "premium" to "premium",
            "assistente" to "ai",
            "ai" to "ai"
        )

        // Track which routes we've already added to avoid duplicates
        val addedRoutes = mutableSetOf<String>()

        screenMappings.forEach { (keyword, route) ->
            if (addedRoutes.contains(route)) return@forEach

            // Check for various patterns that suggest navigation
            val patterns = listOf(
                // Direct navigation phrases
                "vai ao $keyword", "vai a $keyword", "vai aos $keyword",
                "visita o $keyword", "visita a $keyword", "visita os $keyword",
                "abre o $keyword", "abre a $keyword", "abre os $keyword",
                "consulta o $keyword", "consulta a $keyword", "consulta os $keyword",
                "acede ao $keyword", "acede a $keyword",
                "entra no $keyword", "entra na $keyword",
                // Recommendations
                "recomendo o $keyword", "recomendo a $keyword",
                "experimenta o $keyword", "experimenta a $keyword",
                "podes ir ao $keyword", "podes ir a $keyword",
                "podes ver no $keyword", "podes ver na $keyword",
                // Simple mentions with context indicators
                "no ecra $keyword", "na secao $keyword",
                "na pagina $keyword", "no menu $keyword"
            )

            // Also detect simple keyword mentions followed by navigation context
            val keywordWithContext = text.contains(keyword, ignoreCase = true) &&
                (text.contains("ecra", ignoreCase = true) ||
                 text.contains("secao", ignoreCase = true) ||
                 text.contains("pagina", ignoreCase = true) ||
                 text.contains("encontras", ignoreCase = true) ||
                 text.contains("podes", ignoreCase = true) ||
                 text.contains("vai", ignoreCase = true))

            val matchesPattern = patterns.any { text.contains(it, ignoreCase = true) }

            if (matchesPattern || keywordWithContext) {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.NAVIGATE_TO,
                        title = "Ir para ${keyword.replaceFirstChar { it.uppercase() }}",
                        description = "Abrir ecra de $keyword",
                        parameters = mapOf("route" to route),
                        priority = ActionPriority.LOW
                    )
                )
                addedRoutes.add(route)
            }
        }

        // Also detect generic screen mentions
        detectGenericScreenMentions(text, actions, addedRoutes)
    }

    /**
     * Detect screen mentions even when not using exact keywords.
     */
    private fun detectGenericScreenMentions(
        text: String,
        actions: MutableList<AiAction>,
        addedRoutes: MutableSet<String>
    ) {
        // Fantasy team mentions
        if (!addedRoutes.contains("fantasy/team") &&
            text.contains("equipa", ignoreCase = true) &&
            (text.contains("gerir", ignoreCase = true) ||
             text.contains("ver", ignoreCase = true) ||
             text.contains("capitao", ignoreCase = true) ||
             text.contains("ciclistas", ignoreCase = true))) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ver Minha Equipa",
                    description = "Gerir a tua equipa Fantasy",
                    parameters = mapOf("route" to "fantasy/team"),
                    priority = ActionPriority.NORMAL
                )
            )
            addedRoutes.add("fantasy/team")
        }

        // Market mentions for buying
        if (!addedRoutes.contains("fantasy/market") &&
            (text.contains("comprar", ignoreCase = true) ||
             text.contains("vender", ignoreCase = true) ||
             text.contains("transferencias", ignoreCase = true) ||
             text.contains("contratar", ignoreCase = true))) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ir ao Mercado",
                    description = "Comprar ou vender ciclistas",
                    parameters = mapOf("route" to "fantasy/market"),
                    priority = ActionPriority.NORMAL
                )
            )
            addedRoutes.add("fantasy/market")
        }

        // Calendar mentions
        if (!addedRoutes.contains("calendar") &&
            (text.contains("proximas corridas", ignoreCase = true) ||
             text.contains("calendario de corridas", ignoreCase = true) ||
             text.contains("worldtour", ignoreCase = true) ||
             text.contains("grande volta", ignoreCase = true))) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ver Calendario",
                    description = "Calendario de corridas",
                    parameters = mapOf("route" to "calendar"),
                    priority = ActionPriority.LOW
                )
            )
            addedRoutes.add("calendar")
        }

        // Fantasy hub mentions
        if (!addedRoutes.contains("apostas") &&
            text.contains("fantasy", ignoreCase = true) &&
            (text.contains("comecar", ignoreCase = true) ||
             text.contains("jogar", ignoreCase = true) ||
             text.contains("criar", ignoreCase = true))) {
            actions.add(
                AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Jogar Fantasy",
                    description = "Hub do jogo Fantasy",
                    parameters = mapOf("route" to "apostas"),
                    priority = ActionPriority.NORMAL
                )
            )
            addedRoutes.add("apostas")
        }
    }

    /**
     * Detect help/tutorial suggestions in text.
     */
    private fun detectHelpSuggestions(text: String, actions: MutableList<AiAction>) {
        // Detect explicit help mentions
        val helpTopics = listOf(
            "fantasy" to "fantasy",
            "jogo" to "fantasy",
            "equipa" to "team",
            "capitao" to "captain",
            "transferencias" to "transfers",
            "wildcard" to "wildcard",
            "triple captain" to "triplecaptain"
        )

        helpTopics.forEach { (keyword, topic) ->
            if ((text.contains("como funciona", ignoreCase = true) ||
                text.contains("o que e", ignoreCase = true) ||
                text.contains("explica", ignoreCase = true)) &&
                text.contains(keyword, ignoreCase = true)) {
                actions.add(
                    AiAction(
                        id = UUID.randomUUID().toString(),
                        type = AiActionType.SHOW_HELP,
                        title = "Ajuda sobre ${keyword.replaceFirstChar { it.uppercase() }}",
                        description = "Ver informacao sobre $keyword",
                        parameters = mapOf("topic" to topic),
                        priority = ActionPriority.NORMAL
                    )
                )
            }
        }
    }
}
