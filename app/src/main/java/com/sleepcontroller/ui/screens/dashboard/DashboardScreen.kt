package com.sleepcontroller.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepcontroller.ui.components.*
import com.sleepcontroller.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // PIN verification dialog state for emergency unlock
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Moon rotation animation — uses graphicsLayer for GPU-accelerated rotation
    val infiniteTransition = rememberInfiniteTransition(label = "moon")
    val moonRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "moonRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sleep Controller",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your personal sleep guardian",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GlassWhite)
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sleep Mode Status Banner
        StatusBanner(
            isActive = uiState.isSleepModeActive || uiState.isMorningMode,
            title = when {
                uiState.isSleepModeActive -> "Sleep Mode Active"
                uiState.isMorningMode -> "Morning Mode"
                else -> "Sleep Mode Inactive"
            },
            subtitle = when {
                uiState.isSleepModeActive -> "Apps are blocked • Sweet dreams"
                uiState.isMorningMode -> "Complete your tasks to unlock apps"
                else -> "Next bedtime: ${uiState.bedtimeFormatted}"
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main circular display
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = when {
                    uiState.isSleepModeActive -> 1f
                    uiState.isMorningMode -> (uiState.completedTasks.toFloat() / uiState.totalTasks.coerceAtLeast(1)).coerceIn(0f, 1f)
                    else -> {
                        val total = uiState.schedule?.let { 24 * 60 } ?: 1
                        val elapsed = total - uiState.minutesUntilBedtime.toInt()
                        (elapsed.toFloat() / total).coerceIn(0f, 1f)
                    }
                },
                size = 200.dp,
                strokeWidth = 12.dp,
                progressColor = when {
                    uiState.isSleepModeActive -> SleepGreen
                    uiState.isMorningMode -> SunriseOrange
                    else -> SoftPurple
                },
                progressEndColor = when {
                    uiState.isSleepModeActive -> SleepGreen
                    uiState.isMorningMode -> SunriseYellow
                    else -> SunriseYellow
                }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Moon/sunrise icon with GPU-accelerated rotation
                    Icon(
                        imageVector = when {
                            uiState.isMorningMode -> Icons.Rounded.WbSunny
                            else -> Icons.Rounded.DarkMode
                        },
                        contentDescription = null,
                        tint = when {
                            uiState.isMorningMode -> SunriseYellow
                            uiState.isSleepModeActive -> SleepGreen
                            else -> SoftPurple
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer { rotationZ = moonRotation }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    when {
                        uiState.isSleepModeActive -> {
                            Text(
                                text = "SLEEPING",
                                style = MaterialTheme.typography.titleMedium,
                                color = SleepGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        uiState.isMorningMode -> {
                            Text(
                                text = "MORNING",
                                style = MaterialTheme.typography.titleMedium,
                                color = SunriseOrange,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "complete tasks",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                        else -> {
                            val hours = uiState.minutesUntilBedtime / 60
                            val mins = uiState.minutesUntilBedtime % 60
                            Text(
                                text = "${hours}h ${mins}m",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "until bedtime",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tonight's Schedule Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Tonight's Schedule",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SleepTimePicker(
                    hour = uiState.schedule?.bedtimeHour ?: 22,
                    minute = uiState.schedule?.bedtimeMinute ?: 0,
                    label = "BEDTIME"
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = "Sleep duration",
                        tint = SunriseYellow,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.sleepDurationFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        color = SunriseYellow,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SLEEP",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
                SleepTimePicker(
                    hour = uiState.schedule?.wakeUpHour ?: 6,
                    minute = uiState.schedule?.wakeUpMinute ?: 0,
                    label = "WAKE UP"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Row — using Material icons instead of emojis
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Streak",
                value = "${uiState.sleepStreak}",
                icon = Icons.Rounded.LocalFireDepartment,
                iconTint = SunriseOrange,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Tasks",
                value = "${uiState.completedTasks}/${uiState.totalTasks}",
                icon = Icons.Rounded.TaskAlt,
                iconTint = SleepGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // Quick Toggle / Lock-Down Section
        // ═══════════════════════════════════════

        // Only show "Enable Sleep Mode" when Inactive
        if (!uiState.isAnyModeActive && !uiState.isBlocking) {
            Button(
                onClick = { viewModel.toggleSleepMode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple)
            ) {
                Icon(
                    Icons.Rounded.DarkMode,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enable Sleep Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // During active session: show locked info card instead of toggle
        if (uiState.isSleepModeActive || uiState.isMorningMode) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = SunriseOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isSleepModeActive) "Sleep Mode Locked"
                                   else "Morning Tasks Required",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.hasUnlocksRemaining)
                                "Use Emergency Unlock (${uiState.remainingUnlocks}/5 left) or complete your tasks."
                            else "All emergency unlocks used tonight.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        // During temporary unlock: show countdown
        if (uiState.isTemporaryUnlock) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = SunriseYellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Temporarily Unlocked",
                            style = MaterialTheme.typography.titleSmall,
                            color = SunriseYellow,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Blocking will resume in 20 minutes. ${uiState.remainingUnlocks}/5 unlocks left.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        // Emergency Unlock button (only during active blocking sessions with unlocks remaining)
        if ((uiState.isSleepModeActive || uiState.isMorningMode) && uiState.hasUnlocksRemaining) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    showPinDialog = true
                    pinInput = ""
                    pinError = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SunriseOrange
                )
            ) {
                Icon(
                    Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = SunriseOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Emergency Unlock (${uiState.remainingUnlocks}/5 left) — 20 min",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = SunriseOrange
                )
            }
        }

        // PIN verification dialog for emergency unlock
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPinDialog = false
                    pinInput = ""
                    pinError = false
                },
                icon = {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = SunriseOrange,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        "Enter PIN to Unlock",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            "Enter your emergency PIN to temporarily disable sleep mode for 20 minutes.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    pinInput = it
                                    pinError = false
                                }
                            },
                            placeholder = {
                                Text("Enter PIN", color = TextTertiary.copy(alpha = 0.5f))
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            isError = pinError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SunriseOrange,
                                unfocusedBorderColor = SurfaceBorder,
                                errorBorderColor = Color(0xFFEF4444),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = SunriseOrange
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Wrong PIN — try again",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.verifyPinAndUnlock(pinInput) { success ->
                                if (success) {
                                    showPinDialog = false
                                    pinInput = ""
                                } else {
                                    pinError = true
                                    pinInput = ""
                                }
                            }
                        },
                        enabled = pinInput.length >= 4
                    ) {
                        Text("Unlock", color = SunriseOrange, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPinDialog = false
                        pinInput = ""
                        pinError = false
                    }) {
                        Text("Cancel", color = TextTertiary)
                    }
                },
                containerColor = DeepNight,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Edit Schedule shortcut
        TextButton(
            onClick = onNavigateToSchedule,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Rounded.Edit,
                contentDescription = null,
                tint = SoftPurple,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Edit Schedule",
                color = SoftPurple,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
