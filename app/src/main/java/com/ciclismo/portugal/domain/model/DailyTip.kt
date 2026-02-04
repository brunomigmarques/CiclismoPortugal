package com.ciclismo.portugal.domain.model

/**
 * Daily tip content to be shown to users.
 * Tips are personalized based on user preferences from onboarding.
 */
data class DailyTip(
    val id: String,
    val title: String,
    val content: String,
    val emoji: String,
    val category: TipCategory,
    val cyclingTypes: Set<CyclingType> = setOf(CyclingType.ALL), // Which cycling types this tip applies to
    val experienceLevels: Set<ExperienceLevel> = ExperienceLevel.entries.toSet() // Which levels this applies to
)

enum class TipCategory(val displayName: String, val color: Long) {
    TRAINING("Treino", 0xFF2196F3),      // Blue
    NUTRITION("Nutrição", 0xFF4CAF50),    // Green
    EQUIPMENT("Equipamento", 0xFFFF9800), // Orange
    SAFETY("Segurança", 0xFFF44336),      // Red
    MOTIVATION("Motivação", 0xFF9C27B0),  // Purple
    TECHNIQUE("Técnica", 0xFF00BCD4),     // Cyan
    RECOVERY("Recuperação", 0xFF795548),  // Brown
    EVENT("Dica de Prova", 0xFFE91E63)    // Pink
}

/**
 * Repository of cycling tips in Portuguese.
 * Tips are selected based on user preferences and day of year.
 */
object DailyTipsRepository {

    val allTips: List<DailyTip> = listOf(
        // TRAINING TIPS
        DailyTip(
            id = "training_1",
            title = "Aquecimento é fundamental",
            content = "Começa sempre com 10-15 minutos de pedalada suave antes de aumentar a intensidade. Isto prepara os músculos e reduz o risco de lesões.",
            emoji = "🔥",
            category = TipCategory.TRAINING
        ),
        DailyTip(
            id = "training_2",
            title = "Cadência ideal",
            content = "Mantém uma cadência entre 80-100 RPM para maior eficiência. Pedalar mais leve e rápido poupa energia nas subidas.",
            emoji = "⚡",
            category = TipCategory.TRAINING,
            cyclingTypes = setOf(CyclingType.ROAD, CyclingType.ALL)
        ),
        DailyTip(
            id = "training_3",
            title = "Treino intervalado",
            content = "Inclui sessões de intervalos no teu treino semanal. 4-6 repetições de 3 minutos em zona alta com 3 minutos de recuperação melhoram significativamente a tua potência.",
            emoji = "📈",
            category = TipCategory.TRAINING,
            experienceLevels = setOf(ExperienceLevel.COMPETITIVE, ExperienceLevel.RECREATIONAL)
        ),
        DailyTip(
            id = "training_4",
            title = "Consistência > Intensidade",
            content = "É melhor treinar 4x por semana de forma moderada do que 2x de forma muito intensa. A consistência constrói a base aeróbica.",
            emoji = "📅",
            category = TipCategory.TRAINING
        ),
        DailyTip(
            id = "training_5",
            title = "Treino de força",
            content = "Complementa o ciclismo com exercícios de força para core e pernas. Agachamentos e pranchas melhoram a estabilidade na bicicleta.",
            emoji = "💪",
            category = TipCategory.TRAINING
        ),

        // NUTRITION TIPS
        DailyTip(
            id = "nutrition_1",
            title = "Hidratação constante",
            content = "Bebe pequenos goles a cada 15-20 minutos, mesmo sem sede. Quando sentes sede, já estás ligeiramente desidratado.",
            emoji = "💧",
            category = TipCategory.NUTRITION
        ),
        DailyTip(
            id = "nutrition_2",
            title = "Carboidratos são amigos",
            content = "Em treinos longos (+90 min), consome 60-90g de carboidratos por hora. Géis, barras ou fruta são ótimas opções.",
            emoji = "🍌",
            category = TipCategory.NUTRITION
        ),
        DailyTip(
            id = "nutrition_3",
            title = "Janela de recuperação",
            content = "Nos 30 minutos após o treino, come algo com proteína e carboidratos. Um batido de proteína com banana é perfeito.",
            emoji = "🥤",
            category = TipCategory.NUTRITION
        ),
        DailyTip(
            id = "nutrition_4",
            title = "Pequeno-almoço de campeão",
            content = "Come um pequeno-almoço rico em carboidratos 2-3 horas antes de provas. Aveia com fruta é uma excelente escolha.",
            emoji = "🥣",
            category = TipCategory.NUTRITION
        ),
        DailyTip(
            id = "nutrition_5",
            title = "Sal nas provas longas",
            content = "Em provas de +3 horas ou calor intenso, repõe eletrólitos. Cápsulas de sal ou bebidas isotónicas previnem cãibras.",
            emoji = "🧂",
            category = TipCategory.NUTRITION
        ),

        // EQUIPMENT TIPS
        DailyTip(
            id = "equipment_1",
            title = "Pressão dos pneus",
            content = "Verifica a pressão dos pneus antes de cada saída. Pneus bem calibrados melhoram o rendimento e previnem furos.",
            emoji = "🔧",
            category = TipCategory.EQUIPMENT
        ),
        DailyTip(
            id = "equipment_2",
            title = "Corrente limpa",
            content = "Limpa e lubrifica a corrente regularmente. Uma corrente suja pode aumentar o atrito e desgastar os componentes.",
            emoji = "⛓️",
            category = TipCategory.EQUIPMENT
        ),
        DailyTip(
            id = "equipment_3",
            title = "Altura do selim",
            content = "A altura correta do selim é quando a perna fica quase totalmente esticada no ponto mais baixo da pedalada. Ajusta se sentires dor nos joelhos.",
            emoji = "📏",
            category = TipCategory.EQUIPMENT
        ),
        DailyTip(
            id = "equipment_4",
            title = "Kit de reparação",
            content = "Leva sempre câmara de ar suplente, espátulas e bomba. Um furo a 30km de casa sem material é muito frustrante!",
            emoji = "🧰",
            category = TipCategory.EQUIPMENT
        ),
        DailyTip(
            id = "equipment_5",
            title = "Capacete obrigatório",
            content = "Nunca andes sem capacete, mesmo em treinos curtos. Substitui-o após qualquer queda ou a cada 3-5 anos.",
            emoji = "⛑️",
            category = TipCategory.EQUIPMENT
        ),

        // BTT SPECIFIC
        DailyTip(
            id = "btt_1",
            title = "Suspensão ajustada",
            content = "Ajusta o SAG da suspensão ao teu peso. Normalmente 25-30% do curso total para trilhos técnicos.",
            emoji = "🏔️",
            category = TipCategory.EQUIPMENT,
            cyclingTypes = setOf(CyclingType.BTT)
        ),
        DailyTip(
            id = "btt_2",
            title = "Olha para onde queres ir",
            content = "Em BTT, a bicicleta vai para onde olhas. Foca-te no caminho, não nos obstáculos que queres evitar.",
            emoji = "👀",
            category = TipCategory.TECHNIQUE,
            cyclingTypes = setOf(CyclingType.BTT, CyclingType.GRAVEL)
        ),
        DailyTip(
            id = "btt_3",
            title = "Peso atrás nas descidas",
            content = "Em descidas técnicas, baixa o selim e desloca o peso para trás. Mantém os cotovelos e joelhos fletidos.",
            emoji = "⬇️",
            category = TipCategory.TECHNIQUE,
            cyclingTypes = setOf(CyclingType.BTT)
        ),

        // GRAVEL SPECIFIC
        DailyTip(
            id = "gravel_1",
            title = "Pneus mais largos",
            content = "Em gravel, pneus de 38-45mm oferecem melhor conforto e tração. Experimenta pressões mais baixas em terreno solto.",
            emoji = "🛞",
            category = TipCategory.EQUIPMENT,
            cyclingTypes = setOf(CyclingType.GRAVEL)
        ),
        DailyTip(
            id = "gravel_2",
            title = "Mãos nos drops",
            content = "Em descidas de gravilha, usa a parte inferior do guiador para mais controlo e melhor travagem.",
            emoji = "🚴",
            category = TipCategory.TECHNIQUE,
            cyclingTypes = setOf(CyclingType.GRAVEL)
        ),

        // SAFETY TIPS
        DailyTip(
            id = "safety_1",
            title = "Sê visível",
            content = "Usa sempre roupa com cores vivas e luzes, mesmo de dia. Luzes intermitentes aumentam muito a visibilidade.",
            emoji = "💡",
            category = TipCategory.SAFETY
        ),
        DailyTip(
            id = "safety_2",
            title = "Comunica com os carros",
            content = "Faz contacto visual com condutores em cruzamentos. Um aceno de agradecimento melhora a relação ciclistas-condutores.",
            emoji = "👋",
            category = TipCategory.SAFETY,
            cyclingTypes = setOf(CyclingType.ROAD, CyclingType.ALL)
        ),
        DailyTip(
            id = "safety_3",
            title = "Treina com companhia",
            content = "Avisa sempre alguém do teu percurso e hora prevista de regresso. Leva o telemóvel carregado.",
            emoji = "📱",
            category = TipCategory.SAFETY
        ),
        DailyTip(
            id = "safety_4",
            title = "Cuidado com a fadiga",
            content = "Os acidentes acontecem mais frequentemente quando estás cansado. Reduz riscos no final de treinos longos.",
            emoji = "😴",
            category = TipCategory.SAFETY
        ),

        // MOTIVATION TIPS
        DailyTip(
            id = "motivation_1",
            title = "Cada pedalada conta",
            content = "Não existem treinos maus. Mesmo um passeio curto é melhor do que ficar no sofá. Celebra cada saída!",
            emoji = "🎯",
            category = TipCategory.MOTIVATION
        ),
        DailyTip(
            id = "motivation_2",
            title = "Define objetivos",
            content = "Inscreve-te numa prova! Ter uma data no calendário dá motivação extra para treinar consistentemente.",
            emoji = "🏁",
            category = TipCategory.MOTIVATION
        ),
        DailyTip(
            id = "motivation_3",
            title = "Encontra parceiros de treino",
            content = "Treinar em grupo é mais divertido e motivador. Junta-te a um clube ou grupo de ciclismo local.",
            emoji = "👥",
            category = TipCategory.MOTIVATION
        ),
        DailyTip(
            id = "motivation_4",
            title = "Regista o progresso",
            content = "Usa apps como Strava para registar os treinos. Ver o progresso ao longo do tempo é muito motivador!",
            emoji = "📊",
            category = TipCategory.MOTIVATION
        ),
        DailyTip(
            id = "motivation_5",
            title = "Varia os percursos",
            content = "Explora novos caminhos regularmente. A novidade mantém o ciclismo fresco e entusiasmante.",
            emoji = "🗺️",
            category = TipCategory.MOTIVATION
        ),

        // RECOVERY TIPS
        DailyTip(
            id = "recovery_1",
            title = "Sono é treino",
            content = "O corpo recupera durante o sono. Tenta dormir 7-9 horas, especialmente após treinos intensos.",
            emoji = "😴",
            category = TipCategory.RECOVERY
        ),
        DailyTip(
            id = "recovery_2",
            title = "Dia de descanso",
            content = "Inclui pelo menos 1-2 dias de descanso por semana. A recuperação é quando o corpo fica mais forte.",
            emoji = "🛋️",
            category = TipCategory.RECOVERY
        ),
        DailyTip(
            id = "recovery_3",
            title = "Alongamentos pós-treino",
            content = "Dedica 10 minutos a alongar após cada treino. Foca nos quadríceps, isquiotibiais e costas.",
            emoji = "🧘",
            category = TipCategory.RECOVERY
        ),
        DailyTip(
            id = "recovery_4",
            title = "Foam roller",
            content = "Usa um rolo de espuma para auto-massagem. Ajuda a libertar tensão muscular e acelera a recuperação.",
            emoji = "🎢",
            category = TipCategory.RECOVERY
        ),

        // EVENT/RACE TIPS
        DailyTip(
            id = "event_1",
            title = "Reconhece o percurso",
            content = "Se possível, treina no percurso da prova antes. Conhecer as subidas e descidas dá vantagem tática.",
            emoji = "🔍",
            category = TipCategory.EVENT
        ),
        DailyTip(
            id = "event_2",
            title = "Não experimentes nada novo",
            content = "Na véspera ou dia da prova, não uses equipamento ou alimentação que não tenhas testado em treino.",
            emoji = "⚠️",
            category = TipCategory.EVENT
        ),
        DailyTip(
            id = "event_3",
            title = "Chega cedo",
            content = "Chega à prova com pelo menos 1 hora de antecedência. Tempo para levantar dorsal, aquecer e ir à casa de banho.",
            emoji = "⏰",
            category = TipCategory.EVENT
        ),
        DailyTip(
            id = "event_4",
            title = "Começa conservador",
            content = "Na partida, resiste ao impulso de acompanhar os mais rápidos. Gere o esforço para o final da prova.",
            emoji = "🐢",
            category = TipCategory.EVENT
        ),
        DailyTip(
            id = "event_5",
            title = "Diverte-te!",
            content = "Lembra-te porque começaste a pedalar. Independentemente do resultado, desfruta da experiência!",
            emoji = "🎉",
            category = TipCategory.EVENT
        ),

        // BEGINNER SPECIFIC
        DailyTip(
            id = "beginner_1",
            title = "Começa devagar",
            content = "Nos primeiros meses, foca-te em construir consistência. Não te preocupes com velocidade ou distância.",
            emoji = "🌱",
            category = TipCategory.TRAINING,
            experienceLevels = setOf(ExperienceLevel.BEGINNER)
        ),
        DailyTip(
            id = "beginner_2",
            title = "Aprende a mudar mudanças",
            content = "Pratica mudar de mudança em terreno plano. Muda antes de precisar - nas subidas é mais difícil.",
            emoji = "⚙️",
            category = TipCategory.TECHNIQUE,
            experienceLevels = setOf(ExperienceLevel.BEGINNER)
        ),
        DailyTip(
            id = "beginner_3",
            title = "Usa as duas mãos",
            content = "Pratica largar uma mão do guiador para beber ou sinalizar. É essencial para a segurança na estrada.",
            emoji = "✋",
            category = TipCategory.TECHNIQUE,
            experienceLevels = setOf(ExperienceLevel.BEGINNER)
        )
    )

    /**
     * Get a tip for today based on user preferences.
     * Uses day of year to ensure same tip shows all day.
     */
    fun getTipForToday(
        cyclingType: CyclingType = CyclingType.ALL,
        experienceLevel: ExperienceLevel = ExperienceLevel.RECREATIONAL
    ): DailyTip {
        // Filter tips based on preferences
        val relevantTips = allTips.filter { tip ->
            (tip.cyclingTypes.contains(CyclingType.ALL) || tip.cyclingTypes.contains(cyclingType)) &&
            tip.experienceLevels.contains(experienceLevel)
        }

        // Use day of year as seed for consistent daily tip
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val index = dayOfYear % relevantTips.size

        return relevantTips[index]
    }

    /**
     * Get multiple tips for a carousel or list.
     */
    fun getTipsForCarousel(
        cyclingType: CyclingType = CyclingType.ALL,
        experienceLevel: ExperienceLevel = ExperienceLevel.RECREATIONAL,
        count: Int = 5
    ): List<DailyTip> {
        val relevantTips = allTips.filter { tip ->
            (tip.cyclingTypes.contains(CyclingType.ALL) || tip.cyclingTypes.contains(cyclingType)) &&
            tip.experienceLevels.contains(experienceLevel)
        }

        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val startIndex = dayOfYear % relevantTips.size

        return (0 until count).map { offset ->
            relevantTips[(startIndex + offset) % relevantTips.size]
        }
    }
}
