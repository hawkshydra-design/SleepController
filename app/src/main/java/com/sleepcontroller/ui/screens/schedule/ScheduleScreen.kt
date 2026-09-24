package com.sleepcontroller.ui.screens.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepcontroller.ui.components.GlassCard
import com.sleepcontroller.ui.components.SleepTimePicker
import com.sleepcontroller.ui.theme.*
import com.sleepcontroller.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Text(
            text = "Sleep Schedule",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure your sleep routine",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bedtime & Wake-up Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showTimePicker = true }
                ) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = null,
                        tint = SoftPurple,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SleepTimePicker(
                        hour = uiState.bedtimeHour,
                        minute = uiState.bedtimeMinute,
                        label = "BEDTIME"
                    )
                    Text(
                        text = "Tap to edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftPurple
                    )
                }

                // Arrow
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = TimeUtils.formatDuration(uiState.sleepDurationMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = SunriseYellow
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = SunriseYellow,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SleepTimePicker(
                        hour = uiState.wakeUpHour,
                        minute = uiState.wakeUpMinute,
                        label = "WAKE UP"
                    )
                    Text(
                        text = "Auto-calculated",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sleep Duration Slider
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = SoftPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sleep Duration",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = TimeUtils.formatDuration(uiState.sleepDurationMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    color = SunriseYellow,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = uiState.sleepDurationMinutes.toFloat(),
                onValueChange = { viewModel.updateSleepDuration(it.toInt()) },
                valueRange = 420f..540f,
                steps = 7, // 15-min increments: (540-420)/15 - 1 = 7
                colors = SliderDefaults.colors(
                    thumbColor = SoftPurple,
                    activeTrackColor = SoftPurple,
                    inactiveTrackColor = SurfaceBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("7h", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text("9h", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Days of Week Selector
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Active Days",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayLabels.forEachIndexed { index, label ->
                    val isEnabled = uiState.daysEnabled[index]
                    val bgColor by animateColorAsState(
                        targetValue = if (isEnabled) SoftPurple else GlassWhite,
                        label = "dayBg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isEnabled) TextPrimary else TextTertiary,
                        label = "dayText"
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable { viewModel.toggleDay(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wind-down Period
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = null,
                    tint = SunriseOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wind-down Warning",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${uiState.windDownMinutes} min",
                    style = MaterialTheme.typography.titleMedium,
                    color = SunriseOrange,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = uiState.windDownMinutes.toFloat(),
                onValueChange = { viewModel.updateWindDown(it.toInt()) },
                valueRange = 0f..60f,
                steps = 3, // 0, 15, 30, 45, 60
                colors = SliderDefaults.colors(
                    thumbColor = SunriseOrange,
                    activeTrackColor = SunriseOrange,
                    inactiveTrackColor = SurfaceBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Get a notification before sleep mode activates",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = { viewModel.saveSchedule() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SoftPurple
            )
        ) {
            Icon(
                Icons.Rounded.Save,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (uiState.isSaved) "Schedule Saved" else "Save Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (uiState.isSaved) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Saved",
                    modifier = Modifier.size(18.dp),
                    tint = SleepGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.bedtimeHour,
            initialMinute = uiState.bedtimeMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateBedtime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("Set", color = SoftPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = TextTertiary)
                }
            },
            title = { Text("Set Bedtime", color = TextPrimary) },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = DeepNight,
                        selectorColor = SoftPurple,
                        containerColor = DuskPurple,
                        periodSelectorSelectedContainerColor = SoftPurple.copy(alpha = 0.3f),
                        timeSelectorSelectedContainerColor = SoftPurple.copy(alpha = 0.3f)
                    )
                )
            },
            containerColor = DuskPurple,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
