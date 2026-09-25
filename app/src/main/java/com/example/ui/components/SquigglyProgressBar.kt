package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive squiggly wave progress bar, matching the Android 13/14
 * media wave and the user's reference designs.
 */
@Composable
fun SquigglyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color,
    trackColor: Color
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val totalWidth = size.width
        val midY = size.height / 2f
        val clampedProgress = progress.coerceIn(0.04f, 1f)
        val activeWidth = totalWidth * clampedProgress

        // Wavelength and amplitude for smooth sinusoidal wave
        val waveStep = 20.dp.toPx()
        val amplitude = 3.5.dp.toPx()

        if (activeWidth > 0f) {
            val path = Path().apply {
                moveTo(0f, midY)
                var currentX = 0f
                var waveIndex = 0
                while (currentX < activeWidth) {
                    val nextX = (currentX + waveStep / 2).coerceAtMost(activeWidth)
                    val cX = currentX + (nextX - currentX) / 2
                    val cY = if (waveIndex % 2 == 0) midY - amplitude else midY + amplitude
                    quadraticBezierTo(cX, cY, nextX, midY)
                    currentX = nextX
                    waveIndex++
                }
            }

            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Remaining track as straight line
        if (activeWidth < totalWidth) {
            drawLine(
                color = trackColor,
                start = Offset(activeWidth + 4.dp.toPx(), midY),
                end = Offset(totalWidth, midY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
