package com.sleepcontroller.ui.screens.tasks

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepcontroller.ui.components.GlassCard
import com.sleepcontroller.ui.theme.*

/**
 * Main screen for morning task management and completion.
 *
 * Two modes:
 * - **Configuration mode**: Add, edit, toggle, and delete morning tasks.
 * - **Morning mode**: Complete tasks to unlock apps after wake-up.
 *
 * This is a slim orchestrator — all sub-components have been extracted into:
 * - [ActiveTaskCard] — task completion card during morning mode
 * - [TaskCard] — configuration card with toggle/edit/delete
 * - [AddTaskDialog] — add/edit dialog with type selection
 * - [MorningProgressBanner] — progress indicator during morning mode
 * - [PhotoCaptureButton] — camera capture with permission handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningTasksScreen(
    viewModel: MorningTasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(NightBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            TasksHeader(
                isMorningMode = uiState.isMorningModeActive,
                completedCount = uiState.completedCount,
                totalCount = uiState.totalCount
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Morning mode progress banner OR configuration info
            if (uiState.isMorningModeActive) {
                MorningProgressBanner(
                    completedCount = uiState.completedCount,
                    totalCount = uiState.totalCount,
                    allDone = uiState.allTasksCompleted
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                ConfigurationInfoBanner()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Task List
            if (uiState.tasks.isEmpty()) {
                EmptyTasksPlaceholder(modifier = Modifier.weight(1f))
            } else {
                TaskList(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }

            // Add Task Button (only in configuration mode)
            if (!uiState.isMorningModeActive) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.showAddDialog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftPurple)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Morning Task",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Add/Edit Dialog
        if (uiState.showAddDialog) {
            AddTaskDialog(
                editingTask = uiState.editingTask,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { title, type, timedSeconds ->
                    if (uiState.editingTask != null) {
                        viewModel.updateTask(
                            uiState.editingTask!!.copy(
                                title = title,
                                type = type,
                                timedDurationSeconds = timedSeconds
                            )
                        )
                    } else {
                        viewModel.addTask(title, type, timedSeconds)
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════
// Private helper composables
// ═══════════════════════════════════════════

@Composable
private fun TasksHeader(
    isMorningMode: Boolean,
    completedCount: Int,
    totalCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Morning Tasks",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isMorningMode) {
                    "$completedCount/$totalCount completed"
                } else {
                    "Configure tasks to complete each morning"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isMorningMode) SunriseOrange else TextTertiary
            )
        }
    }
}

@Composable
private fun ConfigurationInfoBanner() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌅", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "All enabled tasks must be completed each morning before your apps are unblocked. Photo tasks verify EXIF data to ensure fresh photos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun EmptyTasksPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📝", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No tasks yet",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Text(
                text = "Add morning tasks to build healthy habits",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun TaskList(
    uiState: TasksUiState,
    viewModel: MorningTasksViewModel,
    modifier: Modifier = Modifier
) {
    // Pre-computed from ViewModel — no filtering during scroll recomposition
    val enabledTasks by viewModel.enabledTasks.collectAsStateWithLifecycle()
    val disabledTasks by viewModel.disabledTasks.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        if (uiState.isMorningModeActive) {
            // Active / completion cards during morning mode
            items(enabledTasks, key = { it.id }) { task ->
                val state = uiState.completionStates[task.id]
                ActiveTaskCard(
                    task = task,
                    state = state,
                    onComplete = { viewModel.completeCheckboxTask(task.id) },
                    onStartTimer = { viewModel.startTimer(task.id) },
                    onPhotoSubmit = { uri -> viewModel.submitPhoto(task.id, uri) }
                )
            }
        } else {
            // Configuration mode
            items(enabledTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggle = { viewModel.toggleTaskEnabled(task) },
                    onEdit = { viewModel.showEditDialog(task) },
                    onDelete = { viewModel.deleteTask(task) }
                )
            }
            if (disabledTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "DISABLED",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(disabledTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { viewModel.toggleTaskEnabled(task) },
                        onEdit = { viewModel.showEditDialog(task) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
