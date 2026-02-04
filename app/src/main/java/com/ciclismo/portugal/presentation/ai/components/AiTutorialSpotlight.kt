package com.ciclismo.portugal.presentation.ai.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Position where the spotlight tooltip should appear relative to the highlighted element.
 */
enum class SpotlightPosition {
    ABOVE,
    BELOW,
    START,
    END
}

/**
 * Data class representing a tutorial step.
 */
data class TutorialStep(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector = Icons.Default.Lightbulb,
    val position: SpotlightPosition = SpotlightPosition.BELOW,
    val highlightPadding: Dp = 8.dp
)

/**
 * Tutorial spotlight that highlights a UI element with a tooltip.
 * Use this to guide users through the app on first visit.
 *
 * @param step The tutorial step to display
 * @param isVisible Whether the spotlight is visible
 * @param onDismiss Called when user dismisses the spotlight
 * @param onNext Called when user taps "Next" (for multi-step tutorials)
 * @param hasNext Whether there's a next step
 * @param stepIndex Current step index (1-based)
 * @param totalSteps Total number of steps
 */
@Composable
fun AiTutorialSpotlight(
    step: TutorialStep,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNext: (() -> Unit)? = null,
    hasNext: Boolean = false,
    stepIndex: Int = 1,
    totalSteps: Int = 1,
    modifier: Modifier = Modifier
) {
    // Pulse animation for the highlight
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(300)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(200)
        ),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                // Header with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF8B5CF6)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                step.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
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
                    Text(
                        text = step.description,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer with step indicator and actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step indicator
                        if (totalSteps > 1) {
                            Text(
                                text = "$stepIndex de $totalSteps",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Actions
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onDismiss,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Entendi",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            if (hasNext && onNext != null) {
                                Button(
                                    onClick = onNext,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    )
                                ) {
                                    Text(
                                        text = "Proximo",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Wrapper that adds a spotlight effect around content.
 * Use this to highlight specific UI elements during tutorials.
 */
@Composable
fun SpotlightWrapper(
    isHighlighted: Boolean,
    highlightColor: Color = Color(0xFF6366F1),
    highlightPadding: Dp = 4.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "highlight_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    Box(
        modifier = modifier
            .then(
                if (isHighlighted) {
                    Modifier
                        .padding(highlightPadding)
                        .clip(RoundedCornerShape(12.dp))
                        .background(highlightColor.copy(alpha = 0.1f))
                        .padding(2.dp)
                } else {
                    Modifier
                }
            )
    ) {
        content()

        // Animated border when highlighted
        if (isHighlighted) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                highlightColor.copy(alpha = borderAlpha),
                                highlightColor.copy(alpha = borderAlpha * 0.5f),
                                highlightColor.copy(alpha = borderAlpha)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Scrim overlay for tutorials that dims the background.
 * Tap anywhere to dismiss.
 */
@Composable
fun TutorialScrim(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismiss() }
            )
        }
    }
}

/**
 * Pre-defined tutorial steps for common screens.
 */
object TutorialSteps {
    val MARKET_INTRO = TutorialStep(
        id = "market_intro",
        title = "Bem-vindo ao Mercado",
        description = "Aqui podes comprar e vender ciclistas para a tua equipa. " +
                "Usa os filtros para encontrar os melhores ciclistas dentro do teu orcamento.",
        position = SpotlightPosition.BELOW
    )

    val MARKET_CATEGORIES = TutorialStep(
        id = "market_categories",
        title = "Categorias",
        description = "Cada ciclista tem uma categoria (GC, Climber, Sprint, etc.). " +
                "Precisas de um numero minimo de cada categoria na tua equipa.",
        position = SpotlightPosition.BELOW
    )

    val TEAM_CAPTAIN = TutorialStep(
        id = "team_captain",
        title = "Capitao",
        description = "O capitao ganha pontos duplos! Escolhe bem - " +
                "toca num ciclista e seleciona 'Definir Capitao'.",
        position = SpotlightPosition.ABOVE
    )

    val TEAM_WILDCARDS = TutorialStep(
        id = "team_wildcards",
        title = "Wildcards",
        description = "Usa wildcards estrategicamente: Triple Captain (3x pontos), " +
                "Bench Boost (todos contam), ou Wildcard (transferencias ilimitadas).",
        position = SpotlightPosition.BELOW
    )

    val LEAGUES_INTRO = TutorialStep(
        id = "leagues_intro",
        title = "Ligas",
        description = "Junta-te a ligas publicas ou cria uma privada para competir com amigos. " +
                "Ganha pontos baseado na performance dos teus ciclistas.",
        position = SpotlightPosition.BELOW
    )
}
