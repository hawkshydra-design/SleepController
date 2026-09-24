package com.sleepcontroller.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sleepcontroller.di.ServiceEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires after a 20-minute temporary emergency unlock expires.
 * Re-activates sleep mode so app blocking resumes automatically.
 *
 * Scheduled by [AlarmUtils.scheduleEmergencyExpiry] when the user
 * triggers emergency unlock (uses 1-4 of 5).
 *
 * On the 5th (final) unlock, this receiver is NOT scheduled because
 * sleep mode is fully disabled for the rest of the night.
 */
class EmergencyExpiryReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "EmergencyExpiry"
        const val ACTION_EMERGENCY_EXPIRED = "com.sleepcontroller.EMERGENCY_EXPIRED"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_EMERGENCY_EXPIRED) return

        Log.d(TAG, "Emergency unlock expired — returning to sleep mode")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext, ServiceEntryPoint::class.java
                )
                val sessionManager = entryPoint.sessionManager()

                // Return to sleep mode — blocking resumes
                sessionManager.startSleepMode(context)

                Log.d(TAG, "Sleep mode re-activated after emergency unlock expiry")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-activate sleep mode", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
