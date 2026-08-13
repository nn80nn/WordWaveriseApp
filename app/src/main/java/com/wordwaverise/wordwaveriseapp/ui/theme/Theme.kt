package com.wordwaverise.wordwaveriseapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** What the user picked in Профиль → Оформление. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

val WaveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun materialSchemeOf(c: WaveColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.secondary, onPrimary = c.onAccent,
        primaryContainer = c.surfaceElevated, onPrimaryContainer = c.textPrimary,
        secondary = c.primary, onSecondary = c.onAccent,
        tertiary = c.brass, onTertiary = c.onAccent,
        background = c.background, onBackground = c.textPrimary,
        surface = c.surface, onSurface = c.textPrimary,
        surfaceVariant = c.surfaceElevated, onSurfaceVariant = c.textSecondary,
        outline = c.borderStrong, outlineVariant = c.border,
        error = c.error, onError = c.onAccent, scrim = c.scrim
    )
} else {
    lightColorScheme(
        primary = c.secondary, onPrimary = c.onAccent,
        primaryContainer = c.surfaceElevated, onPrimaryContainer = c.textPrimary,
        secondary = c.primary, onSecondary = c.onAccent,
        tertiary = c.brass, onTertiary = c.onAccent,
        background = c.background, onBackground = c.textPrimary,
        surface = c.surface, onSurface = c.textPrimary,
        surfaceVariant = c.surfaceElevated, onSurfaceVariant = c.textSecondary,
        outline = c.borderStrong, outlineVariant = c.border,
        error = c.error, onError = c.onAccent, scrim = c.scrim
    )
}

/**
 * Dynamic colour is deliberately absent: the whole point of «Deep Water» is
 * that the app looks like itself, not like the wallpaper it was installed over.
 */
@Composable
fun WordWaveriseAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (darkTheme) DarkWave else LightWave

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // The activity already draws edge to edge, so the bars are
            // transparent and only the icon polarity is ours to set.
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalWaveColors provides colors,
        LocalContentColor provides colors.textPrimary,
        // Every bare Text(...) in the app inherits Nunito and the ink of the
        // active theme from here — which is why screens that only pass
        // fontSize/fontWeight still change with the theme.
        LocalTextStyle provides TextStyle(fontFamily = Nunito, color = colors.textPrimary)
    ) {
        MaterialTheme(
            colorScheme = materialSchemeOf(colors),
            typography = Typography,
            shapes = WaveShapes,
            content = content
        )
    }
}
