package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ClassEvent
import com.example.data.model.ClassStatus
import com.example.ui.theme.StatusChangedYellow
import com.example.ui.theme.extraLargePlus
import com.example.util.ScheduleTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ClassDetailDialog(
    event: ClassEvent,
    currentTimeMillis: Long = System.currentTimeMillis(),
    is24HourFormat: Boolean = true,
    onDismiss: () -> Unit,
    reminderEnabled: Boolean = false,
    onToggleReminder: (ClassEvent) -> Unit
) {
    var liveTimeMillis by remember(event.id) { mutableLongStateOf(currentTimeMillis) }
    var dialogVisible by remember(event.id) { mutableStateOf(false) }
    var dragOffsetY by remember(event.id) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    fun dismissWithAnimation() {
        if (!dialogVisible) return
        dialogVisible = false
    }

    fun resetDrag() {
        scope.launch {
            androidx.compose.animation.core.Animatable(dragOffsetY).animateTo(
                0f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMedium
                )
            ) {
                dragOffsetY = value
            }
        }
    }

    LaunchedEffect(event.id) {
        dialogVisible = true
    }

    LaunchedEffect(dialogVisible) {
        if (!dialogVisible) {
            delay(300L)
            onDismiss()
        }
    }

    LaunchedEffect(event.id) {
        while (true) {
            liveTimeMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val status = event.getStatus(liveTimeMillis)
    val statusText = when (status) {
        ClassStatus.ONGOING -> "Сейчас идёт"
        ClassStatus.CANCELLED -> "Отменено"
        ClassStatus.COMPLETED -> "Завершено"
        ClassStatus.UPCOMING_SOON -> "Скоро начнётся"
        ClassStatus.SCHEDULED -> "Предстоит"
    }

    val scheme = MaterialTheme.colorScheme
    val headerContainer by animateColorAsState(
        targetValue = when (status) {
            ClassStatus.ONGOING -> scheme.primary
            ClassStatus.CANCELLED -> scheme.errorContainer
            ClassStatus.COMPLETED -> scheme.surfaceContainerHighest
            ClassStatus.UPCOMING_SOON -> scheme.tertiaryContainer
            ClassStatus.SCHEDULED -> scheme.primaryContainer
        },
        animationSpec = spring(),
        label = "detail_header_container"
    )
    val headerContent = when (status) {
        ClassStatus.ONGOING -> scheme.onPrimary
        ClassStatus.CANCELLED -> scheme.onErrorContainer
        ClassStatus.COMPLETED -> scheme.onSurfaceVariant
        ClassStatus.UPCOMING_SOON -> scheme.onTertiaryContainer
        ClassStatus.SCHEDULED -> scheme.onPrimaryContainer
    }

    val startStr = ScheduleTimeFormatter.formatTime(event.startTimeMillis, is24HourFormat)
    val endStr = ScheduleTimeFormatter.formatTime(event.endTimeMillis, is24HourFormat)
    val dateStr = ScheduleTimeFormatter
        .formatDate(event.startTimeMillis, "EEEE, d MMMM yyyy")
        .replaceFirstChar { it.uppercase() }

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 46.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = dialogVisible,
                modifier = Modifier.fillMaxWidth(),
                enter = fadeIn(animationSpec = tween(100)) +
                    slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                exit = fadeOut(animationSpec = tween(140)) +
                    slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = spring(
                            dampingRatio = 0.88f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                Surface(
                    shape = MaterialTheme.shapes.extraLargePlusTop(),
                    color = scheme.surfaceContainerLow,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .pointerInput(event.id) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                },
                                onDragEnd = {
                                    if (dragOffsetY > with(density) { 180.dp.toPx() }) {
                                        dismissWithAnimation()
                                    } else {
                                        resetDrag()
                                    }
                                },
                                onDragCancel = { resetDrag() }
                            )
                        }
                        .then(Modifier.offsetY(dragOffsetY))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(40.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(scheme.outlineVariant)
                        )
                        Spacer(Modifier.height(14.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ---- Header card ----
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(headerContainer)
                                    .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DetailChip(statusText, headerContent, headerContent.copy(alpha = 0.16f))
                                        if (event.displayLessonType.isNotBlank()) {
                                            DetailChip(event.displayLessonType, headerContent, headerContent.copy(alpha = 0.10f))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(headerContent.copy(alpha = 0.12f))
                                            .clickable { dismissWithAnimation() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Закрыть",
                                            tint = headerContent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = event.title,
                                    fontSize = 26.sp,
                                    lineHeight = 31.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.3).sp,
                                    color = headerContent,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                if (status == ClassStatus.ONGOING) {
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Осталось ${event.getRemainingMinutes(liveTimeMillis)} мин",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = headerContent.copy(alpha = 0.9f)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(modifier = Modifier.padding(end = 8.dp)) {
                                        SquigglyProgressBar(
                                            progress = event.getProgress(liveTimeMillis),
                                            activeColor = headerContent,
                                            trackColor = headerContent.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }

                            // ---- Schedule change notice ----
                            if (event.hasChanges && !event.changeDetails.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.large)
                                        .background(StatusChangedYellow.copy(alpha = 0.16f))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = StatusChangedYellow,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Изменение в расписании",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusChangedYellow
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = event.changeDetails.orEmpty(),
                                            fontSize = 14.sp,
                                            color = scheme.onSurface
                                        )
                                    }
                                }
                            }

                            // ---- Time tile ----
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.large)
                                    .background(scheme.surfaceContainerHigh)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconBadge(Icons.Outlined.AccessTime, scheme.primaryContainer, scheme.onPrimaryContainer, 52.dp)
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$startStr — $endStr",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.2).sp,
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = dateStr,
                                        fontSize = 13.sp,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                                DetailChip(
                                    text = "${event.durationMinutes} мин",
                                    contentColor = scheme.onSecondaryContainer,
                                    containerColor = scheme.secondaryContainer
                                )
                            }

                            // ---- Room / teacher tiles ----
                            val hasRoom = event.location.isNotBlank()
                            val hasTeacher = event.teacher.isNotBlank()
                            if (hasRoom || hasTeacher) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (hasRoom) {
                                        InfoTile(
                                            icon = Icons.Outlined.MeetingRoom,
                                            label = "Аудитория",
                                            value = event.location,
                                            badgeContainer = scheme.tertiaryContainer,
                                            badgeContent = scheme.onTertiaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (hasTeacher) {
                                        InfoTile(
                                            icon = Icons.Outlined.Person,
                                            label = "Преподаватель",
                                            value = event.teacher,
                                            badgeContainer = scheme.secondaryContainer,
                                            badgeContent = scheme.onSecondaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        if (status != ClassStatus.COMPLETED && status != ClassStatus.CANCELLED) {
                            Button(
                                onClick = {
                                    onToggleReminder(event)
                                    dismissWithAnimation()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = CircleShape,
                                colors = if (reminderEnabled) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = scheme.secondaryContainer,
                                        contentColor = scheme.onSecondaryContainer
                                    )
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                Icon(
                                    imageVector = if (reminderEnabled) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (reminderEnabled) "Отключить напоминание" else "Напомнить за 5 минут",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(text: String, contentColor: Color, containerColor: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun IconBadge(icon: ImageVector, container: Color, content: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.36f))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
private fun InfoTile(
    icon: ImageVector,
    label: String,
    value: String,
    badgeContainer: Color,
    badgeContent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        IconBadge(icon, badgeContainer, badgeContent, 40.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun androidx.compose.material3.Shapes.extraLargePlusTop() =
    this.extraLargePlus.copy(
        bottomStart = CornerSize(0.dp),
        bottomEnd = CornerSize(0.dp)
    )

private fun Modifier.offsetY(value: Float): Modifier =
    this.graphicsLayer { translationY = value }
