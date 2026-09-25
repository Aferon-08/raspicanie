package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.notifications.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra("event_id") ?: return
        val title = intent.getStringExtra("event_title") ?: "Занятие"
        val startTimeMillis = intent.getLongExtra("event_time", 0L)
        val location = intent.getStringExtra("event_location") ?: ""
        val teacher = intent.getStringExtra("event_teacher") ?: ""
        val leadTimeMinutes = intent.getIntExtra("lead_time_min", 15)

        Log.d("AlarmReceiver", "Received alarm for event $title ($eventId) at $startTimeMillis")

        val helper = NotificationHelper(context)
        helper.showClassNotification(
            eventId = eventId,
            title = title,
            startTimeMillis = startTimeMillis,
            location = location,
            teacher = teacher,
            leadTimeMinutes = leadTimeMinutes
        )
    }
}
