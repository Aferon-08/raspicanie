package com.example.data.model

enum class ClassStatus { ONGOING, UPCOMING_SOON, SCHEDULED, COMPLETED, CANCELLED }
enum class ChangeType { TIME_CHANGED, LOCATION_CHANGED, DETAILS_CHANGED, CANCELLED, NEW_CLASS, RESTORED }

data class SubgroupInfo(val number: String, val teacher: String, val room: String)

data class ClassEvent(
    val id: String,
    val title: String,
    val description: String = "",
    val teacher: String = "",
    val location: String = "",
    val lessonType: String = "",
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isCancelled: Boolean = false,
    val hasChanges: Boolean = false,
    val changeDetails: String? = null,
    val rawSummary: String = "",
    /** Explicit subgroup rows used when several parallel events are presented as one lesson. */
    val subgroups: List<SubgroupInfo> = emptyList()
) {
    fun getStatus(currentTimeMillis: Long = System.currentTimeMillis()): ClassStatus = when {
        isCancelled -> ClassStatus.CANCELLED
        currentTimeMillis in startTimeMillis..endTimeMillis -> ClassStatus.ONGOING
        currentTimeMillis < startTimeMillis && (startTimeMillis - currentTimeMillis) <= 120 * 60 * 1000 -> ClassStatus.UPCOMING_SOON
        currentTimeMillis < startTimeMillis -> ClassStatus.SCHEDULED
        else -> ClassStatus.COMPLETED
    }

    val durationMinutes: Int
        get() = ((endTimeMillis - startTimeMillis) / (60 * 1000)).toInt().coerceAtLeast(0)

    fun getProgress(currentTimeMillis: Long = System.currentTimeMillis()): Float {
        if (currentTimeMillis <= startTimeMillis) return 0f
        if (currentTimeMillis >= endTimeMillis) return 1f
        val total = (endTimeMillis - startTimeMillis).toFloat()
        return if (total > 0) (currentTimeMillis - startTimeMillis) / total else 0f
    }

    fun getRemainingMinutes(currentTimeMillis: Long = System.currentTimeMillis()): Int {
        if (currentTimeMillis >= endTimeMillis) return 0
        return ((endTimeMillis - currentTimeMillis) / (60 * 1000)).toInt().coerceAtLeast(1)
    }

    fun getMinutesUntilStart(currentTimeMillis: Long = System.currentTimeMillis()): Int {
        if (currentTimeMillis >= startTimeMillis) return 0
        return ((startTimeMillis - currentTimeMillis) / (60 * 1000)).toInt().coerceAtLeast(1)
    }

    val displayLessonType: String
        get() {
            if (lessonType.isNotBlank()) return lessonType
            val text = (title + " " + description + " " + rawSummary).lowercase()
            return when {
                "лекци" in text -> "Лекция"
                "лаб" in text -> "Лаб"
                "практик" in text || "семинар" in text -> "Практика"
                "зачет" in text || "зачёт" in text -> "Зачёт"
                "экзамен" in text -> "Экзамен"
                "консульт" in text -> "Консультация"
                else -> "Лекция"
            }
        }

    val isOnline: Boolean
        get() {
            val text = (title + " " + description + " " + location + " " + lessonType + " " + rawSummary).lowercase()
            return listOf("онлайн", "online", "zoom", "конференц", "дистанцион", "вебинар").any { it in text }
        }

    val onlineMeetingUrl: String?
        get() {
            val text = description + "\n" + location + "\n" + rawSummary
            return Regex("""https?://[^\s<>"]+""").findAll(text)
                .map { it.value.trimEnd('.', ',', ';', ')', ']') }
                .firstOrNull { it.contains("zoom", ignoreCase = true) }
        }

    val onlineMeetingId: String?
        get() {
            val text = description + "\n" + rawSummary
            val patterns = listOf(
                Regex("""(?:идентификатор(?:\s+конференции)?|номер(?:\s+конференции)?|conference\s+id|meeting\s+id|meetingid)\s*[:№-]?\s*([\d\s-]{6,})""", RegexOption.IGNORE_CASE),
                Regex("""(?:zoom\s*)?(?:id)\s*[:№-]?\s*([\d\s-]{6,})""", RegexOption.IGNORE_CASE)
            )
            return patterns.asSequence().mapNotNull {
                it.find(text)?.groupValues?.getOrNull(1)?.trim()?.replace(Regex("\\s+"), " ")
            }.firstOrNull()
        }

    val onlineMeetingPassword: String?
        get() {
            val text = description + "\n" + rawSummary
            return Regex("""(?:пароль|password|passcode|код\s+доступа)\s*[:№-]?\s*([^\s,;]+)""", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.getOrNull(1)?.trim()?.trim('.', ',', ';')
        }

    private val cachedDisplaySubgroups: List<SubgroupInfo> by lazy(LazyThreadSafetyMode.NONE) {
        buildDisplaySubgroups()
    }

    val displaySubgroups: List<SubgroupInfo>
        get() = cachedDisplaySubgroups

    private fun buildDisplaySubgroups(): List<SubgroupInfo> {
            if (subgroups.isNotEmpty()) {
                return subgroups.map { subgroup ->
                    subgroup.copy(
                        teacher = subgroup.teacher
                            .takeIf { it.isNotBlank() && !it.trim().matches(Regex("""(?i)подгруппа\s*\d+""")) }
                            ?: extractTeacherName()
                    )
                }
            }

            val list = mutableListOf<SubgroupInfo>()

            // All teachers mentioned in the event: every "Преподаватель: ..." line of the
            // description plus the (possibly comma-separated) teacher field.
            val teacherPool = (
                TEACHER_LINE_REGEX.findAll(description).flatMap { splitNames(it.groupValues[1]) }.toList() +
                    splitNames(teacher)
                ).distinct()
            val roomPool = splitNames(location)

            if (description.contains("подгрупп", ignoreCase = true) || description.contains("ауд", ignoreCase = true)) {
                val allLines = description.lines().filter { it.isNotBlank() }
                val blockLines = allLines.filter { it.contains("подгрупп", ignoreCase = true) }
                    .ifEmpty { allLines.filter { it.contains("ауд", ignoreCase = true) } }
                blockLines.forEachIndexed { i, line ->
                    val num = SUBGROUP_NUMBER_REGEX.find(line)?.groupValues?.getOrNull(1)
                        ?.takeIf { it.isNotBlank() } ?: (i + 1).toString()
                    val room = if ("ауд" in line.lowercase()) {
                        "ауд. " + line.substringAfter("ауд").trim().trim('.', ':', ' ')
                    } else {
                        roomPool.takeIf { it.size == blockLines.size }?.getOrNull(i) ?: location
                    }
                    val ownName = line.substringBefore("ауд")
                        .replace(SUBGROUP_LABEL_REGEX, " ")
                        .replace(Regex("""(?i)(?:Преподаватель|Педагог|Тренер|Учитель|Инструктор|Ведущий)\s*:"""), " ")
                        .trim().trim(',', ';', ':', '-', '–', '—', '.', ' ')
                    val teacherName = ownName.ifBlank {
                        // The line names no teacher: take the i-th teacher instead of always the first.
                        teacherPool.getOrNull(i) ?: teacher
                    }
                    list.add(SubgroupInfo(num, teacherName, room))
                }
            }
            if (list.isEmpty() && teacherPool.size > 1) {
                teacherPool.forEachIndexed { i, t ->
                    val room = roomPool.takeIf { it.size == teacherPool.size }?.getOrNull(i)
                        ?: location.ifEmpty { "ауд. —" }
                    list.add(SubgroupInfo((i + 1).toString(), t, room))
                }
            }
        return list
    }

    private fun splitNames(text: String): List<String> =
        text.split(Regex("""[,;\n]""")).map { it.trim() }
            .filter { it.isNotBlank() && !it.matches(Regex("""(?i)подгруппа\s*\d+""")) }

    private companion object {
        val TEACHER_LINE_REGEX = Regex(
            """(?im)(?:Преподаватель|Педагог|Тренер|Учитель|Инструктор|Ведущий)\s*:\s*(.+)$"""
        )
        val SUBGROUP_NUMBER_REGEX = Regex("""(?i)(?:под)?групп\w*\s*(\d+)""")
        val SUBGROUP_LABEL_REGEX = Regex("""(?i)(?:под)?групп\w*\s*\d*""")
    }

    private fun extractTeacherName(): String {
        if (teacher.isNotBlank() && !teacher.trim().matches(Regex("""(?i)подгруппа\s*\d+"""))) {
            return teacher.trim()
        }

        val descriptionTeacher = Regex(
            """(?im)^(?:Преподаватель|Педагог|Тренер|Учитель|Инструктор|Ведущий)\s*:\s*(.+)$"""
        ).find(description)?.groupValues?.getOrNull(1)?.trim()
        if (!descriptionTeacher.isNullOrBlank()) return descriptionTeacher

        Regex("""\(([^()]+)\)\s*$""").find(rawSummary)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.split(Regex("""\s+""")).size in 2..4 }
            ?.let { return it }

        Regex("""\s[-–—:]\s*(.+)$""").find(rawSummary)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.split(Regex("""\s+""")).size in 2..4 }
            ?.let { return it }

        return ""
    }
}

data class ScheduleChange(
    val id: Long = 0,
    val eventId: String,
    val eventTitle: String,
    val changeType: ChangeType,
    val detectedAtMillis: Long,
    val details: String,
    val eventDateMillis: Long
)
