package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.data.RobotMode

@Composable
fun RobotCompanionTheme(
    mode: RobotMode = RobotMode.MANUAL,
    content: @Composable () -> Unit
) {
    val isAuto = mode == RobotMode.AUTO

    // Smooth animation between Dark Green (MANUAL) and Dark Blue (AUTO)
    val animatedBg by animateColorAsState(
        targetValue = if (isAuto) DarkBlueBackground else DarkGreenBackground,
        animationSpec = tween(durationMillis = 600),
        label = "bg_anim"
    )

    val animatedSurface by animateColorAsState(
        targetValue = if (isAuto) DarkBlueSurface else DarkGreenSurface,
        animationSpec = tween(durationMillis = 600),
        label = "surface_anim"
    )

    val animatedPrimary by animateColorAsState(
        targetValue = if (isAuto) DarkBlueAccent else DarkGreenAccent,
        animationSpec = tween(durationMillis = 600),
        label = "primary_anim"
    )

    val animatedCard by animateColorAsState(
        targetValue = if (isAuto) DarkBlueCard else DarkGreenCard,
        animationSpec = tween(durationMillis = 600),
        label = "card_anim"
    )

    val colorScheme = darkColorScheme(
        primary = animatedPrimary,
        onPrimary = Color.Black,
        primaryContainer = animatedCard,
        onPrimaryContainer = TextPrimary,
        secondary = if (isAuto) DarkBlueAccentDark else DarkGreenAccentDark,
        onSecondary = Color.White,
        background = animatedBg,
        onBackground = TextPrimary,
        surface = animatedSurface,
        onSurface = TextPrimary,
        surfaceVariant = animatedCard,
        onSurfaceVariant = TextSecondary,
        outline = if (isAuto) DarkBlueBorder else DarkGreenBorder,
        error = StatusOfflineRed,
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
