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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Surface
import com.example.data.model.ClassEvent
import com.example.data.model.ClassStatus
import com.example.ui.theme.StatusChangedYellow
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
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            ClassStatus.ONGOING -> MaterialTheme.colorScheme.primary
            ClassStatus.CANCELLED -> MaterialTheme.colorScheme.error
            ClassStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
            ClassStatus.UPCOMING_SOON, ClassStatus.SCHEDULED -> MaterialTheme.colorScheme.secondary
        },
        animationSpec = spring(),
        label = "detail_status_color"
    )
    val statusText = when (status) {
        ClassStatus.ONGOING -> "Сейчас идёт"
        ClassStatus.CANCELLED -> "Отменено"
        ClassStatus.COMPLETED -> "Завершено"
        ClassStatus.UPCOMING_SOON -> "Скоро начнётся"
        ClassStatus.SCHEDULED -> "Предстоит"
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
                .padding(top = 64.dp),
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
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    ),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
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
                                onDragCancel = {
                                    resetDrag()
                                }
                            )
                        }
                        .then(
                            Modifier.offsetY(dragOffsetY)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(end = 48.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = event.displayLessonType,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(statusColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = statusColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = event.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { dismissWithAnimation() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Закрыть",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (event.hasChanges && !event.changeDetails.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(StatusChangedYellow.copy(alpha = 0.15f))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = StatusChangedYellow,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Изменение в расписании",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusChangedYellow
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = event.changeDetails,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            DetailRow(
                                icon = Icons.Outlined.AccessTime,
                                iconTint = MaterialTheme.colorScheme.primary,
                                label = "Дата и время",
                                value = dateStr + "\n" +
                                    startStr + " — " + endStr +
                                    " (" + event.durationMinutes + " мин)"
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )

                            if (event.location.isNotBlank()) {
                                DetailRow(
                                    icon = Icons.Outlined.MeetingRoom,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    label = "Место проведения / Аудитория",
                                    value = event.location
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }

                            if (event.teacher.isNotBlank()) {
                                DetailRow(
                                    icon = Icons.Outlined.Person,
                                    iconTint = MaterialTheme.colorScheme.secondary,
                                    label = "Преподаватель",
                                    value = event.teacher
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (
                            status != ClassStatus.COMPLETED &&
                            status != ClassStatus.CANCELLED
                        ) {
                            Button(
                                onClick = {
                                    onToggleReminder(event)
                                    dismissWithAnimation()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = if (reminderEnabled) {
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor =
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    ButtonDefaults.buttonColors()
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = if (reminderEnabled) {
                                        Icons.Outlined.NotificationsOff
                                    } else {
                                        Icons.Outlined.Notifications
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (reminderEnabled) {
                                        "Отключить напоминание"
                                    } else {
                                        "Напомнить за 5 минут"
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}

private fun Modifier.offsetY(value: Float): Modifier =
    this.graphicsLayer {
        translationY = value
    }

