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
import com.example.data.repository.ScheduleRepository
import com.example.data.repository.SavedGroup
import com.example.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    // Events for the selected date with filters and search applied
    val dayEvents: StateFlow<List<ClassEvent>> = combine(allEvents, _uiState) { events, state ->
        val calSelected = com.example.util.ScheduleTimeFormatter.getCalendar(state.selectedDateMillis)
        val selYear = calSelected.get(Calendar.YEAR)
        val selDay = calSelected.get(Calendar.DAY_OF_YEAR)

        val calEvent = com.example.util.ScheduleTimeFormatter.getCalendar()
        val now = System.currentTimeMillis()

        events.filter { ev ->
            calEvent.timeInMillis = ev.startTimeMillis
            val isSameDay = calEvent.get(Calendar.YEAR) == selYear && calEvent.get(Calendar.DAY_OF_YEAR) == selDay
            if (!isSameDay) return@filter false

            // Hide cancelled classes when the schedule toggle is disabled.
            if (!state.showCancelledClasses && ev.isCancelled) return@filter false

            // Filter logic
            val matchesFilter = when (state.filter) {
                ClassFilter.ALL -> true
                ClassFilter.UPCOMING_ONLY -> ev.endTimeMillis > now && !ev.isCancelled
                ClassFilter.CHANGES_ONLY -> ev.hasChanges || ev.isCancelled
            }
            if (!matchesFilter) return@filter false

            // Search query logic
            if (state.searchQuery.isNotBlank()) {
                val q = state.searchQuery.trim().lowercase()
                val inTitle = ev.title.lowercase().contains(q)
                val inTeacher = ev.teacher.lowercase().contains(q)
                val inLocation = ev.location.lowercase().contains(q)
                val inDesc = ev.description.lowercase().contains(q)
                inTitle || inTeacher || inLocation || inDesc
            } else {
                true
            }
        }.sortedBy { it.startTimeMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Same selected-day filtering as dayEvents, but keeps cancelled classes in the list
    // so the UI can animate their disappearance and reappearance.
    val dayEventsIncludingCancelled: StateFlow<List<ClassEvent>> = combine(allEvents, _uiState) { events, state ->
        val calSelected = com.example.util.ScheduleTimeFormatter.getCalendar(state.selectedDateMillis)
        val selYear = calSelected.get(Calendar.YEAR)
        val selDay = calSelected.get(Calendar.DAY_OF_YEAR)

        val calEvent = com.example.util.ScheduleTimeFormatter.getCalendar()
        val now = System.currentTimeMillis()

        events.filter { ev ->
            calEvent.timeInMillis = ev.startTimeMillis
            val isSameDay = calEvent.get(Calendar.YEAR) == selYear && calEvent.get(Calendar.DAY_OF_YEAR) == selDay
            if (!isSameDay) return@filter false

            val matchesFilter = when (state.filter) {
                ClassFilter.ALL -> true
                ClassFilter.UPCOMING_ONLY -> ev.endTimeMillis > now && !ev.isCancelled
                ClassFilter.CHANGES_ONLY -> ev.hasChanges || ev.isCancelled
            }
            if (!matchesFilter) return@filter false

            if (state.searchQuery.isNotBlank()) {
                val q = state.searchQuery.trim().lowercase()
                ev.title.lowercase().contains(q) ||
                    ev.teacher.lowercase().contains(q) ||
                    ev.location.lowercase().contains(q) ||
                    ev.description.lowercase().contains(q)
            } else {
                true
            }
        }.sortedBy { it.startTimeMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Class count per day for badges on DaySelectorStrip
    val classCountByDay: StateFlow<Map<Long, Int>> = allEvents.combine(_uiState) { events, state ->
        val map = mutableMapOf<Long, Int>()
        val cal = com.example.util.ScheduleTimeFormatter.getCalendar()
        events.forEach { ev ->
            if (!state.showCancelledClasses && ev.isCancelled) return@forEach
            cal.timeInMillis = ev.startTimeMillis
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
        _uiState.value = _uiState.value.copy(
            groupId = group.id,
            groupTitle = group.title,
            customUrl = group.url,
            selectedDateMillis = getTodayStartMillis(),
            savedGroups = repository.getSavedGroups(),
            syncFeedback = null
        )
        viewModelScope.launch {
            repository.clearChangeLog()
            refreshSchedule()
        }
    }

    fun addSavedGroup(title: String, url: String) {
        viewModelScope.launch {
            runCatching {
                repository.addGroup(title, url)
            }.onSuccess { group ->
                repository.clearChangeLog()
                _uiState.value = _uiState.value.copy(
                    groupId = group.id,
                    groupTitle = group.title,
                    customUrl = group.url,
                    selectedDateMillis = getTodayStartMillis(),
                    savedGroups = repository.getSavedGroups(),
                    syncFeedback = null
                )
                refreshSchedule()
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
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            when (val result = repository.syncSchedule()) {
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
