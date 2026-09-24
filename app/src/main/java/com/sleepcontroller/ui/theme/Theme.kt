package com.sleepcontroller.ui.theme

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val SleepColorScheme = darkColorScheme(
    primary = SoftPurple,
    onPrimary = TextPrimary,
    primaryContainer = DuskPurple,
    onPrimaryContainer = LightPurple,
    secondary = SunriseYellow,
    onSecondary = NightBlack,
    secondaryContainer = DuskPurple,
    onSecondaryContainer = SunriseYellow,
    tertiary = SleepBlue,
    onTertiary = TextPrimary,
    tertiaryContainer = DuskPurple,
    onTertiaryContainer = SleepBlue,
    error = SleepRed,
    onError = TextPrimary,
    errorContainer = Color(0xFF3D1111),
    onErrorContainer = SleepRed,
    background = NightBlack,
    onBackground = TextPrimary,
    surface = DeepNight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = TextTertiary,
    inverseSurface = TextPrimary,
    inverseOnSurface = NightBlack,
    inversePrimary = DuskPurple,
    surfaceTint = SoftPurple,
)

@Composable
fun SleepControllerTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? ComponentActivity
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(NightBlack.toArgb()),
                navigationBarStyle = SystemBarStyle.dark(NightBlack.toArgb())
            )
        }
    }

    MaterialTheme(
        colorScheme = SleepColorScheme,
        typography = SleepTypography,
        shapes = SleepShapes,
        content = content
    )
}
