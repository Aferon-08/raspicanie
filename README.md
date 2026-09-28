# КЭМС Расписание

Android-приложение для просмотра расписания занятий, отслеживания изменений и получения напоминаний о предстоящих занятиях.

Приложение получает расписание в формате **iCalendar (`.ics`)**, преобразует события в доменную модель, хранит актуальное расписание локально, сравнивает новые версии и показывает изменения пользователю. Интерфейс реализован на **Jetpack Compose + Material 3**.

> **Документация соответствует ветке `main` на 28 сентября 2026 года.**
>
> Сборка проекта при подготовке этого README не запускалась: README составлен по фактическому содержимому исходного кода и Gradle-конфигурации ветки `main`.

---

## Содержание

- [Возможности](#возможности)
- [Архитектура](#архитектура)
- [Поток данных](#поток-данных)
- [Структура проекта](#структура-проекта)
- [Модель занятия](#модель-занятия)
- [Получение и разбор iCalendar](#получение-и-разбор-icalendar)
- [Синхронизация и определение изменений](#синхронизация-и-определение-изменений)
- [Система групп](#система-групп)
- [Локальное хранение](#локальное-хранение)
- [Уведомления](#уведомления)
- [Пользовательский интерфейс](#пользовательский-интерфейс)
- [Производительность](#производительность)
- [Технологический стек](#технологический-стек)
- [Требования](#требования)
- [Сборка](#сборка)
- [Конфигурация секретов](#конфигурация-секретов)
- [Тестирование](#тестирование)
- [Где что менять](#где-что-менять)
- [Ограничения](#ограничения)
- [Git](#git)
- [Лицензия](#лицензия)

---

## Возможности

### Расписание

- просмотр занятий по дням;
- выбор нужного дня;
- отображение времени начала и окончания;
- определение текущего занятия;
- определение ближайшего занятия;
- визуальные состояния предстоящего, текущего и завершённого занятия;
- отображение преподавателя;
- отображение аудитории;
- отображение типа занятия;
- отображение изменений;
- отображение отменённых занятий;
- поддержка нескольких параллельных подгрупп;
- подробный просмотр занятия;
- поддержка онлайн-занятий и извлечение Zoom-данных.

### Синхронизация

- загрузка календаря по HTTP(S);
- поддержка собственного URL календаря;
- локальное кеширование;
- сравнение старой и новой версии расписания;
- определение изменения времени;
- определение изменения аудитории;
- определение изменения данных занятия;
- определение отмены;
- определение восстановления;
- определение новых занятий;
- сохранение истории изменений.

### Группы

Пользователь может работать с несколькими сохранёнными группами:

- добавить группу;
- указать её название;
- указать URL календаря;
- переключить активную группу;
- изменить существующую группу;
- удалить группу;
- хранить активную группу между запусками.

Для обратной совместимости текущая система сохранённых групп синхронизируется со старыми настройками `groupId`, `groupTitle` и `customUrl`.

### Уведомления

- напоминание о предстоящем занятии;
- настраиваемое количество минут до начала;
- `AlarmManager`;
- exact alarm при наличии возможности;
- fallback на обычный alarm;
- восстановление расписания уведомлений после перезагрузки;
- переход из уведомления непосредственно к выбранному занятию.

### Дополнительные разделы

В приложении присутствуют:

- профиль и настройки;
- заметки;
- раздел пропусков;
- журнал изменений расписания.

### Интерфейс

- Jetpack Compose;
- Material 3;
- светлая тема;
- тёмная тема;
- системная тема;
- dynamic color;
- адаптивная навигация;
- spring-анимации;
- выразительные карточки занятий;
- анимированная нижняя панель;
- волнистый progress;
- анимированные шестерёнки текущего занятия;
- отдельный bottom sheet с подробностями занятия.

---

# Архитектура

Проект разделён на несколько логических уровней.

```text
┌──────────────────────────────────────────────┐
│                  UI / Compose                │
│ ScheduleScreen / Profile / Notes / Passes   │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│             ScheduleViewModel                │
│       state + actions + lifecycle             │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────┐
│             ScheduleRepository               │
│ groups / sync / settings / database / alarms│
└──────────────┬───────────────┬───────────────┘
               │               │
       ┌───────▼──────┐ ┌──────▼────────────┐
       │ Remote layer │ │   Local layer     │
       │ OkHttp + ICS  │ │ Room + preferences│
       └───────┬──────┘ └──────┬────────────┘
               │               │
               ▼               ▼
        Calendar .ics      cached schedule
               │
               ▼
          ScheduleDiff
               │
               ▼
        change history
```

### Точка входа приложения

`PlanovoApp` — Android `Application`.

Он создаёт основные объекты приложения, в том числе `ScheduleRepository`.

`MainActivity` запускает Compose-интерфейс.

### ViewModel

`ScheduleViewModel` связывает UI и репозиторий.

Через ViewModel проходят:

- состояние расписания;
- состояние загрузки;
- ошибки;
- выбранный день;
- активная группа;
- настройки;
- действия пользователя;
- обновление расписания.

Для Flow используется lifecycle-aware сбор состояния.

### Repository

`ScheduleRepository` является центральной точкой доступа к данным.

Он отвечает за:

- текущую группу;
- список групп;
- настройки;
- URL календаря;
- синхронизацию;
- Room;
- журнал изменений;
- заметки;
- пропуски;
- уведомления;
- формат времени;
- тему.

---

# Поток данных

Полный цикл обновления расписания выглядит примерно так:

```text
URL группы
   │
   ▼
CalendarRemoteDataSource
   │
   │ HTTP
   ▼
iCalendar text
   │
   ▼
IcsParser
   │
   │ List<ClassEvent>
   ▼
ScheduleRepository
   │
   ├───────────────┐
   │               │
   ▼               ▼
старое состояние   новое состояние
   │               │
   └───────┬───────┘
           ▼
      ScheduleDiff
           │
      ┌────┴─────┐
      ▼          ▼
  изменения   новые события
      │          │
      └────┬─────┘
           ▼
    Room transaction
           │
           ├── ScheduleEntity
           └── ChangeEntity
           │
           ▼
    ScheduleViewModel
           │
           ▼
     Jetpack Compose
```

Отдельно после синхронизации планируются уведомления для будущих занятий.

---

# Структура проекта

Ключевая структура исходников:

```text
raspicanie/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt
│       │   │   ├── PlanovoApp.kt
│       │   │   │
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── AppDatabase.kt
│       │   │   │   │   ├── Entities.kt
│       │   │   │   │   └── ScheduleDao.kt
│       │   │   │   ├── model/
│       │   │   │   │   └── ClassEvent.kt
│       │   │   │   ├── remote/
│       │   │   │   │   ├── CalendarRemoteDataSource.kt
│       │   │   │   │   └── IcsParser.kt
│       │   │   │   └── repository/
│       │   │   │       ├── SavedGroup.kt
│       │   │   │       ├── ScheduleDiff.kt
│       │   │   │       └── ScheduleRepository.kt
│       │   │   │
│       │   │   ├── notifications/
│       │   │   │   ├── NotificationHelper.kt
│       │   │   │   └── NotificationScheduler.kt
│       │   │   │
│       │   │   ├── receiver/
│       │   │   │   ├── AlarmReceiver.kt
│       │   │   │   └── BootReceiver.kt
│       │   │   │
│       │   │   ├── ui/
│       │   │   │   ├── components/
│       │   │   │   │   ├── ClassCard.kt
│       │   │   │   │   ├── ClassDetailDialog.kt
│       │   │   │   │   ├── DaySelectorStrip.kt
│       │   │   │   │   ├── ExpressiveBottomBar.kt
│       │   │   │   │   ├── SquigglyProgressBar.kt
│       │   │   │   │   └── SubgroupClassCard.kt
│       │   │   │   ├── screens/
│       │   │   │   │   ├── ScheduleScreen.kt
│       │   │   │   │   ├── ScheduleViewModel.kt
│       │   │   │   │   ├── ProfileScreen.kt
│       │   │   │   │   ├── NotesScreen.kt
│       │   │   │   │   └── PassesScreen.kt
│       │   │   │   └── theme/
│       │   │   │       ├── Color.kt
│       │   │   │       ├── Theme.kt
│       │   │   │       └── Type.kt
│       │   │   │
│       │   │   └── util/
│       │   │       └── ScheduleTimeFormatter.kt
│       │   │
│       │   ├── res/
│       │   │   ├── drawable/
│       │   │   ├── mipmap-*/
│       │   │   ├── values/
│       │   │   ├── values-v31/
│       │   │   └── xml/
│       │   │
│       │   └── ...
│       │
│       ├── test/
│       │   └── java/com/example/
│       │       ├── ExampleUnitTest.kt
│       │       ├── ExampleRobolectricTest.kt
│       │       └── ScheduleDiffTest.kt
│       │
│       └── androidTest/
│           └── java/com/example/
│               └── ExampleInstrumentedTest.kt
│
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── .env.example
├── build.gradle.kts
├── gradle.properties
├── metadata.json
└── settings.gradle.kts
```

---

# Модель занятия

Главная доменная модель — `ClassEvent`.

Она содержит:

| Поле | Назначение |
|---|---|
| `id` | идентификатор события |
| `title` | название занятия |
| `description` | описание |
| `teacher` | преподаватель |
| `location` | аудитория/место |
| `lessonType` | тип занятия |
| `startTimeMillis` | начало |
| `endTimeMillis` | окончание |
| `isCancelled` | признак отмены |
| `hasChanges` | наличие изменений |
| `changeDetails` | описание изменений |
| `rawSummary` | исходный SUMMARY |
| `subgroups` | список подгрупп |

### Статусы

`ClassStatus` содержит:

```text
ONGOING
UPCOMING_SOON
SCHEDULED
COMPLETED
CANCELLED
```

Занятие считается `UPCOMING_SOON`, если до его начала осталось не более двух часов.

### Дополнительные вычисления

`ClassEvent` умеет вычислять:

- продолжительность;
- процент выполнения;
- оставшееся время;
- время до начала;
- отображаемый тип;
- признак онлайн-занятия;
- Zoom URL;
- Zoom ID;
- пароль онлайн-конференции.

### Типы изменений

`ChangeType`:

```text
TIME_CHANGED
LOCATION_CHANGED
DETAILS_CHANGED
CANCELLED
NEW_CLASS
RESTORED
```

---

# Получение и разбор iCalendar

## Источник

Для стандартной группы используется URL:

```text
https://planovo.pro/api/v1/public/groups/{groupId}/calendar.ics
```

Также пользователь может сохранить собственный URL.

## HTTP

`CalendarRemoteDataSource` использует OkHttp.

Настроены:

- timeout подключения — 15 секунд;
- timeout чтения — 20 секунд;
- redirects;
- User-Agent;
- Accept для iCalendar/text/plain.

Перед разбором проверяется HTTP-ответ и наличие `BEGIN:VCALENDAR`.

## Поддерживаемые поля

`IcsParser` обрабатывает:

- `UID`;
- `SUMMARY`;
- `DESCRIPTION`;
- `LOCATION`;
- `URL`;
- `STATUS`;
- `DTSTART`;
- `DTEND`;
- `TZID`;
- UTC-время через `Z`;
- date-only значения;
- folded lines;
- iCalendar escape-последовательности.

## Временные зоны

Парсер учитывает:

1. `TZID`, указанный в событии;
2. UTC;
3. timezone календаря;
4. timezone устройства как fallback.

## Преподаватель

Преподаватель может быть извлечён из разных вариантов внешнего календаря.

Например:

```text
Математика (Иванов И.И.)
```

может стать:

```text
title   = Математика
teacher = Иванов И.И.
```

Также поддерживается формат:

```text
Математика - Иванов И.И.
```

И поиск полей вида:

```text
Преподаватель: Иванов И.И.
Педагог: Иванов И.И.
Тренер: Иванов И.И.
Учитель: Иванов И.И.
```

## Отмена

Событие может определяться как отменённое через `STATUS:CANCELLED`, а также по признакам отмены в данных события.

## Онлайн-занятия

Для онлайн-занятий из текста события извлекаются:

- URL;
- Zoom-ссылка;
- meeting ID;
- пароль/passcode.

---

# Синхронизация и определение изменений

Для сравнения расписаний используется `ScheduleDiff`.

На вход поступают:

- старые `ScheduleEntity`;
- новый список `ClassEvent`;
- текущее время.

На выходе:

```text
ScheduleDiffResult
├── events
├── changes
└── removedEventIds
```

### Новое занятие

Если событие отсутствовало в старой версии, но появилось в новой и подходит под условия отслеживания изменений, создаётся:

```text
ChangeType.NEW_CLASS
```

### Изменение времени

Сравниваются:

- начало;
- окончание.

При изменении создаётся `TIME_CHANGED`.

В детали попадает старое и новое время.

### Изменение аудитории

Сравнивается `location`.

Результат:

```text
ChangeType.LOCATION_CHANGED
```

### Отмена и восстановление

При переходе:

```false → true
```

создаётся:

```text
CANCELLED
```

При переходе:

```true → false
```

создаётся:

```text
RESTORED
```

### Другие изменения

Сравниваются:

- название;
- преподаватель;
- описание;
- исходный SUMMARY.

При отличиях создаётся `DETAILS_CHANGED`.

### Атомарное обновление

Репозиторий обновляет расписание и журнал изменений через транзакцию Room.

Концептуально:

```text
BEGIN TRANSACTION

очистить актуальное расписание
        ↓
записать новую версию
        ↓
записать обнаруженные изменения

COMMIT
```

Это позволяет не оставлять БД в состоянии, где часть расписания уже новая, а часть ещё старая.

---

# Система групп

Модель группы — `SavedGroup`.

Группа содержит:

- ID;
- название;
- URL;
- статистику;
- время обновления.

В `ScheduleRepository` реализованы:

```text
getSavedGroups()
getActiveGroup()
switchGroup(id)
addGroup(title, url)
updateSavedGroup(id, title, url)
deleteGroup(id)
```

### Группа по умолчанию

В репозитории определена стандартная группа:

```text
ID: 41
Название: 2423 УИР · 3 курс
```

Её календарь строится через:

```text
https://planovo.pro/api/v1/public/groups/41/calendar.ics
```

### Миграция старых настроек

Если список сохранённых групп ещё не существует, репозиторий создаёт его из старых параметров:

- `PREF_GROUP_ID`;
- `PREF_GROUP_TITLE`;
- `PREF_CUSTOM_URL`.

Это позволяет перейти от старой модели одной группы к текущей модели нескольких групп без потери настроек.

---

# Локальное хранение

Проект использует два основных механизма.

## Room

Room хранит структурированные данные:

- актуальное расписание;
- историю изменений.

Ключевые файлы:

```text
data/local/AppDatabase.kt
data/local/Entities.kt
data/local/ScheduleDao.kt
```

Основные сущности:

- `ScheduleEntity`;
- `ChangeEntity`.

## SharedPreferences

Небольшие пользовательские настройки хранятся в:

```text
planovo_schedule_prefs
```

Среди них:

| Ключ/настройка | Назначение |
|---|---|
| `PREF_GROUP_ID` | ID группы |
| `PREF_CUSTOM_URL` | собственный URL |
| `PREF_LEAD_TIME` | время напоминания |
| `PREF_IS_24_HOUR` | 24-часовой формат |
| `PREF_THEME_MODE` | тема |
| `PREF_DYNAMIC_COLOR` | dynamic color |
| `PREF_NOTES` | заметки |
| `PREF_MISSED_CLASSES` | пропуски |
| `PREF_GROUP_TITLE` | название группы |
| `PREF_SHOW_CANCELLED` | отображение отменённых |
| `PREF_DEBUG_ANIMATION` | debug-анимация |
| `PREF_SAVED_GROUPS` | список групп |
| `PREF_ACTIVE_GROUP_ID` | активная группа |
| `PREF_LAST_SYNCED_GROUP_ID` | последняя синхронизированная группа |

---

# Уведомления

Система уведомлений состоит из:

```text
NotificationHelper
NotificationScheduler
AlarmReceiver
BootReceiver
```

## Разрешения

Manifest содержит:

- `POST_NOTIFICATIONS`;
- `VIBRATE`;
- `RECEIVE_BOOT_COMPLETED`;
- `SCHEDULE_EXACT_ALARM`.

## Планирование

`NotificationScheduler` создаёт alarm для будущих занятий.

При этом:

- отменённые занятия не должны получать напоминания;
- прошедшие trigger time не планируются;
- учитывается установленное пользователем время предупреждения;
- существующие alarm'ы могут быть пересозданы.

При возможности используется:

```text
setExactAndAllowWhileIdle()
```

Если exact alarm недоступен, предусмотрен fallback на обычный alarm.

## После перезагрузки

`BootReceiver` получает:

```text
android.intent.action.BOOT_COMPLETED
```

и инициирует восстановление будущих уведомлений.

## Переход из уведомления

В intent может передаваться:

```text
selected_event_id
```

После открытия приложения это позволяет определить нужное занятие.

---

# Пользовательский интерфейс

## ScheduleScreen

Главный экран отвечает за:

- выбор дня;
- отображение списка;
- обновление;
- загрузку;
- ошибки;
- навигацию;
- группы;
- выбранное занятие.

## ClassCard

`ClassCard` — основной компонент занятия.

Карточка отображает:

- время;
- название;
- преподавателя;
- аудиторию;
- тип;
- статус;
- прогресс;
- изменения;
- отмену;
- интерактивное состояние.

В текущей реализации также присутствуют:

- spring-анимации;
- анимация нажатия;
- анимация шестерёнок;
- progress текущего занятия;
- expressive shapes.

## SubgroupClassCard

Используется для объединённых параллельных занятий.

Каждая подгруппа может иметь:

- номер;
- преподавателя;
- аудиторию.

Это позволяет представить несколько вариантов одного занятия единым визуальным блоком без потери информации.

## ClassDetailDialog

Подробности занятия отображаются в отдельном bottom sheet.

Он показывает расширенную информацию о выбранном событии и действия, связанные с ним.

## DaySelectorStrip

Отдельный компонент отвечает за выбор дня расписания.

## ExpressiveBottomBar

Нижняя навигация выполнена отдельным Compose-компонентом.

В текущей версии используется компактный icon-only вариант со spring-анимацией.

## SquigglyProgressBar

Используется для визуального отображения прогресса занятия в выразительном стиле.

## Темизация

Тема расположена в:

```text
ui/theme/Color.kt
ui/theme/Theme.kt
ui/theme/Type.kt
```

Поддерживаются:

- Light;
- Dark;
- System;
- Dynamic Color.

---

# Производительность

Проект содержит несколько оптимизаций, важных именно для Compose-интерфейса расписания.

### Ограничение частоты live-обновлений

Карточкам не требуется перерисовываться каждую секунду.

Вместо этого состояние времени обновляется с более редким интервалом, достаточным для отображения текущего прогресса.

### LazyColumn

Список занятий строится через lazy-контейнер, поэтому элементы за пределами видимой области не обязаны постоянно находиться в активном UI-дереве.

### Стабильные ключи

Для элементов расписания используются идентификаторы событий, что помогает Compose корректно сопоставлять элементы при изменении списка.

### Кеширование вычислений

Для подгрупп и производных UI-значений используются локальные вычисления/кеширование там, где это уменьшает повторную работу во время рекомпозиции.

### SharedPreferences

Статистика активной группы не перезаписывается без необходимости, если значение не изменилось.

### Анимации

Анимации карточек и навигации построены на Compose Animation API. При дальнейшем изменении UI важно не превращать часто обновляемое состояние времени в источник постоянной рекомпозиции большого списка.

---

# Технологический стек

| Технология | Использование |
|---|---|
| Kotlin | основной язык |
| Jetpack Compose | пользовательский интерфейс |
| Material 3 | дизайн-система |
| AndroidX Activity Compose | Activity + Compose |
| AndroidX Lifecycle | lifecycle-aware состояние |
| ViewModel | состояние экранов |
| Room | локальная БД |
| KSP | генерация кода Room/Moshi |
| OkHttp | HTTP-запросы |
| Retrofit | HTTP-инфраструктура проекта |
| Moshi | сериализация |
| Kotlin Coroutines | асинхронная работа |
| SharedPreferences | небольшие настройки |
| AlarmManager | уведомления |
| Firebase BOM | Firebase-зависимости |
| Firebase AI | AI-интеграция |
| Firebase App Check | Firebase App Check |
| Secrets Gradle Plugin | секреты |
| Robolectric | JVM Android-тесты |
| Gradle Version Catalog | управление версиями |

### Версии

Текущий `gradle/libs.versions.toml` содержит, в частности:

- Android Gradle Plugin — `9.1.1`;
- Kotlin — `2.2.10`;
- KSP — `2.3.5`;
- Compose BOM — `2025.10.01`;
- Room — `2.7.0`;
- Coroutines — `1.10.2`;
- Retrofit — `2.12.0`;
- Moshi — `1.15.2`;
- Firebase BOM — `34.17.0`;
- Robolectric — `4.16.1`;
- Secrets Gradle Plugin — `2.0.1`;
- Google Services Plugin — `4.5.0`.

---

# Требования

Для разработки необходимы:

- Android Studio с поддержкой используемой версии Android Gradle Plugin;
- JDK 11;
- Android SDK API 36;
- Android SDK Build Tools;
- доступ к интернету для загрузки Gradle-зависимостей и календаря.

Параметры приложения:

```text
applicationId = com.aistudio.planovoschedule.kxbqza

minSdk    = 24
targetSdk = 36
compileSdk = 36.1

versionCode = 1
versionName = 1.0
```

Gradle Wrapper использует:

```text
Gradle 9.3.1
```

---

# Сборка

## Клонирование

```bash
git clone https://github.com/Aferon-08/raspicanie.git
cd raspicanie
```

По умолчанию актуальная стабильная ветка проекта:

```text
main
```

### Debug

```bash
./gradlew assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/
```

### Установка на устройство

```bash
./gradlew installDebug
```

### Unit tests

```bash
./gradlew test
```

### Instrumentation tests

```bash
./gradlew connectedAndroidTest
```

Для последней команды требуется подключённое Android-устройство или эмулятор.

### Release

Release использует отдельную signing configuration.

Ожидаются переменные:

```text
KEYSTORE_PATH
STORE_PASSWORD
KEY_PASSWORD
```

Пример:

```bash
KEYSTORE_PATH=/path/to/my-upload-key.jks \
STORE_PASSWORD='...' \
KEY_PASSWORD='...' \
./gradlew assembleRelease
```

Реальные пароли и keystore не должны добавляться в Git.

---

# Конфигурация секретов

Проект подключает Secrets Gradle Plugin.

Файлы:

```text
.env
.env.example
```

В `.env.example` предусмотрен:

```text
GEMINI_API_KEY
```

Сейчас строка ключа оставлена закомментированной, чтобы placeholder не был ошибочно принят за настоящий секрет.

**Никогда не коммитьте реальные API-ключи, пароли или signing credentials.**

## Google Services

В проект подключён Google Services Gradle Plugin.

Конфигурация допускает отсутствие `google-services.json` и выдаёт предупреждение вместо немедленного падения Gradle-конфигурации.

Это позволяет работать с исходным проектом без обязательного помещения Firebase-конфига в репозиторий.

---

# Тестирование

В проекте есть два уровня тестов.

## JVM

Каталог:

```text
app/src/test/
```

Содержит:

- `ExampleUnitTest.kt`;
- `ExampleRobolectricTest.kt`;
- `ScheduleDiffTest.kt`.

Особенно важен `ScheduleDiffTest`: он покрывает логику определения изменений расписания.

Запуск:

```bash
./gradlew test
```

## Android instrumentation

Каталог:

```text
app/src/androidTest/
```

Содержит:

```text
ExampleInstrumentedTest.kt
```

Запуск:

```bash
./gradlew connectedAndroidTest
```

---

# Где что менять

| Задача | Файл/каталог |
|---|---|
| Главный экран | `ui/screens/ScheduleScreen.kt` |
| Состояние главного экрана | `ui/screens/ScheduleViewModel.kt` |
| Внешний вид карточки | `ui/components/ClassCard.kt` |
| Подгруппы | `ui/components/SubgroupClassCard.kt` |
| Подробности занятия | `ui/components/ClassDetailDialog.kt` |
| Выбор дня | `ui/components/DaySelectorStrip.kt` |
| Нижняя навигация | `ui/components/ExpressiveBottomBar.kt` |
| Прогресс | `ui/components/SquigglyProgressBar.kt` |
| Модель занятия | `data/model/ClassEvent.kt` |
| Загрузка календаря | `data/remote/CalendarRemoteDataSource.kt` |
| Парсинг `.ics` | `data/remote/IcsParser.kt` |
| Синхронизация | `data/repository/ScheduleRepository.kt` |
| Сравнение версий | `data/repository/ScheduleDiff.kt` |
| Группы | `data/repository/SavedGroup.kt` + `ScheduleRepository.kt` |
| Room | `data/local/` |
| Уведомления | `notifications/` |
| AlarmReceiver | `receiver/AlarmReceiver.kt` |
| Восстановление после boot | `receiver/BootReceiver.kt` |
| Цвета/тема | `ui/theme/` |
| Форматирование времени | `ui/util/ScheduleTimeFormatter.kt` |

---

# Ограничения

1. **README не заменяет проверку сборки.** При подготовке этой версии документации Gradle-сборка не запускалась.
2. **Парсер зависит от структуры внешнего iCalendar.** Нестандартное содержимое `.ics` может потребовать изменения эвристик извлечения преподавателя, типа занятия или подгрупп.
3. **Exact alarm зависит от Android и разрешений пользователя.** Приложение предусматривает fallback, но поведение планировщика определяется версией ОС и настройками устройства.
4. **Firebase-конфигурация зависит от окружения.** Исходный проект может работать без локального `google-services.json`, но соответствующие Firebase-возможности требуют корректной конфигурации.
5. **Release signing локальный.** Signing credentials намеренно не хранятся в репозитории.
6. **Анимации требуют осторожности при дальнейшей оптимизации.** Изменение состояния, участвующего одновременно в большом количестве Compose-компонентов, может повлиять на плавность интерфейса.
7. **Лицензия отдельно не заявлена.** До публичного распространения проекта стоит добавить выбранный LICENSE-файл.

---

# Git

Актуальная ветка для опубликованного состояния проекта:

```text
main
```

В репозитории также существовала рабочая ветка:

```text
Chat-gpt
```

На момент подготовки этого README текущий код `main` уже содержит изменения, ранее разрабатывавшиеся в `Chat-gpt`, поэтому документация ориентирована именно на **HEAD ветки `main`**, а не на историческое состояние до слияния.

---

# Лицензия

Отдельный файл лицензии в текущем репозитории не заявлен.

Если проект будет распространяться как open-source, рекомендуется добавить LICENSE с выбранными условиями использования.

---

# Полезные файлы

| Файл | Назначение |
|---|---|
| `app/build.gradle.kts` | конфигурация Android-модуля |
| `gradle/libs.versions.toml` | версии зависимостей |
| `gradle/wrapper/gradle-wrapper.properties` | версия Gradle Wrapper |
| `settings.gradle.kts` | конфигурация Gradle |
| `.env.example` | пример секретов |
| `AndroidManifest.xml` | permissions, Activity и receivers |
| `ScheduleRepository.kt` | центральная логика данных |
| `ScheduleDiff.kt` | сравнение расписаний |
| `IcsParser.kt` | iCalendar parser |
| `ClassEvent.kt` | доменная модель |
| `ScheduleDao.kt` | Room DAO |
| `ClassCard.kt` | карточка занятия |
| `ScheduleScreen.kt` | экран расписания |
| `ScheduleViewModel.kt` | состояние экрана |
| `NotificationScheduler.kt` | планирование уведомлений |

---

## Репозиторий

https://github.com/Aferon-08/raspicanie
