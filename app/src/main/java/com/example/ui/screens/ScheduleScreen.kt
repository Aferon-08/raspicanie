package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.animateItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ClassEvent
import com.example.ui.components.ClassCard
import com.example.ui.components.ClassDetailDialog
import com.example.ui.components.ExpressiveBottomBar
import com.example.ui.components.DaySelectorStrip
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
    val allChanges by viewModel.allChanges.collectAsStateWithLifecycle()
    val classCounts by viewModel.classCountByDay.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedEventForDetail by remember { mutableStateOf<ClassEvent?>(null) }

    // Android 13+ Notification permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.triggerTestNotification(uiState.leadTimeMinutes)
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

    val dayTimeSpan = remember(dayEvents, uiState.is24HourFormat) {
        if (dayEvents.isEmpty()) "" else {
            val first = dayEvents.minByOrNull { it.startTimeMillis }?.startTimeMillis
            val last = dayEvents.maxByOrNull { it.endTimeMillis }?.endTimeMillis
            if (first != null && last != null) {
                "${com.example.util.ScheduleTimeFormatter.formatTime(first, uiState.is24HourFormat)}—${com.example.util.ScheduleTimeFormatter.formatTime(last, uiState.is24HourFormat)}"
            } else ""
        }
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
            .fillMaxSize()
            .statusBarsPadding(),
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
            transitionSpec = { fadeIn() togetherWith fadeOut() },
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
                                .padding(horizontal = 20.dp, vertical = 8.dp),
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
                                    fontSize = 32.sp,
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

                            // Circular avatar container on the right
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { viewModel.selectTab(BottomNavTab.PROFILE) }
                            ) {
                                if (uiState.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Groups,
                                        contentDescription = "Группа",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(26.dp)
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
                                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                                label = "day_header_title"
                            ) { title ->
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = title,
                                    fontSize = 22.sp,
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
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = pairsWord,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (dayTimeSpan.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = dayTimeSpan,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp)
                                .animateContentSize(animationSpec = spring()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Отменённые занятия",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.showCancelledClasses) "Показывать в расписании" else "Скрыты из расписания",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = uiState.showCancelledClasses,
                                onCheckedChange = viewModel::setShowCancelledClasses,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Classes List
                        if (dayEvents.isEmpty()) {
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
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(dayEvents, key = { it.id }) { event ->
                                    ClassCard(
                                        event = event,
                                        is24HourFormat = uiState.is24HourFormat,
                                        onCardClick = { selectedEventForDetail = it },
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(
                                                durationMillis = 420,
                                                delayMillis = 45
                                            ),
                                            fadeOutSpec = tween(durationMillis = 220),
                                            placementSpec = spring()
                                        )
                                    )
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
            onDismiss = { selectedEventForDetail = null },
            onSetReminder = {
                viewModel.setReminderForClass(it)
                selectedEventForDetail = null
            }
        )
    }
}
