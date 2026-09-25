package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.ClassEvent
import com.example.receiver.AlarmReceiver

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleClassReminder(event: ClassEvent, leadTimeMinutes: Int) {
        if (event.isCancelled) return

        val triggerTime = event.startTimeMillis - (leadTimeMinutes * 60 * 1000)
        val now = System.currentTimeMillis()

        if (triggerTime <= now) {
            // Event reminder time has already passed
            return
        }

        // Limit scheduling to classes occurring within the next 3 days
        val maxFutureLimit = now + (3L * 24 * 60 * 60 * 1000)
        if (triggerTime > maxFutureLimit) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("event_id", event.id)
            putExtra("event_title", event.title)
            putExtra("event_time", event.startTimeMillis)
            putExtra("event_location", event.location)
            putExtra("event_teacher", event.teacher)
            putExtra("lead_time_min", leadTimeMinutes)
        }

        val requestCode = event.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
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
            Log.d("NotificationScheduler", "Scheduled alarm for ${event.title} at $triggerTime")
        } catch (e: SecurityException) {
            // Exact alarm permission not granted, fallback to inexact alarm
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: IllegalStateException) {
                Log.w("NotificationScheduler", "Concurrent alarm limit reached: ${ex.message}")
            } catch (ex: Throwable) {
                Log.w("NotificationScheduler", "Fallback alarm could not be set: ${ex.message}")
            }
        } catch (e: IllegalStateException) {
            Log.w("NotificationScheduler", "Concurrent alarm limit reached: ${e.message}")
        } catch (e: Throwable) {
            Log.w("NotificationScheduler", "Could not schedule alarm: ${e.message}")
        }
    }

    fun cancelClassReminder(eventId: String) {
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
            Log.w("NotificationScheduler", "Failed to cancel alarm for $eventId", e)
        }
    }

    fun cancelAll(events: List<ClassEvent>) {
        for (event in events) {
            cancelClassReminder(event.id)
        }
    }

    fun rescheduleAll(events: List<ClassEvent>, leadTimeMinutes: Int) {
        // Schedule only the nearest upcoming classes (limit 5) to stay far below any system limits
        val cappedEvents = events.take(5)
        for (event in cappedEvents) {
            scheduleClassReminder(event, leadTimeMinutes)
        }
    }
}
