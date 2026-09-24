package com.sleepcontroller.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepcontroller.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        GlassWhiteStrong,
                        GlassWhite.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        GlassWhite,
                        GlassWhite.copy(alpha = 0.03f)
                    )
                )
            )
            .background(DeepNight.copy(alpha = 0.75f))
            .padding(20.dp),
        content = content
    )
}

@Composable
fun StatusBanner(
    isActive: Boolean,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val gradientColors = if (isActive) {
        listOf(SoftPurple.copy(alpha = 0.3f * pulseAlpha), DuskPurple.copy(alpha = 0.1f))
    } else {
        listOf(DeepNight, DeepNight)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isActive) Modifier.border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            SoftPurple.copy(alpha = 0.4f * pulseAlpha),
                            SleepGreen.copy(alpha = 0.2f * pulseAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) else Modifier.border(
                    width = 1.dp,
                    color = SurfaceBorder.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp)
                )
            )
            .background(Brush.horizontalGradient(gradientColors))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Animated status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) SleepGreen.copy(alpha = pulseAlpha)
                            else TextTertiary
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) SleepGreen else TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
fun CircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = SurfaceBorder,
    progressColor: Color = SoftPurple,
    progressEndColor: Color = SunriseYellow,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = 360f * animatedProgress
            val stroke = strokeWidth.toPx()

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(this.size.width - stroke, this.size.height - stroke),
                topLeft = Offset(stroke / 2, stroke / 2)
            )

            // Progress
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(progressColor, progressEndColor, progressColor)
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(this.size.width - stroke, this.size.height - stroke),
                topLeft = Offset(stroke / 2, stroke / 2)
            )
        }

        content()
    }
}

@Composable
fun SleepTimePicker(
    hour: Int,
    minute: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val amPm = if (hour < 12) "AM" else "PM"

            Text(
                text = "%d:%02d".format(displayHour, minute),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = amPm,
                style = MaterialTheme.typography.titleMedium,
                color = SoftPurple,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

/**
 * Permission card with clickable "Grant" button.
 * Bug #1 fix: Added .clickable modifier to the Grant button.
 */
@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isGranted) SleepGreen.copy(alpha = 0.12f)
                        else SleepRed.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = if (isGranted) "Granted" else "Not granted",
                    tint = if (isGranted) SleepGreen else SleepRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Grant / Granted button — BUG #1 FIX: now clickable
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isGranted) { onRequest() }
                    .background(
                        brush = if (isGranted) Brush.horizontalGradient(
                            listOf(SleepGreen.copy(alpha = 0.15f), SleepGreen.copy(alpha = 0.15f))
                        )
                        else Brush.horizontalGradient(
                            listOf(SoftPurple.copy(alpha = 0.25f), SleepRed.copy(alpha = 0.2f))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isGranted) "Granted" else "Grant",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGranted) SleepGreen else TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Stat card using Material icons instead of emojis.
 * Supports both the new icon parameter and legacy emoji string.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = SoftPurple,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
    }
}

/**
 * Legacy StatCard overload for backward compatibility with emoji strings.
 * Converts common emoji strings to Material icons.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    icon: String = "",
    modifier: Modifier = Modifier
) {
    val (imageVector, tint) = when (icon) {
        "🔥" -> Icons.Rounded.LocalFireDepartment to SunriseOrange
        "✅" -> Icons.Rounded.TaskAlt to SleepGreen
        "😴" -> Icons.Rounded.Bedtime to SoftPurple
        "🌙" -> Icons.Rounded.DarkMode to SoftPurple
        "⭐" -> Icons.Rounded.Star to SunriseYellow
        "⚡" -> Icons.Rounded.Bolt to SunriseOrange
        "📊" -> Icons.Rounded.BarChart to SleepBlue
        else -> Icons.Rounded.Info to SoftPurple
    }
    StatCard(
        label = label,
        value = value,
        icon = imageVector,
        iconTint = tint,
        modifier = modifier
    )
}
