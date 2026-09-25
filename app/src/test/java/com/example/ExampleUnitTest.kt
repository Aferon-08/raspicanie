package com.example

import com.example.data.model.ClassStatus
import com.example.data.remote.IcsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testIcsParsing() {
        val sampleIcs = """
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Planovo//Calendar//RU
BEGIN:VEVENT
UID:test_event_101
SUMMARY:Современная хореография (Иванова А.С.)
DESCRIPTION:Преподаватель: Иванова Анна Сергеевна\nГруппа 41
LOCATION:Зал 3
STATUS:CONFIRMED
DTSTART:20260925T140000Z
DTEND:20260925T153000Z
END:VEVENT
BEGIN:VEVENT
UID:test_event_102
SUMMARY:Классический танец
DESCRIPTION:Занятие отменено
LOCATION:Зал 1
STATUS:CANCELLED
DTSTART:20260925T160000Z
DTEND:20260925T173000Z
END:VEVENT
END:VCALENDAR
        """.trimIndent()

        val events = IcsParser.parse(sampleIcs)
        assertEquals(2, events.size)

        val ev1 = events[0]
        assertEquals("test_event_101", ev1.id)
        assertEquals("Современная хореография", ev1.title)
        assertEquals("Иванова Анна Сергеевна", ev1.teacher)
        assertEquals("Зал 3", ev1.location)
        assertFalse(ev1.isCancelled)
        assertEquals(90, ev1.durationMinutes)

        val ev2 = events[1]
        assertEquals("test_event_102", ev2.id)
        assertTrue(ev2.isCancelled)
        assertEquals(ClassStatus.CANCELLED, ev2.getStatus())
    }
}
