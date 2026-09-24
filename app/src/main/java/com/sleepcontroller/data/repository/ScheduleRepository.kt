package com.sleepcontroller.data.repository

import com.sleepcontroller.data.db.dao.SleepScheduleDao
import com.sleepcontroller.data.db.entity.SleepSchedule
import com.sleepcontroller.domain.repository.IScheduleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for sleep schedule CRUD operations.
 *
 * Cleaned up: session state and history operations have been extracted into
 * [SessionRepository] and [HistoryRepository] respectively.
 */
@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: SleepScheduleDao
) : IScheduleRepository {
    override fun getActiveSchedule(): Flow<SleepSchedule?> = scheduleDao.getActiveSchedule()

    override suspend fun getActiveScheduleOnce(): SleepSchedule? = scheduleDao.getActiveScheduleOnce()

    override fun getAllSchedules(): Flow<List<SleepSchedule>> = scheduleDao.getAllSchedules()

    override suspend fun saveSchedule(schedule: SleepSchedule): Long {
        scheduleDao.deactivateAll()
        return scheduleDao.insert(schedule.copy(isActive = true, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun updateSchedule(schedule: SleepSchedule) {
        scheduleDao.update(schedule.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteSchedule(schedule: SleepSchedule) = scheduleDao.delete(schedule)
}
