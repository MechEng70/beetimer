package io.github.mecheng70.beetimer.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import io.github.mecheng70.beetimer.BeeTimerApp
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.HiveRun
import io.github.mecheng70.beetimer.scheduler.RunState
import io.github.mecheng70.beetimer.ui.theme.AppType
import io.github.mecheng70.beetimer.ui.theme.BeeTheme
import io.github.mecheng70.beetimer.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MS_PER_S = 1000L
private const val S_PER_MIN = 60L
private const val S_PER_H = 3600L
private const val DISABLED_ALPHA = 0.4f

val Context.app: BeeTimerApp get() = applicationContext as BeeTimerApp

fun BeeTimerApp.send(event: Event) {
    scope.launch { engine.dispatch(event) }
}

/** "1:42" or "1:02:05". Never negative. */
fun formatCountdown(ms: Long): String {
    val total = (ms.coerceAtLeast(0) + MS_PER_S - 1) / MS_PER_S
    val h = total / S_PER_H
    val m = (total % S_PER_H) / S_PER_MIN
    val s = total % S_PER_MIN
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatDuration(seconds: Int): String = formatCountdown(seconds * MS_PER_S)

/** Step names and instructions read mid-sentence: "Step 3 of 7, heat and vaporize". */
fun String.midSentence(): String = replaceFirstChar { it.lowercaseChar() }

/** Wall clock that ticks every second, for countdowns. */
@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(MS_PER_S - value % MS_PER_S)
    }
}

enum class Tone { DEVICE, PASSIVE, IDLE }

/** Amber while holding the vaporizer, teal while sealed and waiting, gray otherwise. */
fun toneOf(run: HiveRun?): Tone = when {
    run == null || run.isTerminal || run.state == RunState.WAITING_FOR_DEVICE -> Tone.IDLE
    run.currentStep?.deviceBound == true -> Tone.DEVICE
    else -> Tone.PASSIVE
}

@Composable
fun toneColors(tone: Tone): Pair<Color, Color> {
    val c = BeeTheme.colors
    return when (tone) {
        Tone.DEVICE -> c.deviceBg to c.deviceFg
        Tone.PASSIVE -> c.passiveBg to c.passiveFg
        Tone.IDLE -> c.idleBg to c.idleFg
    }
}

/** White card with a thin border, like the mockup phone frame. */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = BeeTheme.colors
    val shape = RoundedCornerShape(Dimens.CardCorner)
    Column(
        modifier
            .clip(shape)
            .background(c.card)
            .border(Dimens.Border, c.cardBorder, shape)
            .padding(Dimens.CardPadding),
        content = content,
    )
}

/** Small state or countdown chip. [strong] is the deeper amber of the current device step. */
@Composable
fun Chip(text: String, tone: Tone, modifier: Modifier = Modifier, strong: Boolean = false) {
    val (bg, fg) = toneColors(tone)
    val fill = if (strong && tone == Tone.DEVICE) BeeTheme.colors.deviceStrong else bg
    Text(
        text,
        style = AppType.chip,
        color = fg,
        modifier = modifier
            .background(fill, RoundedCornerShape(Dimens.SmallCorner))
            .padding(horizontal = Dimens.ChipPaddingH, vertical = Dimens.ChipPaddingV),
    )
}

enum class ButtonKind { OUTLINED, FILLED, DANGER }

/** Mockup button: white, thin gray border, 8 dp corners, as wide as its text unless told otherwise. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.OUTLINED,
    enabled: Boolean = true,
    minHeight: Dp = Dimens.ButtonHeight,
    style: TextStyle = AppType.button,
    selected: Boolean = false,
) {
    val c = BeeTheme.colors
    val shape = RoundedCornerShape(Dimens.SmallCorner)
    val bg = if (kind == ButtonKind.FILLED) c.buttonFill else c.card
    val fg = when (kind) {
        ButtonKind.FILLED -> c.onButtonFill
        ButtonKind.DANGER -> c.danger
        ButtonKind.OUTLINED -> c.ink
    }
    val border = when {
        kind == ButtonKind.FILLED -> null
        selected -> BorderStroke(Dimens.SelectedBorder, c.ink)
        else -> BorderStroke(Dimens.Border, c.buttonBorder)
    }
    Box(
        modifier
            .heightIn(min = minHeight)
            .clip(shape)
            .background(bg)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = Dimens.ButtonPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = style, color = fg)
    }
}

/** Plain text action, for Settings, Edit, Up, Down and Delete. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, color: Color = BeeTheme.colors.ink, enabled: Boolean = true) {
    Text(
        text,
        style = AppType.button,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.SmallCorner))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = Dimens.Gap, vertical = Dimens.RowPadding),
    )
}

@Composable
fun RowDivider() = HorizontalDivider(thickness = Dimens.Border, color = BeeTheme.colors.rowDivider)

/** Underlined tab row (mockup): plain labels, the selected one dark with a 2 dp underline. */
@Composable
fun TabStrip(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = BeeTheme.colors
    Box(modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.align(Alignment.BottomStart), thickness = Dimens.Border, color = c.tabDivider)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.TabGap),
        ) {
            labels.forEachIndexed { i, label ->
                val on = i == selected
                Text(
                    label,
                    style = AppType.tab,
                    color = if (on) c.ink else c.inkMuted,
                    modifier = Modifier
                        .clickable { onSelect(i) }
                        .drawBehind {
                            if (on) {
                                val h = Dimens.TabUnderline.toPx()
                                drawLine(c.ink, Offset(0f, size.height - h / 2), Offset(size.width, size.height - h / 2), h)
                            }
                        }
                        .padding(vertical = Dimens.TabPadding),
                )
            }
        }
    }
}

@Composable
fun VSpace(height: Dp) = Box(Modifier.height(height))
