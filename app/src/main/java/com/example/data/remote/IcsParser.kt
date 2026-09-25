package com.example.data.remote

import com.example.data.model.ClassEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object IcsParser {

    private val moskowTimeZone: TimeZone = TimeZone.getTimeZone("Europe/Moscow")

    /**
     * Parses standard iCalendar string (.ics) into a list of ClassEvent items.
     */
    fun parse(icsContent: String): List<ClassEvent> {
        val unfoldedLines = unfoldLines(icsContent)
        val events = mutableListOf<ClassEvent>()

        var inEvent = false
        var currentUid = ""
        var currentSummary = ""
        var currentDescription = ""
        var currentLocation = ""
        var currentStatus = ""
        var startMillis: Long? = null
        var endMillis: Long? = null

        for (line in unfoldedLines) {
            val trimmed = line.trim()
            if (trimmed.equals("BEGIN:VEVENT", ignoreCase = true)) {
                inEvent = true
                currentUid = ""
                currentSummary = ""
                currentDescription = ""
                currentLocation = ""
                currentStatus = ""
                startMillis = null
                endMillis = null
                continue
            }

            if (trimmed.equals("END:VEVENT", ignoreCase = true)) {
                if (inEvent && startMillis != null) {
                    val actualEnd = endMillis ?: (startMillis + 60 * 60 * 1000) // Default 1 hour
                    val cleanSummary = unescapeIcs(currentSummary).trim()
                    val cleanDesc = unescapeIcs(currentDescription).trim()
                    val cleanLoc = unescapeIcs(currentLocation).trim()
                    val isCancelled = currentStatus.equals("CANCELLED", ignoreCase = true) ||
                            cleanSummary.contains("отменен", ignoreCase = true) ||
                            cleanDesc.contains("отменен", ignoreCase = true)

                    val (extractedTitle, extractedTeacher) = extractTitleAndTeacher(cleanSummary, cleanDesc)
                    val finalUid = if (currentUid.isNotBlank()) currentUid else "event_${startMillis}_${cleanSummary.hashCode()}"

                    events.add(
                        ClassEvent(
                            id = finalUid,
                            title = extractedTitle.ifBlank { "Занятие" },
                            description = cleanDesc,
                            teacher = extractedTeacher,
                            location = cleanLoc,
                            startTimeMillis = startMillis,
                            endTimeMillis = actualEnd,
                            isCancelled = isCancelled,
                            rawSummary = cleanSummary
                        )
                    )
                }
                inEvent = false
                continue
            }

            if (!inEvent) continue

            val colonIdx = line.indexOf(':')
            if (colonIdx == -1) continue

            val keyPart = line.substring(0, colonIdx).trim()
            val value = line.substring(colonIdx + 1).trim()

            val propName = if (keyPart.contains(';')) {
                keyPart.substring(0, keyPart.indexOf(';')).uppercase(Locale.ROOT)
            } else {
                keyPart.uppercase(Locale.ROOT)
            }

            when (propName) {
                "UID" -> currentUid = value
                "SUMMARY" -> currentSummary = value
                "DESCRIPTION" -> currentDescription = value
                "LOCATION" -> currentLocation = value
                "STATUS" -> currentStatus = value
                "DTSTART" -> startMillis = parseIcsDateTime(keyPart, value)
                "DTEND" -> endMillis = parseIcsDateTime(keyPart, value)
            }
        }

        return events.sortedBy { it.startTimeMillis }
    }

    private fun unfoldLines(content: String): List<String> {
        val rawLines = content.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val result = mutableListOf<String>()

        for (line in rawLines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (result.isNotEmpty()) {
                    val lastIdx = result.size - 1
                    result[lastIdx] = result[lastIdx] + line.substring(1)
                }
            } else if (line.isNotBlank()) {
                result.add(line)
            }
        }
        return result
    }

    private fun parseIcsDateTime(keyPart: String, value: String): Long? {
        val cleanValue = value.trim()
        if (cleanValue.isEmpty()) return null

        // Check if TZID parameter exists in keyPart (e.g. DTSTART;TZID=Europe/Moscow)
        var specifiedTimeZone: TimeZone? = null
        if (keyPart.contains("TZID=", ignoreCase = true)) {
            val tzidValue = keyPart.substringAfter("TZID=", "").substringBefore(';').substringBefore(':')
            if (tzidValue.isNotBlank()) {
                specifiedTimeZone = TimeZone.getTimeZone(tzidValue)
            }
        }

        // Format 1: 20260925T140000Z (UTC)
        if (cleanValue.endsWith("Z", ignoreCase = true)) {
            val format = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return runCatching { format.parse(cleanValue)?.time }.getOrNull()
        }

        // Format 2: 20260925T140000 (Local / with specified TZ)
        if (cleanValue.contains("T")) {
            val pattern = if (cleanValue.length >= 15) "yyyyMMdd'T'HHmmss" else "yyyyMMdd'T'HHmm"
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = specifiedTimeZone ?: moskowTimeZone
            }
            return runCatching { format.parse(cleanValue.substring(0, pattern.length.coerceAtMost(cleanValue.length)))?.time }.getOrNull()
        }

        // Format 3: 20260925 (Date only)
        if (cleanValue.length == 8) {
            val format = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = specifiedTimeZone ?: moskowTimeZone
            }
            return runCatching { format.parse(cleanValue)?.time }.getOrNull()
        }

        return null
    }

    private fun unescapeIcs(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }

    private fun extractTitleAndTeacher(summary: String, description: String): Pair<String, String> {
        var title = summary
        var teacher = ""

        // Try extracting teacher from description if present
        // Patterns: "Преподаватель: Иванов И.И.", "Тренер: ...", "Педагог: ..."
        val teacherPrefixes = listOf("Преподаватель:", "Педагог:", "Тренер:", "Учитель:", "Инструктор:", "Ведущий:")
        for (prefix in teacherPrefixes) {
            val idx = description.indexOf(prefix, ignoreCase = true)
            if (idx != -1) {
                val after = description.substring(idx + prefix.length).trim()
                teacher = after.lines().firstOrNull()?.trim().orEmpty()
                break
            }
        }

        // Clean title if summary has teacher in parentheses
        val matchParens = Regex("""^(.*?)\s*\((.*?)\)$""").find(summary)
        if (matchParens != null) {
            val (part1, part2) = matchParens.destructured
            if (part2.contains(Regex("""[А-ЯA-Z]\.[А-ЯA-Z]\.""")) || part2.split(" ").size in 2..3) {
                title = part1.trim()
                if (teacher.isBlank()) {
                    teacher = part2.trim()
                }
            }
        } else if (summary.contains(" - ")) {
            val parts = summary.split(" - ")
            if (parts.size == 2) {
                title = parts[0].trim()
                if (teacher.isBlank()) {
                    teacher = parts[1].trim()
                }
            }
        }

        return Pair(title, teacher)
    }
}
