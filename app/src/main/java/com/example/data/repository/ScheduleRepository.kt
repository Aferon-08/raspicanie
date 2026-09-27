package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ChangeEntity
import com.example.data.local.ScheduleEntity
import com.example.data.model.ClassEvent
import com.example.data.model.ScheduleChange
import com.example.data.remote.CalendarRemoteDataSource
import com.example.data.remote.IcsParser
import com.example.notifications.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

sealed interface SyncResult {
    data object Success : SyncResult
    data class SuccessWithChanges(val changesCount: Int) : SyncResult
    data class FallbackUsed(val message: String) : SyncResult
    data class Error(val message: String) : SyncResult
}

class ScheduleRepository(
    context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val remoteDataSource: CalendarRemoteDataSource = CalendarRemoteDataSource(),
    private val notificationScheduler: NotificationScheduler = NotificationScheduler(context)
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("planovo_schedule_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_GROUP_ID = "41"
        const val BASE_CALENDAR_URL = "https://planovo.pro/api/v1/public/groups/%s/calendar.ics"
        const val PREF_GROUP_ID = "pref_group_id"
        const val PREF_CUSTOM_URL = "pref_custom_url"
        const val PREF_LEAD_TIME = "pref_lead_time"
        const val PREF_IS_24_HOUR = "pref_is_24_hour"
        const val DEFAULT_LEAD_TIME = 15
        const val PREF_THEME_MODE = "pref_theme_mode"
        const val PREF_DYNAMIC_COLOR = "pref_dynamic_color"
        const val PREF_NOTES = "pref_notes"
        const val PREF_MISSED_CLASSES = "pref_missed_classes"
        const val PREF_GROUP_TITLE = "pref_group_title"
        const val PREF_SHOW_CANCELLED = "pref_show_cancelled"
        const val PREF_DEBUG_ANIMATION = "pref_debug_animation"
    }

    var groupId: String
        get() = prefs.getString(PREF_GROUP_ID, DEFAULT_GROUP_ID) ?: DEFAULT_GROUP_ID
        set(value) = prefs.edit().putString(PREF_GROUP_ID, value.trim()).apply()

    var customUrl: String?
        get() = prefs.getString(PREF_CUSTOM_URL, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(PREF_CUSTOM_URL, value?.trim()).apply()

    var leadTimeMinutes: Int
        get() = prefs.getInt(PREF_LEAD_TIME, DEFAULT_LEAD_TIME)
        set(value) = prefs.edit().putInt(PREF_LEAD_TIME, value).apply()

    var groupTitle: String
        get() = prefs.getString(PREF_GROUP_TITLE, "2423 УИР · 3 курс") ?: "2423 УИР · 3 курс"
        set(value) = prefs.edit().putString(PREF_GROUP_TITLE, value).apply()

    var showCancelledClasses: Boolean
        get() = prefs.getBoolean(PREF_SHOW_CANCELLED, true)
        set(value) = prefs.edit().putBoolean(PREF_SHOW_CANCELLED, value).apply()

    var debugAnimationMode: Boolean
        get() = prefs.getBoolean(PREF_DEBUG_ANIMATION, false)
        set(value) = prefs.edit().putBoolean(PREF_DEBUG_ANIMATION, value).apply()

    var is24HourFormat: Boolean
        get() = prefs.getBoolean(PREF_IS_24_HOUR, true)
        set(value) = prefs.edit().putBoolean(PREF_IS_24_HOUR, value).apply()


    var themeMode: String
        get() = prefs.getString(PREF_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(PREF_THEME_MODE, value).apply()

    var dynamicColor: Boolean
        get() = prefs.getBoolean(PREF_DYNAMIC_COLOR, true)
        set(value) = prefs.edit().putBoolean(PREF_DYNAMIC_COLOR, value).apply()

    fun getNotes(): Map<String, String> = runCatching {
        val json = org.json.JSONObject(prefs.getString(PREF_NOTES, "{}") ?: "{}")
        json.keys().asSequence().associateWith { key -> json.optString(key) }
    }.getOrDefault(emptyMap())

    fun saveNotes(notes: Map<String, String>) {
        val json = org.json.JSONObject()
        notes.forEach { (key, value) -> json.put(key, value) }
        prefs.edit().putString(PREF_NOTES, json.toString()).apply()
    }

    fun getMissedClasses(): Set<String> =
        prefs.getStringSet(PREF_MISSED_CLASSES, emptySet())?.toSet() ?: emptySet()

    fun saveMissedClasses(ids: Set<String>) {
        prefs.edit().putStringSet(PREF_MISSED_CLASSES, ids).apply()
    }

    fun updateSettings(newGroupId: String, newCustomUrl: String?, newLeadTimeMinutes: Int) {
        groupId = newGroupId
        customUrl = newCustomUrl
        leadTimeMinutes = newLeadTimeMinutes
    }

    fun getCalendarUrl(): String {
        val custom = customUrl
        return if (!custom.isNullOrBlank()) {
            custom
        } else {
            String.format(BASE_CALENDAR_URL, groupId)
        }
    }

    fun getAllEvents(): Flow<List<ClassEvent>> {
        return database.scheduleDao().getAllEvents().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getEventsForDay(dayStartMillis: Long): Flow<List<ClassEvent>> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayStartMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = cal.timeInMillis

        return database.scheduleDao().getEventsForRange(startOfDay, endOfDay).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAllChanges(): Flow<List<ScheduleChange>> {
        return database.scheduleDao().getAllChanges().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun syncSchedule(): SyncResult {
        val url = getCalendarUrl()
        Log.d("ScheduleRepository", "Syncing schedule from $url")

        val remoteResult = remoteDataSource.fetchCalendarIcs(url)
        val icsString: String
        var isFallback = false
        var fallbackMsg = ""

        if (remoteResult.isSuccess) {
            icsString = remoteResult.getOrThrow()
        } else {
            val err = remoteResult.exceptionOrNull()?.message ?: "Не удалось загрузить данные"
            Log.w("ScheduleRepository", "Network fetch failed: $err")

            // Check if local DB already has data
            val existing = database.scheduleDao().getAllEventsList()
            if (existing.isNotEmpty()) {
                return SyncResult.Error("Ошибка обновления ($err). Отображаются сохраненные данные.")
            } else {
                // First-run offline fallback: use realistic template for group
                icsString = remoteDataSource.generateDemoIcs()
                isFallback = true
                fallbackMsg = "Не удалось подключиться к серверу ($err). Загружено демонстрационное расписание."
            }
        }

        val parsedEvents = IcsParser.parse(icsString)
        if (parsedEvents.isEmpty() && !isFallback) {
            return SyncResult.Error("Календарь пуст или не содержит занятий")
        }

        val oldEvents = database.scheduleDao().getAllEventsList().associateBy { it.id }
        val diff = ScheduleDiff.calculate(oldEvents, parsedEvents)
        val newChanges = diff.changes.map(ChangeEntity::fromDomain)

        // Cancel alarms for events removed from the remote calendar before replacing the DB.
        // Their old rows disappear during the transaction, so they cannot be found afterwards.
        if (diff.removedEventIds.isNotEmpty()) {
            notificationScheduler.cancelAlarms(diff.removedEventIds)
        }

        database.scheduleDao().updateScheduleWithDiff(diff.events.map(ScheduleEntity::fromDomain), newChanges)

        // Reschedule alarms for upcoming classes
        rescheduleUpcomingNotifications()

        return when {
            isFallback -> SyncResult.FallbackUsed(fallbackMsg)
            newChanges.isNotEmpty() -> SyncResult.SuccessWithChanges(newChanges.size)
            else -> SyncResult.Success
        }
    }

    suspend fun rescheduleUpcomingNotifications() {
        try {
            // Refresh only alarms explicitly enabled by the user.
            // The enabled state survives schedule synchronization and device reboot.
            val allEvents = database.scheduleDao().getAllEventsList().map { it.toDomain() }
            notificationScheduler.cancelAlarms(allEvents)
            notificationScheduler.rescheduleEnabled(allEvents)
        } catch (e: Exception) {
            Log.w("ScheduleRepository", "Failed to reschedule notifications", e)
        }
    }

    fun getEnabledReminderIds(): Set<String> =
        notificationScheduler.getEnabledReminderIds()

    fun canScheduleExactAlarms(): Boolean =
        notificationScheduler.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): Intent? =
        notificationScheduler.exactAlarmSettingsIntent()

    fun isReminderEnabled(eventId: String): Boolean =
        notificationScheduler.isReminderEnabled(eventId)

    fun toggleReminder(event: ClassEvent): Boolean =
        notificationScheduler.toggleReminder(event)

    suspend fun clearChangeLog() {
        database.scheduleDao().clearChanges()
    }
}
