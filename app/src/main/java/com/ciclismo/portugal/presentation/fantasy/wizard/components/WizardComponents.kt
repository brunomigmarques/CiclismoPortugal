package com.ciclismo.portugal.presentation.fantasy.wizard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ciclismo.portugal.domain.model.Cyclist
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.ciclismo.portugal.domain.rules.CyclistEligibility
import com.ciclismo.portugal.presentation.fantasy.wizard.WizardStep

// Category colors
val CategoryGC = Color(0xFFFFD700)      // Gold
val CategoryClimber = Color(0xFFE74C3C) // Red
val CategorySprint = Color(0xFF2ECC71)  // Green
val CategoryTT = Color(0xFF3498DB)      // Blue
val CategoryHills = Color(0xFFF39C12)   // Orange
val CategoryOneDay = Color(0xFF9B59B6)  // Purple

fun getCategoryColor(category: CyclistCategory): Color = when (category) {
    CyclistCategory.GC -> CategoryGC
    CyclistCategory.CLIMBER -> CategoryClimber
    CyclistCategory.SPRINT -> CategorySprint
    CyclistCategory.TT -> CategoryTT
    CyclistCategory.HILLS -> CategoryHills
    CyclistCategory.ONEDAY -> CategoryOneDay
}

fun getCategoryName(category: CyclistCategory): String = when (category) {
    CyclistCategory.CLIMBER -> "Climber"
    CyclistCategory.HILLS -> "Puncher"
    CyclistCategory.TT -> "TT"
    CyclistCategory.SPRINT -> "Sprint"
    CyclistCategory.GC -> "GC"
    CyclistCategory.ONEDAY -> "Clássicas"
}

// Cycling images per category from Unsplash (free to use)
private fun getCyclingImageForStep(step: WizardStep): String = when (step) {
    WizardStep.SelectGC -> "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=800&q=80" // Tour de France leader
    WizardStep.SelectClimbers -> "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=800&q=80" // Mountain climbing
    WizardStep.SelectSprinters -> "https://images.unsplash.com/photo-1594495894542-a46cc73e081a?w=800&q=80" // Sprint finish
    WizardStep.SelectTT -> "https://images.unsplash.com/photo-1502126829571-83575bb53030?w=800&q=80" // Time trial aero position
    WizardStep.SelectPunchers -> "https://images.unsplash.com/photo-1471506480208-91b3a4cc78be?w=800&q=80" // Hilly terrain attack
    WizardStep.SelectOneDay -> "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&q=80" // Classic race cobbles
    WizardStep.Review -> "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=800&q=80" // Peloton
}

/**
 * Header with progress bar and step info
 */
@Composable
fun WizardHeader(
    currentStep: WizardStep,
    remainingBudget: Double,
    totalSelected: Int,
    onBack: () -> Unit
) {
    val categoryColor = currentStep.category?.let { getCategoryColor(it) } ?: Color(0xFF4CAF50)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF006600))
    ) {
        // Background cycling image
        AsyncImage(
            model = getCyclingImageForStep(currentStep),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.25f
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF006600).copy(alpha = 0.7f),
                            Color(0xFF004400).copy(alpha = 0.9f)
                        )
                    )
                )
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top row: Back button + Step indicator + Budget
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White
                )
            }

            // Step indicator and title combined
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentStep.titlePt,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Passo ${currentStep.stepNumber}/${currentStep.totalSteps} • $totalSelected/15",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            // Budget indicator
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%.1f", remainingBudget)}M",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Disponivel",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { currentStep.stepNumber.toFloat() / currentStep.totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFFFFCD00),
            trackColor = Color.White.copy(alpha = 0.3f)
        )
    }
    } // Close Box
}

/**
 * Visual indicator showing selection slots (●●○) - compact version
 */
@Composable
fun SelectionSlotsIndicator(
    selected: Int,
    required: Int,
    categoryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(required) { index ->
            val isFilled = index < selected
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) categoryColor
                        else categoryColor.copy(alpha = 0.15f)
                    )
                    .border(
                        width = 2.dp,
                        color = categoryColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isFilled) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        color = categoryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            if (index < required - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Count text
        Text(
            text = "$selected/$required",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (selected >= required) Color(0xFF4CAF50) else categoryColor
        )
    }
}

/**
 * Cyclist card for wizard selection
 */
@Composable
fun WizardCyclistCard(
    cyclist: Cyclist,
    isSelected: Boolean,
    eligibility: CyclistEligibility,
    onSelect: () -> Unit,
    onDeselect: () -> Unit
) {
    val isEligible = eligibility is CyclistEligibility.Eligible
    val categoryColor = getCategoryColor(cyclist.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEligible || isSelected) {
                if (isSelected) onDeselect() else onSelect()
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> categoryColor.copy(alpha = 0.15f)
                !isEligible -> Color.Gray.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, categoryColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo - top aligned to show face
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = cyclist.photoUrl,
                    contentDescription = cyclist.fullName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter // Top-down crop to show face
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name and team
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cyclist.fullName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (!isEligible && !isSelected) Color.Gray else Color.Unspecified
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = getCategoryName(cyclist.category),
                            fontSize = 10.sp,
                            color = if (!isEligible && !isSelected) Color.Gray else categoryColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = cyclist.teamName,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Show ineligibility reason
                if (!isEligible && !isSelected) {
                    val reason = when (eligibility) {
                        is CyclistEligibility.InsufficientBudget -> "Orcamento insuficiente"
                        is CyclistEligibility.TooManyFromSameTeam -> "Max 3 da mesma equipa"
                        is CyclistEligibility.CategoryFull -> "Categoria completa"
                        is CyclistEligibility.AlreadyInTeam -> "Ja selecionado"
                        else -> ""
                    }
                    if (reason.isNotBlank()) {
                        Text(
                            text = reason,
                            fontSize = 10.sp,
                            color = Color(0xFFE57373)
                        )
                    }
                }
            }

            // Price
            Text(
                text = cyclist.displayPrice,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (!isEligible && !isSelected) Color.Gray else Color(0xFF006600)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Select/Deselect button
            IconButton(
                onClick = { if (isSelected) onDeselect() else onSelect() },
                enabled = isEligible || isSelected,
                modifier = Modifier.size(48.dp) // Larger touch target
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        contentDescription = "Remover",
                        tint = Color(0xFFE57373), // Red tint for removal
                        modifier = Modifier.size(28.dp)
                    )
                } else if (isEligible) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = "Adicionar",
                        tint = Color(0xFF006600),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "Indisponivel",
                        tint = Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Row showing selected cyclists at bottom
 */
@Composable
fun SelectedCyclistsRow(
    cyclists: List<Cyclist>,
    categoryColor: Color,
    onRemove: (Cyclist) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = categoryColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Selecionados (${cyclists.size})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = categoryColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cyclists, key = { it.id }) { cyclist ->
                    SelectedCyclistChip(
                        cyclist = cyclist,
                        color = categoryColor,
                        onRemove = { onRemove(cyclist) }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedCyclistChip(
    cyclist: Cyclist,
    color: Color,
    onRemove: () -> Unit
) {
    val categoryColor = getCategoryColor(cyclist.category)

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small photo
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = cyclist.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Category badge (compact)
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = categoryColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = getCategoryName(cyclist.category),
                    fontSize = 9.sp,
                    color = categoryColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = cyclist.fullName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = cyclist.displayPrice,
                fontSize = 11.sp,
                color = Color(0xFF006600)
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = "Remover",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFE57373) // Red for clear removal action
                )
            }
        }
    }
}

/**
 * Navigation buttons (Back / Next)
 */
@Composable
fun WizardNavigationButtons(
    canGoBack: Boolean,
    canGoNext: Boolean,
    currentCount: Int,
    requiredCount: Int,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (canGoBack) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Voltar")
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.weight(if (canGoBack) 1f else 2f),
            enabled = canGoNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF006600)
            )
        ) {
            Text(
                text = if (canGoNext) "Proximo" else "Faltam ${requiredCount - currentCount}",
                color = Color.White
            )
            if (canGoNext) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}
