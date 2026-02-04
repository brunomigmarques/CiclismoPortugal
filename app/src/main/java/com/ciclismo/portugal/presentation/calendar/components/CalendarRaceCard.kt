package com.ciclismo.portugal.presentation.calendar.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciclismo.portugal.R
import com.ciclismo.portugal.domain.model.CalendarItem
import com.ciclismo.portugal.domain.model.RaceType
import com.ciclismo.portugal.presentation.theme.AppColors

// Fantasy race accent color (gold/yellow for the stripe)
private val FantasyAccent = Color(0xFFFFD700)
private val GrandTourPink = Color(0xFFE91E63)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarRaceCard(
    raceItem: CalendarItem.RaceItem,
    onClick: () -> Unit,
    isUpcoming: Boolean = false
) {
    // Color based on race type - similar to prova type colors
    val typeColor = when (raceItem.raceType) {
        RaceType.GRAND_TOUR -> GrandTourPink
        RaceType.STAGE_RACE -> Color(0xFF006600) // Green like BTT
        RaceType.ONE_DAY -> Color(0xFF9C27B0) // Purple
    }

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

    Box(modifier = Modifier.fillMaxWidth()) {
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
            // Color stripe on left side indicating type (same as ProvaCard)
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
                // Header row with type chip and Fantasy badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Chip (same style as ProvaCard)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = when (raceItem.raceType) {
                                RaceType.GRAND_TOUR -> "Grand Tour"
                                RaceType.STAGE_RACE -> "Volta"
                                RaceType.ONE_DAY -> "Clássica"
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = typeColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    // Fantasy badge + Active indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Active badge (if race is ongoing)
                        if (raceItem.isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF4CAF50)
                            ) {
                                Text(
                                    text = "AO VIVO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Fantasy icon
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = "Jogo das Apostas",
                            modifier = Modifier.size(18.dp),
                            tint = FantasyAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title (race name)
                Text(
                    text = raceItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location row (same as ProvaCard)
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
                        text = raceItem.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date/stages row (similar to distances in ProvaCard)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = typeColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            append(raceItem.formattedDateRange)
                            if (raceItem.stages > 1) {
                                append(" • ${raceItem.stages} etapas")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Category badge row
                Spacer(modifier = Modifier.height(4.dp))
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
                        text = raceItem.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // UCI World Tour logo on the right with darker gray background
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_uci_worldtour),
                    contentDescription = "UCI World Tour",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
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
