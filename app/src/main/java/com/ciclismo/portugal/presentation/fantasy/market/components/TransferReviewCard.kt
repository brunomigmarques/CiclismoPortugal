package com.ciclismo.portugal.presentation.fantasy.market.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Unified inline card that replaces multiple confirmation dialogs.
 * Shows transfer summary, penalty info, and action buttons in one place.
 */
@Composable
fun TransferReviewCard(
    pendingAdditions: Int,
    pendingRemovals: Int,
    transferCount: Int,
    freeTransfers: Int,
    transferPenalty: Int,
    hasUnlimitedTransfers: Boolean,
    hasWildcard: Boolean,
    isTeamComplete: Boolean,
    isConfirming: Boolean,
    onConfirm: () -> Unit,
    onUseWildcard: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPendingChanges = pendingAdditions > 0 || pendingRemovals > 0
    val showPenaltyWarning = transferPenalty > 0 && !hasUnlimitedTransfers

    AnimatedVisibility(
        visible = hasPendingChanges,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (showPenaltyWarning) {
                                    listOf(Color(0xFFF39C12), Color(0xFFE67E22))
                                } else {
                                    listOf(Color(0xFF4CAF50), Color(0xFF45A049))
                                }
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (showPenaltyWarning) Icons.Default.Warning else Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showPenaltyWarning) "Rever Transferencias" else "Confirmar Alteracoes",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }

                        // Discard button
                        IconButton(
                            onClick = onDiscard,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Descartar",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Transfer summary row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Additions
                        if (pendingAdditions > 0) {
                            TransferStatItem(
                                icon = Icons.Default.Add,
                                value = "+$pendingAdditions",
                                label = "Entradas",
                                color = Color(0xFF4CAF50)
                            )
                        }

                        // Removals
                        if (pendingRemovals > 0) {
                            TransferStatItem(
                                icon = Icons.Default.Remove,
                                value = "-$pendingRemovals",
                                label = "Saidas",
                                color = Color(0xFFE53935)
                            )
                        }

                        // Transfer count / free
                        TransferStatItem(
                            icon = Icons.Default.SwapHoriz,
                            value = "$transferCount/$freeTransfers",
                            label = if (hasUnlimitedTransfers) "Wildcard" else "Gratis",
                            color = if (hasUnlimitedTransfers) Color(0xFF9C27B0) else Color(0xFF2196F3)
                        )

                        // Penalty (if any)
                        if (showPenaltyWarning) {
                            TransferStatItem(
                                icon = Icons.Default.Warning,
                                value = "-$transferPenalty",
                                label = "Pontos",
                                color = Color(0xFFE53935)
                            )
                        }
                    }

                    // Penalty explanation
                    if (showPenaltyWarning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFF39C12),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cada transferencia extra custa 4 pontos. " +
                                            "Tens $transferCount mas so $freeTransfers sao gratis.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF795548),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Wildcard button (if penalty and wildcard available)
                        if (showPenaltyWarning && hasWildcard) {
                            OutlinedButton(
                                onClick = onUseWildcard,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF9C27B0)
                                ),
                                enabled = !isConfirming
                            ) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Wildcard", fontSize = 13.sp)
                            }
                        }

                        // Confirm button
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showPenaltyWarning) Color(0xFFF39C12) else Color(0xFF4CAF50)
                            ),
                            enabled = !isConfirming
                        ) {
                            if (isConfirming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A confirmar...", fontSize = 13.sp)
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val buttonText = when {
                                    showPenaltyWarning -> "Confirmar (-$transferPenalty pts)"
                                    isTeamComplete -> "Confirmar Equipa"
                                    else -> "Confirmar"
                                }
                                Text(buttonText, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
