package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Expressive animated wave progress indicator.
 *
 * The filled portion follows the real lesson progress while the wave itself
 * continuously moves, giving the indicator the "alive" Material 3 Expressive
 * feeling from the reference videos.
 */
@Composable
fun SquigglyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color,
    trackColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val totalWidth = size.width
        val midY = size.height / 2f
        val clampedProgress = progress.coerceIn(0f, 1f)
        val activeWidth = totalWidth * clampedProgress

        val wavelength = 20.dp.toPx()
        val amplitude = 3.5.dp.toPx()
        val strokeWidth = 3.dp.toPx()

        val step = 2.dp.toPx().coerceAtLeast(1f)

        fun wavePath(endX: Float): Path {
            val path = Path()
            path.moveTo(0f, midY)

            var x = 0f
            while (x <= endX) {
                val y = midY + amplitude * sin(
                    ((x / wavelength) * 2f * PI.toFloat()) + phase
                )
                path.lineTo(x, y)
                x += step
            }
            return path
        }

        // The track is wavy as well, so the whole indicator has the same
        // expressive motion rather than switching to a straight line.
        drawPath(
            path = wavePath(totalWidth),
            color = trackColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        if (activeWidth > 0f) {
            drawPath(
                path = wavePath(activeWidth),
                color = activeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
