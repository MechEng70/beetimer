package io.github.mecheng70.beetimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.mecheng70.beetimer.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Every color, text style and size in the app. Values come from the approved mockups
// (docs/mockups/varroa-timer-mockups.html). Screens use these tokens, never literals.
// Semantics: amber = device-bound step holding the vaporizer, teal = passive sealed wait,
// gray = queued or not started. No dynamic color.

/** All app colors for one theme. */
@Immutable
data class AppColors(
    val page: Color,
    val card: Color,
    val cardBorder: Color,
    val ink: Color,
    val inkMuted: Color,
    val rowDivider: Color,
    val tabDivider: Color,
    val buttonBorder: Color,
    val buttonFill: Color,
    val onButtonFill: Color,
    val deviceBg: Color,
    val deviceFg: Color,
    val deviceStrong: Color,
    val passiveBg: Color,
    val passiveFg: Color,
    val idleBg: Color,
    val idleFg: Color,
    val danger: Color,
    val done: Color,
    val isDark: Boolean,
)

private val Light = AppColors(
    page = Color(0xFFF4F4F2),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFD3D1C7),
    ink = Color(0xFF1F1F1D),
    inkMuted = Color(0xFF5F5E5A),
    rowDivider = Color(0xFFEEECE6),
    tabDivider = Color(0xFFE5E3DC),
    buttonBorder = Color(0xFFB4B2A9),
    buttonFill = Color(0xFF1F1F1D),
    onButtonFill = Color(0xFFFFFFFF),
    deviceBg = Color(0xFFFAEEDA),
    deviceFg = Color(0xFF633806),
    deviceStrong = Color(0xFFFAC775),
    passiveBg = Color(0xFFE1F5EE),
    passiveFg = Color(0xFF085041),
    idleBg = Color(0xFFF1EFE8),
    idleFg = Color(0xFF5F5E5A),
    danger = Color(0xFFA32D2D),
    done = Color(0xFF3B6D11),
    isDark = false,
)

private val Dark = AppColors(
    page = Color(0xFF121210),
    card = Color(0xFF1C1B18),
    cardBorder = Color(0xFF3A3934),
    ink = Color(0xFFF1EFE8),
    inkMuted = Color(0xFFB4B2A9),
    rowDivider = Color(0xFF2C2B27),
    tabDivider = Color(0xFF34332E),
    buttonBorder = Color(0xFF5F5E5A),
    buttonFill = Color(0xFFF1EFE8),
    onButtonFill = Color(0xFF1F1F1D),
    deviceBg = Color(0xFF4A2D05),
    deviceFg = Color(0xFFFAEEDA),
    deviceStrong = Color(0xFF7A4A0C),
    passiveBg = Color(0xFF0B3A30),
    passiveFg = Color(0xFFA8E6D2),
    idleBg = Color(0xFF2C2B27),
    idleFg = Color(0xFFB4B2A9),
    danger = Color(0xFFFF8A80),
    done = Color(0xFF9CD67A),
    isDark = true,
)

/** Alarm screen: dark amber with light text in both themes (mockup 3). */
object AlarmColors {
    val Background = Color(0xFF412402)
    val Text = Color(0xFFFAEEDA)
    val Accent = Color(0xFFFAC775)
    val Border = Color(0xFFBA7517)
}

/** Inter, bundled in res/font (SIL OFL 1.1, assets/licenses/Inter-OFL.txt) so weights render the same on every phone. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun inter(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Inter, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

/** Text styles. Only the Hive tab countdown and the alarm hive name are large. */
object AppType {
    val body = inter(14, 20)
    val label = inter(13, 18)
    val hiveName = inter(15, 20, FontWeight.SemiBold)
    val status = inter(14, 20, FontWeight.Medium)
    val statusSub = inter(13, 18)
    val tab = inter(14, 20, FontWeight.Medium)
    val button = inter(14, 20, FontWeight.Medium)
    val gloveButton = inter(16, 22, FontWeight.SemiBold)
    val title = inter(18, 24, FontWeight.SemiBold)
    val section = inter(14, 20, FontWeight.SemiBold)
    val stateLine = inter(16, 22, FontWeight.SemiBold)
    /** Countdown and clock-time chips: read at a glance in sun. */
    val timeChip = inter(15, 20, FontWeight.Bold)
    /** Device, Passive, Queued chips. */
    val stateChip = inter(14, 18, FontWeight.Medium)
    val comingTime = inter(14, 20, FontWeight.Bold)
    val countdown = TextStyle(fontFamily = Inter, fontSize = 52.sp, lineHeight = 1.2.em, fontWeight = FontWeight.Bold)
    val alarmName = TextStyle(fontFamily = Inter, fontSize = 48.sp, lineHeight = 1.15.em, fontWeight = FontWeight.Bold)
    val alarmFinished = inter(16, 22)
    val alarmLabel = inter(14, 20)
    val alarmNext = inter(26, 32, FontWeight.SemiBold)
    val alarmTimer = inter(16, 22, FontWeight.Medium)
    val alarmNote = inter(14, 20)
    val alarmButton = inter(17, 22, FontWeight.Bold)
}

/** Sizes shared by every screen. */
object Dimens {
    val PageMargin = 8.dp
    val CardPadding = 12.dp
    val CardCorner = 16.dp
    val SmallCorner = 8.dp
    val AlarmCorner = 12.dp
    val Border = 1.dp
    val SelectedBorder = 2.dp
    val RowPadding = 10.dp
    val Gap = 8.dp
    val SectionGap = 12.dp
    val ButtonHeight = 44.dp
    val ButtonPadding = 12.dp
    /** Hive tab +1 min and Skip (gloves). */
    val GloveButton = 56.dp
    /** Alarm screen Done, +1 min and Snooze (gloves). */
    val AlarmButton = 64.dp
    val TabGap = 14.dp
    val TabUnderline = 2.dp
    val TabPadding = 8.dp
    val ChipPaddingH = 10.dp
    val ChipPaddingV = 4.dp
    val ChipMinHeight = 28.dp
    val IconTouch = 48.dp
    val TopBarHeight = 56.dp
    val Icon = 24.dp
    val StepIconButton = 44.dp
    val StatusPaddingH = 12.dp
    val StatusPaddingV = 10.dp
    val StepIcon = 20.dp
    val ComingTimeWidth = 64.dp
}

val LocalAppColors = staticCompositionLocalOf { Light }

object BeeTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private fun materialScheme(c: AppColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.ink, onPrimary = c.card,
        background = c.page, onBackground = c.ink,
        surface = c.card, onSurface = c.ink,
        surfaceVariant = c.idleBg, onSurfaceVariant = c.inkMuted,
        surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerHighest = c.card,
        outline = c.buttonBorder, outlineVariant = c.rowDivider, error = c.danger,
    )
} else {
    lightColorScheme(
        primary = c.ink, onPrimary = c.card,
        background = c.page, onBackground = c.ink,
        surface = c.card, onSurface = c.ink,
        surfaceVariant = c.idleBg, onSurfaceVariant = c.inkMuted,
        surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerHighest = c.card,
        outline = c.buttonBorder, outlineVariant = c.rowDivider, error = c.danger,
    )
}

/** Material defaults (dialogs, text fields) brought down to the app's sizes. */
private val MaterialTypography = Typography(
    headlineSmall = AppType.title,
    titleLarge = AppType.title,
    titleMedium = AppType.hiveName,
    titleSmall = AppType.section,
    bodyLarge = AppType.body,
    bodyMedium = AppType.body,
    bodySmall = AppType.label,
    labelLarge = AppType.button,
    labelMedium = AppType.label,
    labelSmall = AppType.label,
)

@Composable
fun BeeTimerTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) Dark else Light
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = materialScheme(colors), typography = MaterialTypography, content = content)
    }
}
