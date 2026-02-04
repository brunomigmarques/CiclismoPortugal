package com.ciclismo.portugal.presentation.fantasy.wizard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.rules.FantasyGameRules
import com.ciclismo.portugal.presentation.fantasy.wizard.WizardStep

/**
 * Review screen content showing all selections before confirmation
 */
@Composable
fun WizardReviewContent(
    allSelections: Map<CyclistCategory, List<Cyclist>>,
    remainingBudget: Double,
    onEditStep: (Int) -> Unit,
    onConfirm: () -> Unit,
    isConfirming: Boolean,
    // Transfer tracking (optional for edit mode)
    isEditMode: Boolean = false,
    transferCount: Int = 0,
    transferPenalty: Int = 0,
    remainingFreeTransfers: Int = 2,
    hasUnlimitedTransfers: Boolean = false
) {
    val totalSpent = FantasyGameRules.INITIAL_BUDGET - remainingBudget
    val totalCyclists = allSelections.values.sumOf { it.size }
    val hasPenalty = isEditMode && transferPenalty > 0 && !hasUnlimitedTransfers

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF006600).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (totalCyclists == 15) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (totalCyclists == 15) Color(0xFF4CAF50) else Color(0xFFF39C12),
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (totalCyclists == 15) "Equipa Completa!" else "Equipa Incompleta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Default.Group,
                            label = "Ciclistas",
                            value = "$totalCyclists/15",
                            color = if (totalCyclists == 15) Color(0xFF4CAF50) else Color(0xFFF39C12)
                        )
                        StatItem(
                            icon = Icons.Default.AttachMoney,
                            label = "Gasto",
                            value = "${String.format("%.1f", totalSpent)}M",
                            color = Color(0xFF006600)
                        )
                        StatItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = "Disponivel",
                            value = "${String.format("%.1f", remainingBudget)}M",
                            color = Color(0xFF3498DB)
                        )
                    }
                }
            }
        }

        // Transfer info card (only in edit mode)
        if (isEditMode && transferCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasPenalty) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = if (hasPenalty) Color(0xFFF39C12) else Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Transferencias",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }

                            if (hasUnlimitedTransfers) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF9C27B0).copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "WILDCARD",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9C27B0),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$transferCount",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = if (hasPenalty) Color(0xFFF39C12) else Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "Total",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            if (!hasUnlimitedTransfers) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$remainingFreeTransfers",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = "Gratis",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                if (hasPenalty) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "-$transferPenalty",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = Color(0xFFE53935)
                                        )
                                        Text(
                                            text = "Pontos",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        if (hasPenalty) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "4 pontos por cada transferencia extra",
                                fontSize = 11.sp,
                                color = Color(0xFFE57373),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }

        // Category sections with edit buttons
        WizardStep.selectionSteps.forEach { step ->
            val cyclists = allSelections[step.category] ?: emptyList()

            item {
                ReviewCategorySection(
                    stepNumber = step.stepNumber,
                    title = step.titlePt,
                    requiredCount = step.requiredCount,
                    cyclists = cyclists,
                    categoryColor = getCategoryColor(step.category!!),
                    onEdit = { onEditStep(step.stepNumber) }
                )
            }
        }

        // Spacer before button
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Confirm button
        item {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isConfirming && totalCyclists == 15,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006600),
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isConfirming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (isEditMode) "A atualizar equipa..." else "A criar equipa...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    val buttonText = when {
                        totalCyclists != 15 -> "COMPLETA A EQUIPA"
                        isEditMode && hasPenalty -> "CONFIRMAR (-$transferPenalty pts)"
                        isEditMode -> "CONFIRMAR ALTERACOES"
                        else -> "CONFIRMAR EQUIPA"
                    }
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ReviewCategorySection(
    stepNumber: Int,
    title: String,
    requiredCount: Int,
    cyclists: List<Cyclist>,
    categoryColor: Color,
    onEdit: () -> Unit
) {
    val isComplete = cyclists.size >= requiredCount

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete)
                MaterialTheme.colorScheme.surface
            else
                Color(0xFFFFF3E0)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Step number badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(categoryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isComplete) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "$stepNumber",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = title.replace("Escolhe ", ""),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${cyclists.size}/$requiredCount selecionados",
                            fontSize = 11.sp,
                            color = if (isComplete) Color(0xFF4CAF50) else Color(0xFFF39C12)
                        )
                    }
                }

                TextButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        modifier = Modifier.size(16.dp),
                        tint = categoryColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", fontSize = 12.sp, color = categoryColor)
                }
            }

            if (cyclists.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = categoryColor.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(12.dp))

                // Cyclists list
                cyclists.forEach { cyclist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cyclist.fullName,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = cyclist.displayPrice,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF006600)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nenhum ciclista selecionado",
                    fontSize = 12.sp,
                    color = Color(0xFFE57373)
                )
            }
        }
    }
}
