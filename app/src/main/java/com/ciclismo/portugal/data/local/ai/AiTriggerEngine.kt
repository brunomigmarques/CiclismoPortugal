package com.ciclismo.portugal.data.local.ai

import android.content.Context
import android.util.Log
import com.ciclismo.portugal.domain.model.AiAction
import com.ciclismo.portugal.domain.model.AiActionType
import com.ciclismo.portugal.domain.model.AiSuggestion
import com.ciclismo.portugal.domain.model.AiSuggestionType
import com.ciclismo.portugal.domain.model.AiTriggerContext
import com.ciclismo.portugal.domain.model.AiTriggerType
import com.ciclismo.portugal.domain.model.SuggestionPriority
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine that evaluates trigger conditions and generates contextual AI suggestions.
 * Manages cooldowns and dismissed triggers to avoid repetition.
 */
@Singleton
class AiTriggerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AiTriggerEngine"
        private const val PREFS_NAME = "ai_trigger_prefs"
        private const val COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes between same trigger
        private const val DISMISS_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours after dismiss
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Track last trigger times for cooldown
    private val lastTriggerTimes = mutableMapOf<AiTriggerType, Long>()

    // Track dismissed triggers
    private val dismissedTriggers = mutableMapOf<AiTriggerType, Long>()

    init {
        loadDismissedTriggers()
    }

    /**
     * Evaluate all triggers and return the highest priority suggestion, if any.
     */
    fun evaluateTriggers(context: AiTriggerContext): AiSuggestion? {
        val now = System.currentTimeMillis()

        Log.d(TAG, "========== EVALUATING ${AiTriggerType.entries.size} TRIGGERS ==========")

        // Evaluate all triggers with detailed logging
        val shouldTriggerResults = AiTriggerType.entries.map { trigger ->
            val should = shouldTrigger(trigger, context)
            val onCooldown = isOnCooldown(trigger, now)
            val dismissed = isDismissed(trigger, now)
            Log.d(TAG, "${trigger.name}: should=$should, cooldown=$onCooldown, dismissed=$dismissed")
            trigger to should
        }

        // Filter by cooldown/dismissed
        val activeTriggers = shouldTriggerResults
            .filter { (_, should) -> should }
            .map { (trigger, _) -> trigger }
            .filter { !isOnCooldown(it, now) }
            .filter { !isDismissed(it, now) }
            .sortedByDescending { getTriggerPriority(it, context) }

        Log.d(TAG, "✅ Active triggers (after filters): ${activeTriggers.map { it.name }}")

        return activeTriggers.firstOrNull()?.let { trigger ->
            Log.d(TAG, "🎯 Generating suggestion for: ${trigger.name}")
            generateSuggestion(trigger, context).also {
                lastTriggerTimes[trigger] = now
            }
        } ?: run {
            Log.d(TAG, "❌ No active triggers to suggest")
            null
        }
    }

    /**
     * Mark a trigger as dismissed by the user.
     */
    fun dismissTrigger(triggerType: AiTriggerType) {
        val now = System.currentTimeMillis()
        dismissedTriggers[triggerType] = now
        saveDismissedTrigger(triggerType, now)
        Log.d(TAG, "Dismissed trigger: ${triggerType.name}")
    }

    /**
     * Clear a specific trigger's cooldown (e.g., when context changes significantly).
     */
    fun clearCooldown(triggerType: AiTriggerType) {
        lastTriggerTimes.remove(triggerType)
    }

    /**
     * Check if a trigger condition is met.
     */
    private fun shouldTrigger(trigger: AiTriggerType, ctx: AiTriggerContext): Boolean {
        return when (trigger) {
            AiTriggerType.NO_CAPTAIN -> {
                ctx.teamSize > 0 && !ctx.hasCaptain && ctx.currentScreen == "fantasy/team"
            }

            AiTriggerType.INCOMPLETE_TEAM -> {
                ctx.teamSize in 1 until ctx.maxTeamSize &&
                    ctx.hoursUntilNextRace != null &&
                    ctx.hoursUntilNextRace < 48
            }

            AiTriggerType.TRANSFER_PENALTY_THRESHOLD -> {
                ctx.pendingTransfers > ctx.freeTransfers &&
                    !ctx.wildcardActive &&
                    ctx.currentScreen == "fantasy/market"
            }

            AiTriggerType.WILDCARD_OPPORTUNITY -> {
                ctx.hasWildcard &&
                    !ctx.wildcardActive &&
                    ctx.pendingTransfers > 4 &&
                    ctx.currentScreen == "fantasy/market"
            }

            AiTriggerType.RACE_DEADLINE -> {
                ctx.hoursUntilNextRace != null &&
                    ctx.hoursUntilNextRace < 24 &&
                    ctx.teamSize > 0
            }

            AiTriggerType.TRANSFER_DEADLINE -> {
                ctx.hoursUntilNextRace != null &&
                    ctx.hoursUntilNextRace < 6 &&
                    ctx.teamSize > 0
            }

            AiTriggerType.FIRST_VISIT_MARKET -> {
                !ctx.hasVisitedScreen && ctx.currentScreen == "fantasy/market"
            }

            AiTriggerType.FIRST_VISIT_MY_TEAM -> {
                !ctx.hasVisitedScreen && ctx.currentScreen == "fantasy/team"
            }

            AiTriggerType.FIRST_VISIT_LEAGUES -> {
                !ctx.hasVisitedScreen && ctx.currentScreen == "fantasy/leagues"
            }

            AiTriggerType.IDLE_ON_SCREEN -> {
                ctx.idleTimeMs > 30000 && ctx.currentScreen == "fantasy/market"
            }

            AiTriggerType.REPEATED_ERRORS -> {
                ctx.errorCount >= 3
            }

            AiTriggerType.UNDERVALUED_CYCLIST -> false // Requires AI analysis
            AiTriggerType.FORM_DROP_ALERT -> false // Requires data analysis
        }
    }

    /**
     * Get priority for a trigger based on context.
     */
    private fun getTriggerPriority(trigger: AiTriggerType, ctx: AiTriggerContext): Int {
        return when (trigger) {
            AiTriggerType.NO_CAPTAIN -> 90
            AiTriggerType.TRANSFER_DEADLINE -> 95
            AiTriggerType.RACE_DEADLINE -> 85
            AiTriggerType.INCOMPLETE_TEAM -> 80
            AiTriggerType.TRANSFER_PENALTY_THRESHOLD -> 70
            AiTriggerType.WILDCARD_OPPORTUNITY -> 75
            AiTriggerType.REPEATED_ERRORS -> 60
            AiTriggerType.FIRST_VISIT_MARKET -> 40
            AiTriggerType.FIRST_VISIT_MY_TEAM -> 40
            AiTriggerType.FIRST_VISIT_LEAGUES -> 40
            AiTriggerType.IDLE_ON_SCREEN -> 30
            AiTriggerType.UNDERVALUED_CYCLIST -> 50
            AiTriggerType.FORM_DROP_ALERT -> 55
        }
    }

    /**
     * Generate a suggestion for a triggered condition.
     */
    private fun generateSuggestion(trigger: AiTriggerType, ctx: AiTriggerContext): AiSuggestion {
        return when (trigger) {
            AiTriggerType.NO_CAPTAIN -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.EXPANDABLE_CARD,
                title = "Capitao em falta!",
                shortText = "Ainda nao tens capitao definido",
                description = "O capitao ganha pontos duplos. Escolhe um ciclista para maximizar os teus pontos na proxima corrida.",
                priority = SuggestionPriority.HIGH,
                quickAction = AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.SHOW_HELP,
                    title = "Como escolher capitao",
                    description = "Ver dicas para escolher o melhor capitao",
                    parameters = mapOf("topic" to "captain")
                ),
                triggerScreen = ctx.currentScreen
            )

            AiTriggerType.INCOMPLETE_TEAM -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.EXPANDABLE_CARD,
                title = "Equipa incompleta",
                shortText = "Faltam ${ctx.maxTeamSize - ctx.teamSize} ciclistas",
                description = "A proxima corrida comeca em ${ctx.hoursUntilNextRace}h. Completa a tua equipa para ganhar pontos!",
                priority = SuggestionPriority.HIGH,
                quickAction = AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ir ao Mercado",
                    description = "Completar equipa",
                    parameters = mapOf("route" to "fantasy/market")
                ),
                triggerScreen = ctx.currentScreen
            )

            AiTriggerType.TRANSFER_PENALTY_THRESHOLD -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.MINI_TIP,
                title = "Penalizacao de transferencias",
                shortText = "Vais perder ${(ctx.pendingTransfers - ctx.freeTransfers) * 4} pontos",
                description = "Tens ${ctx.pendingTransfers} transferencias pendentes mas so ${ctx.freeTransfers} gratis. Cada extra custa 4 pontos.",
                priority = SuggestionPriority.NORMAL,
                triggerScreen = ctx.currentScreen
            )

            AiTriggerType.WILDCARD_OPPORTUNITY -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.EXPANDABLE_CARD,
                title = "Usar Wildcard?",
                shortText = "Evita penalizacao de ${(ctx.pendingTransfers - ctx.freeTransfers) * 4} pts",
                description = "Tens um Wildcard disponivel! Ativa-o para fazer transferencias ilimitadas sem perder pontos.",
                priority = SuggestionPriority.HIGH,
                quickAction = AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.USE_WILDCARD,
                    title = "Ativar Wildcard",
                    description = "Transferencias ilimitadas sem penalizacao"
                ),
                triggerScreen = ctx.currentScreen
            )

            AiTriggerType.RACE_DEADLINE -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.EXPANDABLE_CARD,
                title = "Corrida em ${ctx.hoursUntilNextRace}h!",
                shortText = "${ctx.nextRaceName ?: "Proxima corrida"} comeca em breve",
                description = "Verifica a tua equipa e faz os ultimos ajustes antes do deadline.",
                priority = SuggestionPriority.HIGH,
                quickAction = AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ver Equipa",
                    description = "Verificar equipa",
                    parameters = mapOf("route" to "fantasy/team")
                ),
                triggerScreen = ctx.currentScreen
            )

            AiTriggerType.TRANSFER_DEADLINE -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.EXPANDABLE_CARD,
                title = "Deadline em ${ctx.hoursUntilNextRace}h!",
                shortText = "Ultimas horas para transferencias",
                description = "O mercado fecha em breve. Faz os teus ultimos ajustes agora!",
                priority = SuggestionPriority.URGENT,
                quickAction = AiAction(
                    id = UUID.randomUUID().toString(),
                    type = AiActionType.NAVIGATE_TO,
                    title = "Ir ao Mercado",
                    description = "Fazer transferencias",
                    parameters = mapOf("route" to "fantasy/market")
                ),
                triggerScreen = ctx.currentScreen
            )

            AiTriggerType.FIRST_VISIT_MARKET -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.MINI_TIP,
                title = "Bem-vindo ao Mercado!",
                shortText = "Precisa de ajuda a escolher ciclistas?",
                description = "Usa a pesquisa e filtros para encontrar os melhores ciclistas para a tua equipa.",
                priority = SuggestionPriority.LOW,
                triggerScreen = ctx.currentScreen,
                autoExpireMs = 15000
            )

            AiTriggerType.FIRST_VISIT_MY_TEAM -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.MINI_TIP,
                title = "A tua equipa",
                shortText = "Nao te esquecas de definir o capitao!",
                description = "Aqui podes gerir a tua equipa, definir o capitao e ativar power-ups.",
                priority = SuggestionPriority.LOW,
                triggerScreen = ctx.currentScreen,
                autoExpireMs = 15000
            )

            AiTriggerType.FIRST_VISIT_LEAGUES -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.MINI_TIP,
                title = "Ligas Fantasy",
                shortText = "Cria ou entra numa liga com amigos!",
                description = "Compete contra outros jogadores ou cria uma liga privada para os teus amigos.",
                priority = SuggestionPriority.LOW,
                triggerScreen = ctx.currentScreen,
                autoExpireMs = 15000
            )

            AiTriggerType.IDLE_ON_SCREEN -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.MINI_TIP,
                title = "Precisa de ajuda?",
                shortText = "Posso sugerir ciclistas para ti",
                description = "Parece que estas a ter dificuldade. Posso ajudar-te a encontrar os melhores ciclistas!",
                priority = SuggestionPriority.LOW,
                triggerScreen = ctx.currentScreen
            )

            AiTriggerType.REPEATED_ERRORS -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.EXPANDABLE_CARD,
                title = "Algo nao esta a funcionar?",
                shortText = "Posso ajudar a resolver",
                description = "Parece que encontraste alguns problemas. Conta-me o que esta a acontecer.",
                priority = SuggestionPriority.NORMAL,
                triggerScreen = ctx.currentScreen
            )

            AiTriggerType.UNDERVALUED_CYCLIST,
            AiTriggerType.FORM_DROP_ALERT -> AiSuggestion(
                id = UUID.randomUUID().toString(),
                type = AiSuggestionType.MINI_TIP,
                title = "Oportunidade detetada",
                shortText = "Ciclista interessante disponivel",
                description = "Detetei uma oportunidade no mercado que pode interessar-te.",
                priority = SuggestionPriority.LOW,
                triggerScreen = ctx.currentScreen
            )
        }
    }

    private fun isOnCooldown(trigger: AiTriggerType, now: Long): Boolean {
        val lastTime = lastTriggerTimes[trigger] ?: return false
        return (now - lastTime) < COOLDOWN_MS
    }

    private fun isDismissed(trigger: AiTriggerType, now: Long): Boolean {
        val dismissTime = dismissedTriggers[trigger] ?: return false
        return (now - dismissTime) < DISMISS_DURATION_MS
    }

    private fun loadDismissedTriggers() {
        AiTriggerType.entries.forEach { trigger ->
            val dismissTime = prefs.getLong("dismissed_${trigger.name}", 0L)
            if (dismissTime > 0) {
                dismissedTriggers[trigger] = dismissTime
            }
        }
    }

    private fun saveDismissedTrigger(trigger: AiTriggerType, time: Long) {
        prefs.edit()
            .putLong("dismissed_${trigger.name}", time)
            .apply()
    }
}
