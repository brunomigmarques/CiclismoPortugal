package com.ciclismo.portugal.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextPrimaryDark,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = Secondary,
    onSecondary = TextPrimaryLight,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = TextSecondaryDark,
    tertiary = TypeGranFondo,
    onTertiary = TextPrimaryLight,
    error = Error,
    errorContainer = Error,
    onError = TextPrimaryDark,
    onErrorContainer = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = TextPrimaryDark,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = Secondary,
    onSecondary = TextPrimaryLight,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = TypeGranFondo,
    onTertiary = TextPrimaryLight,
    error = Error,
    errorContainer = Error,
    onError = TextPrimaryDark,
    onErrorContainer = TextPrimaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = TextSecondaryLight
)

@Composable
fun CiclismoPortugalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
