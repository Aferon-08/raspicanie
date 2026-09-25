package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ChangeType
import com.example.data.model.ClassEvent
import com.example.data.model.ScheduleChange

@Entity(tableName = "schedule_events")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val teacher: String,
    val location: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isCancelled: Boolean,
    val hasChanges: Boolean,
    val changeDetails: String?,
    val rawSummary: String,
    val lastSyncedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): ClassEvent = ClassEvent(
        id = id,
        title = title,
        description = description,
        teacher = teacher,
        location = location,
        startTimeMillis = startTimeMillis,
        endTimeMillis = endTimeMillis,
        isCancelled = isCancelled,
        hasChanges = hasChanges,
        changeDetails = changeDetails,
        rawSummary = rawSummary
    )

    companion object {
        fun fromDomain(event: ClassEvent, syncedAt: Long = System.currentTimeMillis()): ScheduleEntity =
            ScheduleEntity(
                id = event.id,
                title = event.title,
                description = event.description,
                teacher = event.teacher,
                location = event.location,
                startTimeMillis = event.startTimeMillis,
                endTimeMillis = event.endTimeMillis,
                isCancelled = event.isCancelled,
                hasChanges = event.hasChanges,
                changeDetails = event.changeDetails,
                rawSummary = event.rawSummary,
                lastSyncedAtMillis = syncedAt
            )
    }
}

@Entity(tableName = "schedule_changes")
data class ChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val eventTitle: String,
    val changeType: String,
    val detectedAtMillis: Long,
    val details: String,
    val eventDateMillis: Long
) {
    fun toDomain(): ScheduleChange = ScheduleChange(
        id = id,
        eventId = eventId,
        eventTitle = eventTitle,
        changeType = runCatching { ChangeType.valueOf(changeType) }.getOrDefault(ChangeType.TIME_CHANGED),
        detectedAtMillis = detectedAtMillis,
        details = details,
        eventDateMillis = eventDateMillis
    )

    companion object {
        fun fromDomain(change: ScheduleChange): ChangeEntity = ChangeEntity(
            id = change.id,
            eventId = change.eventId,
            eventTitle = change.eventTitle,
            changeType = change.changeType.name,
            detectedAtMillis = change.detectedAtMillis,
            details = change.details,
            eventDateMillis = change.eventDateMillis
        )
    }
}
