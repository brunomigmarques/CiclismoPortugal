package com.ciclismo.portugal.presentation.fantasy.team.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciclismo.portugal.domain.model.FantasyTeam
import com.ciclismo.portugal.domain.model.Race

/**
 * Seccao de Extras/Wildcards para ativar por corrida.
 * Aparece no topo da equipa e permite ativar wildcards para a proxima corrida.
 *
 * @param compactMode When true, shows only a minimal chip that expands on click.
 *                    When false (default), shows the full collapsible card.
 */
@Composable
fun RaceExtrasSection(
    team: FantasyTeam,
    nextRace: Race?,
    onActivateTripleCaptain: (String) -> Unit,
    onDeactivateTripleCaptain: () -> Unit,
    onActivateBenchBoost: (String) -> Unit,
    onDeactivateBenchBoost: () -> Unit,
    onActivateWildcard: (String) -> Unit,
    onDeactivateWildcard: () -> Unit,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    if (nextRace == null) return

    var isExpanded by remember { mutableStateOf(false) }

    // In compact mode, show minimal chip that expands to full card
    if (compactMode) {
        CompactWildcardsBar(
            team = team,
            raceId = nextRace.id,
            raceName = nextRace.name,
            isExpanded = isExpanded,
            onToggleExpanded = { isExpanded = !isExpanded },
            onActivateTripleCaptain = { onActivateTripleCaptain(nextRace.id) },
            onDeactivateTripleCaptain = onDeactivateTripleCaptain,
            onActivateBenchBoost = { onActivateBenchBoost(nextRace.id) },
            onDeactivateBenchBoost = onDeactivateBenchBoost,
            onActivateWildcard = { onActivateWildcard(nextRace.id) },
            onDeactivateWildcard = onDeactivateWildcard,
            modifier = modifier
        )
        return
    }

    // Check if any wildcard is active for this race
    val hasActiveWildcardForRace = team.hasActiveWildcardForRace(nextRace.id)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFD700).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header - clickable to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Extras para ${nextRace.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (hasActiveWildcardForRace) {
                            Text(
                                text = "Wildcards ativos para esta corrida",
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50)
                            )
                        } else {
                            Text(
                                text = "Toca para ativar wildcards",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Fechar" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Triple Captain
                    WildcardItem(
                        icon = Icons.Default.Star,
                        name = "Triple Captain (3x)",
                        description = "Capitao marca pontos triplos em vez de duplos",
                        isAvailable = team.hasTripleCaptain,
                        isActive = team.isTripleCaptainActiveForRace(nextRace.id),
                        isUsed = team.tripleCaptainUsed,
                        onActivate = { onActivateTripleCaptain(nextRace.id) },
                        onDeactivate = onDeactivateTripleCaptain,
                        activeColor = Color(0xFFFFD700)
                    )

                    // Bench Boost
                    WildcardItem(
                        icon = Icons.Default.Groups,
                        name = "Bench Boost",
                        description = "Todos os 15 ciclistas contam pontos",
                        isAvailable = team.hasBenchBoost,
                        isActive = team.isBenchBoostActiveForRace(nextRace.id),
                        isUsed = team.benchBoostUsed,
                        onActivate = { onActivateBenchBoost(nextRace.id) },
                        onDeactivate = onDeactivateBenchBoost,
                        activeColor = Color(0xFF4CAF50)
                    )

                    // Wildcard
                    WildcardItem(
                        icon = Icons.Default.SwapHoriz,
                        name = "Wildcard",
                        description = "Transferencias ilimitadas sem penalizacao",
                        isAvailable = team.hasWildcard,
                        isActive = team.isWildcardActiveForRace(nextRace.id),
                        isUsed = team.wildcardUsed,
                        onActivate = { onActivateWildcard(nextRace.id) },
                        onDeactivate = onDeactivateWildcard,
                        activeColor = Color(0xFF2196F3)
                    )

                    // Info text
                    Text(
                        text = "Os wildcards sao usados uma vez por temporada. Ativa antes da corrida comecar.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WildcardItem(
    icon: ImageVector,
    name: String,
    description: String,
    isAvailable: Boolean,
    isActive: Boolean,
    isUsed: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    activeColor: Color
) {
    val backgroundColor = when {
        isActive -> activeColor.copy(alpha = 0.2f)
        !isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val borderColor = when {
        isActive -> activeColor
        !isAvailable -> Color.Transparent
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isActive) activeColor.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (!isAvailable) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface
                )
                if (isUsed && !isActive) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "USADO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                Color.Gray.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Toggle/Button
        if (isActive) {
            // Active - show deactivate button
            TextButton(
                onClick = onDeactivate,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = activeColor
                )
            ) {
                Text("ATIVO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Desativar",
                    modifier = Modifier.size(16.dp)
                )
            }
        } else if (isAvailable) {
            // Available - show activate button
            Button(
                onClick = onActivate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("ATIVAR", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        } else {
            // Not available (already used)
            Text(
                text = "Indisponivel",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Banner compacto para mostrar wildcards ativos (alternativa ao RaceExtrasSection expandido)
 */
@Composable
fun ActiveWildcardsChip(
    team: FantasyTeam,
    raceId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeWildcards = mutableListOf<String>()
    if (team.isTripleCaptainActiveForRace(raceId)) activeWildcards.add("3x")
    if (team.isBenchBoostActiveForRace(raceId)) activeWildcards.add("BB")
    if (team.isWildcardActiveForRace(raceId)) activeWildcards.add("WC")

    if (activeWildcards.isEmpty()) return

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFD700)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = activeWildcards.joinToString(" + "),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.Black
            )
        }
    }
}

/**
 * Compact wildcards bar - minimal UI that expands to show full wildcard controls.
 * Shows available/active wildcards in a single row with quick actions.
 */
@Composable
private fun CompactWildcardsBar(
    team: FantasyTeam,
    raceId: String,
    raceName: String,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onActivateTripleCaptain: () -> Unit,
    onDeactivateTripleCaptain: () -> Unit,
    onActivateBenchBoost: () -> Unit,
    onDeactivateBenchBoost: () -> Unit,
    onActivateWildcard: () -> Unit,
    onDeactivateWildcard: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Count available and active wildcards
    val hasAnyWildcard = team.hasTripleCaptain || team.hasBenchBoost || team.hasWildcard
    val activeCount = listOf(
        team.isTripleCaptainActiveForRace(raceId),
        team.isBenchBoostActiveForRace(raceId),
        team.isWildcardActiveForRace(raceId)
    ).count { it }

    Column(modifier = modifier.fillMaxWidth()) {
        // Compact bar (always visible)
        Surface(
            onClick = onToggleExpanded,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (activeCount > 0) Color(0xFFFFD700).copy(alpha = 0.2f)
                   else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (activeCount > 0) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Wildcards",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    // Quick status chips
                    Spacer(modifier = Modifier.width(8.dp))
                    if (activeCount > 0) {
                        // Show active wildcards inline
                        if (team.isTripleCaptainActiveForRace(raceId)) {
                            WildcardMiniChip("3x", Color(0xFFFFD700))
                        }
                        if (team.isBenchBoostActiveForRace(raceId)) {
                            WildcardMiniChip("BB", Color(0xFF4CAF50))
                        }
                        if (team.isWildcardActiveForRace(raceId)) {
                            WildcardMiniChip("WC", Color(0xFF2196F3))
                        }
                    } else if (hasAnyWildcard) {
                        Text(
                            text = "Disponiveis",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Fechar" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Expanded content
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Extras para $raceName",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    // Compact wildcard items in a row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactWildcardButton(
                            name = "3x",
                            fullName = "Triple Captain",
                            isAvailable = team.hasTripleCaptain,
                            isActive = team.isTripleCaptainActiveForRace(raceId),
                            isUsed = team.tripleCaptainUsed,
                            color = Color(0xFFFFD700),
                            onActivate = onActivateTripleCaptain,
                            onDeactivate = onDeactivateTripleCaptain,
                            modifier = Modifier.weight(1f)
                        )

                        CompactWildcardButton(
                            name = "BB",
                            fullName = "Bench Boost",
                            isAvailable = team.hasBenchBoost,
                            isActive = team.isBenchBoostActiveForRace(raceId),
                            isUsed = team.benchBoostUsed,
                            color = Color(0xFF4CAF50),
                            onActivate = onActivateBenchBoost,
                            onDeactivate = onDeactivateBenchBoost,
                            modifier = Modifier.weight(1f)
                        )

                        CompactWildcardButton(
                            name = "WC",
                            fullName = "Wildcard",
                            isAvailable = team.hasWildcard,
                            isActive = team.isWildcardActiveForRace(raceId),
                            isUsed = team.wildcardUsed,
                            color = Color(0xFF2196F3),
                            onActivate = onActivateWildcard,
                            onDeactivate = onDeactivateWildcard,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WildcardMiniChip(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CompactWildcardButton(
    name: String,
    fullName: String,
    isAvailable: Boolean,
    isActive: Boolean,
    isUsed: Boolean,
    color: Color,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isActive -> color.copy(alpha = 0.2f)
        !isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        onClick = {
            when {
                isActive -> onDeactivate()
                isAvailable -> onActivate()
                else -> { /* Used, do nothing */ }
            }
        },
        enabled = isAvailable || isActive,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, color) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = when {
                    isActive -> color
                    !isAvailable -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = when {
                    isActive -> "ATIVO"
                    isUsed -> "USADO"
                    else -> "Disponivel"
                },
                fontSize = 9.sp,
                color = when {
                    isActive -> color
                    isUsed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
    }
}
