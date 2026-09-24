package com.sleepcontroller.ui.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepcontroller.ui.theme.*
import kotlin.random.Random

/**
 * Full-screen blocker overlay UI rendered in a system window.
 *
 * Features:
 *  - Animated star field with proper state-based animation (Bug #3 fix)
 *  - Breathing moon animation with glow
 *  - Calming message encouraging sleep
 *  - "Go Home" button to return to launcher
 *  - One-time emergency unlock (disabled after use)
 *
 * Bug #3 fix: Replaced System.currentTimeMillis() in Canvas with proper
 * Compose animateFloat states to prevent continuous recomposition.
 */
@Composable
fun BlockerOverlayContent(
    blockedAppName: String,
    onDismiss: () -> Unit,
    onEmergencyUnlock: () -> Unit,
    onVerifyPin: (String, (Boolean) -> Unit) -> Unit,
    isEmergencyUnlockAvailable: () -> Boolean,
    remainingUnlocks: Int = 5
) {
    // Reactive: re-evaluates on each recomposition (backed by collectAsState in OverlayBlockerService)
    val emergencyAvailable = isEmergencyUnlockAvailable()
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Breathing animation for the moon
    val infiniteTransition = rememberInfiniteTransition(label = "overlay")
    val moonScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moonScale"
    )
    val moonGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moonGlow"
    )
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050316),
                        Color(0xFF0D0B21),
                        Color(0xFF1A1040),
                        Color(0xFF0D0B21)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated star field — Bug #3 fix: uses state-based animation
        StarField()

        // Floating purple orbs (ambient particles)
        FloatingOrbs(glowAlpha = moonGlow)

        // Main content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Glowing moon icon (Material icon replaces emoji)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(moonScale),
                contentAlignment = Alignment.Center
            ) {
                // Glow ring
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .blur(30.dp)
                        .clip(CircleShape)
                        .background(SoftPurple.copy(alpha = moonGlow * 0.5f))
                )
                // Moon icon
                Icon(
                    Icons.Rounded.DarkMode,
                    contentDescription = "Sleep mode",
                    tint = LightPurple,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Time to Sleep",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "This app is blocked during\nyour sleep time",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Blocked app info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = blockedAppName.substringAfterLast("."),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Go Home button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftPurple
                )
            ) {
                Icon(
                    Icons.Rounded.Home,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Go Home",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Emergency unlock
            if (emergencyAvailable) {
                OutlinedButton(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SunriseOrange
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(
                        Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = SunriseOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Emergency Unlock ($remainingUnlocks/5 left) — 20 min",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = SunriseOrange
                    )
                }
            } else {
                Text(
                    text = "All 5 emergency unlocks used tonight",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(
                    Icons.Rounded.Bedtime,
                    contentDescription = null,
                    tint = TextTertiary.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sweet dreams",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary.copy(alpha = 0.4f)
                )
            }
        }

        // Emergency unlock PIN verification dialog
        if (showConfirmDialog) {
            var pinInput by remember { mutableStateOf("") }
            var pinError by remember { mutableStateOf(false) }
            var isVerifying by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    showConfirmDialog = false
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
                            onVerifyPin(pinInput) { isValid ->
                                if (isValid) {
                                    showConfirmDialog = false
                                    pinInput = ""
                                    onEmergencyUnlock()
                                } else {
                                    pinError = true
                                    pinInput = ""
                                }
                            }
                        },
                        enabled = pinInput.length >= 4 && !isVerifying
                    ) {
                        Text("Unlock", color = SunriseOrange, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
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
    }
}

/**
 * Animated starfield with proper Compose state-based twinkling.
 *
 * Bug #3 fix: Previously used System.currentTimeMillis() inside Canvas draw,
 * which forced continuous recomposition with no frame budget control.
 * Now uses batched animateFloat groups (4 groups) to efficiently animate
 * star alpha without per-star recomposition overhead.
 */
@Composable
private fun StarField() {
    // Pre-generate star positions (stable across recompositions)
    val stars = remember {
        List(80) {
            StarData(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 3f + 1f,
                group = it % 4  // Batch into 4 animation groups
            )
        }
    }

    // 4 batched twinkle animations instead of 80 individual ones
    val infiniteTransition = rememberInfiniteTransition(label = "stars")

    val groupAlphas = (0..3).map { group ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2500 + group * 700,
                    easing = EaseInOutSine
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "starGroup$group"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEach { star ->
            val alpha = groupAlphas[star.group].value

            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.6f),
                radius = star.size,
                center = Offset(
                    star.x * size.width,
                    star.y * size.height
                )
            )
        }
    }
}

/**
 * Floating ambient orbs that drift slowly
 */
@Composable
private fun FloatingOrbs(glowAlpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbFloat"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Top-left purple orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SoftPurple.copy(alpha = glowAlpha * 0.15f),
                    Color.Transparent
                ),
                radius = 300f
            ),
            radius = 200f,
            center = Offset(size.width * 0.2f, size.height * 0.15f + orbOffset)
        )

        // Bottom-right blue orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SleepBlue.copy(alpha = glowAlpha * 0.1f),
                    Color.Transparent
                ),
                radius = 250f
            ),
            radius = 180f,
            center = Offset(size.width * 0.85f, size.height * 0.75f - orbOffset)
        )

        // Center-bottom sunset orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SunriseOrange.copy(alpha = glowAlpha * 0.05f),
                    Color.Transparent
                ),
                radius = 200f
            ),
            radius = 150f,
            center = Offset(size.width * 0.5f, size.height * 0.9f + orbOffset * 0.5f)
        )
    }
}

private data class StarData(
    val x: Float,
    val y: Float,
    val size: Float,
    val group: Int
)
