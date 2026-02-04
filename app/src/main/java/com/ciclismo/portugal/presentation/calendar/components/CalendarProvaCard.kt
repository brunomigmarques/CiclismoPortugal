package com.ciclismo.portugal.presentation.calendar.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.ciclismo.portugal.domain.model.Prova
import com.ciclismo.portugal.presentation.components.ReminderDialog
import com.ciclismo.portugal.presentation.theme.AppColors
import com.ciclismo.portugal.presentation.theme.TypeBTT
import com.ciclismo.portugal.presentation.theme.TypeEstrada
import com.ciclismo.portugal.presentation.theme.TypeGravel

@Composable
fun CalendarProvaCard(
    prova: Prova,
    onClick: () -> Unit,
    onReminderUpdate: (Int) -> Unit,
    isUpcoming: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showReminderDialog by remember { mutableStateOf(false) }
    val typeColor = getProvaTypeColor(prova.tipo)

    // Animated border for upcoming event
    val infiniteTransition = rememberInfiniteTransition(label = "upcoming_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    if (showReminderDialog) {
        ReminderDialog(
            currentReminderDays = prova.reminderDays,
            onDismiss = { showReminderDialog = false },
            onConfirm = { days ->
                onReminderUpdate(days)
                showReminderDialog = false
            }
        )
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isUpcoming) {
                        Modifier.border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    AppColors.Green.copy(alpha = borderAlpha),
                                    AppColors.Green.copy(alpha = borderAlpha * 0.6f),
                                    AppColors.Green.copy(alpha = borderAlpha)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else Modifier
                )
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isUpcoming) 8.dp else 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Color stripe on left side indicating type
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(typeColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // Header row with type chip and reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = prova.tipo,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = typeColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    // Reminder badge and button (compact)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Source badge if available
                        prova.source?.let { source ->
                            Text(
                                text = source,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        // Reminder icon button (small)
                        IconButton(
                            onClick = { showReminderDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Lembrete: ${getReminderText(prova.reminderDays)}",
                                tint = if (prova.reminderDays > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = prova.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = prova.local,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Distances row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = typeColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = prova.distancias,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Event Image on the right
            prova.imageUrl?.let { imageUrl ->
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = prova.nome,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

        // "PRÓXIMO" badge for upcoming event
        if (isUpcoming) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = AppColors.Green,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "PRÓXIMO",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun getProvaTypeColor(tipo: String): Color {
    return when (tipo) {
        "Estrada" -> TypeEstrada
        "BTT" -> TypeBTT
        "Gravel" -> TypeGravel
        else -> TypeEstrada
    }
}

private fun getReminderText(days: Int): String {
    return when (days) {
        0 -> "Sem lembrete"
        1 -> "1 dia antes"
        7 -> "1 semana antes"
        14 -> "2 semanas antes"
        30 -> "1 mês antes"
        else -> "$days dias antes"
    }
}
