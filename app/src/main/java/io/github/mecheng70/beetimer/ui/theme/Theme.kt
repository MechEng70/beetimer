package io.github.mecheng70.beetimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// All colors and type for the app. Nothing inline in screens.
// Semantics (mockups): amber = device-bound step holding the vaporizer,
// teal = passive sealed wait, gray = queued or not started.

private object Palette {
    val Ink = Color(0xFF1F1F1D)
    val InkMuted = Color(0xFF5F5E5A)
    val Paper = Color(0xFFF4F4F2)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFD3D1C7)

    val AmberBg = Color(0xFFFAEEDA)
    val AmberFg = Color(0xFF633806)
    val AmberStrong = Color(0xFFFAC775)
    val AmberDeep = Color(0xFF412402)
    val AmberBorder = Color(0xFFBA7517)

    val TealBg = Color(0xFFE1F5EE)
    val TealFg = Color(0xFF085041)

    val GrayBg = Color(0xFFF1EFE8)
    val GrayFg = Color(0xFF5F5E5A)

    val Danger = Color(0xFFA32D2D)
    val Done = Color(0xFF3B6D11)

    val DarkBg = Color(0xFF121210)
    val DarkCard = Color(0xFF1E1D1A)
    val DarkInk = Color(0xFFF1EFE8)
    val DarkInkMuted = Color(0xFFB4B2A9)
    val DarkAmberBg = Color(0xFF4A2D05)
    val DarkTealBg = Color(0xFF0B3A30)
    val DarkTealFg = Color(0xFFA8E6D2)
    val DarkGrayBg = Color(0xFF2C2B27)
    val DarkDanger = Color(0xFFFF8A80)
    val DarkDone = Color(0xFF9CD67A)
}

/** Status colors that Material 3 has no role for. */
@Immutable
data class StatusColors(
    val deviceBg: Color,
    val deviceFg: Color,
    val deviceStrong: Color,
    val passiveBg: Color,
    val passiveFg: Color,
    val idleBg: Color,
    val idleFg: Color,
    val danger: Color,
    val done: Color,
)

/** Alarm screen: dark amber with light text in both themes (mockup 3). */
object AlarmColors {
    val Background = Palette.AmberDeep
    val Text = Palette.AmberBg
    val Accent = Palette.AmberStrong
    val Border = Palette.AmberBorder
}

private val LightStatus = StatusColors(
    deviceBg = Palette.AmberBg, deviceFg = Palette.AmberFg, deviceStrong = Palette.AmberStrong,
    passiveBg = Palette.TealBg, passiveFg = Palette.TealFg,
    idleBg = Palette.GrayBg, idleFg = Palette.GrayFg,
    danger = Palette.Danger, done = Palette.Done,
)

private val DarkStatus = StatusColors(
    deviceBg = Palette.DarkAmberBg, deviceFg = Palette.AmberBg, deviceStrong = Palette.AmberStrong,
    passiveBg = Palette.DarkTealBg, passiveFg = Palette.DarkTealFg,
    idleBg = Palette.DarkGrayBg, idleFg = Palette.DarkInkMuted,
    danger = Palette.DarkDanger, done = Palette.DarkDone,
)

private val LightScheme = lightColorScheme(
    primary = Palette.Ink, onPrimary = Palette.Card,
    secondary = Palette.AmberFg, onSecondary = Palette.Card,
    background = Palette.Paper, onBackground = Palette.Ink,
    surface = Palette.Card, onSurface = Palette.Ink,
    surfaceVariant = Palette.GrayBg, onSurfaceVariant = Palette.InkMuted,
    outline = Palette.Line, error = Palette.Danger,
)

private val DarkScheme = darkColorScheme(
    primary = Palette.DarkInk, onPrimary = Palette.DarkBg,
    secondary = Palette.AmberStrong, onSecondary = Palette.AmberDeep,
    background = Palette.DarkBg, onBackground = Palette.DarkInk,
    surface = Palette.DarkCard, onSurface = Palette.DarkInk,
    surfaceVariant = Palette.DarkGrayBg, onSurfaceVariant = Palette.DarkInkMuted,
    outline = Palette.DarkGrayBg, error = Palette.DarkDanger,
)

/** Large type for bright sun and gloves. */
private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.SemiBold, lineHeight = 64.sp),
    displayMedium = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.SemiBold, lineHeight = 56.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    labelMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
)

/** Sizes shared by every screen. */
object Dimens {
    val TouchMin = 64.dp
    val TouchAlarm = 72.dp
    val Gutter = 16.dp
    val Gap = 12.dp
    val Corner = 12.dp
}

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun BeeTimerTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalStatusColors provides if (dark) DarkStatus else LightStatus,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
