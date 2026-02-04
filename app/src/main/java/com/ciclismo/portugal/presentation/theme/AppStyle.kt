package com.ciclismo.portugal.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * High-quality cycling images from Unsplash for consistent app styling.
 * All images are optimized for mobile display with appropriate quality.
 */
object AppImages {
    // Home / Main sections
    const val HOME_HEADER = "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=1200&q=80" // Mountain cycling panorama
    const val CALENDAR_HEADER = "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?w=1200&q=80" // Cyclists racing on road
    const val NEWS_HEADER = "https://images.unsplash.com/photo-1571068316344-75bc76f77890?w=1200&q=80" // Cycling race peloton
    const val PROFILE_HEADER = "https://images.unsplash.com/photo-1594495894542-a46cc73e081a?w=1200&q=80" // Sprint finish

    // Event types
    const val ROAD_CYCLING = "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=800&q=80" // Professional peloton
    const val BTT = "https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?w=800&q=80" // Mountain bike trail
    const val GRAVEL = "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&q=80" // Gravel/forest path

    // Detail pages
    const val EVENT_DETAIL = "https://images.unsplash.com/photo-1471506480208-91b3a4cc78be?w=1200&q=80" // Scenic cycling route
    const val RACE_DETAIL = "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=1200&q=80" // General cycling

    // Fantasy
    const val FANTASY_HEADER = "https://images.unsplash.com/photo-1502126829571-83575bb53030?w=1200&q=80" // Time trial
    const val FANTASY_TEAM = "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=800&q=80" // Peloton

    // WorldTour / Race details
    const val WORLDTOUR_HEADER = "https://images.unsplash.com/photo-1534787238916-9ba6764efd4f?w=1200&q=80" // Tour de France peloton
    const val RACE_DETAIL_HEADER = "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?w=1200&q=80" // Professional cycling race

    // UCI World Tour Logo - Now using local drawable: R.drawable.ic_uci_worldtour

    // Misc
    const val EMPTY_STATE = "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&q=80"
    const val ERROR_STATE = "https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?w=800&q=80"
}

/**
 * App-wide styling constants for consistency
 */
object AppStyle {
    // Card styling
    val CardCornerRadius = 12.dp
    val CardElevation = 4.dp
    val CardPadding = 16.dp

    // Button styling
    val ButtonHeight = 56.dp
    val ButtonCornerRadius = 12.dp

    // Header styling
    val HeaderHeight = 180.dp
    val HeaderImageHeight = 200.dp

    // Spacing
    val ScreenPadding = 16.dp
    val ItemSpacing = 12.dp
    val SectionSpacing = 24.dp

    // Typography weights
    val TitleWeight = FontWeight.Bold
    val SubtitleWeight = FontWeight.SemiBold
    val BodyWeight = FontWeight.Normal
}

/**
 * Portuguese flag themed colors for consistent branding
 */
object AppColors {
    val Green = Color(0xFF006600)
    val GreenDark = Color(0xFF004400)
    val GreenLight = Color(0xFF00AA00)

    val Red = Color(0xFFCC0000)
    val RedDark = Color(0xFF990000)

    val Yellow = Color(0xFFFFCC00)
    val YellowDark = Color(0xFFFFAA00)
    val YellowLight = Color(0xFFFFE680)
    val Gold = Color(0xFFFFD700)

    // Gradient for headers
    val GreenGradient = listOf(Green, GreenLight)
    val YellowGradient = listOf(YellowDark, Yellow, YellowLight)
    val BlueGradient = listOf(Color(0xFF1565C0), Color(0xFF1976D2), Color(0xFF42A5F5))

    // Overlay gradients
    val DarkOverlay = listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.7f))
    val LightOverlay = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
}

/**
 * Reusable header component with background image and gradient overlay.
 * Matches the onboarding screen style.
 */
@Composable
fun AppHeader(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    height: Dp = AppStyle.HeaderHeight,
    overlayColors: List<Color> = AppColors.DarkOverlay,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // Background image
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(colors = overlayColors)
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppStyle.ScreenPadding)
                .padding(top = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Row(content = actions)
            }
        }
    }
}

/**
 * Consistent card wrapper for the entire app
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppStyle.CardCornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = AppStyle.CardElevation),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(AppStyle.CardPadding),
            content = content
        )
    }
}

/**
 * Card with image background (like cycling type cards in onboarding)
 */
@Composable
fun AppImageCard(
    imageUrl: String,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    onClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(AppStyle.CardCornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = AppStyle.CardElevation),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            content()
        }
    }
}

/**
 * Primary action button with consistent styling
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AppColors.Green
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AppStyle.ButtonHeight),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(AppStyle.ButtonCornerRadius)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Section title with consistent styling
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        action?.invoke()
    }
}
