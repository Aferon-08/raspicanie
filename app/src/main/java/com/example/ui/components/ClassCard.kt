package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassEvent
import com.example.data.model.ClassStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.util.ScheduleTimeFormatter

@Composable
fun ClassCard(
    event: ClassEvent,
    currentTimeMillis: Long = System.currentTimeMillis(),
    is24HourFormat: Boolean = true,
    onCardClick: (ClassEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep the card's clock alive independently from parent recompositions.
    // This makes the progress bar and remaining-time label update every second.
    var liveTimeMillis by remember(event.id) {
        mutableLongStateOf(currentTimeMillis)
    }

    LaunchedEffect(event.id) {
        while (true) {
            liveTimeMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val status = event.getStatus(liveTimeMillis)
    val startStr = ScheduleTimeFormatter.formatTime(event.startTimeMillis, is24HourFormat)
    val endStr = ScheduleTimeFormatter.formatTime(event.endTimeMillis, is24HourFormat)

    val isOngoing = status == ClassStatus.ONGOING
    val isCancelled = status == ClassStatus.CANCELLED
    val isCompleted = status == ClassStatus.COMPLETED
    val isUpcomingSoon = status == ClassStatus.UPCOMING_SOON

    // Colors matching Material 3 Expressive
    val targetCardBackground = when {
        isOngoing -> MaterialTheme.colorScheme.primary
        isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        isCompleted -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val cardBackground by animateColorAsState(
        targetValue = targetCardBackground,
        animationSpec = spring(),
        label = "class_card_background"
    )

    val cardElevation by animateDpAsState(
        targetValue = if (isOngoing) 4.dp else 0.dp,
        animationSpec = spring(),
        label = "class_card_elevation"
    )

    val contentPrimaryColor = when {
        isOngoing -> MaterialTheme.colorScheme.onPrimary
        isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val contentSecondaryColor = when {
        isOngoing -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick(event) }
            .testTag("class_card_${event.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Top Row: Time column + Lesson Type Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Time column
                Column {
                    Text(
                        text = startStr,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor,
                        textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = endStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentSecondaryColor
                    )
                }

                // Lesson type badge (e.g. Лекция, Лаб, Практика)
                val chipBg = if (isOngoing) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                }
                val chipTextColor = if (isOngoing) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipBg)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isCancelled) "Отменено" else event.displayLessonType,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCancelled) MaterialTheme.colorScheme.error else chipTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Class Title
            Text(
                text = event.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentPrimaryColor,
                lineHeight = 23.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subgroups or Teacher info
            val subgroups = event.displaySubgroups
            if (subgroups.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    subgroups.forEach { sub ->
                        val subBg = if (isOngoing) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                        val subCircleBg = if (isOngoing) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(subBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            // Subgroup number circle
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(subCircleBg)
                            ) {
                                Text(
                                    text = sub.number,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = contentPrimaryColor
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Teacher name
                            Text(
                                text = sub.teacher,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = contentPrimaryColor,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Room number
                            if (sub.room.isNotBlank()) {
                                Text(
                                    text = sub.room,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = contentSecondaryColor
                                )
                            }
                        }
                    }
                }
            } else {
                // Single teacher and room
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (event.teacher.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = contentSecondaryColor,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = event.teacher,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = contentPrimaryColor
                            )
                        }
                    }

                    if (event.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.MeetingRoom,
                                contentDescription = null,
                                tint = contentSecondaryColor,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = event.location,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = contentSecondaryColor
                            )
                        }
                    }
                }
            }

            // Bottom Section: Ongoing wave progress OR Upcoming countdown
            if (isOngoing) {
                Spacer(modifier = Modifier.height(14.dp))
                val remainingMin = event.getRemainingMinutes(liveTimeMillis)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "СЕЙЧАС ИДЁТ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "осталось $remainingMin мин",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }

                // Smoothly animate the real progress between one-second clock updates.
                val targetProgress = event.getProgress(liveTimeMillis)
                val animatedProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(durationMillis = 950),
                    label = "lesson_progress"
                )

                SquigglyProgressBar(
                    progress = animatedProgress,
                    activeColor = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else if (isUpcomingSoon) {
                Spacer(modifier = Modifier.height(12.dp))
                val minutesUntil = event.getMinutesUntilStart(liveTimeMillis)
                val timeUntilText = if (minutesUntil >= 60) {
                    val hours = minutesUntil / 60
                    val mins = minutesUntil % 60
                    if (mins > 0) "через $hours ч $mins мин" else "через $hours ч"
                } else {
                    "через $minutesUntil мин"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Начнётся $timeUntilText",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Завершено",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = contentSecondaryColor
                )
            }
        }
    }
}
