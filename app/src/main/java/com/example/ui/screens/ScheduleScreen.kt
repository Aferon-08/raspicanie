package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ClassEvent
import com.example.ui.components.ClassCard
import com.example.ui.components.ClassDetailDialog
import com.example.ui.components.ExpressiveBottomBar
import com.example.ui.components.DaySelectorStrip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dayEvents by viewModel.dayEvents.collectAsStateWithLifecycle()
    val dayEventsIncludingCancelled by viewModel.dayEventsIncludingCancelled.collectAsStateWithLifecycle()
    val allChanges by viewModel.allChanges.collectAsStateWithLifecycle()
    val classCounts by viewModel.classCountByDay.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val classListState = rememberLazyListState()
    val scheduleScope = rememberCoroutineScope()
    var selectedEventForDetail by remember { mutableStateOf<ClassEvent?>(null) }
    var pendingReminderEvent by remember { mutableStateOf<ClassEvent?>(null) }
    val context = LocalContext.current

    // Android 13+ Notification permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingReminderEvent?.let { event ->
                pendingReminderEvent = null
                val exactIntent = viewModel.repository.exactAlarmSettingsIntent()
                if (exactIntent != null) {
                    runCatching { context.startActivity(exactIntent) }
                    snackbarHostState.showSnackbar(
                        "Для точного времени напоминаний разрешите точные будильники"
                    )
                }
                viewModel.toggleReminderForClass(event)
                selectedEventForDetail = null
            }
        } else {
            pendingReminderEvent = null
            snackbarHostState.showSnackbar("Разрешение на уведомления не выдано")
        }
    }

    LaunchedEffect(uiState.syncFeedback) {
        uiState.syncFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncFeedback()
        }
    }

    // Today / Tomorrow / Day Name calculation
    val dayHeaderTitle = remember(uiState.selectedDateMillis) {
        val todayStart = ScheduleViewModel.getTodayStartMillis()
        val diffDays = ((uiState.selectedDateMillis - todayStart) / (24 * 60 * 60 * 1000)).toInt()
        when (diffDays) {
            0 -> "Сегодня"
            1 -> "Завтра"
            -1 -> "Вчера"
            else -> {
                val dow = com.example.util.ScheduleTimeFormatter.formatDate(uiState.selectedDateMillis, "EEEE")
                dow.replaceFirstChar { it.titlecase(Locale("ru")) }
            }
        }
    }

    val dayHeaderSubtitle = remember(uiState.selectedDateMillis) {
        com.example.util.ScheduleTimeFormatter.formatDate(uiState.selectedDateMillis, "EEEE, d MMMM")
    }

    val pairsWord = remember(dayEvents.size) {
        val count = dayEvents.size
        when {
            count % 10 == 1 && count % 100 != 11 -> "$count пара"
            count % 10 in 2..4 && (count % 100 !in 12..14) -> "$count пары"
            else -> "$count пар"
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ExpressiveBottomBar(
                currentTab = uiState.currentTab,
                changesCount = allChanges.size,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentTab,
            transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f))) togetherWith (fadeOut(tween(140)) + scaleOut(targetScale = 1.03f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 460f))) },
            label = "tab_content_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { currentTab ->
            when (currentTab) {
                BottomNavTab.PASSES -> {
                    PassesScreen(
                        changes = allChanges,
                        missedClassesCount = uiState.missedClasses.size,
                        onClearChanges = { viewModel.clearChanges() },
                        onSimulateChange = { viewModel.simulateChange() }
                    )
                }
                BottomNavTab.NOTES -> {
                    NotesScreen(
                        notes = uiState.notes,
                        onSaveNote = { title, content -> viewModel.setNoteForClass(title, content) }
                    )
                }
                BottomNavTab.PROFILE -> {
                    ProfileScreen(
                        uiState = uiState,
                        onSaveSettings = { gId, url, lead, title ->
                            viewModel.updateSettings(gId, url, lead, title)
                        },
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        onSetDynamicColor = { viewModel.setDynamicColor(it) },
                        onSet24HourFormat = { viewModel.set24HourFormat(it) },
                        onSetDebugAnimationMode = { viewModel.setDebugAnimationMode(it) },
                        onTriggerTestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.triggerTestNotification(uiState.leadTimeMinutes)
                            }
                        },
                        onSimulateChange = { viewModel.simulateChange() },
                        onRefresh = { viewModel.refreshSchedule() }
                    )
                }
                BottomNavTab.SCHEDULE -> {
                    // MAIN SCHEDULE SCREEN matching screenshots 1 & 2
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Top Bar: "Расписание" + Subtitle + Group Avatar Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectTab(BottomNavTab.PROFILE) }
                            ) {
                                Text(
                                    text = "Расписание",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.groupTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Compact cancelled-classes toggle.
                            val cancelledToggleColor by animateColorAsState(
                                targetValue = if (uiState.showCancelledClasses) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
                                label = "cancelled_toggle_color"
                            )
                            val cancelledToggleScale by animateFloatAsState(
                                targetValue = if (uiState.showCancelledClasses) 1f else 0.88f,
                                animationSpec = spring(dampingRatio = 0.62f, stiffness = 500f),
                                label = "cancelled_toggle_scale"
                            )
                            val cancelledToggleRotation by animateFloatAsState(
                                targetValue = if (uiState.showCancelledClasses) 0f else -14f,
                                animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
                                label = "cancelled_toggle_rotation"
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .graphicsLayer {
                                        scaleX = cancelledToggleScale
                                        scaleY = cancelledToggleScale
                                        rotationZ = cancelledToggleRotation
                                    }
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(cancelledToggleColor)
                                    .clickable {
                                        val wasAtTop = classListState.firstVisibleItemIndex == 0 &&
                                            classListState.firstVisibleItemScrollOffset < 12
                                        viewModel.setShowCancelledClasses(!uiState.showCancelledClasses)
                                        if (wasAtTop) {
                                            scheduleScope.launch { classListState.scrollToItem(0) }
                                        }
                                    }
                            ) {
                                if (uiState.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(21.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.EventBusy,
                                        contentDescription = if (uiState.showCancelledClasses) {
                                            "Скрыть отменённые занятия"
                                        } else {
                                            "Показать отменённые занятия"
                                        },
                                        tint = if (uiState.showCancelledClasses) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer {
                                                scaleX = 1.06f
                                                scaleY = 1.06f
                                            }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Day Selector Strip (matching screenshots)
                        DaySelectorStrip(
                            selectedDateMillis = uiState.selectedDateMillis,
                            onDateSelected = { viewModel.selectDate(it) },
                            eventsCountMap = classCounts
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Day Summary Row ("Сегодня", "среда, 16 сентября", "4 пары", "08:15—14:25")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            AnimatedContent(
                                targetState = dayHeaderTitle,
                                transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f))) togetherWith (fadeOut(tween(120)) + scaleOut(targetScale = 1.02f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 460f))) },
                                label = "day_header_title"
                            ) { title ->
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dayHeaderSubtitle,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (dayEvents.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = pairsWord,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Cancelled-class filter is now a compact animated action in the bottom bar.

                        // 4. Classes List
                        if (dayEventsIncludingCancelled.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(32.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Занятий нет",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "В этот день у группы нет запланированных пар",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = classListState,
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(dayEventsIncludingCancelled, key = { it.id }) { event ->
                                    val visible = uiState.showCancelledClasses || !event.isCancelled

                                    AnimatedVisibility(
                                        visible = visible,
                                        enter = expandVertically(
                                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)
                                        ) + fadeIn(tween(260)) + scaleIn(
                                            initialScale = 0.92f,
                                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)
                                        ),
                                        exit = shrinkVertically(
                                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)
                                        ) + fadeOut(tween(180)) + scaleOut(
                                            targetScale = 0.92f,
                                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(bottom = 14.dp)
                                        ) {
                                            ClassCard(
                                                event = event,
                                                is24HourFormat = uiState.is24HourFormat,
                                                forceOngoingAnimation = uiState.debugAnimationMode,
                                                onCardClick = { selectedEventForDetail = it }
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
    }

    // Detail Dialog
    selectedEventForDetail?.let { event ->
        ClassDetailDialog(
            event = event,
            is24HourFormat = uiState.is24HourFormat,
            reminderEnabled = event.id in uiState.reminderEventIds,
            onDismiss = { selectedEventForDetail = null },
            onToggleReminder = { event ->
                if (event.id in uiState.reminderEventIds) {
                    viewModel.toggleReminderForClass(event)
                    selectedEventForDetail = null
                } else {
                    val notificationsGranted =
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (!notificationsGranted) {
                        pendingReminderEvent = event
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        val exactIntent = viewModel.repository.exactAlarmSettingsIntent()
                        if (exactIntent != null) {
                            runCatching { context.startActivity(exactIntent) }
                            snackbarHostState.showSnackbar(
                                "Разрешите точные будильники для максимально точных напоминаний"
                            )
                        }
                        viewModel.toggleReminderForClass(event)
                        selectedEventForDetail = null
                    }
                }
            }
        )
    }
}
