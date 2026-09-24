package com.sleepcontroller.ui.screens.tasks

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepcontroller.ui.theme.*

/**
 * Morning progress banner displayed at the top of the tasks screen
 * during active morning mode. Shows completion count and animated
 * progress bar with celebration state when all tasks are done.
 */
@Composable
fun MorningProgressBanner(
    completedCount: Int,
    totalCount: Int,
    allDone: Boolean
) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    val pulseScale by rememberInfiniteTransition(label = "banner").animateFloat(
        initialValue = 1f,
        targetValue = if (allDone) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    if (allDone) listOf(SleepGreen.copy(alpha = 0.2f), SleepGreen.copy(alpha = 0.1f))
                    else listOf(SunriseOrange.copy(alpha = 0.15f), SunriseYellow.copy(alpha = 0.1f))
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (allDone) "🎉" else "🌅",
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (allDone) "All Tasks Complete!" else "Good Morning!",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (allDone) SleepGreen else SunriseOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (allDone) "Apps are now unlocked. Have a great day!"
                        else "$completedCount of $totalCount tasks completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (allDone) SleepGreen else SunriseOrange,
                trackColor = SurfaceBorder,
            )
        }
    }
}
