package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassEvent
import com.example.data.model.ClassStatus
import com.example.ui.theme.StatusChangedYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.util.ScheduleTimeFormatter

@Composable
fun ClassDetailDialog(
    event: ClassEvent,
    currentTimeMillis: Long = System.currentTimeMillis(),
    is24HourFormat: Boolean = true,
    onDismiss: () -> Unit,
    onSetReminder: (ClassEvent) -> Unit
) {
    var liveTimeMillis by remember(event.id) { mutableLongStateOf(currentTimeMillis) }

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
            ClassStatus.UPCOMING -> MaterialTheme.colorScheme.secondary
        },
        animationSpec = spring(),
        label = "detail_status_color"
    )
    val statusText = when (status) {
        ClassStatus.ONGOING -> "Сейчас идёт"
        ClassStatus.CANCELLED -> "Отменено"
        ClassStatus.COMPLETED -> "Завершено"
        ClassStatus.UPCOMING -> "Предстоит"
    }

    val startStr = ScheduleTimeFormatter.formatTime(event.startTimeMillis, is24HourFormat)
    val endStr = ScheduleTimeFormatter.formatTime(event.endTimeMillis, is24HourFormat)
    val dateStr = ScheduleTimeFormatter.formatDate(event.startTimeMillis, "EEEE, d MMMM yyyy").replaceFirstChar { it.uppercase() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
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
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Change notification pill
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

                // Date & Time block
                DetailRow(
                    icon = Icons.Outlined.AccessTime,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = "Дата и время",
                    value = "$dateStr\n$startStr — $endStr (${event.durationMinutes} мин)"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Location block
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

                // Teacher block
                if (event.teacher.isNotBlank()) {
                    DetailRow(
                        icon = Icons.Outlined.Person,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        label = "Преподаватель",
                        value = event.teacher
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                // Description
                if (event.description.isNotBlank()) {
                    DetailRow(
                        icon = Icons.Outlined.Description,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "Дополнительно",
                        value = event.description
                    )
                }
            }
        },
        confirmButton = {
            if (status != ClassStatus.COMPLETED && status != ClassStatus.CANCELLED) {
                Button(
                    onClick = {
                        onSetReminder(event)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Напомнить", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
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
