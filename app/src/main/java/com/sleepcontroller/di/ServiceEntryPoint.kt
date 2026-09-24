package com.sleepcontroller.di

import com.sleepcontroller.data.db.AppDatabase
import com.sleepcontroller.data.preferences.SleepPreferences
import com.sleepcontroller.data.repository.AppBlockRepository
import com.sleepcontroller.data.repository.HistoryRepository
import com.sleepcontroller.data.repository.ScheduleRepository
import com.sleepcontroller.data.repository.SessionRepository
import com.sleepcontroller.service.SleepSessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for components that cannot use @AndroidEntryPoint directly.
 *
 * Used by:
 *  - AppBlockerAccessibilityService (extends AccessibilityService)
 *  - OverlayBlockerService (extends Service with custom lifecycle)
 *  - SleepScheduleService
 *  - AlarmService
 *  - BroadcastReceivers
 *
 * This ensures all components share the same Hilt-managed singletons,
 * eliminating the state desync caused by manual instantiation.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceEntryPoint {
    fun sleepPreferences(): SleepPreferences
    fun sessionRepository(): SessionRepository
    fun scheduleRepository(): ScheduleRepository
    fun historyRepository(): HistoryRepository
    fun appBlockRepository(): AppBlockRepository
    fun sessionManager(): SleepSessionManager
    fun appDatabase(): AppDatabase
}
