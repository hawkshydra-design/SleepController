package com.sleepcontroller.ui.screens.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepcontroller.data.db.entity.MorningTask
import com.sleepcontroller.data.db.entity.TaskType
import com.sleepcontroller.ui.components.GlassCard
import com.sleepcontroller.ui.theme.*
import android.net.Uri
import kotlinx.coroutines.delay

/**
 * Active task card shown during morning mode completion.
 * Displays the task with its current completion state and
 * provides the appropriate action button based on task type.
 *
 * Checkbox tasks now have a 2-step confirmation:
 *  1. Tap "Done" → confirmation dialog appears
 *  2. "Yes, I did it" is disabled for 5 seconds
 *  3. After 5s → button enables → tap to complete
 *
 * Also shows cooldown timer when 30s between-task cooldown is active.
 */
@Composable
fun ActiveTaskCard(
    task: MorningTask,
    state: TaskCompletionState?,
    onComplete: () -> Unit,
    onStartTimer: () -> Unit,
    onPhotoSubmit: (Uri) -> Unit
) {
    val isCompleted = state?.isCompleted == true
    val cooldownActive = (state?.cooldownSecondsRemaining ?: 0) > 0

    // Confirmation dialog state for checkbox tasks
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmCountdown by remember { mutableIntStateOf(5) }
    var confirmEnabled by remember { mutableStateOf(false) }

    // Confirmation dialog with 5-second wait
    if (showConfirmDialog) {
        LaunchedEffect(Unit) {
            confirmCountdown = 5
            confirmEnabled = false
            while (confirmCountdown > 0) {
                delay(1000)
                confirmCountdown--
            }
            confirmEnabled = true
        }

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                confirmEnabled = false
            },
            containerColor = DeepNight,
            title = {
                Text(
                    text = "Confirm: ${task.title}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Did you really complete this task?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        confirmEnabled = false
                        onComplete()
                    },
                    enabled = confirmEnabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleepGreen,
                        disabledContainerColor = SurfaceBorder.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (confirmEnabled) "Yes, I did it ✓"
                               else "Wait ${confirmCountdown}s...",
                        color = if (confirmEnabled) TextPrimary
                                else TextTertiary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    confirmEnabled = false
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    val bgColor by animateColorAsState(
        targetValue = if (isCompleted) SleepGreen.copy(alpha = 0.1f) else DeepNight.copy(alpha = 0.7f),
        label = "taskBg"
    )

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Completion indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) SleepGreen.copy(alpha = 0.2f) else SurfaceBorder.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = SleepGreen,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = when (task.type) {
                            TaskType.CHECKBOX -> "✅"
                            TaskType.TIMED -> "⏱️"
                            TaskType.PHOTO -> "📸"
                        },
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCompleted) SleepGreen else TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                when {
                    isCompleted -> {
                        Text(
                            text = "Completed ✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleepGreen
                        )
                    }
                    cooldownActive -> {
                        Text(
                            text = "Wait ${state?.cooldownSecondsRemaining}s before next task",
                            style = MaterialTheme.typography.labelSmall,
                            color = SunriseYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    task.type == TaskType.TIMED && state?.timerRunning == true -> {
                        val remaining = state.timerSecondsRemaining ?: 0
                        val minutes = remaining / 60
                        val seconds = remaining % 60
                        Text(
                            text = "%d:%02d remaining".format(minutes, seconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = SunriseYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    task.type == TaskType.PHOTO && state?.verificationMessage?.isNotEmpty() == true -> {
                        Text(
                            text = state.verificationMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.photoVerified) SleepGreen else SleepRed
                        )
                    }
                }
            }

            // Action button
            if (!isCompleted) {
                when (task.type) {
                    TaskType.CHECKBOX -> {
                        Button(
                            onClick = { showConfirmDialog = true },
                            enabled = !cooldownActive,
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftPurple,
                                disabledContainerColor = SurfaceBorder.copy(alpha = 0.3f)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text(
                                text = if (cooldownActive) "${state?.cooldownSecondsRemaining}s"
                                       else "Done",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (cooldownActive) TextTertiary else TextPrimary
                            )
                        }
                    }
                    TaskType.TIMED -> {
                        if (state?.timerRunning != true) {
                            Button(
                                onClick = onStartTimer,
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SunriseOrange),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Text("Start", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    TaskType.PHOTO -> {
                        PhotoCaptureButton(onPhotoSubmit)
                    }
                }
            }
        }
    }
}
