package com.sleepcontroller.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sleepcontroller.ui.components.GlassCard
import com.sleepcontroller.ui.components.PermissionCard
import com.sleepcontroller.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // PIN change state
    var showPinDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh permissions on screen resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show success snackbar
    LaunchedEffect(uiState.pinSaveSuccess) {
        if (uiState.pinSaveSuccess == true) {
            // Auto-clear after showing
            viewModel.clearPinSaveStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ═══════════════════════════════════════
        // Permissions Section
        // ═══════════════════════════════════════
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Security,
                contentDescription = null,
                tint = SoftPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Permission count badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(28.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (uiState.permissionStatus.allGranted) SleepGreen.copy(alpha = 0.12f)
                        else SunriseOrange.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${uiState.permissionStatus.grantedCount}/${uiState.permissionStatus.totalCount} granted",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.permissionStatus.allGranted) SleepGreen else SunriseOrange,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            title = "Usage Stats Access",
            description = "Detect which app is in foreground",
            isGranted = uiState.permissionStatus.usageStats,
            onRequest = { viewModel.openUsageStatsSettings() }
        )
        Spacer(modifier = Modifier.height(8.dp))
        PermissionCard(
            title = "Draw Over Apps",
            description = "Show blocker overlay on locked apps",
            isGranted = uiState.permissionStatus.overlay,
            onRequest = { viewModel.openOverlaySettings() }
        )
        Spacer(modifier = Modifier.height(8.dp))
        PermissionCard(
            title = "Accessibility Service",
            description = "Detect app launches in real-time",
            isGranted = uiState.permissionStatus.accessibility,
            onRequest = { viewModel.openAccessibilitySettings() }
        )
        Spacer(modifier = Modifier.height(8.dp))
        PermissionCard(
            title = "Battery Optimization",
            description = "Keep blocker running reliably",
            isGranted = uiState.permissionStatus.batteryOptimization,
            onRequest = { viewModel.openBatterySettings() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ═══════════════════════════════════════
        // Emergency PIN — Hashed storage
        // ═══════════════════════════════════════
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Shield,
                contentDescription = null,
                tint = SunriseOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Pin,
                    contentDescription = null,
                    tint = SunriseOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Emergency Override PIN",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (uiState.isPinConfigured) SleepGreen.copy(alpha = 0.12f)
                            else SunriseOrange.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (uiState.isPinConfigured) "Configured ✓" else "Not set",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isPinConfigured) SleepGreen else SunriseOrange,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "This PIN is required for emergency unlock during sleep mode. " +
                        "Your PIN is stored securely using SHA-256 encryption.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Change PIN button
            Button(
                onClick = { showPinDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftPurple.copy(alpha = 0.2f),
                    contentColor = SoftPurple
                )
            ) {
                Icon(
                    if (uiState.isPinConfigured) Icons.Rounded.Edit else Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isPinConfigured) "Change PIN" else "Set PIN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = TextTertiary.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PIN is hashed — it cannot be viewed, only changed.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═══════════════════════════════════════
        // Preferences
        // ═══════════════════════════════════════
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription = null,
                tint = SleepBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Notifications toggle
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = SoftPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Sleep reminders & alarm alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
                Switch(
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.updateNotifications(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SoftPurple,
                        checkedTrackColor = SoftPurple.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = SurfaceBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grace period
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = SunriseYellow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task Grace Period",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Skip tasks after ${uiState.gracePeriodMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
                Text(
                    text = "${uiState.gracePeriodMinutes} min",
                    style = MaterialTheme.typography.titleSmall,
                    color = SunriseYellow,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = uiState.gracePeriodMinutes.toFloat(),
                onValueChange = { viewModel.updateGracePeriod(it.toInt()) },
                valueRange = 30f..120f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = SunriseYellow,
                    activeTrackColor = SunriseYellow,
                    inactiveTrackColor = SurfaceBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ═══════════════════════════════════════
        // About
        // ═══════════════════════════════════════
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(SoftPurple.copy(alpha = 0.3f), DuskPurple.copy(alpha = 0.5f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DarkMode,
                        contentDescription = null,
                        tint = LightPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Sleep Controller",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v1.0.0 • Personal Use",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // PIN Change Dialog
    if (showPinDialog) {
        PinChangeDialog(
            onDismiss = { showPinDialog = false },
            onSave = { newPin ->
                viewModel.updatePin(newPin)
                showPinDialog = false
            }
        )
    }
}

/**
 * Dialog for setting or changing the emergency override PIN.
 * Requires entering and confirming a 4-6 digit PIN.
 */
@Composable
fun PinChangeDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = SoftPurple,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                "Set Emergency PIN",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Enter a 4–6 digit PIN for emergency unlock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // New PIN field
                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            newPin = it
                            error = null
                        }
                    },
                    label = { Text("New PIN", color = TextTertiary) },
                    visualTransformation = if (pinVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                imageVector = if (pinVisible) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                contentDescription = if (pinVisible) "Hide" else "Show",
                                tint = TextTertiary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftPurple,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = SoftPurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Confirm PIN field
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            confirmPin = it
                            error = null
                        }
                    },
                    label = { Text("Confirm PIN", color = TextTertiary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftPurple,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = SoftPurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error message
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = SunriseOrange
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        newPin.length < 4 -> error = "PIN must be at least 4 digits"
                        newPin != confirmPin -> error = "PINs don't match"
                        else -> onSave(newPin)
                    }
                }
            ) {
                Text("Save", color = SoftPurple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextTertiary)
            }
        },
        containerColor = DeepNight,
        shape = RoundedCornerShape(24.dp)
    )
}
