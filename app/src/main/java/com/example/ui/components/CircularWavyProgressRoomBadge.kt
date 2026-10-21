package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Room label surrounded by the lesson's progress, with waves while it is ongoing. */
@Composable
internal fun CircularWavyProgressRoomBadge(
    roomText: String,
    progress: Float,
    isOngoing: Boolean,
    size: Dp,
    activeColor: Color,
    trackColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "room_badge_progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).semantics(mergeDescendants = true) {
            contentDescription = "Аудитория: $roomText"
        }
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = activeColor,
            trackColor = trackColor,
            amplitude = { if (isOngoing) 1f else 0f },
            waveSpeed = if (isOngoing) 10.dp else 0.dp
        )
        Text(
            text = roomText,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp)
        )
    }
}
