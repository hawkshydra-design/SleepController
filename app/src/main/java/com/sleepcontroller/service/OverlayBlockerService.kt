package com.sleepcontroller.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sleepcontroller.data.repository.SessionRepository
import com.sleepcontroller.di.ServiceEntryPoint
import com.sleepcontroller.ui.overlay.BlockerOverlayContent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map

/**
 * Shows a full-screen overlay when a blocked app is opened during sleep mode.
 * Renders Compose UI in a system overlay window (SYSTEM_ALERT_WINDOW).
 *
 * Features:
 *  - Full-screen blocker with calming "go to sleep" message
 *  - One-time emergency unlock button per sleep session
 *  - Animated star field background
 *  - Auto-dismisses when sleep mode ends
 *
 * Fixed: Uses Hilt EntryPoint for DI, removed runBlocking ANR.
 */
class OverlayBlockerService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "OverlayBlocker"
        const val ACTION_DISMISS = "com.sleepcontroller.ACTION_OVERLAY_DISMISS"
        const val ACTION_EMERGENCY_UNLOCK = "com.sleepcontroller.ACTION_EMERGENCY_UNLOCK"
        var isShowing = false
            private set
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionManager: SleepSessionManager

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        // Use Hilt EntryPoint instead of manual instantiation
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext, ServiceEntryPoint::class.java
        )
        sessionRepository = entryPoint.sessionRepository()
        sessionManager = entryPoint.sessionManager()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        NotificationHelper.createAllChannels(this)
        startForeground(
            NotificationHelper.NOTIFICATION_ID_SLEEP,
            NotificationHelper.buildSleepActiveNotification(this)
        )
        Log.d(TAG, "Overlay blocker service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                removeOverlay()
                return START_NOT_STICKY
            }
            ACTION_EMERGENCY_UNLOCK -> {
                serviceScope.launch {
                    handleEmergencyUnlock()
                }
                return START_NOT_STICKY
            }
        }

        val blockedPackage = intent?.getStringExtra("blocked_package") ?: "Unknown"
        Log.d(TAG, "Showing overlay for: $blockedPackage")

        if (!isShowing) {
            showOverlay(blockedPackage)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlay()
        serviceScope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
        Log.d(TAG, "Overlay blocker service destroyed")
    }

    private fun showOverlay(blockedPackage: String) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        // Create reactive flows for emergency unlock state
        val unlockAvailableFlow = sessionRepository.emergencyUnlockCount.map { count ->
            count < com.sleepcontroller.data.repository.SessionRepository.MAX_EMERGENCY_UNLOCKS
        }
        val unlockCountFlow = sessionRepository.emergencyUnlockCount

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayBlockerService)
            setViewTreeSavedStateRegistryOwner(this@OverlayBlockerService)

            setContent {
                val isUnlockAvailable by unlockAvailableFlow.collectAsState(initial = true)
                val unlockCount by unlockCountFlow.collectAsState(initial = 0)
                val remaining = com.sleepcontroller.data.repository.SessionRepository.MAX_EMERGENCY_UNLOCKS - unlockCount

                BlockerOverlayContent(
                    blockedAppName = blockedPackage,
                    onDismiss = {
                        // Send user back home
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(homeIntent)
                        removeOverlay()
                    },
                    onEmergencyUnlock = {
                        serviceScope.launch { handleEmergencyUnlock() }
                    },
                    onVerifyPin = { pin, callback ->
                        serviceScope.launch {
                            val entryPoint = EntryPointAccessors.fromApplication(
                                applicationContext, ServiceEntryPoint::class.java
                            )
                            val prefs = entryPoint.sleepPreferences()
                            val isValid = prefs.verifyPin(pin)
                            callback(isValid)
                        }
                    },
                    isEmergencyUnlockAvailable = { isUnlockAvailable },
                    remainingUnlocks = remaining
                )
            }
        }

        try {
            windowManager?.addView(overlayView, params)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            isShowing = true
            Log.d(TAG, "Overlay displayed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    private fun removeOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
            isShowing = false
            Log.d(TAG, "Overlay removed")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay", e)
        }
    }

    /**
     * Delegate emergency unlock to the SessionManager — the single source of truth.
     * Previously this method duplicated the unlock logic from SessionManager.
     */
    private suspend fun handleEmergencyUnlock() {
        val success = sessionManager.useEmergencyUnlock(this)
        if (!success) {
            Log.d(TAG, "Emergency unlock already used this session")
            return
        }

        removeOverlay()
        stopSelf()
        Log.d(TAG, "Emergency unlock activated — sleep mode disabled")
    }
}
