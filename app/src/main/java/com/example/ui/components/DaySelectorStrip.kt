package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


private class BlobDayShape(
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val p = progress.coerceIn(0f, 1f)
        val w = size.width
        val h = size.height

        // Start as a soft square and morph into an asymmetric, organic blob.
        val base = minOf(w, h) * 0.24f
        val rTopLeft = base * (1f + 0.45f * p)
        val rTopRight = base * (1f + 0.05f * p)
        val rBottomRight = base * (1f + 0.62f * p)
        val rBottomLeft = base * (1f + 0.18f * p)
        val k = 0.5522848f

        val path = Path()
        path.moveTo(rTopLeft, 0f)
        path.lineTo(w - rTopRight, 0f)
        path.cubicTo(
            w - rTopRight + rTopRight * k, 0f,
            w, rTopRight - rTopRight * k,
            w, rTopRight
        )
        path.lineTo(w, h - rBottomRight)
        path.cubicTo(
            w, h - rBottomRight + rBottomRight * k,
            w - rBottomRight * k, h,
            w - rBottomRight, h
        )
        path.lineTo(rBottomLeft, h)
        path.cubicTo(
            rBottomLeft - rBottomLeft * k, h,
            0f, h - rBottomLeft + rBottomLeft * k,
            0f, h - rBottomLeft
        )
        path.lineTo(0f, rTopLeft)
        path.cubicTo(
            0f, rTopLeft - rTopLeft * k,
            rTopLeft - rTopLeft * k, 0f,
            rTopLeft, 0f
        )

        return Outline.Generic(path)
    }
}

data class DayItem(
    val dateMillis: Long,
    val dayOfWeek: String,
    val dayOfMonth: String,
    val isToday: Boolean,
    val classCount: Int = 0
)

@Composable
fun DaySelectorStrip(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    eventsCountMap: Map<Long, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val days = remember {
        val list = mutableListOf<DayItem>()
        val cal = com.example.util.ScheduleTimeFormatter.getCalendar().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis

        // 4 days in past, 21 days in future
        cal.add(Calendar.DAY_OF_YEAR, -4)

        for (i in 0 until 26) {
            val millis = cal.timeInMillis
            val rawDow = com.example.util.ScheduleTimeFormatter.formatDate(millis, "EEE").replace(".", "")
            val formattedDow = rawDow.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
            val dom = com.example.util.ScheduleTimeFormatter.formatDate(millis, "d")
            val isToday = millis == todayStart

            list.add(
                DayItem(
                    dateMillis = millis,
                    dayOfWeek = formattedDow,
                    dayOfMonth = dom,
                    isToday = isToday
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val listState = rememberLazyListState()

    // Scroll to selected date initially
    LaunchedEffect(selectedDateMillis) {
        val index = days.indexOfFirst { it.dateMillis == selectedDateMillis }
        if (index >= 0) {
            listState.animateScrollToItem((index - 1).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier.fillMaxWidth().height(96.dp)
    ) {
        items(days, key = { it.dateMillis }) { day ->
            val isSelected = day.dateMillis == selectedDateMillis
            val hasClasses = (eventsCountMap[day.dateMillis] ?: 0) > 0

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                animationSpec = spring(),
                label = "day_container_color"
            )

            val dowColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "day_dow_color"
            )

            val domColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                label = "day_dom_color"
            )

            val selectedScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.98f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
                label = "day_selected_scale"
            )

            val shapeProgress by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
                label = "day_blob_shape"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .size(68.dp)
                    .padding(top = 13.dp)
                    .graphicsLayer {
                        scaleX = selectedScale
                        scaleY = selectedScale
                    }
                    .clip(BlobDayShape(shapeProgress))
                    .background(containerColor)
                    .clickable { onDateSelected(day.dateMillis) }
                    .testTag("day_item_${day.dayOfMonth}")
            ) {
                // Day of week (e.g. Пн, Вт, Ср)
                Text(
                    text = day.dayOfWeek,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = dowColor
                )

                Spacer(modifier = Modifier.height(0.dp))

                // Day number (e.g. 14, 15, 16)
                Text(
                    text = day.dayOfMonth,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = domColor
                )

                if (hasClasses && !isSelected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}
