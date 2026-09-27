package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SavedGroup
import kotlin.math.abs

@Composable
fun ProfileScreen(
    uiState: ScheduleUiState,
    onSaveSettings: (String, String?, Int, String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSet24HourFormat: (Boolean) -> Unit,
    onSetDebugAnimationMode: (Boolean) -> Unit,
    onTriggerTestNotification: () -> Unit,
    onSimulateChange: () -> Unit,
    onRefresh: () -> Unit,
    onSwitchGroup: (String) -> Unit,
    onAddGroup: (String, String) -> Unit,
    onEditGroup: (String, String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var groupManagerExpanded by remember { mutableStateOf(false) }
    var groupIdInput by remember(uiState.groupId) { mutableStateOf(uiState.groupId) }
    var groupTitleInput by remember(uiState.groupTitle) { mutableStateOf(uiState.groupTitle) }
    var customUrlInput by remember(uiState.customUrl) { mutableStateOf(uiState.customUrl ?: "") }
    var selectedLeadTime by remember(uiState.leadTimeMinutes) { mutableIntStateOf(uiState.leadTimeMinutes) }
    var groupDialog by remember { mutableStateOf<GroupDialogState?>(null) }
    val activeGroupUrl = uiState.customUrl
        ?: ("https://planovo.pro/api/v1/public/groups/" + uiState.groupId + "/calendar.ics")

    AnimatedContent(
        targetState = groupManagerExpanded,
        transitionSpec = {
            (fadeIn(animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f)) +
                scaleIn(initialScale = 0.965f, animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f)))
                .togetherWith(
                    fadeOut(animationSpec = spring(dampingRatio = 0.82f, stiffness = 460f)) +
                        scaleOut(targetScale = 1.015f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 460f))
                )
        },
        label = "profile_group_mode",
        modifier = modifier.fillMaxSize()
    ) { expanded ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text("Профиль и настройки", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (expanded) "Сохранённые группы · листай карточки влево или вправо"
                    else "Параметры группы, стиль Material 3 Expressive и уведомления",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                if (expanded) {
                    SavedGroupsCarousel(
                        groups = uiState.savedGroups,
                        activeGroupId = activeGroupUrl,
                        onGroupSelected = onSwitchGroup,
                        onAdd = { groupDialog = GroupDialogState.Create },
                        onEdit = { groupDialog = GroupDialogState.Edit(it) }
                    )
                } else {
                    CurrentGroupCard(
                        title = uiState.groupTitle,
                        url = uiState.customUrl ?: ("https://planovo.pro/api/v1/public/groups/" + uiState.groupId + "/calendar.ics"),
                        confirmedToday = uiState.savedGroups.firstOrNull { it.url == activeGroupUrl }?.confirmedToday ?: 0,
                        onAddGroup = { groupManagerExpanded = true },
                        onManageGroups = { groupManagerExpanded = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth().animateContentSize(
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Группа и расписание", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = groupTitleInput,
                                onValueChange = { groupTitleInput = it },
                                label = { Text("Название группы / курс") },
                                placeholder = { Text("Например: 2423 УИР · 3 курс") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = groupIdInput,
                                onValueChange = { groupIdInput = it },
                                label = { Text("ID группы Planovo (по умолч. 41)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = customUrlInput,
                                onValueChange = { customUrlInput = it },
                                label = { Text("Пользовательский .ics URL (опционально)") },
                                placeholder = { Text("https://planovo.pro/api/v1/public/groups/41/calendar.ics") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    onSaveSettings(
                                        groupIdInput.trim(),
                                        customUrlInput.trim().ifEmpty { null },
                                        selectedLeadTime,
                                        groupTitleInput.trim()
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Сохранить и обновить")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Outlined.Palette, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Тема и динамический цвет", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Динамический цвет (Material You)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                        "Цвета интерфейса подстраиваются под обои системы"
                                    else "Доступно на Android 12+",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = uiState.dynamicColor, onCheckedChange = onSetDynamicColor)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("24-часовой формат времени", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (uiState.is24HourFormat) "24 часа (например: 09:45, 14:25)"
                                    else "12 часов (например: 09:45 AM, 02:25 PM)",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = uiState.is24HourFormat, onCheckedChange = onSet24HourFormat)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Режим оформления", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                ThemeMode.SYSTEM to "Системная",
                                ThemeMode.DARK to "Тёмная",
                                ThemeMode.LIGHT to "Светлая"
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = uiState.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Уведомления о занятиях", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Напоминать до начала пары:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15, 30, 60).forEach { mins ->
                                FilterChip(
                                    selected = selectedLeadTime == mins,
                                    onClick = {
                                        selectedLeadTime = mins
                                        onSaveSettings(
                                            groupIdInput.trim(),
                                            customUrlInput.trim().ifEmpty { null },
                                            mins,
                                            groupTitleInput.trim()
                                        )
                                    },
                                    label = { Text(mins.toString() + " м") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onTriggerTestNotification, modifier = Modifier.fillMaxWidth()) {
                            Text("Отправить тестовое уведомление")
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Служебные действия", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("Синхронизация") }
                            OutlinedButton(onClick = onSimulateChange, modifier = Modifier.weight(1f)) {
                                Text("Тест переноса")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onSetDebugAnimationMode(!uiState.debugAnimationMode) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (uiState.debugAnimationMode)
                                    "Остановить анимацию карточек"
                                else "Запустить отладочную анимацию карточек"
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    groupDialog?.let { dialog ->
        GroupEditorDialog(
            state = dialog,
            onDismiss = { groupDialog = null },
            onSave = { title, url ->
                when (dialog) {
                    GroupDialogState.Create -> onAddGroup(title, url)
                    is GroupDialogState.Edit -> onEditGroup(dialog.group.id, title, url)
                }
                groupDialog = null
            },
            onDelete = if (dialog is GroupDialogState.Edit) {
                {
                    onDeleteGroup(dialog.group.id)
                    groupDialog = null
                }
            } else null
        )
    }
}

private sealed interface GroupDialogState {
    data object Create : GroupDialogState
    data class Edit(val group: SavedGroup) : GroupDialogState
}

@Composable
private fun CurrentGroupCard(
    title: String,
    url: String,
    confirmedToday: Int,
    onAddGroup: () -> Unit,
    onManageGroups: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(
                        confirmedToday.toString() + " подтверждённых занятий сегодня",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onAddGroup, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Добавить группу")
                }
                OutlinedButton(onClick = onManageGroups, modifier = Modifier.weight(0.65f)) {
                    Text("Все группы")
                }
            }
        }
    }
}

@Composable
private fun SavedGroupsCarousel(
    groups: List<SavedGroup>,
    activeGroupId: String,
    onGroupSelected: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (SavedGroup) -> Unit
) {
    if (groups.isEmpty()) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Сохранённых групп пока нет", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Добавить группу")
                }
            }
        }
        return
    }

    var selectedIndex by remember(groups, activeGroupId) {
        mutableIntStateOf(groups.indexOfFirst { it.url == activeGroupId }.coerceAtLeast(0))
    }
    val dragOffset = remember { Animatable(0f) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth - 56.dp).coerceAtMost(420.dp)
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val sideGapPx = cardWidthPx * 0.78f

        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Мои группы", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Свайпни карточку ← → для переключения",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Outlined.Add, "Добавить группу", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(238.dp)
                    .pointerInput(groups, selectedIndex) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val threshold = cardWidthPx * 0.20f
                                val releasedOffset = dragOffset.value
                                when {
                                    releasedOffset <= -threshold && selectedIndex < groups.lastIndex -> {
                                        selectedIndex += 1
                                        onGroupSelected(groups[selectedIndex].id)
                                    }
                                    releasedOffset >= threshold && selectedIndex > 0 -> {
                                        selectedIndex -= 1
                                        onGroupSelected(groups[selectedIndex].id)
                                    }
                                }
                                dragOffset.animateTo(
                                    0f,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 480f)
                                )
                            },
                            onHorizontalDrag = { _, amount ->
                                dragOffset.snapTo(dragOffset.value + amount)
                            }
                        )
                    }
            ) {
                (-1..1).forEach { relative ->
                    val index = selectedIndex + relative
                    if (index !in groups.indices) return@forEach
                    val group = groups[index]
                    val isCurrent = relative == 0
                    val progress = if (cardWidthPx == 0f) 0f else (dragOffset.value / cardWidthPx).coerceIn(-1f, 1f)
                    val translation = relative * sideGapPx + if (isCurrent) dragOffset.value else dragOffset.value * 0.22f
                    val distance = abs(relative.toFloat() + if (isCurrent) progress else progress * 0.18f)
                    val scale = (1f - distance * 0.10f).coerceAtLeast(0.84f)

                    GroupCarouselCard(
                        group = group,
                        isCurrent = isCurrent,
                        modifier = Modifier
                            .width(cardWidth)
                            .align(Alignment.Center)
                            .graphicsLayer {
                                translationX = translation
                                scaleX = scale
                                scaleY = scale
                                rotationY = (-progress * 8f) + relative * 2.5f
                                cameraDistance = 14f * density.density
                                alpha = if (distance > 1.4f) 0f else 1f
                            },
                        onEdit = { onEdit(group) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                groups.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == selectedIndex) 18.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == selectedIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCarouselCard(
    group: SavedGroup,
    isCurrent: Boolean,
    modifier: Modifier,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 5.dp else 1.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(46.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(group.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(
                            if (group.confirmedToday == 0) "Нет подтверждённых занятий сегодня"
                            else group.confirmedToday.toString() + " подтверждённых занятий сегодня",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(group.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(38.dp)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Icon(
                    Icons.Outlined.EditNote,
                    contentDescription = "Редактировать группу",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun GroupEditorDialog(
    state: GroupDialogState,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val existing = (state as? GroupDialogState.Edit)?.group
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var url by remember(existing?.id) { mutableStateOf(existing?.url ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Добавить группу" else "Редактировать группу") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    placeholder = { Text("Например: 2423 УИР") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Ссылка на .ics") },
                    placeholder = { Text("https://.../calendar.ics") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, url) },
                enabled = title.isNotBlank() && url.startsWith("http")
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.DeleteOutline, "Удалить группу", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}
