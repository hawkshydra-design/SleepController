package com.sleepcontroller.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sleepcontroller.data.preferences.SleepPreferences
import com.sleepcontroller.ui.navigation.*
import com.sleepcontroller.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: SleepPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SleepControllerTheme {
                val coroutineScope = rememberCoroutineScope()
                var isFirstLaunch by remember { mutableStateOf<Boolean?>(null) }
                var isPinConfigured by remember { mutableStateOf(true) }

                // Check first launch state
                LaunchedEffect(Unit) {
                    isFirstLaunch = preferences.isFirstLaunch.first()
                    isPinConfigured = preferences.isPinConfigured.first()
                }

                when {
                    isFirstLaunch == null -> {
                        // Loading state — show nothing briefly
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NightBlack)
                        )
                    }
                    isFirstLaunch == true || !isPinConfigured -> {
                        // Show PIN setup screen on first launch
                        PinSetupScreen(
                            onPinSet = { pin ->
                                coroutineScope.launch {
                                    preferences.setOverridePin(pin)
                                    preferences.setFirstLaunch(false)
                                    isFirstLaunch = false
                                    isPinConfigured = true
                                }
                            }
                        )
                    }
                    else -> {
                        MainScreen()
                    }
                }
            }
        }
    }
}

/**
 * Full-screen PIN setup shown on first app launch.
 * Forces the user to set a custom emergency override PIN before using the app.
 */
@Composable
fun PinSetupScreen(onPinSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableIntStateOf(1) } // 1 = enter, 2 = confirm

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SoftPurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = SoftPurple,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Set Your Emergency PIN",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This PIN is required for emergency unlock\nduring sleep mode. Choose wisely!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (step == 1) {
                // Step 1: Enter PIN
                Text(
                    text = "Enter a 4–6 digit PIN",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            pin = it
                            error = null
                        }
                    },
                    placeholder = { Text("● ● ● ●", color = TextTertiary.copy(alpha = 0.3f)) },
                    visualTransformation = if (pinVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                imageVector = if (pinVisible) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                contentDescription = null,
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
            } else {
                // Step 2: Confirm PIN
                Text(
                    text = "Confirm your PIN",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            confirmPin = it
                            error = null
                        }
                    },
                    placeholder = { Text("● ● ● ●", color = TextTertiary.copy(alpha = 0.3f)) },
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
            }

            // Error
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = SunriseOrange
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Next / Confirm button
            Button(
                onClick = {
                    if (step == 1) {
                        if (pin.length < 4) {
                            error = "PIN must be at least 4 digits"
                        } else {
                            step = 2
                            error = null
                        }
                    } else {
                        if (confirmPin != pin) {
                            error = "PINs don't match — try again"
                            confirmPin = ""
                        } else {
                            onPinSet(pin)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                enabled = if (step == 1) pin.length >= 4 else confirmPin.length >= 4
            ) {
                Text(
                    text = if (step == 1) "Next" else "Set PIN & Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            if (step == 2) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    step = 1
                    confirmPin = ""
                    error = null
                }) {
                    Text("Go Back", color = TextTertiary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = SleepGreen.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Your PIN is stored securely with SHA-256 encryption",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on Settings screen
    val showBottomBar = currentRoute?.contains("SettingsRoute") != true

    Scaffold(
        containerColor = NightBlack,
        bottomBar = {
            if (showBottomBar) {
                SleepBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavGraph(navController = navController)
        }
    }
}

@Composable
fun SleepBottomNavBar(
    currentRoute: String?,
    onNavigate: (Any) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, NightBlack.copy(alpha = 0.95f)),
                    startY = 0f,
                    endY = 40f
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DeepNight.copy(alpha = 0.95f),
                            DuskPurple.copy(alpha = 0.6f),
                            DeepNight.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                // Match by checking if the route class name is in the current destination
                val routeName = item.route::class.simpleName ?: ""
                val selected = currentRoute?.contains(routeName) == true

                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.1f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "scale"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (selected) SoftPurple else TextTertiary,
                    label = "iconColor"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (selected) TextPrimary else TextTertiary,
                    label = "labelColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onNavigate(item.route) }
                        .then(
                            if (selected) Modifier
                                .background(
                                    SoftPurple.copy(alpha = 0.12f),
                                    RoundedCornerShape(16.dp)
                                )
                            else Modifier
                        )
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )

                    // Active indicator dot
                    if (selected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SunriseYellow)
                        )
                    }
                }
            }
        }
    }
}
