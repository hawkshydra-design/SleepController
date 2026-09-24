package com.sleepcontroller.domain.usecase

import android.content.Context
import com.sleepcontroller.service.SleepSessionManager
import javax.inject.Inject

/**
 * Uses the one-time emergency unlock for the current sleep session.
 * Returns true if unlock was available and successfully applied.
 */
class UseEmergencyUnlockUseCase @Inject constructor(
    private val sessionManager: SleepSessionManager
) {
    suspend operator fun invoke(context: Context) {
        sessionManager.useEmergencyUnlock(context)
    }
}
