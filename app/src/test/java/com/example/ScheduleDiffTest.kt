package com.example

import com.example.data.local.ScheduleEntity
import com.example.data.model.ChangeType
import com.example.data.model.ClassEvent
import com.example.data.repository.ScheduleDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleDiffTest {

    private fun entity(event: ClassEvent) = ScheduleEntity.fromDomain(event)

    private fun event(
        id: String = "event",
        title: String = "Математика",
        teacher: String = "Иванов И.И.",
        location: String = "101",
        start: Long = 1_000_000L,
        end: Long = 4_600_000L,
        cancelled: Boolean = false,
        description: String = "Обычное занятие"
    ) = ClassEvent(
        id = id,
        title = title,
        teacher = teacher,
        location = location,
        startTimeMillis = start,
        endTimeMillis = end,
        isCancelled = cancelled,
        description = description
    )

    @Test
    fun detectsEndTimeChange() {
        val old = event(end = 4_600_000L)
        val new = event(end = 5_200_000L)

        val result = ScheduleDiff.calculate(mapOf(old.id to entity(old)), listOf(new), nowMillis = 0)

        assertEquals(1, result.changes.size)
        assertEquals(ChangeType.TIME_CHANGED, result.changes.single().changeType)
        assertTrue(result.events.single().hasChanges)
    }

    @Test
    fun detectsTeacherAndTitleChanges() {
        val old = event()
        val new = event(title = "Физика", teacher = "Петров П.П.")

        val result = ScheduleDiff.calculate(mapOf(old.id to entity(old)), listOf(new), nowMillis = 0)

        assertEquals(1, result.changes.size)
        assertEquals(ChangeType.DETAILS_CHANGED, result.changes.single().changeType)
        assertTrue(result.changes.single().details.contains("название"))
        assertTrue(result.changes.single().details.contains("преподаватель"))
    }

    @Test
    fun detectsLocationChangeToBlank() {
        val old = event(location = "101")
        val new = event(location = "")

        val result = ScheduleDiff.calculate(mapOf(old.id to entity(old)), listOf(new), nowMillis = 0)

        assertEquals(ChangeType.LOCATION_CHANGED, result.changes.single().changeType)
        assertTrue(result.changes.single().details.contains("не указана"))
    }

    @Test
    fun detectsRestoredClass() {
        val old = event(cancelled = true)
        val new = event(cancelled = false)

        val result = ScheduleDiff.calculate(mapOf(old.id to entity(old)), listOf(new), nowMillis = 0)

        assertEquals(ChangeType.RESTORED, result.changes.single().changeType)
    }

    @Test
    fun reportsRemovedEventIds() {
        val old = event(id = "removed")
        val result = ScheduleDiff.calculate(
            mapOf(old.id to entity(old)),
            emptyList(),
            nowMillis = 0
        )

        assertEquals(setOf("removed"), result.removedEventIds)
    }
}
