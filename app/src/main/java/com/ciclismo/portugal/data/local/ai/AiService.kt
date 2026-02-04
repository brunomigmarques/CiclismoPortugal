package com.ciclismo.portugal.data.local.ai

import android.util.Log
import com.ciclismo.portugal.BuildConfig
import com.ciclismo.portugal.domain.model.AiResponse
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.Race
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servico de AI para o assistente de ciclismo.
 *
 * Usa Google Gemini API (Flash-Lite para custos minimos).
 * Inclui rate limiting para respeitar o tier gratuito.
 */
@Singleton
class AiService @Inject constructor(
    private val capabilityChecker: AiCapabilityChecker,
    private val usageTracker: AiUsageTracker,
    private val responseParser: AiResponseParser
) {
    companion object {
        private const val TAG = "AiService"
        private const val MODEL_NAME = "gemini-2.0-flash" // Fast and efficient model
    }

    private var generativeModel: GenerativeModel? = null
    private var currentMode: AiMode = AiMode.CLOUD
    private var isInitialized = false

    /**
     * Inicializa o servico de AI.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY

            if (apiKey.isBlank()) {
                Log.w(TAG, "Gemini API key not configured, using fallback responses")
                currentMode = AiMode.DISABLED
                isInitialized = true
                return@withContext true
            }

            // Configure the generative model
            val config = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1024 // Keep responses concise to save tokens
            }

            generativeModel = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                generationConfig = config
            )

            currentMode = AiMode.CLOUD
            isInitialized = true

            Log.d(TAG, "AI Service initialized with Gemini Flash")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AI: ${e.message}", e)
            currentMode = AiMode.DISABLED
            false
        }
    }

    /**
     * Gera resposta para uma mensagem do utilizador.
     */
    suspend fun generateResponse(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                initialize()
            }

            // Check usage limits
            if (!usageTracker.canMakeRequest()) {
                val stats = usageTracker.getUsageStats()
                Log.w(TAG, "Quota exceeded: ${stats.dailyCount}/${stats.dailyLimit}")
                return@withContext Result.failure(
                    QuotaExceededException("Limite diario atingido: ${stats.dailyCount}/${stats.dailyLimit}")
                )
            }

            // If API key not configured or model not available, use fallback
            if (generativeModel == null || currentMode == AiMode.DISABLED) {
                Log.d(TAG, "Using fallback response (no API key)")
                return@withContext Result.success(generateFallbackResponse(prompt))
            }

            Log.d(TAG, "Generating response with Gemini API")

            // Generate response with Gemini
            val response = generativeModel!!.generateContent(prompt)
            val text = response.text

            if (text.isNullOrBlank()) {
                Log.w(TAG, "Empty response from Gemini")
                return@withContext Result.success(generateFallbackResponse(prompt))
            }

            // Record successful request
            usageTracker.recordRequest()

            Log.d(TAG, "Response generated successfully")
            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response: ${e.message}", e)

            // Check for specific error types and provide better messages
            val errorMessage = when {
                e.message?.contains("quota", ignoreCase = true) == true ||
                e.message?.contains("rate limit", ignoreCase = true) == true ||
                e.message?.contains("429", ignoreCase = true) == true -> {
                    "O limite da API do Google Gemini foi atingido. " +
                    "Isto e um limite da Google, nao da app. Tenta novamente em alguns minutos."
                }
                e.message?.contains("API key", ignoreCase = true) == true ||
                e.message?.contains("unauthorized", ignoreCase = true) == true ||
                e.message?.contains("invalid", ignoreCase = true) == true ||
                e.message?.contains("401", ignoreCase = true) == true -> {
                    "Chave de API Gemini invalida ou nao configurada. Contacta o suporte."
                }
                e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("connection", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    "Erro de ligacao. Verifica a tua ligacao a internet e tenta novamente."
                }
                else -> {
                    "Desculpa, ocorreu um erro ao processar o pedido. " +
                    "Tenta novamente mais tarde.\n\nErro: ${e.message}"
                }
            }

            // Return fallback on error (don't count as usage)
            Result.success(errorMessage)
        }
    }

    /**
     * Gera recomendacoes de transferencias.
     */
    suspend fun getTransferRecommendations(
        team: FantasyTeam,
        teamCyclists: List<Cyclist>,
        availableCyclists: List<Cyclist>,
        nextRace: Race?,
        budget: Double
    ): Result<String> {
        val prompt = AiPromptTemplates.transferRecommendation(
            team = team,
            teamCyclists = teamCyclists,
            availableCyclists = availableCyclists,
            nextRace = nextRace,
            budget = budget
        )
        return generateResponse(prompt)
    }

    /**
     * Gera analise da equipa.
     */
    suspend fun getTeamAnalysis(
        team: FantasyTeam,
        teamCyclists: List<Cyclist>,
        activeCyclists: List<Cyclist>,
        captain: Cyclist?
    ): Result<String> {
        val prompt = AiPromptTemplates.teamAnalysis(
            team = team,
            teamCyclists = teamCyclists,
            activeCyclists = activeCyclists,
            captain = captain
        )
        return generateResponse(prompt)
    }

    /**
     * Gera previsao de pontos para um ciclista.
     */
    suspend fun getPointsPrediction(
        cyclist: Cyclist,
        nextRace: Race,
        recentForm: Int
    ): Result<String> {
        val prompt = AiPromptTemplates.pointsPrediction(
            cyclist = cyclist,
            nextRace = nextRace,
            recentForm = recentForm
        )
        return generateResponse(prompt)
    }

    /**
     * Gera ajuda de navegacao.
     */
    suspend fun getNavigationHelp(
        currentScreen: String,
        userQuestion: String
    ): Result<String> {
        val prompt = AiPromptTemplates.navigationHelp(
            currentScreen = currentScreen,
            userQuestion = userQuestion
        )
        return generateResponse(prompt)
    }

    /**
     * Chat geral com o assistente.
     */
    suspend fun chat(
        userMessage: String,
        conversationContext: String = ""
    ): Result<String> {
        val prompt = AiPromptTemplates.generalChat(
            userMessage = userMessage,
            conversationContext = conversationContext
        )
        return generateResponse(prompt)
    }

    /**
     * Chat com contexto do utilizador e parsing de acoes.
     * Retorna AiResponse com mensagem e acoes sugeridas.
     */
    suspend fun chatWithActions(
        userMessage: String,
        conversationContext: String = "",
        userContext: AiPromptTemplates.UserContext? = null
    ): Result<AiResponse> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                initialize()
            }

            // Check usage limits
            if (!usageTracker.canMakeRequest()) {
                val stats = usageTracker.getUsageStats()
                return@withContext Result.failure(
                    QuotaExceededException("Limite diario atingido: ${stats.dailyCount}/${stats.dailyLimit}")
                )
            }

            val prompt = AiPromptTemplates.generalChat(
                userMessage = userMessage,
                conversationContext = conversationContext,
                userContext = userContext
            )

            // If API key not configured or model not available, use fallback
            if (generativeModel == null || currentMode == AiMode.DISABLED) {
                Log.d(TAG, "Using fallback response (no API key)")
                val fallback = generateFallbackResponse(prompt)
                return@withContext Result.success(responseParser.parseResponse(fallback))
            }

            Log.d(TAG, "Generating response with actions using Gemini API")

            // Generate response with Gemini
            val response = generativeModel!!.generateContent(prompt)
            val text = response.text

            if (text.isNullOrBlank()) {
                Log.w(TAG, "Empty response from Gemini")
                val fallback = generateFallbackResponse(prompt)
                return@withContext Result.success(responseParser.parseResponse(fallback))
            }

            // Record successful request
            usageTracker.recordRequest()

            // Parse response to extract message and actions
            val parsedResponse = responseParser.parseResponse(text)
            Log.d(TAG, "Response parsed: ${parsedResponse.actions.size} actions detected")

            Result.success(parsedResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response: ${e.message}", e)

            // Check for specific error types and provide better messages
            val errorMessage = when {
                e.message?.contains("quota", ignoreCase = true) == true ||
                e.message?.contains("rate limit", ignoreCase = true) == true ||
                e.message?.contains("429", ignoreCase = true) == true -> {
                    "O limite da API do Google Gemini foi atingido. " +
                    "Isto e um limite da Google, nao da app. Tenta novamente em alguns minutos."
                }
                e.message?.contains("API key", ignoreCase = true) == true ||
                e.message?.contains("unauthorized", ignoreCase = true) == true ||
                e.message?.contains("invalid", ignoreCase = true) == true ||
                e.message?.contains("401", ignoreCase = true) == true -> {
                    "Chave de API Gemini invalida ou nao configurada. Contacta o suporte."
                }
                e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("connection", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    "Erro de ligacao. Verifica a tua ligacao a internet e tenta novamente."
                }
                else -> {
                    "Desculpa, ocorreu um erro ao processar o pedido. Tenta novamente mais tarde."
                }
            }

            Result.success(
                AiResponse(
                    message = errorMessage,
                    actions = emptyList()
                )
            )
        }
    }

    /**
     * Obtem sugestoes rapidas para o utilizador.
     */
    fun getQuickSuggestions(): List<String> {
        return AiPromptTemplates.quickSuggestions()
    }

    /**
     * Obtem o modo atual de AI.
     */
    fun getCurrentMode(): AiMode = currentMode

    /**
     * Define o modo de AI.
     */
    fun setMode(mode: AiMode) {
        currentMode = mode
        Log.d(TAG, "AI mode changed to: $mode")
    }

    /**
     * Verifica se AI esta disponivel.
     */
    fun isAvailable(): Boolean = isInitialized && currentMode != AiMode.DISABLED

    /**
     * Obtem estatisticas de uso.
     */
    suspend fun getUsageStats(): AiUsageStats = usageTracker.getUsageStats()

    // ========== Fallback Responses ==========

    /**
     * Gera resposta de fallback quando API nao esta disponivel.
     */
    private fun generateFallbackResponse(prompt: String): String {
        return when {
            prompt.contains("transferencias", ignoreCase = true) ||
            prompt.contains("comprar", ignoreCase = true) ||
            prompt.contains("vender", ignoreCase = true) -> {
                generateTransferFallback()
            }

            prompt.contains("analisa", ignoreCase = true) ||
            prompt.contains("equipa", ignoreCase = true) -> {
                generateTeamAnalysisFallback()
            }

            prompt.contains("previsao", ignoreCase = true) ||
            prompt.contains("pontos", ignoreCase = true) -> {
                generatePredictionFallback()
            }

            prompt.contains("ajuda", ignoreCase = true) ||
            prompt.contains("como", ignoreCase = true) ||
            prompt.contains("funciona", ignoreCase = true) -> {
                generateHelpFallback(prompt)
            }

            else -> generateGeneralFallback()
        }
    }

    private fun generateTransferFallback(): String {
        return """
            **Recomendacoes de Transferencias**

            Para melhores recomendacoes, analisa:

            **1. Forma atual** - Verifica ciclistas com boa forma recente
            **2. Calendario** - Adapta a equipa ao tipo de corrida
            **3. Preco** - Ciclistas em subida de preco podem valorizar

            **Dicas gerais:**
            - Vende ciclistas sem corridas proximas
            - Compra escaladores para montanha
            - Compra sprinters para etapas planas

            Vai ao **Mercado** para ver os ciclistas disponiveis!
        """.trimIndent()
    }

    private fun generateTeamAnalysisFallback(): String {
        return """
            **Analise da Equipa**

            Para otimizar a tua equipa:

            **Verifica:**
            - Tens diversidade de especialidades?
            - O capitao e o teu melhor pontuador?
            - Os ciclistas ativos sao adequados a proxima corrida?

            **Recomendacoes:**
            - Ativa escaladores para corridas de montanha
            - Ativa sprinters para etapas planas
            - Usa o Triple Captain em corridas importantes

            Vai a **Minha Equipa** para gerir a formacao!
        """.trimIndent()
    }

    private fun generatePredictionFallback(): String {
        return """
            **Previsao de Pontos**

            Os pontos dependem de varios fatores:

            **Fatores positivos:**
            - Vitoria de etapa (+15 pts)
            - Top 10 (+5-10 pts)
            - Camisola de lider (+5 pts)
            - Boa forma recente

            **Fatores negativos:**
            - Ma forma
            - Tipo de corrida nao adequado
            - Lesao ou abandono

            Consulta os **detalhes do ciclista** para ver estatisticas!
        """.trimIndent()
    }

    private fun generateHelpFallback(prompt: String): String {
        return when {
            prompt.contains("triple captain", ignoreCase = true) -> {
                """
                    **Triple Captain**

                    Multiplica os pontos do capitao por 3 (em vez de 2).

                    **Como usar:**
                    1. Vai a "Minha Equipa"
                    2. Ativa nos "Extras"
                    3. Escolhe a corrida

                    **Dica:** Usa numa corrida onde o capitao vai pontuar muito!
                """.trimIndent()
            }

            prompt.contains("wildcard", ignoreCase = true) -> {
                """
                    **Wildcard**

                    Permite transferencias ilimitadas sem penalizacao.

                    **Quando usar:**
                    - Para reconstruir a equipa
                    - Apos lesoes importantes
                    - Uma vez por temporada!
                """.trimIndent()
            }

            else -> generateGeneralFallback()
        }
    }

    private fun generateGeneralFallback(): String {
        return """
            **DS Assistant**

            Posso ajudar-te com:

            - **Equipa** - Analise e recomendacoes
            - **Transferencias** - Quem comprar/vender
            - **Wildcards** - Como e quando usar
            - **Corridas** - Estrategia por tipo

            **Sugestoes:**
            - "Analisa a minha equipa"
            - "Quem devo comprar?"
            - "Como funciona o Triple Captain?"
        """.trimIndent()
    }

    /**
     * Reset daily quota (for debug/testing only).
     */
    suspend fun resetQuota() {
        usageTracker.resetDailyCount()
        Log.d(TAG, "Quota reset completed")
    }
}

/**
 * Exception thrown when AI quota is exceeded.
 */
class QuotaExceededException(message: String) : Exception(message)
