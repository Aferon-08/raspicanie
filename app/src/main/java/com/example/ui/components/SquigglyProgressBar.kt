package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Wavy progress indicator in the style of the Material 3 Expressive
 * media progress bar (Pixel / Android 16):
 *  - the completed part is a moving wave,
 *  - a small gap follows it,
 *  - the remaining part is a straight rounded track ending with a dot.
 *
 * The wave amplitude eases in from the start and springs to full size
 * when the indicator appears; progress changes are spring-animated.
 */
@Composable
fun SquigglyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color,
    trackColor: Color
) {
    val transition = rememberInfiniteTransition(label = "wavy_progress")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Spring on progress and on amplitude (amplitude 0 -> 1 on first composition).
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 60f),
        label = "wavy_progress_value"
    )
    val amplitudeFactor by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 120f),
        label = "wavy_amplitude"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val width = size.width
        val midY = size.height / 2f
        val strokeWidth = 4.dp.toPx()
        val wavelength = 28.dp.toPx()
        val amplitude = 3.dp.toPx() * amplitudeFactor
        val gap = 6.dp.toPx()
        val dotRadius = 2.dp.toPx()
        val step = 1.5.dp.toPx().coerceAtLeast(1f)

        val activeEnd = (width * animatedProgress).coerceIn(0f, width)

        // Straight track after the wave (with a gap), shortened to leave room for the end dot.
        val trackStart = (activeEnd + gap + strokeWidth / 2f).coerceAtMost(width)
        val trackEnd = width - dotRadius * 2f - gap
        if (trackEnd > trackStart) {
            drawLine(
                color = trackColor,
                start = Offset(trackStart, midY),
                end = Offset(trackEnd, midY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        // End dot.
        drawCircle(
            color = if (activeEnd >= width - dotRadius * 4f) activeColor else trackColor,
            radius = dotRadius,
            center = Offset(width - dotRadius, midY)
        )

        // Wave for the completed part. Amplitude fades in over the first 24dp
        // so the start of the wave sits flat on the baseline.
        if (activeEnd > 0.5f) {
            val ramp = 24.dp.toPx()
            val path = Path()
            var x = 0f
            path.moveTo(0f, midY)
            while (x < activeEnd) {
                val edge = (x / ramp).coerceIn(0f, 1f)
                val y = midY + amplitude * edge *
                    sin((x / wavelength) * 2f * PI.toFloat() - phase)
                path.lineTo(x, y)
                x += step
            }
            val edgeEnd = (activeEnd / ramp).coerceIn(0f, 1f)
            path.lineTo(
                activeEnd,
                midY + amplitude * edgeEnd *
                    sin((activeEnd / wavelength) * 2f * PI.toFloat() - phase)
            )
            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
