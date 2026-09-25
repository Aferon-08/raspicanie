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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
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
                targetValue = if (isSelected) 1f else 0.94f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
                label = "day_selected_scale"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(52.dp)
                    .height(76.dp)
                    .graphicsLayer {
                        scaleX = selectedScale
                        scaleY = selectedScale
                    }
                    .clip(RoundedCornerShape(if (isSelected) 26.dp else 22.dp))
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

                Spacer(modifier = Modifier.height(4.dp))

                // Day number (e.g. 14, 15, 16)
                Text(
                    text = day.dayOfMonth,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = domColor
                )

                if (hasClasses && !isSelected) {
                    Spacer(modifier = Modifier.height(3.dp))
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
