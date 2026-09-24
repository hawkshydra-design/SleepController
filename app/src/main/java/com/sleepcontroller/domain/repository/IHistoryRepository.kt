package com.sleepcontroller.domain.repository

import com.sleepcontroller.data.db.entity.SleepHistory
import kotlinx.coroutines.flow.Flow

/**
 * Contract for sleep history persistence.
 */
interface IHistoryRepository {
    fun getRecentHistory(limit: Int = 30): Flow<List<SleepHistory>>
    fun getAllHistory(): Flow<List<SleepHistory>>
    fun getHistoryBetween(start: Long, end: Long): Flow<List<SleepHistory>>
    suspend fun getHistoryForDate(date: Long): SleepHistory?
    suspend fun saveHistory(history: SleepHistory): Long
    suspend fun updateHistory(history: SleepHistory)
    suspend fun getAverageSleepDuration(): Float?
    suspend fun getCurrentStreak(): Int
}
