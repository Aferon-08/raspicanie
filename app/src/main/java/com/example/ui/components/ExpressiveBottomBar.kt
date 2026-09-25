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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
        Triple(BottomNavTab.SCHEDULE, "📅", "Расписание"),
        Triple(BottomNavTab.PASSES, "🚫", "Пропуски"),
        Triple(BottomNavTab.NOTES, "📝", "Заметки"),
        Triple(BottomNavTab.PROFILE, "👤", "Профиль")
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
        items.forEach { (tab, emoji, label) ->
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

            val itemScale by animateFloatAsState(
                targetValue = if (selected) 1.08f else 0.94f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 480f),
                label = "nav_scale"
            )

            val itemRotation by animateFloatAsState(
                targetValue = if (selected) 0f else 0f,
                animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
                label = "nav_rotation"
            )

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        scaleX = itemScale
                        scaleY = itemScale
                        rotationZ = itemRotation
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
                        Text(
                            text = emoji,
                            fontSize = 27.sp,
                            lineHeight = 30.sp
                        )
                    }
                } else {
                    Text(
                        text = emoji,
                        fontSize = 27.sp,
                        lineHeight = 30.sp
                    )
                }
            }
        }
    }
}
