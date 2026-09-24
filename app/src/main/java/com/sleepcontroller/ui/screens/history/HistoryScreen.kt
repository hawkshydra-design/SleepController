package com.sleepcontroller.ui.screens.history

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepcontroller.data.db.entity.SleepHistory
import com.sleepcontroller.ui.components.CircularProgressIndicator
import com.sleepcontroller.ui.components.GlassCard
import com.sleepcontroller.ui.components.StatCard
import com.sleepcontroller.ui.theme.*
import com.sleepcontroller.util.TimeUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weeklyData by viewModel.weeklyChartData.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBlack)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Header
        item {
            Text(
                text = "Sleep History",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track your sleep patterns",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }

        // ═══════════════════════════════════════
        // Stats Overview (2x2 grid) — Material icons
        // ═══════════════════════════════════════
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Avg Sleep",
                    value = uiState.averageSleepMinutes?.let {
                        TimeUtils.formatDuration(it.toInt())
                    } ?: "--",
                    icon = Icons.Rounded.Bedtime,
                    iconTint = SoftPurple,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Streak",
                    value = "${uiState.currentStreak}",
                    icon = Icons.Rounded.LocalFireDepartment,
                    iconTint = SunriseOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Total Nights",
                    value = "${uiState.totalNights}",
                    icon = Icons.Rounded.DarkMode,
                    iconTint = SoftPurple,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Best Sleep",
                    value = uiState.bestSleepMinutes?.let {
                        TimeUtils.formatDuration(it)
                    } ?: "--",
                    icon = Icons.Rounded.Star,
                    iconTint = SunriseYellow,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ═══════════════════════════════════════
        // Weekly Bar Chart
        // ═══════════════════════════════════════
        if (weeklyData.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Last 7 Nights",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyBarChart(
                        data = weeklyData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            }
        }

        // ═══════════════════════════════════════
        // Task Completion + Emergency Stats
        // ═══════════════════════════════════════
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Task completion ring
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = uiState.taskCompletionRate,
                            size = 70.dp,
                            strokeWidth = 7.dp,
                            progressColor = SleepGreen,
                            progressEndColor = SunriseYellow
                        ) {
                            Text(
                                text = "${(uiState.taskCompletionRate * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tasks Done",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Emergency unlock count
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SunriseOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Bolt,
                                contentDescription = "Emergency unlocks",
                                tint = SunriseOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.emergencyUnlocksTotal}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (uiState.emergencyUnlocksTotal > 0) SunriseOrange else SleepGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Unlocks Used",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // ═══════════════════════════════════════
        // Recent Nights List
        // ═══════════════════════════════════════
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Recent Nights",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (uiState.history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No history yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Complete your first sleep session to see stats",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        } else {
            items(uiState.history, key = { it.id }) { entry ->
                HistoryCard(entry)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ═══════════════════════════════════════════
// Weekly Bar Chart
// ═══════════════════════════════════════════

@Composable
fun WeeklyBarChart(
    data: List<WeeklyBarData>,
    modifier: Modifier = Modifier
) {
    val maxHours = (data.maxOfOrNull { it.hoursSlept } ?: 10f).coerceAtLeast(8f)

    // Animate bars
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
    }

    Column(modifier = modifier) {
        // Chart area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // 8-hour reference line
            val refRatio = 8f / maxHours
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineY = size.height * (1f - refRatio)
                drawLine(
                    color = SleepGreen.copy(alpha = 0.3f),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(8f, 8f)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { barData ->
                    val barHeight = (barData.hoursSlept / maxHours) * animatedProgress.value
                    val isGood = barData.hoursSlept >= 7f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Hours label on top
                        if (barData.hoursSlept > 0) {
                            Text(
                                text = "%.1f".format(barData.hoursSlept),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                fontSize = 9.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        // Bar
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight(barHeight.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        if (isGood) listOf(SleepGreen, SleepGreen.copy(alpha = 0.5f))
                                        else listOf(SunriseOrange, SunriseOrange.copy(alpha = 0.4f))
                                    )
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { barData ->
                Text(
                    text = barData.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Legend
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SleepGreen.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("≥7h", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SunriseOrange.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("<7h", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("- -", style = MaterialTheme.typography.labelSmall, color = SleepGreen.copy(alpha = 0.5f), fontSize = 9.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text("8h target", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
        }
    }
}

// ═══════════════════════════════════════════
// History Card (Enhanced) — Material icons
// ═══════════════════════════════════════════

@Composable
fun HistoryCard(entry: SleepHistory) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    val dateStr = try {
        Instant.ofEpochMilli(entry.date)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    } catch (e: Exception) { "Unknown" }

    val bedtimeStr = try {
        Instant.ofEpochMilli(entry.bedtimeActual)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    } catch (e: Exception) { "--" }

    val wakeUpStr = try {
        entry.wakeUpActual?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .format(timeFormatter)
        } ?: "--"
    } catch (e: Exception) { "--" }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        // Top row: date + duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            entry.sleepDurationMinutes?.let { duration ->
                val isGood = duration >= 420 // 7 hours
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isGood) SleepGreen.copy(alpha = 0.15f)
                            else SunriseOrange.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = TimeUtils.formatDuration(duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isGood) SleepGreen else SunriseOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            } ?: Text(
                text = "In progress",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom row: bedtime → wake-up + task + emergency (icons instead of emojis)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time range with icons
            Icon(
                Icons.Rounded.DarkMode,
                contentDescription = "Bedtime",
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = bedtimeStr,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = TextTertiary.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Rounded.WbSunny,
                contentDescription = "Wake up",
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = wakeUpStr,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.weight(1f))

            // Task count
            if (entry.totalTasks > 0) {
                Icon(
                    Icons.Rounded.TaskAlt,
                    contentDescription = null,
                    tint = if (entry.tasksCompleted >= entry.totalTasks) SleepGreen else SunriseYellow,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${entry.tasksCompleted}/${entry.totalTasks}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.tasksCompleted >= entry.totalTasks) SleepGreen else SunriseYellow,
                    fontWeight = FontWeight.Bold
                )
            }

            // Emergency unlock indicator
            if (entry.emergencyUnlockUsed) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SunriseOrange.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = "Emergency unlock used",
                        tint = SunriseOrange,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
