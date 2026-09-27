package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassEvent
import com.example.data.model.ClassStatus
import com.example.util.ScheduleTimeFormatter

/**
 * Dedicated card for parallel subgroup lessons.
 *
 * Unlike the compact ClassCard, this card gives every subgroup its own row,
 * so the lesson remains one item in the day while all teachers and rooms stay visible.
 */
@Composable
fun SubgroupClassCard(
    event: ClassEvent,
    currentTimeMillis: Long = System.currentTimeMillis(),
    is24HourFormat: Boolean = true,
    onCardClick: (ClassEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val status = event.getStatus(currentTimeMillis)
    val isOngoing = status == ClassStatus.ONGOING
    val isCancelled = status == ClassStatus.CANCELLED

    val primaryColor = if (isOngoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryColor = if (isOngoing) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val cardColor = if (isOngoing) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "subgroup_card_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable { onCardClick(event) },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOngoing) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = secondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = ScheduleTimeFormatter.formatTime(event.startTimeMillis, is24HourFormat),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = " — " + ScheduleTimeFormatter.formatTime(event.endTimeMillis, is24HourFormat),
                            fontSize = 14.sp,
                            color = secondaryColor
                        )
                    }

                    Spacer(modifier = Modifier.size(7.dp))

                    Text(
                        text = event.title,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (event.displayLessonType.isNotBlank()) {
                        Spacer(modifier = Modifier.size(3.dp))
                        Text(
                            text = event.displayLessonType,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isOngoing) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp)
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

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                event.displaySubgroups.forEach { subgroup ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                if (isOngoing) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.09f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            )
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isOngoing) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }
                                )
                        ) {
                            Text(
                                text = subgroup.number,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOngoing) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(11.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Подгруппа " + subgroup.number,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryColor
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = secondaryColor,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = subgroup.teacher.ifBlank { "Преподаватель не указан" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (subgroup.room.isNotBlank()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Icon(
                                    imageVector = Icons.Outlined.MeetingRoom,
                                    contentDescription = null,
                                    tint = secondaryColor,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.size(2.dp))
                                Text(
                                    text = subgroup.room
                                        .replace("Кабинет ", "")
                                        .replace("каб. ", ""),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = primaryColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                    color = secondaryColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

