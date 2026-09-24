package com.sleepcontroller.domain.repository

import com.sleepcontroller.data.db.entity.SleepSchedule
import kotlinx.coroutines.flow.Flow

/**
 * Contract for schedule persistence operations.
 */
interface IScheduleRepository {
    fun getActiveSchedule(): Flow<SleepSchedule?>
    suspend fun getActiveScheduleOnce(): SleepSchedule?
    fun getAllSchedules(): Flow<List<SleepSchedule>>
    suspend fun saveSchedule(schedule: SleepSchedule): Long
    suspend fun updateSchedule(schedule: SleepSchedule)
    suspend fun deleteSchedule(schedule: SleepSchedule)
}
