package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.util.Log
import com.example.data.model.ClassEvent
import com.example.receiver.AlarmReceiver

class NotificationScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "NotificationScheduler"
        private const val PREFS_NAME = "planovo_reminders"
        private const val PREF_ENABLED_IDS = "enabled_event_ids"
        const val REMINDER_LEAD_TIME_MINUTES = 5
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }

    fun isReminderEnabled(eventId: String): Boolean =
        prefs.getStringSet(PREF_ENABLED_IDS, emptySet())?.contains(eventId) == true

    fun getEnabledReminderIds(): Set<String> =
        prefs.getStringSet(PREF_ENABLED_IDS, emptySet())?.toSet() ?: emptySet()

    fun enableReminder(event: ClassEvent): Boolean {
        if (event.isCancelled || event.startTimeMillis <= System.currentTimeMillis()) {
            return false
        }

        val ids = getEnabledReminderIds().toMutableSet()
        ids.add(event.id)
        prefs.edit().putStringSet(PREF_ENABLED_IDS, ids).apply()

        scheduleAlarm(event)
        return true
    }

    fun disableReminder(eventId: String) {
        cancelAlarm(eventId)

        val ids = getEnabledReminderIds().toMutableSet()
        ids.remove(eventId)
        prefs.edit().putStringSet(PREF_ENABLED_IDS, ids).apply()
    }

    fun toggleReminder(event: ClassEvent): Boolean {
        return if (isReminderEnabled(event.id)) {
            disableReminder(event.id)
            false
        } else {
            enableReminder(event)
        }
    }

    private fun cancelAlarm(eventId: String) {
        try {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to cancel alarm for $eventId", e)
        }
    }

    private fun scheduleAlarm(event: ClassEvent) {
        if (event.isCancelled) return

        val triggerTime =
            event.startTimeMillis - REMINDER_LEAD_TIME_MINUTES * 60_000L
        val now = System.currentTimeMillis()

        // A past trigger time must never be passed to AlarmManager:
        // Android fires such an alarm immediately.
        if (triggerTime <= now) {
            cancelAlarm(event.id)
            Log.d(TAG, "Reminder point already passed for ${event.id}; not firing now")
            return
        }

        val maxFutureLimit = now + 3L * 24 * 60 * 60 * 1000
        if (triggerTime > maxFutureLimit) {
            cancelAlarm(event.id)
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("event_id", event.id)
            putExtra("event_title", event.title)
            putExtra("event_time", event.startTimeMillis)
            putExtra("event_location", event.location)
            putExtra("event_teacher", event.teacher)
            putExtra("lead_time_min", REMINDER_LEAD_TIME_MINUTES)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder for ${event.title} at $triggerTime")
        } catch (e: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Throwable) {
                Log.w(TAG, "Fallback alarm could not be set: ${ex.message}")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Could not schedule alarm: ${e.message}")
        }
    }

    fun rescheduleEnabled(events: List<ClassEvent>) {
        val enabledIds = getEnabledReminderIds()
        events.filter { it.id in enabledIds }.forEach { event ->
            cancelAlarm(event.id)
            scheduleAlarm(event)
        }
    }

    fun cancelAlarms(events: List<ClassEvent>) {
        events.forEach { cancelAlarm(it.id) }
    }

    fun cancelAlarms(eventIds: Set<String>) {
        eventIds.forEach { cancelAlarm(it) }

        if (eventIds.isNotEmpty()) {
            val enabled = getEnabledReminderIds().toMutableSet()
            if (enabled.removeAll(eventIds)) {
                prefs.edit().putStringSet(PREF_ENABLED_IDS, enabled).apply()
            }
        }
    }
}
