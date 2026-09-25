package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Calendar
import java.util.concurrent.TimeUnit

class CalendarRemoteDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    suspend fun fetchCalendarIcs(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; PlanovoApp/1.0)")
                .header("Accept", "text/calendar, text/plain, */*")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Сервер вернул код ошибки: ${response.code}")
                )
            }

            val bodyString = response.body?.string() ?: ""
            if (bodyString.isBlank() || !bodyString.contains("BEGIN:VCALENDAR")) {
                return@withContext Result.failure(
                    Exception("Получен некорректный ответ от сервера (не является iCalendar)")
                )
            }

            Result.success(bodyString)
        } catch (e: Exception) {
            Log.e("CalendarRemoteDataSource", "Error fetching calendar", e)
            Result.failure(e)
        }
    }

    /**
     * Generates a realistic mock iCalendar string for Group 41 for offline demo/first-run fallback.
     */
    fun generateDemoIcs(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val datePrefix = String.format("%04d%02d%02d", year, month, day)

        // Today, Tomorrow, Day after tomorrow classes
        val calTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val dateTomorrow = String.format("%04d%02d%02d", calTomorrow.get(Calendar.YEAR), calTomorrow.get(Calendar.MONTH) + 1, calTomorrow.get(Calendar.DAY_OF_MONTH))

        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val dateYesterday = String.format("%04d%02d%02d", calYesterday.get(Calendar.YEAR), calYesterday.get(Calendar.MONTH) + 1, calYesterday.get(Calendar.DAY_OF_MONTH))

        return """
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Planovo//Group 41 Calendar//RU
X-WR-CALNAME:Группа 41 - Расписание
CALSCALE:GREGORIAN
BEGIN:VEVENT
UID:planovo_41_event_1
SUMMARY:Современная хореография (Иванова А.С.)
DESCRIPTION:Преподаватель: Иванова Анна Сергеевна\nФорма: спортивная форма, чешки.\nГруппа 41
LOCATION:Зал 3 (Главный корпус, 2 этаж)
STATUS:CONFIRMED
DTSTART;TZID=Europe/Moscow:${datePrefix}T090000
DTEND;TZID=Europe/Moscow:${datePrefix}T103000
END:VEVENT
BEGIN:VEVENT
UID:planovo_41_event_2
SUMMARY:Классический танец и растяжка
DESCRIPTION:Преподаватель: Смирнов Максим Игоревич\nПодготовка к отчетному концерту
LOCATION:Зал 1 (Зеркальный)
STATUS:CONFIRMED
DTSTART;TZID=Europe/Moscow:${datePrefix}T110000
DTEND;TZID=Europe/Moscow:${datePrefix}T123000
END:VEVENT
BEGIN:VEVENT
UID:planovo_41_event_3
SUMMARY:Актёрское мастерство (Петрова Е.В.)
DESCRIPTION:Преподаватель: Петрова Елена Владимировна\nТема: сценическое движение и этюды
LOCATION:Аудитория 204
STATUS:CONFIRMED
DTSTART;TZID=Europe/Moscow:${datePrefix}T140000
DTEND;TZID=Europe/Moscow:${datePrefix}T153000
END:VEVENT
BEGIN:VEVENT
UID:planovo_41_event_4
SUMMARY:Музыкальная грамота и ритмика
DESCRIPTION:Преподаватель: Соколов Д.Н.\nЗанятие перенесено в зал 2
LOCATION:Зал 2
STATUS:CONFIRMED
DTSTART;TZID=Europe/Moscow:${datePrefix}T160000
DTEND;TZID=Europe/Moscow:${datePrefix}T171500
END:VEVENT
BEGIN:VEVENT
UID:planovo_41_event_5
SUMMARY:Импровизация и джем
DESCRIPTION:Преподаватель: Кузнецов В.А.\nОткрытый класс
LOCATION:Большой зал
STATUS:CONFIRMED
DTSTART;TZID=Europe/Moscow:${dateTomorrow}T100000
DTEND;TZID=Europe/Moscow:${dateTomorrow}T113000
END:VEVENT
BEGIN:VEVENT
UID:planovo_41_event_6
SUMMARY:Основы акробатики
DESCRIPTION:Преподаватель: Морозов И.К.\nРазминка и баланс
LOCATION:Гимнастический зал
STATUS:CONFIRMED
DTSTART;TZID=Europe/Moscow:${dateTomorrow}T120000
DTEND;TZID=Europe/Moscow:${dateTomorrow}T133000
END:VEVENT
BEGIN:VEVENT
UID:planovo_41_event_7
SUMMARY:Постановка номеров (Группа 41)
DESCRIPTION:Преподаватель: Иванова Анна Сергеевна\nРепетиция
LOCATION:Зал 3
STATUS:CANCELLED
DTSTART;TZID=Europe/Moscow:${dateTomorrow}T150000
DTEND;TZID=Europe/Moscow:${dateTomorrow}T163000
END:VEVENT
BEGIN:VEVENT
UID:planovo_41_event_8
SUMMARY:Вводный инструктаж и разминка
DESCRIPTION:Преподаватель: Смирнов Максим Игоревич
LOCATION:Зал 1
STATUS:CONFIRMED
DTSTART;TZID=Europe/Moscow:${dateYesterday}T100000
DTEND;TZID=Europe/Moscow:${dateYesterday}T113000
END:VEVENT
END:VCALENDAR
""".trimIndent()
    }
}
