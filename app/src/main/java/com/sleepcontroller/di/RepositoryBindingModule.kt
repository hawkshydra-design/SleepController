package com.sleepcontroller.di

import com.sleepcontroller.data.repository.HistoryRepository
import com.sleepcontroller.data.repository.ScheduleRepository
import com.sleepcontroller.data.repository.SessionRepository
import com.sleepcontroller.data.repository.TaskRepository
import com.sleepcontroller.domain.repository.IHistoryRepository
import com.sleepcontroller.domain.repository.IScheduleRepository
import com.sleepcontroller.domain.repository.ISessionRepository
import com.sleepcontroller.domain.repository.ITaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds repository implementations to their interfaces.
 * This allows ViewModels and use cases to depend on abstractions
 * rather than concrete implementations — enabling easy mock injection in tests.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepository): IScheduleRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepository): ISessionRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepository): IHistoryRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepository): ITaskRepository
}
