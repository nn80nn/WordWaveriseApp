package com.wordwaverise.wordwaveriseapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wordwaverise.wordwaveriseapp.R

/**
 * Three voices, same as the web.
 *
 * Comfortaa carries display type — a rounded geometric face that gives the
 * product its character. It ships **no italic**: emphasis in display type has
 * to come from weight and colour, because a synthesised slant on a rounded
 * geometric face looks broken. Nunito holds the running text and owns the real
 * italics. JetBrains Mono is reserved for lexicographic apparatus — IPA, parts
 * of speech, counters — anything that should read as dictionary machinery
 * rather than interface chrome.
 */
val Comfortaa = FontFamily(
    Font(R.font.comfortaa_400, FontWeight.Normal),
    Font(R.font.comfortaa_500, FontWeight.Medium),
    Font(R.font.comfortaa_600, FontWeight.SemiBold),
    Font(R.font.comfortaa_700, FontWeight.Bold)
)

val Nunito = FontFamily(
    Font(R.font.nunito_400, FontWeight.Normal),
    Font(R.font.nunito_italic_400, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.nunito_600, FontWeight.SemiBold),
    Font(R.font.nunito_italic_600, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.nunito_700, FontWeight.Bold),
    Font(R.font.nunito_800, FontWeight.ExtraBold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_400, FontWeight.Normal),
    Font(R.font.jetbrains_mono_500, FontWeight.Medium)
)

/**
 * Comfortaa sets wide by default, so display sizes need pulling together —
 * the negative tracking below is the same optical correction the web applies
 * with `.tracking-display`.
 */
val DisplayLarge = TextStyle(
    fontFamily = Comfortaa,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 39.sp,
    letterSpacing = (-0.9).sp
)

val DisplayMedium = TextStyle(
    fontFamily = Comfortaa,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.5).sp
)

val DisplaySmall = TextStyle(
    fontFamily = Comfortaa,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.3).sp
)

/** Small-caps kicker: the editorial mark that replaces a generic pill badge. */
val EyebrowStyle = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 12.sp,
    letterSpacing = 2.4.sp
)

/** IPA, part of speech, counters. */
val ApparatusStyle = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 1.1.sp
)

val Typography = Typography(
    displayLarge = DisplayLarge,
    displayMedium = DisplayMedium,
    displaySmall = DisplaySmall,
    headlineLarge = TextStyle(
        fontFamily = Comfortaa, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Comfortaa, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Comfortaa, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Comfortaa, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 23.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Nunito, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelSmall = EyebrowStyle
)
