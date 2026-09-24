package com.sleepcontroller.data.repository

import com.sleepcontroller.data.db.dao.MorningTaskDao
import com.sleepcontroller.data.db.entity.MorningTask
import com.sleepcontroller.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: MorningTaskDao
) : ITaskRepository {
    override fun getEnabledTasks(): Flow<List<MorningTask>> = taskDao.getEnabledTasks()

    override fun getAllTasks(): Flow<List<MorningTask>> = taskDao.getAllTasks()

    override suspend fun getTaskById(id: Long): MorningTask? = taskDao.getTaskById(id)

    override suspend fun getEnabledTaskCount(): Int = taskDao.getEnabledTaskCount()

    override suspend fun addTask(task: MorningTask): Long = taskDao.insert(task)

    override suspend fun updateTask(task: MorningTask) = taskDao.update(task)

    override suspend fun deleteTask(task: MorningTask) = taskDao.delete(task)

    override suspend fun deleteTaskById(id: Long) = taskDao.deleteById(id)
}
