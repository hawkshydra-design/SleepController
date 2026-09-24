package com.sleepcontroller.di

import android.content.Context
import com.sleepcontroller.data.preferences.SleepPreferences
import com.sleepcontroller.data.repository.AppBlockRepository
import com.sleepcontroller.data.repository.HistoryRepository
import com.sleepcontroller.data.repository.ScheduleRepository
import com.sleepcontroller.data.repository.SessionRepository
import com.sleepcontroller.data.repository.TaskRepository
import com.sleepcontroller.data.db.dao.BlockedAppDao
import com.sleepcontroller.data.db.dao.MorningTaskDao
import com.sleepcontroller.data.db.dao.SleepHistoryDao
import com.sleepcontroller.data.db.dao.SleepScheduleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSleepPreferences(@ApplicationContext context: Context): SleepPreferences {
        return SleepPreferences(context)
    }

    @Provides
    @Singleton
    fun provideScheduleRepository(scheduleDao: SleepScheduleDao): ScheduleRepository {
        return ScheduleRepository(scheduleDao)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(preferences: SleepPreferences): SessionRepository {
        return SessionRepository(preferences)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(historyDao: SleepHistoryDao): HistoryRepository {
        return HistoryRepository(historyDao)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: MorningTaskDao): TaskRepository {
        return TaskRepository(taskDao)
    }

    @Provides
    @Singleton
    fun provideAppBlockRepository(
        blockedAppDao: BlockedAppDao,
        @ApplicationContext context: Context
    ): AppBlockRepository {
        return AppBlockRepository(blockedAppDao, context)
    }
}
