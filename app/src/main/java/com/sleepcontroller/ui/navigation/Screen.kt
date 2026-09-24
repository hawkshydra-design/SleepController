package com.sleepcontroller.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlin Serialization.
 * Replaces the old string-based `Screen(route = "...")` approach.
 *
 * Each route is a @Serializable object or data class, providing compile-time
 * safety and automatic argument type checking.
 */

// ══════════════════════════════════════
// Route Definitions (type-safe)
// ══════════════════════════════════════

@Serializable object DashboardRoute
@Serializable object ScheduleRoute
@Serializable object TasksRoute
@Serializable object AppManagerRoute
@Serializable object HistoryRoute
@Serializable object SettingsRoute

// ══════════════════════════════════════
// Screen Metadata (for bottom nav display)
// ══════════════════════════════════════

/**
 * Associates navigation metadata (title, icon) with each route.
 * Used by the bottom navigation bar for display purposes.
 */
data class ScreenMeta<T : Any>(
    val route: T,
    val title: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    ScreenMeta(DashboardRoute, "Home", Icons.Rounded.Dashboard),
    ScreenMeta(ScheduleRoute, "Schedule", Icons.Rounded.Schedule),
    ScreenMeta(TasksRoute, "Tasks", Icons.Rounded.TaskAlt),
    ScreenMeta(AppManagerRoute, "Apps", Icons.Rounded.PhoneAndroid),
    ScreenMeta(HistoryRoute, "History", Icons.Rounded.History)
)

// ══════════════════════════════════════
// Legacy compatibility (temporary bridge)
// ══════════════════════════════════════

/**
 * Legacy Screen sealed class kept for backward compatibility.
 * Will be removed once all consumers migrate to type-safe routes.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Rounded.Dashboard)
    data object Schedule : Screen("schedule", "Schedule", Icons.Rounded.Schedule)
    data object Tasks : Screen("tasks", "Tasks", Icons.Rounded.TaskAlt)
    data object AppManager : Screen("app_manager", "Apps", Icons.Rounded.PhoneAndroid)
    data object History : Screen("history", "History", Icons.Rounded.History)
    data object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Schedule,
    Screen.Tasks,
    Screen.AppManager,
    Screen.History
)
