package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.BottomNavTab

private data class NavItem(
    val tab: BottomNavTab,
    val label: String,
    val outlined: ImageVector,
    val filled: ImageVector
)

/**
 * Expressive bottom navigation bar.
 *
 * Follows the M3 Expressive "pill" pattern: the selected item grows into a
 * wide rounded pill with an icon + label, while unselected items stay as
 * compact icon-only circles. The whole thing rides on the theme's
 * [androidx.compose.material3.MotionScheme] spring specs so it feels
 * consistent with the rest of the expressive theme rather than using its
 * own bespoke tuning.
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
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
                    },
                    modifier = Modifier.weight(if (selected) 1.6f else 1f)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "nav_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "nav_content"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "nav_icon_scale"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxHeight()
            .wrapContentWidth()
            .background(color = containerColor, shape = RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = item.label
            }
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (badgeCount > 0) {
            BadgedBox(
                badge = { Badge { Text(text = badgeCount.toString(), fontSize = 9.sp) } }
            ) {
                NavIcon(item, selected, contentColor, iconScale)
            }
        } else {
            NavIcon(item, selected, contentColor, iconScale)
        }

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(spring(stiffness = 300f)) + expandHorizontally(spring(dampingRatio = 0.75f, stiffness = 380f)),
            exit = fadeOut(spring(stiffness = 300f)) + shrinkHorizontally(spring(dampingRatio = 0.75f, stiffness = 380f))
        ) {
            Text(
                text = item.label,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NavIcon(item: NavItem, selected: Boolean, tint: Color, scale: Float) {
    Icon(
        imageVector = if (selected) item.filled else item.outlined,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(24.dp)
            .scale(scale)
    )
}

private fun navItems() = listOf(
    NavItem(BottomNavTab.SCHEDULE, "Расписание", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    NavItem(BottomNavTab.PASSES, "Пропуски", Icons.Outlined.EventBusy, Icons.Filled.EventBusy),
    NavItem(BottomNavTab.NOTES, "Заметки", Icons.Outlined.EditNote, Icons.Filled.EditNote),
    NavItem(BottomNavTab.PROFILE, "Профиль", Icons.Outlined.Person, Icons.Filled.Person)
)

/**
 * Side navigation for wide windows (tablets, foldables, landscape).
 * Same destinations, badge and haptics as [ExpressiveBottomBar].
 */
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
                        Icon(if (selected) item.filled else item.outlined, contentDescription = null)
                    }
                },
                label = { Text(item.label) },
                alwaysShowLabel = true
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
