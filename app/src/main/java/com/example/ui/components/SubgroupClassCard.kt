package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassEvent
import com.example.data.model.ClassStatus
import com.example.data.model.stripSubgroupMarker
import com.example.util.ScheduleTimeFormatter
import kotlinx.coroutines.delay

/**
 * Dedicated card for parallel subgroup lessons designed according to
 * Material 3 Expressive guidelines.
 *
 * Features:
 *  - Real-time remaining time countdown ("СЕЙЧАС ИДЁТ • осталось X мин") without expanding subgroups.
 *  - Circular wavy progress indicator around each subgroup room badge (as seen in Android 16 / M3 Expressive).
 *  - Rotating gears animation in the header background for ongoing classes, synced with day changes and debug mode.
 *  - Expressive corner morphing (32dp -> 20dp on press) with spring physics and tactile scale.
 *  - Pulsing aura halo for ongoing classes.
 */
@Composable
fun SubgroupClassCard(
    event: ClassEvent,
    currentTimeMillis: Long = System.currentTimeMillis(),
    is24HourFormat: Boolean = true,
    forceOngoingAnimation: Boolean = false,
    gearAnimationStartTimeMillis: Long = 0L,
    gearAnimationDurationMillis: Int = 760,
    onCardClick: (ClassEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var liveTimeMillis by remember(event.id) {
        mutableLongStateOf(currentTimeMillis)
    }

    LaunchedEffect(event.id) {
        while (true) {
            liveTimeMillis = System.currentTimeMillis()
            delay(5_000L)
        }
    }

    val animationTimeMillis = if (forceOngoingAnimation) {
        event.startTimeMillis + ((event.endTimeMillis - event.startTimeMillis) / 2L)
    } else {
        liveTimeMillis
    }

    val status = if (forceOngoingAnimation) {
        ClassStatus.ONGOING
    } else {
        event.getStatus(animationTimeMillis)
    }

    val isOngoing = status == ClassStatus.ONGOING
    val isCancelled = status == ClassStatus.CANCELLED
    val isCompleted = status == ClassStatus.COMPLETED
    val isUpcomingSoon = status == ClassStatus.UPCOMING_SOON

    val startStr = ScheduleTimeFormatter.formatTime(event.startTimeMillis, is24HourFormat)
    val endStr = ScheduleTimeFormatter.formatTime(event.endTimeMillis, is24HourFormat)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 700f),
        label = "subgroup_press_scale"
    )

    // Expressive shape morph: corners tighten while the card is pressed.
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 20.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "subgroup_card_corner"
    )
    val cardShape = RoundedCornerShape(cornerRadius)

    val targetCardBackground = when {
        isOngoing -> MaterialTheme.colorScheme.primary
        isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        isCompleted -> MaterialTheme.colorScheme.surfaceContainerLow
        isUpcomingSoon -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val cardBackground by animateColorAsState(
        targetValue = targetCardBackground,
        animationSpec = spring(),
        label = "subgroup_card_background"
    )

    val cardElevation by animateDpAsState(
        targetValue = if (isOngoing) 4.dp else 0.dp,
        animationSpec = spring(),
        label = "subgroup_card_elevation"
    )

    val contentPrimaryColor = when {
        isOngoing -> MaterialTheme.colorScheme.onPrimary
        isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val contentSecondaryColor = when {
        isOngoing -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val haptics = LocalHapticFeedback.current
    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val pulseInfiniteTransition = rememberInfiniteTransition(label = "pulse_subgroup_halo")
    val pulseAlpha by pulseInfiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_subgroup_alpha"
    )

    val dotAlpha by pulseInfiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ongoing_dot_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
    ) {
        if (isOngoing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.04f
                        scaleY = 1.06f
                        alpha = pulseAlpha
                    }
                    .background(MaterialTheme.colorScheme.primary, cardShape)
            )
        }

        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .clipToBounds()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onCardClick(event) }
                .testTag("subgroup_class_card_${event.id}")
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background GearCluster spinning when the class is ongoing
                GearCluster(
                    isOngoing = isOngoing,
                    largeColor = if (isOngoing) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    },
                    smallColor = if (isOngoing) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                    },
                    animationStartTimeMillis = gearAnimationStartTimeMillis,
                    animationDurationMillis = gearAnimationDurationMillis,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 12.dp, y = 8.dp)
                        .size(width = 168.dp, height = 198.dp)
                )

                // Subgroup badge centered inside the large gear (concentric with the gear center)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 24.dp, y = 10.dp)
                        .size(154.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isOngoing) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            )
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Подгруппы",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOngoing) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top header: Time, Chips ("Подгруппы" + Lesson type)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = contentSecondaryColor,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = startStr,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = contentPrimaryColor,
                                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Text(
                                    text = " — $endStr",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = contentSecondaryColor
                                )
                            }

                            Spacer(modifier = Modifier.height(7.dp))

                            Text(
                                text = stripSubgroupMarker(event.title),
                                fontSize = 21.sp,
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentPrimaryColor,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (event.displayLessonType.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isOngoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = event.displayLessonType,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOngoing) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        // Reserve space on the right for the gear with the "Подгруппы" badge
                        Spacer(modifier = Modifier.width(78.dp))
                    }

                    // Real-time lesson status (VISIBLE WITHOUT EXPANDING)
                    if (isOngoing) {
                        val remainingMin = event.getRemainingMinutes(animationTimeMillis)

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimary)
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .graphicsLayer { alpha = dotAlpha }
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "СЕЙЧАС ИДЁТ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.4.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(9.dp))

                            Text(
                                text = "осталось $remainingMin мин",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                            )
                        }
                    } else if (isUpcomingSoon) {
                        val minutesUntil = event.getMinutesUntilStart(animationTimeMillis)
                        val timeUntilText = if (minutesUntil >= 60) {
                            val hours = minutesUntil / 60
                            val mins = minutesUntil % 60
                            if (mins > 0) "через $hours ч $mins мин" else "через $hours ч"
                        } else {
                            "через $minutesUntil мин"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Начнётся $timeUntilText",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    } else if (isCompleted) {
                        Text(
                            text = "Завершено",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = contentSecondaryColor
                        )
                    }

                    // Subgroups list with Expressive micro-cards & Circular Wavy Room Badges
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        event.displaySubgroups.forEachIndexed { index, subgroup ->
                            val rowInteractionSource = remember { MutableInteractionSource() }
                            val isRowPressed by rowInteractionSource.collectIsPressedAsState()
                            val rowScale by animateFloatAsState(
                                targetValue = if (isRowPressed) 0.975f else 1f,
                                animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
                                label = "subgroup_row_scale"
                            )

                            val rowBg = if (isOngoing) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                            val subgroupNumberLabel = subgroup.number.ifBlank { (index + 1).toString() }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = rowScale
                                        scaleY = rowScale
                                    }
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(rowBg)
                                    .clickable(
                                        interactionSource = rowInteractionSource,
                                        indication = null
                                    ) {
                                        onCardClick(
                                            event.copy(
                                                id = event.id + "_subgroup_" + subgroup.number,
                                                title = subgroup.title.ifBlank { event.title },
                                                teacher = subgroup.teacher,
                                                location = subgroup.room.ifBlank { event.location }
                                            )
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Subgroup number pill / badge
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isOngoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = subgroupNumberLabel,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isOngoing) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Teacher name
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = contentSecondaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = subgroup.teacher.ifBlank { "Преподаватель не указан" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = contentPrimaryColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Circular Wavy Room Badge (matching Photo 2)
                                if (subgroup.room.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularWavyProgressRoomBadge(
                                        roomText = subgroup.room,
                                        progress = event.getProgress(animationTimeMillis),
                                        isOngoing = isOngoing,
                                        size = 48.dp,
                                        activeColor = contentPrimaryColor,
                                        trackColor = contentSecondaryColor.copy(alpha = 0.35f),
                                        contentColor = contentPrimaryColor
                                    )
                                }
                            }
                        }
                    }

                    if (isCancelled) {
                        Text(
                            text = "Занятие отменено",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (event.hasChanges && !event.changeDetails.isNullOrBlank()) {
                        Text(
                            text = event.changeDetails.orEmpty(),
                            fontSize = 12.sp,
                            color = contentSecondaryColor,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
