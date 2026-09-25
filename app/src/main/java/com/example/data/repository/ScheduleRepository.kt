package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ChangeEntity
import com.example.data.local.ScheduleEntity
import com.example.data.model.ChangeType
import com.example.data.model.ClassEvent
import com.example.data.model.ScheduleChange
import com.example.data.remote.CalendarRemoteDataSource
import com.example.data.remote.IcsParser
import com.example.notifications.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    var is24HourFormat: Boolean
        get() = prefs.getBoolean(PREF_IS_24_HOUR, true)
        set(value) = prefs.edit().putBoolean(PREF_IS_24_HOUR, value).apply()

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

        // Diff engine: compare with existing DB events to detect modifications
        val oldEvents = database.scheduleDao().getAllEventsList().associateBy { it.id }
        val newChanges = mutableListOf<ChangeEntity>()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val enrichedEvents = parsedEvents.map { newEv ->
            val oldEv = oldEvents[newEv.id]
            var hasChange = false
            var changeDetail: String? = null

            if (oldEv != null) {
                // Compare start time
                if (oldEv.startTimeMillis != newEv.startTimeMillis) {
                    hasChange = true
                    val oldTime = timeFormat.format(Date(oldEv.startTimeMillis))
                    val newTime = timeFormat.format(Date(newEv.startTimeMillis))
                    val detail = "Время изменено с $oldTime на $newTime"
                    changeDetail = detail
                    newChanges.add(
                        ChangeEntity(
                            eventId = newEv.id,
                            eventTitle = newEv.title,
                            changeType = ChangeType.TIME_CHANGED.name,
                            detectedAtMillis = System.currentTimeMillis(),
                            details = detail,
                            eventDateMillis = newEv.startTimeMillis
                        )
                    )
                }

                // Compare cancellation
                if (!oldEv.isCancelled && newEv.isCancelled) {
                    hasChange = true
                    val detail = "Занятие отменено"
                    changeDetail = detail
                    newChanges.add(
                        ChangeEntity(
                            eventId = newEv.id,
                            eventTitle = newEv.title,
                            changeType = ChangeType.CANCELLED.name,
                            detectedAtMillis = System.currentTimeMillis(),
                            details = detail,
                            eventDateMillis = newEv.startTimeMillis
                        )
                    )
                }

                // Compare location
                if (oldEv.location.isNotBlank() && newEv.location.isNotBlank() && oldEv.location != newEv.location) {
                    hasChange = true
                    val detail = "Кабинет изменен: ${newEv.location} (ранее: ${oldEv.location})"
                    changeDetail = detail
                    newChanges.add(
                        ChangeEntity(
                            eventId = newEv.id,
                            eventTitle = newEv.title,
                            changeType = ChangeType.LOCATION_CHANGED.name,
                            detectedAtMillis = System.currentTimeMillis(),
                            details = detail,
                            eventDateMillis = newEv.startTimeMillis
                        )
                    )
                }
            } else if (oldEvents.isNotEmpty() && newEv.startTimeMillis > System.currentTimeMillis()) {
                // New event added in future
                hasChange = true
                val timeStr = timeFormat.format(Date(newEv.startTimeMillis))
                val detail = "Добавлено новое занятие на $timeStr"
                changeDetail = detail
                newChanges.add(
                    ChangeEntity(
                        eventId = newEv.id,
                        eventTitle = newEv.title,
                        changeType = ChangeType.NEW_CLASS.name,
                        detectedAtMillis = System.currentTimeMillis(),
                        details = detail,
                        eventDateMillis = newEv.startTimeMillis
                    )
                )
            }

            ScheduleEntity.fromDomain(
                newEv.copy(hasChanges = hasChange, changeDetails = changeDetail)
            )
        }

        database.scheduleDao().updateScheduleWithDiff(enrichedEvents, newChanges)

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

    fun isReminderEnabled(eventId: String): Boolean =
        notificationScheduler.isReminderEnabled(eventId)

    fun toggleReminder(event: ClassEvent): Boolean =
        notificationScheduler.toggleReminder(event)

    suspend fun clearChangeLog() {
        database.scheduleDao().clearChanges()
    }
}
