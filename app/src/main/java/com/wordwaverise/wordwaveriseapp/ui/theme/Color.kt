package com.wordwaverise.wordwaveriseapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * «Deep Water» — the palette shared with the web client.
 *
 * Warm paper above the surface, deep water below it. Two rules keep the two
 * themes recognisably the same product rather than an inversion of each other:
 *
 *  - paper is never white and ink is never black. #FBF8F2 and #0B2430 carry a
 *    trace of the water's hue, which is what stops flat digital colour from
 *    reading as a template;
 *  - depth comes from stacking hairlines and low-contrast shadow, never from
 *    glow. Below the surface every accent has to carry its own light, so the
 *    accents lift in luminance while keeping their hue.
 */
@Immutable
data class WaveColors(
    val background: Color,
    val backgroundSoft: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderStrong: Color,
    val hairline: Color,
    val tag: Color,

    /** Deep ocean — structure, headings, the calm half of the signature. */
    val primary: Color,
    val primaryDeep: Color,
    val primarySoft: Color,

    /** Tide teal — the interactive accent and the crest of the logo's wave. */
    val secondary: Color,
    val secondaryDeep: Color,
    val secondarySoft: Color,

    /** Warm counterpoint. Rare by design: levels, streaks, editorial marks. */
    val brass: Color,
    val brassDeep: Color,
    val brassSoft: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    /** Text laid over a filled accent (button faces, gradient bands). */
    val onAccent: Color,

    val success: Color,
    val warning: Color,
    val error: Color,
    val premium: Color,

    val scrim: Color,
    val sheen: Color,
    val isDark: Boolean
)

val LightWave = WaveColors(
    background = Color(0xFFFBF8F2),
    backgroundSoft = Color(0xFFF3EDE1),
    surface = Color(0xFFFFFDF9),
    surfaceElevated = Color(0xFFF5EFE3),
    border = Color(0x1F0B2430),
    borderStrong = Color(0x3D0B2430),
    hairline = Color(0x1F0B2430),
    tag = Color(0x1A0D6E84),

    primary = Color(0xFF0F527E),
    primaryDeep = Color(0xFF093756),
    primarySoft = Color(0xFF2A7CAD),

    secondary = Color(0xFF0D818A),
    secondaryDeep = Color(0xFF085C63),
    secondarySoft = Color(0xFF20A6AC),

    brass = Color(0xFF9E6F29),
    brassDeep = Color(0xFF7A541C),
    brassSoft = Color(0xFFC8984E),

    textPrimary = Color(0xFF0B2430),
    textSecondary = Color(0xFF5A6C74),
    textMuted = Color(0xFF8B979B),
    onAccent = Color(0xFFFFFDF9),

    success = Color(0xFF2F7D62),
    warning = Color(0xFFB8802B),
    error = Color(0xFFB3453C),
    premium = Color(0xFF7A5AA8),

    scrim = Color(0x660B2430),
    sheen = Color(0x8CFFFFFF),
    isDark = false
)

val DarkWave = WaveColors(
    background = Color(0xFF04161E),
    backgroundSoft = Color(0xFF07202A),
    surface = Color(0xFF082430),
    surfaceElevated = Color(0xFF0D3140),
    border = Color(0x24A3D6DB),
    borderStrong = Color(0x42A3D6DB),
    hairline = Color(0x24A3D6DB),
    tag = Color(0x2465D0D0),

    primary = Color(0xFF6AB3E8),
    primaryDeep = Color(0xFF3A86BF),
    primarySoft = Color(0xFF98CEF4),

    secondary = Color(0xFF58D0C8),
    secondaryDeep = Color(0xFF2C9E98),
    secondarySoft = Color(0xFF8DE4DE),

    brass = Color(0xFFDBAF6A),
    brassDeep = Color(0xFFB08544),
    brassSoft = Color(0xFFEED0A0),

    textPrimary = Color(0xFFE9F2F1),
    textSecondary = Color(0xFF8FA8AE),
    textMuted = Color(0xFF5F7A7E),
    // On a luminous accent the label has to go dark, exactly as on the web.
    onAccent = Color(0xFF04161E),

    // The literal web tones are too dark to read on deep water; hue is kept,
    // luminance is lifted until each one clears the surface behind it.
    success = Color(0xFF5FBF9B),
    warning = Color(0xFFE0A84E),
    error = Color(0xFFE2766B),
    premium = Color(0xFFB59BE0),

    scrim = Color(0xA6000C12),
    sheen = Color(0x0DFFFFFF),
    isDark = true
)

val LocalWaveColors = staticCompositionLocalOf { LightWave }

/**
 * Shorthand for the active palette: `WaveTheme.colors.secondary`.
 */
object WaveTheme {
    val colors: WaveColors
        @Composable @ReadOnlyComposable get() = LocalWaveColors.current
}

// ── Names carried over from the previous flat palette ──────────────────────
//
// These used to be plain top-level vals, so ~580 call sites read them as bare
// identifiers. Re-declaring them as composable properties lets every one of
// those sites resolve against the active theme without being touched, which is
// what makes a dark theme possible without rewriting all fifteen screens.

val PrimaryCyan: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.secondary
val PrimaryBlue: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.primary
val PrimaryBright: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.primarySoft

val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.textPrimary
val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.textSecondary
val TextTertiary: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.textMuted
val TextPlaceholder: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.textMuted

val BackgroundPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.background
val BackgroundSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.surface
val BackgroundLight: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.surfaceElevated

val BorderLight: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.border
val BorderMedium: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.borderStrong

val Success: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.success
val Error: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.error
val Warning: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.warning
val Info: Color
    @Composable @ReadOnlyComposable get() = LocalWaveColors.current.primary
