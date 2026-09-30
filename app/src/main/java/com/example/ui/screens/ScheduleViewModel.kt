package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PlanovoApp
import com.example.data.local.AppDatabase
import com.example.data.local.ChangeEntity
import com.example.data.local.ScheduleEntity
import com.example.data.model.ChangeType
import com.example.data.model.ClassEvent
import com.example.data.model.ClassStatus
import com.example.data.model.ScheduleChange
import com.example.data.model.SubgroupInfo
import com.example.data.model.stripSubgroupMarker
import com.example.data.repository.ScheduleRepository
import com.example.data.repository.SavedGroup
import com.example.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ClassFilter {
    ALL,
    UPCOMING_ONLY,
    CHANGES_ONLY
}

enum class BottomNavTab {
    SCHEDULE,
    PASSES,
    NOTES,
    PROFILE
}

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

/**
 * Parallel subgroup events are exported as separate calendar entries.
 * They may have slightly different titles (for example, a subgroup suffix),
 * so grouping only by the raw title is too strict.
 */
private fun subgroupBaseTitle(event: ClassEvent): String {
    return event.title.trim().lowercase()
        .replace(Regex("""\s+"""), " ")
        .replace(Regex("""\s*[\[(]\s*(?:под)?групп(?:а|ы)?\s*[0-9а-я-]+\s*[\])]"""), "")
        .replace(Regex("""\s*[\[(]\s*(?:группа|гр\.?|подгруппа)\s*[0-9а-я-]+\s*[\])]"""), "")
        .replace(Regex("""\s*[-–—:]?\s*(?:под)?групп(?:а|ы)?\s*[0-9а-я-]+\s*$"""), "")
        .replace(Regex("""\s*[-–—:]?\s*(?:группа|гр\.?)\s*[0-9а-я-]+\s*$"""), "")
        // Calendar feeds often encode parallel lessons as e.g. "Math (Иванов И.И.)"
        // or "Math - Иванов И.И.". The parser removes the teacher from parentheses,
        // but other feed variants keep it in the title. Normalize these forms too.
        .replace(Regex("""\s*\([^()]*\)\s*$"""), "")
        .replace(Regex("""\s+[-–—:]\s+[^-–—:]+$"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun hasSubgroupMarker(event: ClassEvent): Boolean {
    val text = event.title + "\n" + event.description + "\n" + event.rawSummary
    return Regex("""(?:подгрупп|группа\s*[0-9]|гр\.?\s*[0-9])""", RegexOption.IGNORE_CASE).containsMatchIn(text)
}

private val subgroupTeacherLineRegex = Regex(
    """(?im)(?:под)?групп[аы]?\s*(\d+)\s*[:\-–—)]\s*(.+)$"""
)
private val plainTeacherLineRegex = Regex(
    """(?im)(?:Преподаватель|Педагог|Тренер|Учитель|Инструктор|Ведущий)\s*:\s*(.+)$"""
)
private val subgroupNumberInTitleRegex = Regex("""(?i)(?:под)?групп[аы]?\s*(\d+)""")

private fun cleanTeacherName(raw: String): String =
    raw.substringBefore("ауд").substringBefore("Ауд")
        .replace(Regex("""(?i)^\s*преподаватель\s*:\s*"""), "")
        .trim().trim(',', ';', '.', ' ')

/**
 * Description of a parallel event may list teachers of ALL subgroups (the same text is repeated
 * in every calendar entry). Taking the first one gave the same teacher on every subgroup row,
 * so here we pick the teacher that belongs to this particular event.
 */
private fun resolveSubgroupTeacher(event: ClassEvent, indexInGroup: Int): String? {
    val numbered = subgroupTeacherLineRegex.findAll(event.description)
        .map { it.groupValues[1] to cleanTeacherName(it.groupValues[2]) }
        .filter { it.second.isNotBlank() }
        .toList()
    val plain = plainTeacherLineRegex.findAll(event.description)
        .map { cleanTeacherName(it.groupValues[1]) }
        .filter { it.isNotBlank() }
        .toList()

    val ownNumber = subgroupNumberInTitleRegex
        .find(event.title + " " + event.rawSummary)?.groupValues?.getOrNull(1)

    if (numbered.size > 1) {
        ownNumber?.let { n -> numbered.firstOrNull { it.first == n }?.let { return it.second } }
        numbered.getOrNull(indexInGroup)?.let { return it.second }
    }
    if (plain.size > 1) {
        plain.getOrNull(indexInGroup)?.let { return it }
    }
    return null
}

private fun mergeSubgroupEvents(events: List<ClassEvent>): List<ClassEvent> {
    if (events.size < 2) return events
    return events.groupBy { event ->
        Triple(
            subgroupBaseTitle(event),
            // Allow small timestamp differences between parallel calendar entries.
            event.startTimeMillis / 5_000L,
            event.endTimeMillis / 5_000L
        )
    }.values.flatMap { group ->
        if (group.size < 2) return@flatMap group

        val rows = group
            .sortedWith(compareBy<ClassEvent> { it.location }.thenBy { it.teacher }.thenBy { it.id })
            .mapIndexed { index, event ->
                val candidates = event.displaySubgroups
                val ownNumber = subgroupNumberInTitleRegex
                    .find(event.title + " " + event.rawSummary)?.groupValues?.getOrNull(1)
                // If one event lists several subgroups (shared description), pick the one that
                // belongs to this event instead of always taking the first.
                val parsed = if (candidates.size > 1) {
                    candidates.firstOrNull { ownNumber != null && it.number == ownNumber }
                        ?: candidates.getOrNull(index)
                } else {
                    candidates.firstOrNull()
                }
                SubgroupInfo(
                    number = parsed?.number?.takeIf { it.isNotBlank() } ?: (index + 1).toString(),
                    teacher = resolveSubgroupTeacher(event, index)
                        ?: parsed?.teacher?.takeIf { it.isNotBlank() }
                        ?: event.teacher,
                    room = parsed?.room?.takeIf { it.isNotBlank() } ?: event.location,
                    title = event.title
                )
            }
            .filter { it.teacher.isNotBlank() || it.room.isNotBlank() }

        val distinctDetails = rows
            .map { it.teacher.trim().lowercase() to it.room.trim().lowercase() }
            .distinct()
            .size
        val looksLikeParallelSubgroups =
            group.any(::hasSubgroupMarker) || (rows.size >= 2 && distinctDetails >= 2)

        if (!looksLikeParallelSubgroups || rows.size < 2) return@flatMap group

        val first = group.first()
        listOf(first.copy(
            title = stripSubgroupMarker(first.title),
            teacher = "",
            location = "",
            isCancelled = group.all { it.isCancelled },
            hasChanges = group.any { it.hasChanges },
            changeDetails = group.mapNotNull { it.changeDetails?.takeIf(String::isNotBlank) }
                .distinct().joinToString("\n").ifBlank { null },
            subgroups = rows.mapIndexed { index, row -> row.copy(number = (index + 1).toString()) }
        ))
    }.sortedBy { it.startTimeMillis }
}

data class ScheduleUiState(
    val selectedDateMillis: Long,
    val isSyncing: Boolean = false,
    val syncFeedback: String? = null,
    val filter: ClassFilter = ClassFilter.ALL,
    val showCancelledClasses: Boolean = true,
    val searchQuery: String = "",
    val groupId: String = "41",
    val groupTitle: String = "2423 УИР · 3 курс",
    val customUrl: String? = null,
    val leadTimeMinutes: Int = 15,
    val currentTab: BottomNavTab = BottomNavTab.SCHEDULE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val is24HourFormat: Boolean = true,
    val debugAnimationMode: Boolean = false,
    val notes: Map<String, String> = emptyMap(),
    val missedClasses: Set<String> = emptySet(),
    val reminderEventIds: Set<String> = emptySet(),
    val savedGroups: List<SavedGroup> = emptyList()
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PlanovoApp
    val repository: ScheduleRepository = app.repository
    private val notificationHelper = app.notificationHelper
    private var syncJob: Job? = null

    private val _uiState = MutableStateFlow(
        ScheduleUiState(
            selectedDateMillis = getTodayStartMillis(),
            groupId = repository.groupId,
            groupTitle = repository.groupTitle,
            customUrl = repository.customUrl,
            leadTimeMinutes = repository.leadTimeMinutes,
            themeMode = runCatching { ThemeMode.valueOf(repository.themeMode) }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = repository.dynamicColor,
            is24HourFormat = repository.is24HourFormat,
            showCancelledClasses = repository.showCancelledClasses,
            debugAnimationMode = repository.debugAnimationMode,
            notes = repository.getNotes(),
            missedClasses = repository.getMissedClasses(),
            reminderEventIds = repository.getEnabledReminderIds(),
            savedGroups = repository.getSavedGroups()
        )
    )
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    val allEvents: StateFlow<List<ClassEvent>> = repository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChanges: StateFlow<List<ScheduleChange>> = repository.getAllChanges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Normalize/group events once. Previously every derived flow repeated the
    // subgroup merge and its regular-expression work, so a single state change
    // could process the entire schedule several times.
    private val processedEvents: StateFlow<List<ClassEvent>> = allEvents
        .map(::mergeSubgroupEvents)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Keep one canonical selected-day pipeline and derive the cancelled-filtered
    // list from it. This prevents duplicate Calendar/search/regex work.
    val dayEventsIncludingCancelled: StateFlow<List<ClassEvent>> = combine(processedEvents, _uiState) { events, state ->
        val selected = com.example.util.ScheduleTimeFormatter.getCalendar(state.selectedDateMillis).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = selected.timeInMillis
        val dayEnd = Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val now = System.currentTimeMillis()
        val query = state.searchQuery.trim().lowercase().takeIf(String::isNotBlank)

        events.asSequence()
            .filter { it.startTimeMillis >= dayStart && it.startTimeMillis < dayEnd }
            .filter { event ->
                when (state.filter) {
                    ClassFilter.ALL -> true
                    ClassFilter.UPCOMING_ONLY -> event.endTimeMillis > now && !event.isCancelled
                    ClassFilter.CHANGES_ONLY -> event.hasChanges || event.isCancelled
                }
            }
            .filter { event ->
                query == null ||
                    event.title.lowercase().contains(query) ||
                    event.teacher.lowercase().contains(query) ||
                    event.location.lowercase().contains(query) ||
                    event.description.lowercase().contains(query) ||
                    event.displaySubgroups.any { subgroup ->
                        subgroup.teacher.lowercase().contains(query) || subgroup.room.lowercase().contains(query)
                    }
            }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dayEvents: StateFlow<List<ClassEvent>> = combine(dayEventsIncludingCancelled, _uiState) { events, state ->
        if (state.showCancelledClasses) events else events.filterNot(ClassEvent::isCancelled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Class count per day for badges on DaySelectorStrip.
    val classCountByDay: StateFlow<Map<Long, Int>> = combine(processedEvents, _uiState) { events, state ->
        val map = HashMap<Long, Int>(events.size)
        val cal = com.example.util.ScheduleTimeFormatter.getCalendar()
        events.forEach { event ->
            if (!state.showCancelledClasses && event.isCancelled) return@forEach
            cal.timeInMillis = event.startTimeMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            map[dayStart] = (map[dayStart] ?: 0) + 1
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        // Initial sync on app start
        refreshSchedule()

        // Keep the local schedule fresh while the app process is alive.
        // A request is started approximately once every minute.
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                refreshSchedule()
            }
        }
    }

    fun selectTab(tab: BottomNavTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.themeMode = mode.name
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        repository.dynamicColor = enabled
        _uiState.value = _uiState.value.copy(dynamicColor = enabled)
    }

    fun set24HourFormat(enabled: Boolean) {
        repository.is24HourFormat = enabled
        _uiState.value = _uiState.value.copy(is24HourFormat = enabled)
    }

    fun setDebugAnimationMode(enabled: Boolean) {
        repository.debugAnimationMode = enabled
        _uiState.value = _uiState.value.copy(debugAnimationMode = enabled)
    }

    fun setGroupTitle(title: String) {
        repository.groupTitle = title
        _uiState.value = _uiState.value.copy(groupTitle = title)
    }

    fun switchGroup(groupId: String) {
        val group = repository.switchGroup(groupId) ?: return
        syncJob?.cancel()
        _uiState.value = _uiState.value.copy(
            groupId = group.id,
            groupTitle = group.title,
            customUrl = group.url,
            selectedDateMillis = getTodayStartMillis(),
            savedGroups = repository.getSavedGroups(),
            syncFeedback = null
        )
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            repository.clearChangeLog(group.id)
            performRefreshSchedule(group.id)
        }
    }

    fun addSavedGroup(title: String, url: String) {
        viewModelScope.launch {
            runCatching {
                repository.addGroup(title, url)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    savedGroups = repository.getSavedGroups(),
                    syncFeedback = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    syncFeedback = error.message ?: "Не удалось добавить группу"
                )
            }
        }
    }

    fun editSavedGroup(groupId: String, title: String, url: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateSavedGroup(groupId, title, url)
            }.onSuccess { updated ->
                if (updated != null && updated.id == repository.getActiveGroup().id) {
                    _uiState.value = _uiState.value.copy(
                        groupTitle = updated.title,
                        customUrl = updated.url
                    )
                    refreshSchedule()
                }
                _uiState.value = _uiState.value.copy(savedGroups = repository.getSavedGroups())
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    syncFeedback = error.message ?: "Не удалось сохранить группу"
                )
            }
        }
    }

    fun deleteSavedGroup(groupId: String) {
        val wasActive = repository.getActiveGroup().id == groupId
        if (!repository.deleteGroup(groupId)) {
            _uiState.value = _uiState.value.copy(syncFeedback = "Нельзя удалить единственную сохранённую группу")
            return
        }
        if (wasActive) {
            val active = repository.getActiveGroup()
            _uiState.value = _uiState.value.copy(
                groupId = active.id,
                groupTitle = active.title,
                customUrl = active.url
            )
            refreshSchedule()
        }
        _uiState.value = _uiState.value.copy(savedGroups = repository.getSavedGroups())
    }

    fun selectDate(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(selectedDateMillis = dateMillis)
    }

    fun setFilter(filter: ClassFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun setShowCancelledClasses(show: Boolean) {
        repository.showCancelledClasses = show
        _uiState.value = _uiState.value.copy(showCancelledClasses = show)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearSyncFeedback() {
        _uiState.value = _uiState.value.copy(syncFeedback = null)
    }

    fun setNoteForClass(classId: String, note: String) {
        val updated = _uiState.value.notes.toMutableMap()
        if (note.isBlank()) {
            updated.remove(classId)
        } else {
            updated[classId] = note
        }
        repository.saveNotes(updated)
        _uiState.value = _uiState.value.copy(notes = updated)
    }

    fun toggleMissedClass(classId: String) {
        val updated = _uiState.value.missedClasses.toMutableSet()
        if (classId in updated) {
            updated.remove(classId)
        } else {
            updated.add(classId)
        }
        repository.saveMissedClasses(updated)
        _uiState.value = _uiState.value.copy(missedClasses = updated)
    }

    fun refreshSchedule() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            performRefreshSchedule(repository.getActiveGroup().id)
        }
    }

    private suspend fun performRefreshSchedule(expectedGroupId: String) {
        _uiState.value = _uiState.value.copy(isSyncing = true)
        try {
            when (val result = repository.syncSchedule(expectedGroupId)) {
                is SyncResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSyncing = false, syncFeedback = null)
                }
                is SyncResult.SuccessWithChanges -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncFeedback = "Обнаружено изменений: ${result.changesCount}!"
                    )
                }
                is SyncResult.FallbackUsed -> {
                    _uiState.value = _uiState.value.copy(isSyncing = false, syncFeedback = result.message)
                }
                is SyncResult.Error -> {
                    _uiState.value = _uiState.value.copy(isSyncing = false, syncFeedback = result.message)
                }
            }
            _uiState.value = _uiState.value.copy(savedGroups = repository.getSavedGroups())
        } finally {
            if (syncJob?.isActive != true) {
                _uiState.value = _uiState.value.copy(isSyncing = false)
            }
        }
    }

    fun updateSettings(groupId: String, customUrl: String?, leadTimeMinutes: Int, groupTitle: String = _uiState.value.groupTitle) {
        viewModelScope.launch {
            repository.updateSettings(groupId, customUrl, leadTimeMinutes, groupTitle)
            _uiState.value = _uiState.value.copy(
                groupId = groupId,
                customUrl = customUrl,
                leadTimeMinutes = leadTimeMinutes,
                groupTitle = groupTitle
            )
            refreshSchedule()
        }
    }

    fun triggerTestNotification(leadTimeMinutes: Int = 15) {
        notificationHelper.showTestNotification(leadTimeMinutes)
        _uiState.value = _uiState.value.copy(syncFeedback = "Тестовое уведомление отправлено")
    }

    fun toggleReminderForClass(event: ClassEvent) {
        val enabled = repository.toggleReminder(event)
        val updatedIds = _uiState.value.reminderEventIds.toMutableSet().apply {
            if (enabled) add(event.id) else remove(event.id)
        }
        _uiState.value = _uiState.value.copy(
            reminderEventIds = updatedIds,
            syncFeedback = if (enabled) {
                "Напоминание для «" + event.title + "» включено за 5 минут до начала"
            } else {
                "Напоминание для «" + event.title + "» отключено"
            }
        )
    }
    fun clearChanges() {
        viewModelScope.launch {
            repository.clearChangeLog()
            _uiState.value = _uiState.value.copy(syncFeedback = "Журнал изменений очищен")
        }
    }

    fun simulateChange() {
        viewModelScope.launch {
            val db = AppDatabase.getInstance(getApplication())
            val events = db.scheduleDao().getAllEventsList()
            if (events.isNotEmpty()) {
                val target = events.firstOrNull { it.startTimeMillis > System.currentTimeMillis() } ?: events.first()
                val newStart = target.startTimeMillis + 30 * 60 * 1000
                val newEnd = target.endTimeMillis + 30 * 60 * 1000
                val detail = "Перенос времени: начало сдвинуто на 30 минут позже"

                val updated = target.copy(
                    startTimeMillis = newStart,
                    endTimeMillis = newEnd,
                    hasChanges = true,
                    changeDetails = detail
                )

                db.scheduleDao().insertEvents(listOf(updated))
                db.scheduleDao().insertChange(
                    ChangeEntity(
                        eventId = target.id,
                        eventTitle = target.title,
                        changeType = ChangeType.TIME_CHANGED.name,
                        detectedAtMillis = System.currentTimeMillis(),
                        details = detail,
                        eventDateMillis = newStart
                    )
                )

                notificationHelper.showClassNotification(
                    eventId = "change_${target.id}",
                    title = "Изменение: ${target.title}",
                    startTimeMillis = newStart,
                    location = target.location,
                    teacher = target.teacher,
                    leadTimeMinutes = _uiState.value.leadTimeMinutes
                )

                _uiState.value = _uiState.value.copy(
                    syncFeedback = "Смоделировано изменение: «${target.title}» перенесено!"
                )
            }
        }
    }

    companion object {
        fun getTodayStartMillis(): Long {
            val cal = com.example.util.ScheduleTimeFormatter.getCalendar().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    }
}
