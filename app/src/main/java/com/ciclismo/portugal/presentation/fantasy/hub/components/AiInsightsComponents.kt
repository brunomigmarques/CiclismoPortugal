package com.ciclismo.portugal.presentation.fantasy.hub.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ciclismo.portugal.data.local.premium.PremiumStatus
import com.ciclismo.portugal.domain.model.RaceInsight
import com.ciclismo.portugal.domain.model.TopCandidate

/**
 * Card showing AI-generated Top 3 cyclist candidates for the next race.
 *
 * @param onCyclistPhotoClick Callback when cyclist photo is clicked (navigate to Market)
 * @param onCyclistInsightClick Callback when cyclist row is clicked (show AI insight)
 */
@Composable
fun TopCandidatesCard(
    candidates: List<TopCandidate>,
    isLoading: Boolean,
    premiumStatus: PremiumStatus,
    onUpgradeToPremium: () -> Unit,
    onCyclistPhotoClick: ((cyclistId: String) -> Unit)? = null,
    onCyclistInsightClick: ((candidate: TopCandidate) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3A5F),
                            Color(0xFF0F2744)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Top 3 para a Etapa",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Melhores apostas para vencer",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }

                    // Trial/Premium badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (premiumStatus.isPremium) {
                            Color(0xFFFFD700).copy(alpha = 0.2f)
                        } else {
                            Color.White.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = if (premiumStatus.isPremium) "PREMIUM" else "AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (premiumStatus.isPremium) Color(0xFFFFD700) else Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on access
                when {
                    !premiumStatus.hasAccess -> {
                        // Trial expired - show upgrade prompt
                        PremiumUpgradePrompt(
                            message = "O teu trial expirou. Atualiza para Premium para ver recomendacoes AI!",
                            onUpgrade = onUpgradeToPremium
                        )
                    }
                    isLoading -> {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    candidates.isEmpty() -> {
                        // No candidates yet - could be no races or no cyclists synced
                        Text(
                            text = "Sem previsoes disponiveis. Verifica se ha corridas e ciclistas sincronizados.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        // Show candidates
                        candidates.forEach { candidate ->
                            CandidateItem(
                                candidate = candidate,
                                onPhotoClick = onCyclistPhotoClick?.let { { it(candidate.cyclistId) } },
                                onInsightClick = onCyclistInsightClick?.let { { it(candidate) } }
                            )
                            if (candidate != candidates.last()) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }

                // Trial reminder if active
                if (premiumStatus.isTrialActive && !premiumStatus.isPremium && premiumStatus.trialDaysRemaining <= 3) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF9800).copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Trial: ${premiumStatus.trialDaysRemaining} dias restantes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF9800),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single candidate item in the Top 3 list.
 *
 * @param onPhotoClick Callback when cyclist photo is clicked (navigate to Market profile)
 * @param onInsightClick Callback when row is clicked (show AI insight dialog)
 */
@Composable
private fun CandidateItem(
    candidate: TopCandidate,
    onPhotoClick: (() -> Unit)? = null,
    onInsightClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rankColor = when (candidate.rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.White
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .then(if (onInsightClick != null) Modifier.clickable(onClick = onInsightClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#${candidate.rank}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = rankColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Cyclist photo or placeholder - CLICKABLE to navigate to Market
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .then(if (onPhotoClick != null) Modifier.clickable(onClick = onPhotoClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (candidate.photoUrl != null) {
                AsyncImage(
                    model = candidate.photoUrl,
                    contentDescription = "${candidate.cyclistName} - toca para ver perfil",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "${candidate.cyclistName} - toca para ver perfil",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
            // Visual indicator that photo is clickable
            if (onPhotoClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Cyclist info - click shows AI insight
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = candidate.cyclistName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (onInsightClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Ver insight AI",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = candidate.teamName,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = candidate.reason,
                fontSize = 11.sp,
                color = Color(0xFF4CAF50),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Expected points
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = candidate.expectedPoints,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
            Text(
                text = "pts",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Card showing AI-generated race insights.
 */
@Composable
fun RaceInsightCard(
    insight: RaceInsight?,
    isLoading: Boolean,
    premiumStatus: PremiumStatus,
    onUpgradeToPremium: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Insights da Corrida",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                !premiumStatus.hasAccess -> {
                    Text(
                        text = "Atualiza para Premium para ver insights!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "A gerar insights...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                insight == null -> {
                    Text(
                        text = "Nenhuma corrida ativa para analisar.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                else -> {
                    // Race name and type
                    if (insight.stageType != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = insight.stageType,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Recommendation
                    Text(
                        text = insight.recommendation,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key factors
                    Text(
                        text = "Fatores-chave:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    insight.keyFactors.forEach { factor ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = factor,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Best categories and difficulty
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Best categories
                        Row {
                            insight.bestCategories.take(3).forEach { category ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Difficulty
                        val difficultyColor = when (insight.difficulty.lowercase()) {
                            "easy" -> Color(0xFF4CAF50)
                            "medium" -> Color(0xFFFF9800)
                            "hard" -> Color(0xFFF44336)
                            "extreme" -> Color(0xFF9C27B0)
                            else -> Color.Gray
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = difficultyColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = insight.difficulty,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = difficultyColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Premium upgrade prompt component.
 */
@Composable
private fun PremiumUpgradePrompt(
    message: String,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onUpgrade,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD700)
            )
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Atualizar para Premium",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Trial status banner shown at the top of AI section.
 */
@Composable
fun TrialStatusBanner(
    premiumStatus: PremiumStatus,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = premiumStatus.isTrialActive && !premiumStatus.isPremium,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val backgroundColor = when {
            premiumStatus.trialDaysRemaining <= 1 -> Color(0xFFF44336).copy(alpha = 0.1f)
            premiumStatus.trialDaysRemaining <= 3 -> Color(0xFFFF9800).copy(alpha = 0.1f)
            else -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        }
        val textColor = when {
            premiumStatus.trialDaysRemaining <= 1 -> Color(0xFFF44336)
            premiumStatus.trialDaysRemaining <= 3 -> Color(0xFFFF9800)
            else -> Color(0xFF4CAF50)
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = premiumStatus.message,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                }
                TextButton(onClick = onUpgrade) {
                    Text(
                        text = "Premium",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

/**
 * Dialog showing detailed AI insight for a cyclist candidate.
 */
@Composable
fun CyclistInsightDialog(
    candidate: TopCandidate,
    onDismiss: () -> Unit,
    onViewInMarket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rankColor = when (candidate.rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.White
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = Color(0xFF1E3A5F),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(rankColor.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${candidate.rank}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = rankColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = candidate.cyclistName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = candidate.teamName,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        },
        text = {
            Column {
                // Category badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = candidate.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Insight section
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Insight AI",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // The AI reason/insight
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = candidate.reason,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expected points
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pontos esperados:",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = candidate.expectedPoints,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "pts",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preço:",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${String.format("%.1f", candidate.price)}M",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onViewInMarket,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver no Mercado")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Fechar",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    )
}
