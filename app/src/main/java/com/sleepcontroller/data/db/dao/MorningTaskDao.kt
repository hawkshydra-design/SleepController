package com.sleepcontroller.data.db.dao

import androidx.room.*
import com.sleepcontroller.data.db.entity.MorningTask
import kotlinx.coroutines.flow.Flow

@Dao
interface MorningTaskDao {

    @Query("SELECT * FROM morning_tasks WHERE isEnabled = 1 ORDER BY sortOrder ASC")
    fun getEnabledTasks(): Flow<List<MorningTask>>

    @Query("SELECT * FROM morning_tasks ORDER BY sortOrder ASC")
    fun getAllTasks(): Flow<List<MorningTask>>

    @Query("SELECT * FROM morning_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): MorningTask?

    @Query("SELECT COUNT(*) FROM morning_tasks WHERE isEnabled = 1")
    suspend fun getEnabledTaskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: MorningTask): Long

    @Update
    suspend fun update(task: MorningTask)

    @Delete
    suspend fun delete(task: MorningTask)

    @Query("DELETE FROM morning_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
