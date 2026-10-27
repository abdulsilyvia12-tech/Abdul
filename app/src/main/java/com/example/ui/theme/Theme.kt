package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BlinkPrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = BlinkPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = BlinkCyanLight,
    onSecondary = Color.Black,
    secondaryContainer = BlinkCyan,
    onSecondaryContainer = Color.White,
    tertiary = BlinkPinkLight,
    onTertiary = Color.Black,
    background = BlinkDarkBg,
    onBackground = GhostWhite,
    surface = BlinkDarkSurface,
    onSurface = GhostWhite,
    surfaceVariant = BlinkDarkSurfaceCard,
    onSurfaceVariant = SubtitleGrey,
    outline = BlinkDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = BlinkPrimary,
    onPrimary = Color.White,
    primaryContainer = BlinkPrimaryLight,
    onPrimaryContainer = Color.Black,
    secondary = BlinkCyan,
    onSecondary = Color.White,
    secondaryContainer = BlinkCyanLight,
    onSecondaryContainer = Color.Black,
    tertiary = BlinkPink,
    onTertiary = Color.White,
    background = BlinkLightBg,
    onBackground = Color(0xFF0F172A),
    surface = BlinkLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = BlinkLightSurfaceCard,
    onSurfaceVariant = Color(0xFF475569),
    outline = BlinkLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek modern dark theme for Blinkchat aesthetic
    dynamicColor: Boolean = false, // Keep consistent branding colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
