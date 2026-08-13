package com.wordwaverise.wordwaveriseapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import kotlin.random.Random

// ── Paper tooth ────────────────────────────────────────────────────────────

/**
 * A fixed film of fine noise over the whole surface — the single cheapest
 * thing that stops flat digital colour reading as synthetic. Ported from the
 * web's `body::after` grain layer.
 *
 * The tile is generated once per process from a fixed seed, so the pattern is
 * stable across recompositions and identical on every screen; without the seed
 * the grain would visibly reshuffle on every navigation.
 */
private val grainTile: ImageBitmap by lazy {
    val size = 128
    val rnd = Random(0x5EA)
    val pixels = IntArray(size * size) {
        val v = 128 + rnd.nextInt(-96, 96)
        Color(v, v, v).toArgb()
    }
    android.graphics.Bitmap
        .createBitmap(pixels, size, size, android.graphics.Bitmap.Config.ARGB_8888)
        .asImageBitmap()
}

/** Lays the paper tooth over whatever this modifier is applied to. */
fun Modifier.paperGrain(alpha: Float): Modifier = this.drawWithContent {
    drawContent()
    drawRect(
        brush = ShaderBrush(ImageShader(grainTile, TileMode.Repeated, TileMode.Repeated)),
        alpha = alpha
    )
}

/**
 * The app's ground: theme background plus grain. Dark water takes a touch more
 * grain than paper, because a large near-black field is where banding shows.
 */
@Composable
fun Modifier.waveSurface(): Modifier {
    val colors = WaveTheme.colors
    return this
        .background(colors.background)
        .paperGrain(if (colors.isDark) 0.030f else 0.038f)
}

// ── Signature gradient ─────────────────────────────────────────────────────

/**
 * Ocean → teal, used as a signature rather than as decoration: one gradient,
 * always at the same angle, never more than once per screen.
 */
@Composable
fun signatureGradient(): Brush {
    val c = WaveTheme.colors
    return Brush.linearGradient(
        0f to c.primary,
        0.38f to c.primaryDeep,
        1f to c.secondary,
        start = Offset.Zero,
        end = Offset(1000f, 620f)
    )
}

// ── Editorial marks ────────────────────────────────────────────────────────

/**
 * Small-caps kicker sitting on a hairline. Replaces the pill badge that every
 * generated interface reaches for.
 */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = WaveTheme.colors.secondary,
    ruleWidth: Dp = 22.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            Modifier
                .width(ruleWidth)
                .height(1.dp)
                .background(color.copy(alpha = 0.55f))
        )
        Text(
            text = text.uppercase(),
            style = EyebrowStyle,
            color = color
        )
    }
}

/** Hairline rule that fades out at both ends instead of butting into nothing. */
@Composable
fun RuleFade(modifier: Modifier = Modifier) {
    val hairline = WaveTheme.colors.hairline
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.12f to hairline,
                    0.88f to hairline,
                    1f to Color.Transparent
                )
            )
    )
}

/**
 * The hand-drawn wave that underlines the one word carrying a headline.
 * Drawn as a real stroked path rather than a straight rule, so the crest
 * heights read as brushwork.
 */
@Composable
fun Modifier.waveUnderline(
    color: Color = WaveTheme.colors.secondary,
    strokeWidth: Dp = 2.dp,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 26.dp,
    gap: Dp = 3.dp
): Modifier = this.drawBehind {
    val stroke = strokeWidth.toPx()
    val amp = amplitude.toPx()
    val wave = wavelength.toPx()
    val y = size.height + gap.toPx()
    val path = Path().apply {
        moveTo(0f, y)
        var x = 0f
        var up = true
        while (x < size.width) {
            val half = wave / 2f
            val peak = if (up) y - amp else y + amp
            cubicTo(
                x + half * 0.35f, peak,
                x + half * 0.65f, peak,
                x + half, y
            )
            x += half
            up = !up
        }
    }
    drawPath(path, color = color, style = Stroke(width = stroke))
}

// ── Surfaces ───────────────────────────────────────────────────────────────

/**
 * Bathymetric contour lines — nested swells used behind accent bands. Faint by
 * intent: it should register as texture, not as an illustration.
 */
@Composable
fun Modifier.contourField(
    color: Color = Color.White,
    alpha: Float = 0.10f,
    lines: Int = 5,
    spacing: Dp = 26.dp
): Modifier = this.drawBehind {
    val step = spacing.toPx()
    val amp = step * 0.42f
    repeat(lines) { i ->
        val baseY = step * (i + 0.6f)
        val path = Path().apply {
            moveTo(-step, baseY)
            var x = -step
            var up = true
            while (x < size.width + step) {
                val half = step * 2.2f
                val peak = if (up) baseY - amp else baseY + amp
                cubicTo(
                    x + half * 0.35f, peak,
                    x + half * 0.65f, peak,
                    x + half, baseY
                )
                x += half
                up = !up
            }
        }
        drawPath(path, color = color.copy(alpha = alpha), style = Stroke(width = 1f))
    }
}
