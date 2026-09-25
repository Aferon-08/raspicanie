package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ScheduleTimeFormatter {

    // Planovo schedules are based in Moscow Time (Europe/Moscow, UTC+3)
    val timeZone: TimeZone = TimeZone.getTimeZone("Europe/Moscow")

    fun formatTime(timeMillis: Long, is24Hour: Boolean): String {
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        val format = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = ScheduleTimeFormatter.timeZone
        }
        return format.format(Date(timeMillis))
    }

    fun formatDate(timeMillis: Long, pattern: String, locale: Locale = Locale("ru")): String {
        val format = SimpleDateFormat(pattern, locale).apply {
            timeZone = ScheduleTimeFormatter.timeZone
        }
        return format.format(Date(timeMillis))
    }

    fun getCalendar(timeMillis: Long? = null): Calendar {
        return Calendar.getInstance(timeZone).apply {
            if (timeMillis != null) {
                this.timeInMillis = timeMillis
            }
        }
    }
}
