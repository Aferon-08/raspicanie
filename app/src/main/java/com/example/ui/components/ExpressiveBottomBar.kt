package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.BottomNavTab

/**
 * Compact expressive navigation surface.
 *
 * The selected item grows into a pill while the icon and label gently
 * transition colours. This keeps the navigation visually quiet but alive.
 */
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(30.dp)
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (tab, label, icons) ->
            val selected = currentTab == tab
            val containerColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                animationSpec = spring(),
                label = "nav_container"
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(),
                label = "nav_content"
            )
            val itemWidth by animateDpAsState(
                targetValue = if (selected) 108.dp else 58.dp,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 500f),
                label = "nav_width"
            )

            Box(
                modifier = Modifier.width(itemWidth),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .background(containerColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (tab == BottomNavTab.PASSES && changesCount > 0) {
                            BadgedBox(badge = { Badge { Text(changesCount.toString(), fontSize = 9.sp) } }) {
                                Icon(
                                    imageVector = if (selected) icons.second else icons.first,
                                    contentDescription = label,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (selected) icons.second else icons.first,
                                contentDescription = label,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (selected) {
                            Text(
                                text = label,
                                color = contentColor,
                                fontSize = 11.sp,
                                lineHeight = 13.sp
                            )
                        }
                }
            }
        }
    }
}
