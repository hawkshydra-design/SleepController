package com.sleepcontroller.domain.usecase

import android.content.Context
import com.sleepcontroller.data.repository.ScheduleRepository
import com.sleepcontroller.service.SleepSessionManager
import javax.inject.Inject

/**
 * Starts a sleep session: activates sleep mode, starts foreground service,
 * and blocks apps. Used by both manual toggle and scheduled alarm trigger.
 */
class StartSleepSessionUseCase @Inject constructor(
    private val sessionManager: SleepSessionManager
) {
    suspend operator fun invoke(context: Context) {
        sessionManager.startSleepMode(context)
    }
}
