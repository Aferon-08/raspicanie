package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "planovo_class_reminders_channel"
        const val CHANNEL_NAME = "Напоминания о занятиях"
        const val CHANNEL_DESC = "Уведомления о предстоящих занятиях и изменениях в расписании"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showClassNotification(
        eventId: String,
        title: String,
        startTimeMillis: Long,
        location: String,
        teacher: String,
        leadTimeMinutes: Int
    ) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = timeFormat.format(Date(startTimeMillis))

        val intent = (context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(context.packageName)
            }).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("selected_event_id", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyBuilder = StringBuilder()
        if (leadTimeMinutes > 0) {
            bodyBuilder.append("Через $leadTimeMinutes мин в $timeStr")
        } else {
            bodyBuilder.append("Начало в $timeStr")
        }

        if (location.isNotBlank()) {
            bodyBuilder.append(" • ").append(location)
        }
        if (teacher.isNotBlank()) {
            bodyBuilder.append(" (").append(teacher).append(")")
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Предстоящее занятие: $title")
            .setContentText(bodyBuilder.toString())
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyBuilder.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(defaultSoundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(eventId.hashCode(), notification)
    }

    fun showTestNotification(leadTimeMinutes: Int = 15) {
        showClassNotification(
            eventId = "test_event_${System.currentTimeMillis()}",
            title = "Тестовое занятие: Современный танец",
            startTimeMillis = System.currentTimeMillis() + leadTimeMinutes * 60 * 1000,
            location = "Зал 1 (Зеркальный)",
            teacher = "Иванова А.С.",
            leadTimeMinutes = leadTimeMinutes
        )
    }
}
