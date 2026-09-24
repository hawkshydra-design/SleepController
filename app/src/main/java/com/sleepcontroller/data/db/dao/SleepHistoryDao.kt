package com.sleepcontroller.data.db.dao

import androidx.room.*
import com.sleepcontroller.data.db.entity.SleepHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepHistoryDao {

    @Query("SELECT * FROM sleep_history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<SleepHistory>>

    @Query("SELECT * FROM sleep_history ORDER BY date DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<SleepHistory>>

    @Query("SELECT * FROM sleep_history WHERE date = :date LIMIT 1")
    suspend fun getHistoryForDate(date: Long): SleepHistory?

    @Query("SELECT * FROM sleep_history WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getHistoryBetween(startDate: Long, endDate: Long): Flow<List<SleepHistory>>

    @Query("SELECT COUNT(*) FROM sleep_history WHERE sleepDurationMinutes IS NOT NULL")
    suspend fun getTotalSleepNights(): Int

    @Query("""
        SELECT COUNT(*) FROM (
            SELECT date FROM sleep_history 
            WHERE sleepDurationMinutes IS NOT NULL 
            ORDER BY date DESC
        )
    """)
    suspend fun getCurrentStreak(): Int

    @Query("SELECT AVG(sleepDurationMinutes) FROM sleep_history WHERE sleepDurationMinutes IS NOT NULL")
    suspend fun getAverageSleepDuration(): Float?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SleepHistory): Long

    @Update
    suspend fun update(history: SleepHistory)

    @Delete
    suspend fun delete(history: SleepHistory)
}
