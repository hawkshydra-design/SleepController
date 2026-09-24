package com.sleepcontroller.data.repository

import com.sleepcontroller.data.db.dao.SleepHistoryDao
import com.sleepcontroller.data.db.entity.SleepHistory
import com.sleepcontroller.domain.repository.IHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for sleep history data.
 * Extracted from ScheduleRepository to follow single responsibility.
 */
@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: SleepHistoryDao
) : IHistoryRepository {
    override fun getRecentHistory(limit: Int): Flow<List<SleepHistory>> =
        historyDao.getRecentHistory(limit)

    override fun getAllHistory(): Flow<List<SleepHistory>> =
        historyDao.getAllHistory()

    override fun getHistoryBetween(start: Long, end: Long): Flow<List<SleepHistory>> =
        historyDao.getHistoryBetween(start, end)

    override suspend fun getHistoryForDate(date: Long): SleepHistory? =
        historyDao.getHistoryForDate(date)

    override suspend fun saveHistory(history: SleepHistory): Long =
        historyDao.insert(history)

    override suspend fun updateHistory(history: SleepHistory) =
        historyDao.update(history)

    override suspend fun getAverageSleepDuration(): Float? =
        historyDao.getAverageSleepDuration()

    override suspend fun getCurrentStreak(): Int =
        historyDao.getCurrentStreak()
}
