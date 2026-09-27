package com.example.data.repository

import com.example.data.local.ScheduleEntity
import com.example.data.model.ChangeType
import com.example.data.model.ClassEvent
import com.example.data.model.ScheduleChange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScheduleDiffResult(
    val events: List<ClassEvent>,
    val changes: List<ScheduleChange>,
    val removedEventIds: Set<String>
)

object ScheduleDiff {
    fun calculate(
        oldEvents: Map<String, ScheduleEntity>,
        newEvents: List<ClassEvent>,
        nowMillis: Long = System.currentTimeMillis()
    ): ScheduleDiffResult {
        val changes = mutableListOf<ScheduleChange>()

        val enrichedEvents = newEvents.map { newEvent ->
            val old = oldEvents[newEvent.id]
            if (old == null) {
                if (oldEvents.isNotEmpty() && newEvent.startTimeMillis >= nowMillis) {
                    changes += ScheduleChange(
                        eventId = newEvent.id,
                        eventTitle = newEvent.title,
                        changeType = ChangeType.NEW_CLASS,
                        detectedAtMillis = nowMillis,
                        details = "Добавлено новое занятие",
                        eventDateMillis = newEvent.startTimeMillis
                    )
                    newEvent.copy(hasChanges = true, changeDetails = "Добавлено новое занятие")
                } else {
                    newEvent
                }
            } else {
                val details = mutableListOf<String>()

                if (old.startTimeMillis != newEvent.startTimeMillis ||
                    old.endTimeMillis != newEvent.endTimeMillis
                ) {
                    val timeDetails = buildString {
                        if (old.startTimeMillis != newEvent.startTimeMillis) {
                            append("Время: ")
                            append(formatTime(old.startTimeMillis))
                            append(" → ")
                            append(formatTime(newEvent.startTimeMillis))
                        }
                        if (old.endTimeMillis != newEvent.endTimeMillis) {
                            if (isNotEmpty()) append("; ")
                            append("Окончание: ")
                            append(formatTime(old.endTimeMillis))
                            append(" → ")
                            append(formatTime(newEvent.endTimeMillis))
                        }
                    }
                    details += timeDetails
                    changes += ScheduleChange(
                        eventId = newEvent.id,
                        eventTitle = newEvent.title,
                        changeType = ChangeType.TIME_CHANGED,
                        detectedAtMillis = nowMillis,
                        details = timeDetails,
                        eventDateMillis = newEvent.startTimeMillis
                    )
                }

                if (old.location != newEvent.location) {
                    val locationDetails = "Аудитория: " +
                        old.location.ifBlank { "не указана" } + " → " +
                        newEvent.location.ifBlank { "не указана" }
                    details += locationDetails
                    changes += ScheduleChange(
                        eventId = newEvent.id,
                        eventTitle = newEvent.title,
                        changeType = ChangeType.LOCATION_CHANGED,
                        detectedAtMillis = nowMillis,
                        details = locationDetails,
                        eventDateMillis = newEvent.startTimeMillis
                    )
                }

                if (old.isCancelled != newEvent.isCancelled) {
                    val restored = old.isCancelled && !newEvent.isCancelled
                    val statusDetails = if (restored) "Занятие восстановлено" else "Занятие отменено"
                    details += statusDetails
                    changes += ScheduleChange(
                        eventId = newEvent.id,
                        eventTitle = newEvent.title,
                        changeType = if (restored) ChangeType.RESTORED else ChangeType.CANCELLED,
                        detectedAtMillis = nowMillis,
                        details = statusDetails,
                        eventDateMillis = newEvent.startTimeMillis
                    )
                }

                if (old.title != newEvent.title ||
                    old.teacher != newEvent.teacher ||
                    old.description != newEvent.description ||
                    old.rawSummary != newEvent.rawSummary
                ) {
                    val changedFields = buildList {
                        if (old.title != newEvent.title) add("название")
                        if (old.teacher != newEvent.teacher) add("преподаватель")
                        if (old.description != newEvent.description) add("описание")
                        if (old.rawSummary != newEvent.rawSummary) add("данные")
                    }.distinct().joinToString(", ")
                    val detailChanges = "Изменено: " + changedFields
                    details += detailChanges
                    changes += ScheduleChange(
                        eventId = newEvent.id,
                        eventTitle = newEvent.title,
                        changeType = ChangeType.DETAILS_CHANGED,
                        detectedAtMillis = nowMillis,
                        details = detailChanges,
                        eventDateMillis = newEvent.startTimeMillis
                    )
                }

                if (details.isEmpty()) {
                    newEvent
                } else {
                    newEvent.copy(
                        hasChanges = true,
                        changeDetails = details.joinToString("; ")
                    )
                }
            }
        }

        val newIds = newEvents.mapTo(mutableSetOf()) { it.id }
        val removedEventIds = oldEvents.keys - newIds

        return ScheduleDiffResult(enrichedEvents, changes, removedEventIds)
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
}
