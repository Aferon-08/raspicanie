package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.DecelerateEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassEvent
import com.example.data.model.ClassStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.util.ScheduleTimeFormatter

private class RoomBlobShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val p = Path()
        val r = minOf(w, h) * 0.34f
        val k = 0.5522848f

        p.moveTo(r * 1.15f, 0f)
        p.lineTo(w - r, 0f)
        p.cubicTo(w - r * 0.35f, 0f, w, r * 0.25f, w, r)
        p.cubicTo(w, r * 1.25f, w - r * 0.15f, r * 1.55f, w, r * 1.9f)
        p.lineTo(w, h - r * 0.7f)
        p.cubicTo(w, h - r * 0.2f, w - r * 0.25f, h, w - r * 0.9f, h)
        p.cubicTo(w - r * 1.25f, h, w - r * 1.45f, h - r * 0.18f, w - r * 1.7f, h)
        p.lineTo(r * 0.85f, h)
        p.cubicTo(r * 0.2f, h, 0f, h - r * 0.35f, 0f, h - r * 0.95f)
        p.lineTo(0f, r * 0.85f)
        p.cubicTo(0f, r * 0.2f, r * 0.4f, 0f, r * 1.15f, 0f)
        return Outline.Generic(p)
    }
}


@Composable
private fun Gear(
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    rotation: Float,
    teeth: Int = 12,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val center = androidx.compose.ui.geometry.Offset(
            this.size.width / 2f,
            this.size.height / 2f
        )
        val outer = this.size.minDimension * 0.49f
        val root = outer * 0.76f
        val path = Path()
        val pointsPerTooth = 4
        val totalPoints = teeth * pointsPerTooth

        for (i in 0 until totalPoints) {
            val toothPhase = i % pointsPerTooth
            val angle =
                (i.toFloat() / totalPoints) * (2f * kotlin.math.PI).toFloat() -
                    (kotlin.math.PI.toFloat() / 2f)
            val radius = if (toothPhase == 1 || toothPhase == 2) outer else root
            val x = center.x + kotlin.math.cos(angle) * radius
            val y = center.y + kotlin.math.sin(angle) * radius

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        path.close()
        drawPath(path = path, color = color)
    }
}

@Composable
private fun GearCluster(
    isOngoing: Boolean,
    largeColor: Color,
    smallColor: Color,
    animationTrigger: Int = 0,
    animationDurationMillis: Int = 760,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "gear_rotation")

    val largeRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isOngoing) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "large_gear_rotation"
    )

    val smallRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isOngoing) -360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "small_gear_rotation"
    )

    val entranceRotation = remember { Animatable(0f) }

    LaunchedEffect(animationTrigger) {
        if (animationTrigger > 0) {
            entranceRotation.snapTo(0f)
            entranceRotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(
                    durationMillis = animationDurationMillis,
                    easing = if (animationDurationMillis >= 1500) {
                        // Для учебный день → учебный день сразу задаём высокую
                        // начальную скорость и затем непрерывно замедляемся.
                        DecelerateEasing
                    } else {
                        FastOutSlowInEasing
                    }
                )
            )
        }
    }

    Box(modifier = modifier) {
        // Сначала рисуем маленькую шестерёнку: она находится ЗА большой.
        Gear(
            size = 82.dp,
            color = smallColor,
            rotation = smallRotation - entranceRotation.value * 1.35f,
            teeth = 10,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = 96.dp)
        )

        // Большая шестерёнка рисуется поверх маленькой.
        Gear(
            size = 154.dp,
            color = largeColor,
            rotation = largeRotation + entranceRotation.value,
            teeth = 13,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 12.dp, y = 0.dp)
        )
    }
}


@Composable
fun ClassCard(
    event: ClassEvent,
    currentTimeMillis: Long = System.currentTimeMillis(),
    is24HourFormat: Boolean = true,
    forceOngoingAnimation: Boolean = false,
    gearAnimationTrigger: Int = 0,
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
            // Status and the displayed remaining minutes only change at a human-visible
            // cadence. Updating every second forced every visible card to recompose
            // continuously; five seconds keeps the UI responsive without changing
            // the visual behavior in any meaningful way.
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
    val startStr = ScheduleTimeFormatter.formatTime(event.startTimeMillis, is24HourFormat)
    val endStr = ScheduleTimeFormatter.formatTime(event.endTimeMillis, is24HourFormat)
    val isOngoing = status == ClassStatus.ONGOING
    val isCancelled = status == ClassStatus.CANCELLED
    val isCompleted = status == ClassStatus.COMPLETED
    val isUpcomingSoon = status == ClassStatus.UPCOMING_SOON

    val room = event.displaySubgroups.firstOrNull()?.room?.takeIf { it.isNotBlank() }
        ?: event.location
    val roomText = room.replace("Кабинет ", "").replace("каб. ", "")

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 700f),
        label = "class_press_scale"
    )

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(30.dp))
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            modifier = Modifier
                .fillMaxWidth()
                .height(214.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onCardClick(event) }
                .testTag("class_card_${event.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                val chipBg = if (isOngoing) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                }
                val chipTextColor = if (isOngoing) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.primary
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.width(68.dp)) {
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

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(chipBg)
                                .padding(horizontal = 11.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isCancelled) "Отменено" else event.displayLessonType,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCancelled) MaterialTheme.colorScheme.error else chipTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(9.dp))

                        Text(
                            text = event.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentPrimaryColor,
                            lineHeight = 21.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(9.dp))

                        if (event.isOnline) {
                            val meetingUrl = event.onlineMeetingUrl
                            val meetingId = event.onlineMeetingId
                            val meetingPassword = event.onlineMeetingPassword
                            val hasMeetingDetails = meetingUrl != null || meetingId != null || meetingPassword != null

                            if (hasMeetingDetails) {
                                Spacer(modifier = Modifier.height(9.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isOngoing) {
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                                            } else {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            }
                                        )
                                        .padding(horizontal = 11.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "ОНЛАЙН • ZOOM",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp,
                                        color = if (isOngoing) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )

                                    meetingId?.let { id ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Конференция: $id",
                                            fontSize = 12.sp,
                                            color = contentSecondaryColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    meetingPassword?.let { password ->
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Пароль: $password",
                                            fontSize = 12.sp,
                                            color = contentSecondaryColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    meetingUrl?.let { url ->
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = url,
                                            fontSize = 11.sp,
                                            color = if (isOngoing) {
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        if (isCancelled) {
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Занятие отменено",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(78.dp))
                }

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
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }

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

                val subgroups = event.displaySubgroups
                if (subgroups.size > 1) {
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subgroups.forEach { subgroup ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = subgroup.teacher.ifBlank { "Преподаватель не указан" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentSecondaryColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (subgroup.room.isNotBlank()) {
                                        Icon(
                                            imageVector = Icons.Outlined.MeetingRoom,
                                            contentDescription = null,
                                            tint = contentSecondaryColor,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = subgroup.room.replace("Кабинет ", "").replace("каб. ", ""),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = contentPrimaryColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val teacher = subgroups.firstOrNull()?.teacher?.takeIf { it.isNotBlank() } ?: event.teacher
                    if (teacher.isNotBlank()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = contentSecondaryColor,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = teacher,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentPrimaryColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (room.isNotBlank() && event.displaySubgroups.size <= 1) {
            GearCluster(
                isOngoing = isOngoing,
                largeColor = if (isOngoing) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                },
                smallColor = if (isOngoing) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.42f)
                } else {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.78f)
                },
                animationTrigger = gearAnimationTrigger,
                animationDurationMillis = gearAnimationDurationMillis,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 12.dp, y = 8.dp)
                    .size(width = 168.dp, height = 198.dp)
            )

            // Кабинет расположен внутри большой шестерёнки и не вращается вместе с ней.
            // Отдельный слой поверх шестерёнки: его центр совпадает с центром большой шестерёнки.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = 10.dp)
                    .size(154.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.size(116.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MeetingRoom,
                        contentDescription = null,
                        tint = contentSecondaryColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = roomText,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentPrimaryColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
