package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.BottomNavTab

@Composable
fun ExpressiveBottomBar(
    currentTab: BottomNavTab,
    changesCount: Int,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple(BottomNavTab.SCHEDULE, "Расписание", Icons.Outlined.CalendarMonth to Icons.Filled.CalendarMonth),
        Triple(BottomNavTab.PASSES, "Пропуски", Icons.Outlined.EventBusy to Icons.Filled.EventBusy),
        Triple(BottomNavTab.NOTES, "Заметки", Icons.Outlined.EditNote to Icons.Filled.EditNote),
        Triple(BottomNavTab.PROFILE, "Профиль", Icons.Outlined.Person to Icons.Filled.Person)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 5.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (tab, label, icons) ->
            val selected = currentTab == tab
            val containerColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                animationSpec = spring(),
                label = "nav_container"
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = spring(),
                label = "nav_content"
            )
            val itemWidth by animateDpAsState(
                targetValue = if (selected) 92.dp else 56.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                label = "nav_width"
            )
            val itemScale by animateFloatAsState(
                targetValue = if (selected) 1.04f else 0.94f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 550f),
                label = "nav_scale"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = itemScale
                        scaleY = itemScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .background(containerColor, RoundedCornerShape(19.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 7.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tab == BottomNavTab.PASSES && changesCount > 0) {
                        BadgedBox(
                            badge = { Badge { Text(changesCount.toString(), fontSize = 8.sp) } }
                        ) {
                            Icon(
                                imageVector = if (selected) icons.second else icons.first,
                                contentDescription = label,
                                tint = contentColor,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) icons.second else icons.first,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = label,
                        color = contentColor,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
