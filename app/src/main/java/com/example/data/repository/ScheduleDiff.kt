package com.example.data.repository

import com.example.data.local.ScheduleEntity
import com.example.data.model.ChangeType
import com.example.data.model.ClassEvent
import com.example.data.model.ScheduleChange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScheduleDiffResult(
    val events: List<ScheduleEntity>,
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
            val oldEvent = oldEvents[newEvent.id]
            val eventChanges = mutableListOf<String>()

            if (oldEvent != null) {
                if (oldEvent.startTimeMillis != newEvent.startTimeMillis ||
                    oldEvent.endTimeMillis != newEvent.endTimeMillis
                ) {
                    val oldRange = formatRange(oldEvent.startTimeMillis, oldEvent.endTimeMillis)
                    val newRange = formatRange(newEvent.startTimeMillis, newEvent.endTimeMillis)
                    val detail = "Время изменено с $oldRange на $newRange"
                    eventChanges += detail
                    changes += change(newEvent, ChangeType.TIME_CHANGED, detail)
                }

                if (!oldEvent.isCancelled && newEvent.isCancelled) {
                    val detail = "Занятие отменено"
                    eventChanges += detail
                    changes += change(newEvent, ChangeType.CANCELLED, detail)
                } else if (oldEvent.isCancelled && !newEvent.isCancelled) {
                    val detail = "Занятие снова появилось в расписании"
                    eventChanges += detail
                    changes += change(newEvent, ChangeType.RESTORED, detail)
                }

                if (oldEvent.location != newEvent.location) {
                    val oldLocation = oldEvent.location.ifBlank { "не указана" }
                    val newLocation = newEvent.location.ifBlank { "не указана" }
                    val detail = "Кабинет изменен: $newLocation (ранее: $oldLocation)"
                    eventChanges += detail
                    changes += change(newEvent, ChangeType.LOCATION_CHANGED, detail)
                }

                val changedFields = buildList {
                    if (oldEvent.title != newEvent.title) add("название")
                    if (oldEvent.teacher != newEvent.teacher) add("преподаватель")
                    if (oldEvent.description != newEvent.description) add("описание")
                    if (oldEvent.rawSummary != newEvent.rawSummary) add("данные события")
                }
                if (changedFields.isNotEmpty()) {
                    val detail = "Изменено: ${changedFields.joinToString(", ")}"
                    eventChanges += detail
                    changes += change(newEvent, ChangeType.DETAILS_CHANGED, detail)
                }
            } else if (oldEvents.isNotEmpty() && newEvent.startTimeMillis > nowMillis) {
                val time = formatTime(newEvent.startTimeMillis)
                val detail = "Добавлено новое занятие на $time"
                eventChanges += detail
                changes += change(newEvent, ChangeType.NEW_CLASS, detail)
            }

            ScheduleEntity.fromDomain(
                newEvent.copy(
                    hasChanges = eventChanges.isNotEmpty(),
                    changeDetails = eventChanges.takeIf { it.isNotEmpty() }?.joinToString(" • ")
                )
            )
        }

        val newIds = newEvents.mapTo(mutableSetOf()) { it.id }
        val removedEventIds = oldEvents.keys - newIds

        return ScheduleDiffResult(enrichedEvents, changes, removedEventIds)
    }

    private fun change(
        event: ClassEvent,
        type: ChangeType,
        details: String
    ): ScheduleChange = ScheduleChange(
        eventId = event.id,
        eventTitle = event.title,
        changeType = type,
        detectedAtMillis = System.currentTimeMillis(),
        details = details,
        eventDateMillis = event.startTimeMillis
    )

    private fun formatRange(start: Long, end: Long): String =
        "${formatTime(start)}—${formatTime(end)}"

    private fun formatTime(timeMillis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
}
