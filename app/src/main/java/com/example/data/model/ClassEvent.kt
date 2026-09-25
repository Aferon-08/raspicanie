package com.example.data.model

enum class ClassStatus {
    ONGOING,       // Идёт прямо сейчас
    UPCOMING_SOON, // Начнётся в течение 2 часов
    SCHEDULED,     // Запланировано на сегодня/будущее
    COMPLETED,     // Уже завершено
    CANCELLED      // Отменено
}

enum class ChangeType {
    TIME_CHANGED,     // Перенос времени
    LOCATION_CHANGED, // Смена кабинета/зала
    CANCELLED,        // Отмена занятия
    NEW_CLASS,        // Добавлено новое занятие
    RESTORED          // Возобновлено
}

data class SubgroupInfo(
    val number: String,
    val teacher: String,
    val room: String
)

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
    val rawSummary: String = ""
) {
    fun getStatus(currentTimeMillis: Long = System.currentTimeMillis()): ClassStatus {
        return when {
            isCancelled -> ClassStatus.CANCELLED
            currentTimeMillis in startTimeMillis..endTimeMillis -> ClassStatus.ONGOING
            currentTimeMillis < startTimeMillis && (startTimeMillis - currentTimeMillis) <= 120 * 60 * 1000 -> ClassStatus.UPCOMING_SOON
            currentTimeMillis < startTimeMillis -> ClassStatus.SCHEDULED
            else -> ClassStatus.COMPLETED
        }
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
            val text = "$title $description $rawSummary".lowercase()
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

    val displaySubgroups: List<SubgroupInfo>
        get() {
            val list = mutableListOf<SubgroupInfo>()
            // Check if multiple teachers / subgroups exist in description or teacher field
            if (description.contains("подгрупп", ignoreCase = true) || description.contains("ауд", ignoreCase = true)) {
                val lines = description.lines().filter { it.isNotBlank() }
                var idx = 1
                for (line in lines) {
                    if (line.contains("подгрупп", ignoreCase = true) || line.contains("ауд", ignoreCase = true)) {
                        val num = if (line.contains("1")) "1" else if (line.contains("2")) "2" else "$idx"
                        val room = if ("ауд" in line) "ауд. " + line.substringAfter("ауд").trim().trim('.', ':', ' ') else location
                        val teacherName = line.substringBefore("ауд").substringAfter(":").trim().ifEmpty { teacher }
                        list.add(SubgroupInfo(num, teacherName, room))
                        idx++
                    }
                }
            }
            if (list.isEmpty() && teacher.contains(",")) {
                val teachers = teacher.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (teachers.size > 1) {
                    teachers.forEachIndexed { i, t ->
                        list.add(SubgroupInfo("${i + 1}", t, location.ifEmpty { "ауд. —" }))
                    }
                }
            }
            return list
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
