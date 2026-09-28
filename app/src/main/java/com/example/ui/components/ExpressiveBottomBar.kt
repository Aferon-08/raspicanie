package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.BottomNavTab

private data class NavItem(
    val tab: BottomNavTab,
    val label: String, // used only for accessibility, not drawn
    val outlined: ImageVector,
    val filled: ImageVector
)

private fun navItems() = listOf(
    NavItem(BottomNavTab.SCHEDULE, "Расписание", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    NavItem(BottomNavTab.PASSES, "Пропуски", Icons.Outlined.EventBusy, Icons.Filled.EventBusy),
    NavItem(BottomNavTab.NOTES, "Заметки", Icons.Outlined.EditNote, Icons.Filled.EditNote),
    NavItem(BottomNavTab.PROFILE, "Профиль", Icons.Outlined.Person, Icons.Filled.Person)
)

/**
 * Icon-only bottom navigation. The selected destination is highlighted by a
 * pill that springs wider (neighbours squeeze aside) while the icon pops with
 * an overshooting spring.
 */
@Composable
fun ExpressiveBottomBar(
    currentTab: BottomNavTab,
    changesCount: Int,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember { navItems() }
    val haptics = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(percent = 50),
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentTab == item.tab
                NavPill(
                    item = item,
                    selected = selected,
                    badgeCount = if (item.tab == BottomNavTab.PASSES) changesCount else 0,
                    onClick = {
                        if (!selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onTabSelected(item.tab)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavPill(
    item: NavItem,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit
) {
    // Width share springs (with a little overshoot) instead of jumping.
    val weight by animateFloatAsState(
        targetValue = if (selected) 1.7f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "nav_weight"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "nav_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "nav_content"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 500f),
        label = "nav_icon_scale"
    )

    Box(
        modifier = Modifier
            .weight(weight.coerceAtLeast(0.1f))
            .fillMaxHeight()
            .background(color = containerColor, shape = RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = item.label
            },
        contentAlignment = Alignment.Center
    ) {
        val icon: @Composable () -> Unit = {
            Icon(
                imageVector = if (selected) item.filled else item.outlined,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(26.dp)
                    .scale(iconScale)
            )
        }
        if (badgeCount > 0) {
            BadgedBox(badge = { Badge { Text(badgeCount.toString(), fontSize = 9.sp) } }) { icon() }
        } else {
            icon()
        }
    }
}

/** Icon-only side navigation for wide windows. */
@Composable
fun ExpressiveNavigationRail(
    currentTab: BottomNavTab,
    changesCount: Int,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember { navItems() }
    val haptics = LocalHapticFeedback.current

    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Spacer(Modifier.weight(1f))
        items.forEach { item ->
            val selected = currentTab == item.tab
            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onTabSelected(item.tab)
                    }
                },
                icon = {
                    val badge = if (item.tab == BottomNavTab.PASSES) changesCount else 0
                    BadgedBox(badge = { if (badge > 0) Badge { Text(badge.toString()) } }) {
                        Icon(
                            imageVector = if (selected) item.filled else item.outlined,
                            contentDescription = item.label
                        )
                    }
                },
                label = null,
                alwaysShowLabel = false
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
