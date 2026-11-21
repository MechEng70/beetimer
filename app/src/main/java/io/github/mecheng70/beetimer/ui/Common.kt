package io.github.mecheng70.beetimer.ui

import android.content.Context
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.mecheng70.beetimer.BeeTimerApp
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.HiveRun
import io.github.mecheng70.beetimer.scheduler.RunState
import io.github.mecheng70.beetimer.ui.theme.Dimens
import io.github.mecheng70.beetimer.ui.theme.LocalStatusColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MS_PER_S = 1000L
private const val S_PER_MIN = 60L
private const val S_PER_H = 3600L

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
    val c = LocalStatusColors.current
    return when (tone) {
        Tone.DEVICE -> c.deviceBg to c.deviceFg
        Tone.PASSIVE -> c.passiveBg to c.passiveFg
        Tone.IDLE -> c.idleBg to c.idleFg
    }
}

@Composable
fun Pill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val (bg, fg) = toneColors(tone)
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(Dimens.Corner), modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = Dimens.TouchMin)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BigOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = Dimens.TouchMin)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}
