package com.sleepcontroller.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sleepcontroller.service.NotificationHelper

/**
 * Receives wind-down alarm trigger and shows a "bedtime approaching" notification.
 * Fired N minutes before the scheduled bedtime.
 */
class WindDownReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WindDownReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val minutesLeft = intent.getIntExtra("minutes_left", 30)
        Log.d(TAG, "Wind-down triggered: $minutesLeft minutes until bedtime")
        NotificationHelper.showWindDownNotification(context, minutesLeft)
    }
}
