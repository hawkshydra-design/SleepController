package com.sleepcontroller.domain.usecase

import android.content.Context
import com.sleepcontroller.service.SleepSessionManager
import javax.inject.Inject

/**
 * Ends a sleep session: completes morning tasks, records history,
 * updates streak, stops foreground service, and resets session state.
 */
class EndSleepSessionUseCase @Inject constructor(
    private val sessionManager: SleepSessionManager
) {
    suspend operator fun invoke(context: Context, tasksCompleted: Int, totalTasks: Int) {
        sessionManager.endSession(context, tasksCompleted, totalTasks)
    }
}
