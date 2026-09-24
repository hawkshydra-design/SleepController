package com.sleepcontroller.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sleepcontroller.ui.screens.apps.AppManagerScreen
import com.sleepcontroller.ui.screens.dashboard.DashboardScreen
import com.sleepcontroller.ui.screens.history.HistoryScreen
import com.sleepcontroller.ui.screens.schedule.ScheduleScreen
import com.sleepcontroller.ui.screens.settings.SettingsScreen
import com.sleepcontroller.ui.screens.tasks.MorningTasksScreen

/**
 * Type-safe navigation graph using @Serializable route objects.
 *
 * Routes are defined in Screen.kt as @Serializable objects.
 * This replaces the old string-based `composable("dashboard")` approach
 * with compile-time safe `composable<DashboardRoute>` calls.
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(300))
        }
    ) {
        composable<DashboardRoute> {
            DashboardScreen(
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                onNavigateToSchedule = { navController.navigate(ScheduleRoute) }
            )
        }

        composable<ScheduleRoute> {
            ScheduleScreen()
        }

        composable<TasksRoute> {
            MorningTasksScreen()
        }

        composable<AppManagerRoute> {
            AppManagerScreen()
        }

        composable<HistoryRoute> {
            HistoryScreen()
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
