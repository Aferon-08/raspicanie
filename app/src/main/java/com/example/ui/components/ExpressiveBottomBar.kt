package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
        Triple(BottomNavTab.SCHEDULE, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
        Triple(BottomNavTab.PASSES, Icons.Outlined.EventBusy, Icons.Filled.EventBusy),
        Triple(BottomNavTab.NOTES, Icons.Outlined.EditNote, Icons.Filled.EditNote),
        Triple(BottomNavTab.PROFILE, Icons.Outlined.Person, Icons.Filled.Person)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (tab, outlinedIcon, filledIcon) ->
            val selected = currentTab == tab

            val containerColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
                label = "nav_container"
            )

            val contentColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
                label = "nav_content"
            )

            val itemScale by animateFloatAsState(
                targetValue = if (selected) 1.08f else 0.94f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 480f),
                label = "nav_scale"
            )

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        scaleX = itemScale
                        scaleY = itemScale
                    }
                    .background(
                        color = containerColor,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp)
                    )
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                if (tab == BottomNavTab.PASSES && changesCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge {
                                Text(
                                    text = changesCount.toString(),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selected) filledIcon else outlinedIcon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (selected) filledIcon else outlinedIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }
    }
}
