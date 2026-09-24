package com.sleepcontroller.data.db.dao

import androidx.room.*
import com.sleepcontroller.data.db.entity.SleepSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepScheduleDao {

    @Query("SELECT * FROM sleep_schedules WHERE isActive = 1 LIMIT 1")
    fun getActiveSchedule(): Flow<SleepSchedule?>

    @Query("SELECT * FROM sleep_schedules WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveScheduleOnce(): SleepSchedule?

    @Query("SELECT * FROM sleep_schedules ORDER BY createdAt DESC")
    fun getAllSchedules(): Flow<List<SleepSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: SleepSchedule): Long

    @Update
    suspend fun update(schedule: SleepSchedule)

    @Delete
    suspend fun delete(schedule: SleepSchedule)

    @Query("UPDATE sleep_schedules SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE sleep_schedules SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)
}
