package com.sleepcontroller.domain.repository

import com.sleepcontroller.data.db.entity.MorningTask
import kotlinx.coroutines.flow.Flow

/**
 * Contract for morning task persistence.
 */
interface ITaskRepository {
    fun getEnabledTasks(): Flow<List<MorningTask>>
    fun getAllTasks(): Flow<List<MorningTask>>
    suspend fun getTaskById(id: Long): MorningTask?
    suspend fun getEnabledTaskCount(): Int
    suspend fun addTask(task: MorningTask): Long
    suspend fun updateTask(task: MorningTask)
    suspend fun deleteTask(task: MorningTask)
    suspend fun deleteTaskById(id: Long)
}
