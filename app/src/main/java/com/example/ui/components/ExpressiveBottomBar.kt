package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.screens.BottomNavTab
import kotlin.math.roundToInt

private data class NavItem(
    val tab: BottomNavTab,
    val label: String,
    val outlined: ImageVector,
    val filled: ImageVector
)

private fun navItems() = listOf(
    NavItem(BottomNavTab.SCHEDULE, "Расписание", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    NavItem(BottomNavTab.PASSES, "Пропуски", Icons.Outlined.EventBusy, Icons.Filled.EventBusy),
    NavItem(BottomNavTab.NOTES, "Заметки", Icons.Outlined.EditNote, Icons.Filled.EditNote),
    NavItem(BottomNavTab.PROFILE, "Профиль", Icons.Outlined.Person, Icons.Filled.Person)
)

@Composable
fun ExpressiveBottomBar(
    currentTab: BottomNavTab,
    changesCount: Int,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember { navItems() }
    val haptics = LocalHapticFeedback.current
    
    val selectedIndex = items.indexOfFirst { it.tab == currentTab }.takeIf { it >= 0 } ?: 0
    val animatedIndicatorIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "indicatorIndex"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(84.dp), contentAlignment = Alignment.Center) {
            BoxWithConstraints(modifier = Modifier.fillMaxHeight().widthIn(max = 340.dp).fillMaxWidth()) {

                val itemWidth = maxWidth / items.size
                val indicatorSize = 60.dp

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(itemWidth)
                        .graphicsLayer {
                            translationX = animatedIndicatorIndex * itemWidth.toPx()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(indicatorSize)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(18.dp)
                            )
                    )
                }

                Row(modifier = Modifier.fillMaxSize()) {
                items.forEach { item ->
                    val selected = currentTab == item.tab
                    
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.1f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                        label = "iconScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!selected) {
                                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        onTabSelected(item.tab)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val badge = if (item.tab == BottomNavTab.PASSES) changesCount else 0
                        val iconColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(modifier = Modifier.graphicsLayer { 
                            scaleX = iconScale
                            scaleY = iconScale
                        }) {
                            if (badge > 0) {
                                BadgedBox(badge = { Badge { Text(badge.toString()) } }) {
                                    Icon(
                                        imageVector = if (selected) item.filled else item.outlined,
                                        contentDescription = item.label,
                                        tint = iconColor,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) item.filled else item.outlined,
                                    contentDescription = item.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveNavigationRail(
    currentTab: BottomNavTab,
    changesCount: Int,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember { navItems() }
    val haptics = LocalHapticFeedback.current

    androidx.compose.material3.WideNavigationRail(
        modifier = modifier
    ) {
        items.forEach { item ->
            val selected = currentTab == item.tab
            androidx.compose.material3.WideNavigationRailItem(
                selected = selected,
                railExpanded = true,
                onClick = {
                    if (!selected) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onTabSelected(item.tab)
                    }
                },
                icon = {
                    val badge = if (item.tab == BottomNavTab.PASSES) changesCount else 0
                    if (badge > 0) {
                        BadgedBox(badge = { Badge { Text(badge.toString()) } }) {
                            Icon(
                                imageVector = if (selected) item.filled else item.outlined,
                                contentDescription = item.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) item.filled else item.outlined,
                            contentDescription = item.label
                        )
                    }
                },
                label = { Text(item.label) }
            )
        }
    }
}
