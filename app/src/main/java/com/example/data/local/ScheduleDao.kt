package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule_events ORDER BY startTimeMillis ASC")
    fun getAllEvents(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedule_events WHERE startTimeMillis >= :startMillis AND startTimeMillis < :endMillis ORDER BY startTimeMillis ASC")
    fun getEventsForRange(startMillis: Long, endMillis: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedule_events WHERE startTimeMillis >= :startMillis AND startTimeMillis < :endMillis ORDER BY startTimeMillis ASC")
    suspend fun getEventsForRangeList(startMillis: Long, endMillis: Long): List<ScheduleEntity>

    @Query("SELECT * FROM schedule_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): ScheduleEntity?

    @Query("SELECT * FROM schedule_events WHERE endTimeMillis > :currentTimeMillis ORDER BY startTimeMillis ASC LIMIT :limit")
    suspend fun getUpcomingEvents(currentTimeMillis: Long, limit: Int = 50): List<ScheduleEntity>

    @Query("SELECT * FROM schedule_events WHERE endTimeMillis > :currentTimeMillis AND startTimeMillis <= :maxFutureMillis ORDER BY startTimeMillis ASC LIMIT :limit")
    suspend fun getUpcomingEventsLimited(currentTimeMillis: Long, maxFutureMillis: Long, limit: Int = 50): List<ScheduleEntity>

    @Query("SELECT * FROM schedule_events")
    suspend fun getAllEventsList(): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<ScheduleEntity>)

    @Query("DELETE FROM schedule_events")
    suspend fun clearAllEvents()

    @Query("SELECT * FROM schedule_changes ORDER BY detectedAtMillis DESC")
    fun getAllChanges(): Flow<List<ChangeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChange(change: ChangeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChanges(changes: List<ChangeEntity>)

    @Query("DELETE FROM schedule_changes")
    suspend fun clearChanges()

    @Transaction
    suspend fun updateScheduleWithDiff(
        newEntities: List<ScheduleEntity>,
        newChanges: List<ChangeEntity>
    ) {
        clearAllEvents()
        insertEvents(newEntities)
        if (newChanges.isNotEmpty()) {
            insertChanges(newChanges)
        }
    }
}
