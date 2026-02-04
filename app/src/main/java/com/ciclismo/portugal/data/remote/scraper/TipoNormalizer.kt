package com.ciclismo.portugal.data.remote.scraper

import android.util.Log

/**
 * Normaliza tipos de prova para 3 categorias principais:
 * - BTT (Mountain Bike)
 * - Estrada (Road Cycling - inclui Gran Fondo)
 * - Gravel
 *
 * Usa análise inteligente do nome, descrição e URL para determinar o tipo correto.
 */
object TipoNormalizer {

    private const val TAG = "TipoNormalizer"

    // BTT keywords with weights (higher = more confidence)
    private val BTT_KEYWORDS = mapOf(
        "btt" to 10,
        "mtb" to 10,
        "mountain bike" to 10,
        "mountainbike" to 10,
        "xco" to 8,
        "xc marathon" to 8,
        "xc" to 6,
        "cross country" to 8,
        "marathon btt" to 10,
        "enduro" to 8,
        "downhill" to 8,
        "dh " to 7,
        "trilhos" to 6,
        "trail" to 5,
        "todo-o-terreno" to 8,
        "todo terreno" to 8,
        "off-road" to 6,
        "offroad" to 6,
        "singletracks" to 7,
        "singletrack" to 7,
        "bike park" to 7,
        "maratona btt" to 10,
        "rally btt" to 10,
        "passeio btt" to 10,
        "subida btt" to 10,
        "descida" to 5,
        "monte" to 3,
        "serra" to 3,
        "floresta" to 3,
        // Additional Portuguese BTT event patterns
        "rota de btt" to 10,
        "rota btt" to 10,
        "percurso btt" to 10,
        "circuito btt" to 10,
        "desafio btt" to 10,
        "bttrack" to 10,
        "xc maratona" to 8,
        "marathon mtb" to 10,
        "maratona mtb" to 10,
        "passeio de btt" to 10,
        "passeio mtb" to 10,
        "raid btt" to 10,
        "raid mtb" to 10,
        "ecovia" to 4,
        "ciclovia" to 3,
        "caminho" to 2,
        "ecopista" to 3
    )

    // Estrada keywords with weights
    private val ESTRADA_KEYWORDS = mapOf(
        "estrada" to 10,
        "road" to 10,
        "gran fondo" to 10,
        "granfondo" to 10,
        "gf " to 8,
        "clássica" to 8,
        "classica" to 8,
        "pista" to 6,
        "criterium" to 8,
        "contra-relógio" to 8,
        "contra relógio" to 8,
        "crono" to 7,
        "time trial" to 8,
        "tt " to 6,
        "ciclismo de estrada" to 10,
        "prova de estrada" to 10,
        "volta" to 5,
        "tour" to 4,
        "asfalto" to 6,
        "pelotão" to 5
    )

    // Gravel keywords with weights
    private val GRAVEL_KEYWORDS = mapOf(
        "gravel" to 10,
        "all road" to 8,
        "allroad" to 8,
        "ciclocross" to 8,
        "cyclocross" to 8,
        "cx " to 6,
        "misto" to 5,
        "mixed terrain" to 7,
        "adventure cycling" to 7,
        "bikepacking" to 6,
        "ultraciclismo" to 5
    )

    // URL patterns that strongly indicate BTT
    private val BTT_URL_PATTERNS = listOf(
        "/btt", "-btt", "_btt", "btt-", "btt_", "btt.",
        "/mtb", "-mtb", "_mtb", "mtb-", "mtb_", "mtb.",
        "mountainbike", "mountain-bike",
        "/enduro", "-enduro",
        "/downhill", "-downhill",
        "/xco", "-xco",
        "/trilhos", "-trilhos"
    )

    // URL patterns that strongly indicate Gravel
    private val GRAVEL_URL_PATTERNS = listOf(
        "/gravel", "-gravel", "_gravel", "gravel-", "gravel_", "gravel.",
        "ciclocross", "cyclocross"
    )

    /**
     * Normaliza o tipo de prova analisando múltiplas fontes de informação.
     *
     * @param tipo O tipo original da prova (se disponível)
     * @param nome O nome da prova
     * @param url A URL da prova (opcional, para análise adicional)
     * @param descricao Descrição da prova (opcional)
     * @return O tipo normalizado: "BTT", "Gravel", ou "Estrada"
     */
    fun normalizeAdvanced(
        tipo: String = "",
        nome: String = "",
        url: String = "",
        descricao: String = ""
    ): String {
        // Combine all text for analysis
        val fullText = "$tipo $nome $descricao".lowercase()
        val urlLower = url.lowercase()

        // Calculate scores for each type
        var bttScore = 0
        var estradaScore = 0
        var gravelScore = 0

        // Check keywords in text
        BTT_KEYWORDS.forEach { (keyword, weight) ->
            if (fullText.contains(keyword)) {
                bttScore += weight
            }
        }

        ESTRADA_KEYWORDS.forEach { (keyword, weight) ->
            if (fullText.contains(keyword)) {
                estradaScore += weight
            }
        }

        GRAVEL_KEYWORDS.forEach { (keyword, weight) ->
            if (fullText.contains(keyword)) {
                gravelScore += weight
            }
        }

        // Check URL patterns (high confidence)
        BTT_URL_PATTERNS.forEach { pattern ->
            if (urlLower.contains(pattern)) {
                bttScore += 15  // URL patterns are strong indicators
            }
        }

        GRAVEL_URL_PATTERNS.forEach { pattern ->
            if (urlLower.contains(pattern)) {
                gravelScore += 15
            }
        }

        // Log scores for debugging
        if (bttScore > 0 || gravelScore > 0) {
            Log.d(TAG, "Scores for '$nome': BTT=$bttScore, Estrada=$estradaScore, Gravel=$gravelScore")
        }

        // Determine winner with minimum threshold
        val minThreshold = 5  // Minimum score to be considered

        return when {
            bttScore >= minThreshold && bttScore > estradaScore && bttScore > gravelScore -> "BTT"
            gravelScore >= minThreshold && gravelScore > estradaScore && gravelScore >= bttScore -> "Gravel"
            estradaScore >= minThreshold -> "Estrada"
            // If no clear winner but original type provided
            tipo.isNotBlank() -> normalizeSimple(tipo)
            // Default to Estrada
            else -> "Estrada"
        }
    }

    /**
     * Simple normalization for backwards compatibility
     */
    fun normalize(tipo: String): String {
        val tipoLower = tipo.lowercase()

        // BTT check
        if (BTT_KEYWORDS.keys.any { tipoLower.contains(it) }) {
            return "BTT"
        }

        // Gravel check
        if (GRAVEL_KEYWORDS.keys.any { tipoLower.contains(it) }) {
            return "Gravel"
        }

        // Estrada check or default
        return "Estrada"
    }

    /**
     * Simple normalization without scoring (fast path)
     */
    private fun normalizeSimple(tipo: String): String {
        val tipoLower = tipo.lowercase()

        // BTT check
        if (BTT_KEYWORDS.keys.any { tipoLower.contains(it) }) {
            return "BTT"
        }

        // Gravel check
        if (GRAVEL_KEYWORDS.keys.any { tipoLower.contains(it) }) {
            return "Gravel"
        }

        // Estrada check or default
        return "Estrada"
    }

    /**
     * Detecta o tipo baseado no nome e descrição do evento
     */
    fun detectFromText(text: String): String {
        return normalize(text)
    }

    /**
     * Detecta o tipo baseado no conteúdo da página web da prova.
     * Use quando tiver acesso ao HTML ou texto da página.
     */
    fun detectFromWebContent(
        nome: String,
        url: String,
        pageContent: String
    ): String {
        // Limit content analysis to prevent performance issues
        val limitedContent = pageContent.take(5000).lowercase()

        return normalizeAdvanced(
            tipo = "",
            nome = nome,
            url = url,
            descricao = limitedContent
        )
    }
}
