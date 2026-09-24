package com.sleepcontroller.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.sleepcontroller.data.repository.SessionRepository
import com.sleepcontroller.di.ServiceEntryPoint
import com.sleepcontroller.domain.model.SessionState
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Core accessibility service that detects when blocked apps are launched
 * during sleep mode OR morning mode, and triggers the overlay blocker.
 *
 * Flow: User opens blocked app → AccessibilityService detects WINDOW_STATE_CHANGED
 * → checks if package is in blocked list and sleep/morning mode is active
 * → sends user back (GLOBAL_ACTION_BACK) + launches OverlayBlockerService
 *
 * Uses Hilt EntryPoint for proper DI instead of manual instantiation.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppBlockerA11y"
        private const val DEBOUNCE_MS = 500L
        var isRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sessionRepository: SessionRepository
    private var blockedPackages: Set<String> = emptySet()
    private var lastBlockedTime = 0L
    private var lastBlockedPackage = ""

    // Packages that should NEVER be blocked (phone, settings, this app, etc.)
    private val systemExempt = setOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",           // Samsung launcher
        "com.miui.home",                           // Xiaomi launcher
        "com.huawei.android.launcher",             // Huawei launcher
        "com.sleepcontroller"                       // This app
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d(TAG, "Accessibility service connected")

        // Use Hilt EntryPoint instead of manual instantiation
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext, ServiceEntryPoint::class.java
        )
        sessionRepository = entryPoint.sessionRepository()
        val database = entryPoint.appDatabase()

        // Configure service to listen for window changes across all packages
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // SECURITY: Minimum flags only — no FLAG_REPORT_VIEW_IDS, no FLAG_REQUEST_FILTER_KEY_EVENTS
            // This prevents the service from receiving UI element IDs or key events
        }

        // Load blocked packages initially
        refreshBlockedApps(entryPoint)

        // Observe blocked app list for live updates
        serviceScope.launch {
            database.blockedAppDao().getBlockedApps().collect { apps ->
                blockedPackages = apps.map { it.packageName }.toSet()
                Log.d(TAG, "Blocked list updated: ${blockedPackages.size} apps")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        // SECURITY: Extract ONLY the package name — never read text, className,
        // contentDescription, or any UI content from the event
        val packageName = event.packageName?.toString() ?: return

        // Skip system UI and launchers
        if (packageName in systemExempt) return

        // SECURITY: ONLY process apps that are in the user's blocked list.
        // This means we NEVER process events from:
        //   - Payment/banking apps (Google Pay, Paytm, PhonePe, SBI, etc.)
        //   - Health apps, email apps, messaging apps
        //   - ANY app the user hasn't explicitly chosen to block
        // This protects ALL sensitive/personal data automatically.
        if (packageName !in blockedPackages) return

        // Debounce — avoid rapid-fire blocking on the same package
        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && (now - lastBlockedTime) < DEBOUNCE_MS) return

        serviceScope.launch {
            try {
                // Use SessionState sealed class for proper state checking
                val state = sessionRepository.getSessionStateOnce()

                // Only block during Sleep or Morning mode
                if (!state.isBlocking) return@launch

                lastBlockedTime = now
                lastBlockedPackage = packageName
                val modeName = when (state) {
                    is SessionState.Sleep -> "sleep"
                    is SessionState.Morning -> "morning"
                    else -> "unknown"
                }
                Log.d(TAG, "Blocked app detected: $packageName (mode=$modeName)")

                // Navigate back to prevent the app from loading
                performGlobalAction(GLOBAL_ACTION_BACK)

                // Show the blocker overlay
                showBlockerOverlay(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing event", e)
            }
        }
    }

    private fun showBlockerOverlay(packageName: String) {
        if (OverlayBlockerService.isShowing) return // Already showing

        val intent = Intent(this, OverlayBlockerService::class.java).apply {
            putExtra("blocked_package", packageName)
        }
        startForegroundService(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }

    /**
     * Refresh blocked packages list from Hilt-provided database.
     */
    private fun refreshBlockedApps(entryPoint: ServiceEntryPoint) {
        serviceScope.launch {
            try {
                blockedPackages = entryPoint.appDatabase().blockedAppDao()
                    .getBlockedPackageNames()
                    .toSet()
                Log.d(TAG, "Refreshed: ${blockedPackages.size} blocked packages")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing blocked packages", e)
            }
        }
    }
}
