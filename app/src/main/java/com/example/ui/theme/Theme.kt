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

private val SleekColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekSecondary,
    onSecondary = SleekOnPrimary,
    secondaryContainer = SleekSecondaryContainer,
    onSecondaryContainer = SleekOnSecondaryContainer,
    tertiary = SleekSuccess,
    onTertiary = SleekOnPrimary,
    background = SleekBackground,
    onBackground = SleekOnBackground,
    surface = SleekSurface,
    onSurface = SleekOnBackground,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekTextMuted,
    outline = SleekBorder,
    outlineVariant = SleekBorderLight,
    error = SleekError,
    onError = SleekOnPrimary,
    errorContainer = SleekErrorContainer,
    onErrorContainer = SleekOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = SleekPrimaryContainer,
    onPrimary = SleekOnPrimaryContainer,
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = SleekPrimaryContainer,
    secondary = SleekSecondaryContainer,
    onSecondary = SleekOnSecondaryContainer,
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = SleekSecondaryContainer,
    tertiary = Color(0xFF6DD58C),
    onTertiary = Color(0xFF003915),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to Sleek Interface light aesthetic matching Design HTML
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> SleekColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
