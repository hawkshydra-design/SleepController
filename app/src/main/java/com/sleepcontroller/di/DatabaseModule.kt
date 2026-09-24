package com.sleepcontroller.di

import android.content.Context
import com.sleepcontroller.data.db.AppDatabase
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
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideSleepScheduleDao(db: AppDatabase): SleepScheduleDao = db.sleepScheduleDao()

    @Provides
    fun provideMorningTaskDao(db: AppDatabase): MorningTaskDao = db.morningTaskDao()

    @Provides
    fun provideBlockedAppDao(db: AppDatabase): BlockedAppDao = db.blockedAppDao()

    @Provides
    fun provideSleepHistoryDao(db: AppDatabase): SleepHistoryDao = db.sleepHistoryDao()
}
