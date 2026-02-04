package com.ciclismo.portugal.presentation.fantasy.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ciclismo.portugal.presentation.theme.AppImages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameRulesScreen(
    onNavigateBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with cycling image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            // Background cycling image
            AsyncImage(
                model = AppImages.FANTASY_HEADER,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Header content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Como Jogar",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Regras e pontuação do Fantasy",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Section
            item {
                HeroCard()
            }

            // Team Composition
            item {
                RuleSection(
                    title = "Composição da Equipa",
                    icon = Icons.Default.Groups,
                    iconColor = Color(0xFF2196F3)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Cria a tua equipa com 15 ciclistas:",
                            fontWeight = FontWeight.Medium
                        )
                        CategoryItem("3 Líderes (GC)", "Candidatos à classificação geral")
                        CategoryItem("3 Escaladores", "Especialistas de montanha")
                        CategoryItem("3 Sprinters", "Velocistas para chegadas ao sprint")
                        CategoryItem("2 Contra-Relogistas", "Especialistas em contra-relógio")
                        CategoryItem("2 Punchers", "Atacantes em terreno ondulado")
                        CategoryItem("2 Classicómanos", "Especialistas em clássicas")

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoBox(
                            text = "Orçamento inicial: 100M euros",
                            color = Color(0xFF4CAF50)
                        )
                        InfoBox(
                            text = "Máximo 3 ciclistas da mesma equipa profissional",
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            // Active Team
            item {
                RuleSection(
                    title = "Equipa Ativa vs Suplentes",
                    icon = Icons.Default.SwapHoriz,
                    iconColor = Color(0xFF9C27B0)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Dos 15 ciclistas, escolhes 8 para a equipa ativa:",
                            fontWeight = FontWeight.Medium
                        )
                        BulletPoint("Apenas os 8 ciclistas ativos marcam pontos")
                        BulletPoint("Os 7 suplentes não contam (exceto com Bench Boost)")
                        BulletPoint("Podes trocar ciclistas entre ativo/suplente antes de cada corrida")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Podes mudar a formação antes de cada prova para optimizar pontos!",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Captain
            item {
                RuleSection(
                    title = "Capitão",
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFFFFD700)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Escolhe um capitão para cada corrida:",
                            fontWeight = FontWeight.Medium
                        )
                        PointsRow("Capitão normal", "Pontos x2", Color(0xFF4CAF50))
                        PointsRow("Triplo Capitão (wildcard)", "Pontos x3", Color(0xFFE91E63))

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Escolhe bem! O capitão pode fazer a diferença na classificação.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Points System
            item {
                RuleSection(
                    title = "Sistema de Pontos",
                    icon = Icons.Default.EmojiEvents,
                    iconColor = Color(0xFFFF5722)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // One-day races
                        Text(
                            text = "Corridas de Um Dia (Clássicas):",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        PointsTable(
                            items = listOf(
                                "1º lugar" to "100 pts",
                                "2º lugar" to "70 pts",
                                "3º lugar" to "50 pts",
                                "4º-5º" to "40-35 pts",
                                "6º-10º" to "30-10 pts",
                                "11º-20º" to "5 pts",
                                "21º-30º" to "2 pts"
                            )
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Stage races
                        Text(
                            text = "Etapas (Grand Tours):",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE91E63)
                        )
                        PointsTable(
                            items = listOf(
                                "1º lugar" to "50 pts",
                                "2º lugar" to "35 pts",
                                "3º lugar" to "25 pts",
                                "4º-10º" to "18-2 pts",
                                "11º-20º" to "1 pt"
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Etapas de montanha e contra-relógio: pontos x1.2",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            // Jersey Bonuses
            item {
                RuleSection(
                    title = "Bónus de Camisolas",
                    icon = Icons.Default.Checkroom,
                    iconColor = Color(0xFF00BCD4)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Bónus diários em Grand Tours:",
                            fontWeight = FontWeight.Medium
                        )
                        JerseyRow("Camisola Amarela/Rosa (GC)", "+10 pts/dia", Color(0xFFFFEB3B))
                        JerseyRow("Camisola Verde (Pontos)", "+5 pts/dia", Color(0xFF4CAF50))
                        JerseyRow("Camisola Bolinhas (Montanha)", "+5 pts/dia", Color(0xFFF44336))
                        JerseyRow("Camisola Branca (Jovem)", "+3 pts/dia", Color.White)
                    }
                }
            }

            // Final GC Bonus
            item {
                RuleSection(
                    title = "Bónus Final de Classificação Geral",
                    icon = Icons.Default.MilitaryTech,
                    iconColor = Color(0xFFFFD700)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "No final de cada Grand Tour:",
                            fontWeight = FontWeight.Medium
                        )
                        PointsTable(
                            items = listOf(
                                "1º GC" to "+200 pts",
                                "2º GC" to "+150 pts",
                                "3º GC" to "+100 pts",
                                "4º-5º GC" to "+80-60 pts",
                                "6º-10º GC" to "+50-25 pts"
                            )
                        )
                    }
                }
            }

            // Wildcards
            item {
                RuleSection(
                    title = "Wildcards (Poderes Especiais)",
                    icon = Icons.Default.Bolt,
                    iconColor = Color(0xFFFF9800)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WildcardItem(
                            name = "Triplo Capitão",
                            description = "O capitão marca pontos x3 em vez de x2",
                            usage = "1x por temporada",
                            color = Color(0xFFE91E63)
                        )
                        WildcardItem(
                            name = "Bench Boost",
                            description = "Todos os 15 ciclistas marcam pontos (incluindo suplentes)",
                            usage = "1x por temporada",
                            color = Color(0xFF4CAF50)
                        )
                        WildcardItem(
                            name = "Wildcard",
                            description = "Transferências ilimitadas sem custo de pontos",
                            usage = "2x por temporada",
                            color = Color(0xFF2196F3)
                        )

                        InfoBox(
                            text = "Usa os wildcards estrategicamente em corridas importantes!",
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            // Transfers
            item {
                RuleSection(
                    title = "Transferências",
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    iconColor = Color(0xFF607D8B)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BulletPoint("1 transferência gratuita por semana")
                        BulletPoint("Transferências adicionais custam 4 pontos cada")
                        BulletPoint("Transferências não usadas acumulam (máx. 2)")
                        BulletPoint("Wildcard = transferências ilimitadas grátis")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Os preços dos ciclistas sobem/descem consoante a popularidade.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Leagues
            item {
                RuleSection(
                    title = "Ligas",
                    icon = Icons.Default.Leaderboard,
                    iconColor = Color(0xFF3F51B5)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Compete com outros jogadores:",
                            fontWeight = FontWeight.Medium
                        )
                        BulletPoint("Liga Portugal - Classificação global de todos os jogadores")
                        BulletPoint("Ligas Privadas - Cria ou junta-te a ligas com amigos")
                        BulletPoint("Ligas Regionais - Compete com jogadores da tua zona")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Usa o código da liga para convidar amigos!",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Season
            item {
                RuleSection(
                    title = "Temporada",
                    icon = Icons.Default.DateRange,
                    iconColor = Color(0xFF795548)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "A temporada coincide com o calendário UCI WorldTour:",
                            fontWeight = FontWeight.Medium
                        )
                        BulletPoint("Começa em Janeiro com as clássicas australianas")
                        BulletPoint("Termina em Outubro com Il Lombardia")
                        BulletPoint("Inclui todos os Grand Tours (Giro, Tour, Vuelta)")
                        BulletPoint("Pontos acumulam ao longo de toda a temporada")

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoBox(
                            text = "Nova temporada = equipa nova, pontos a zero!",
                            color = Color(0xFF9C27B0)
                        )
                    }
                }
            }

            // Tips
            item {
                RuleSection(
                    title = "Dicas para Vencer",
                    icon = Icons.Default.Lightbulb,
                    iconColor = Color(0xFFFFC107)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TipItem("1", "Escolhe um capitão diferente para cada tipo de corrida")
                        TipItem("2", "Guarda o Triplo Capitão para corridas importantes (Monumentos)")
                        TipItem("3", "Usa o Bench Boost numa etapa de montanha com muitos dos teus ciclistas")
                        TipItem("4", "Não gastes transferências à toa - planeia com antecedência")
                        TipItem("5", "Acompanha a forma dos ciclistas e lesões")
                    }
                }
            }

            // Footer spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE91E63),
                            Color(0xFF9C27B0)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Fantasy Ciclismo Portugal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cria a tua equipa de sonho e compete com jogadores de todo o país!",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RuleSection(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = iconColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            content()
        }
    }
}

@Composable
private fun CategoryItem(category: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFFE91E63), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = "- $description",
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = "•",
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE91E63)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 14.sp)
    }
}

@Composable
private fun InfoBox(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun PointsRow(label: String, points: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp)
        Text(
            text = points,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun JerseyRow(jersey: String, bonus: String, jerseyColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = jerseyColor,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = jersey, fontSize = 13.sp)
        }
        Text(
            text = bonus,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PointsTable(items: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { (position, points) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = position, fontSize = 13.sp)
                Text(
                    text = points,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun WildcardItem(
    name: String,
    description: String,
    usage: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Bolt,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = description,
                fontSize = 13.sp
            )
            Text(
                text = usage,
                fontSize = 12.sp,
                color = Color.Gray,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
private fun TipItem(number: String, tip: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = Color(0xFFFFC107),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = tip,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
