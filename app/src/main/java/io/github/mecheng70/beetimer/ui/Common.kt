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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import io.github.mecheng70.beetimer.ui.theme.AppIcons
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

enum class ChipKind { TIME, STATE }

/** Chip for a countdown ([ChipKind.TIME], bold 20 sp, 36 dp tall) or a state ([ChipKind.STATE], 14 sp). */
@Composable
fun Chip(text: String, tone: Tone, modifier: Modifier = Modifier, kind: ChipKind = ChipKind.STATE, strong: Boolean = false) {
    val (bg, fg) = toneColors(tone)
    val fill = if (strong && tone == Tone.DEVICE) BeeTheme.colors.deviceStrong else bg
    val time = kind == ChipKind.TIME
    Box(
        modifier
            .heightIn(min = if (time) Dimens.TimeChipMinHeight else Dimens.ChipMinHeight)
            .background(fill, RoundedCornerShape(Dimens.SmallCorner))
            .padding(horizontal = if (time) Dimens.TimeChipPaddingH else Dimens.ChipPaddingH, vertical = Dimens.ChipPaddingV),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = if (time) AppType.timeChip else AppType.stateChip, color = fg, maxLines = 1)
    }
}

/** Icon with a 48 dp touch target. */
@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = BeeTheme.colors.ink, enabled: Boolean = true) {
    Box(
        Modifier
            .size(Dimens.IconTouch)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Dimens.Icon))
    }
}

/** Top bar on the page background: optional back arrow, title, optional trailing icon. */
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    backDescription: String = "",
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Dimens.TopBarHeight).padding(horizontal = Dimens.Gap / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconAction(AppIcons.ArrowBack, backDescription, onBack)
        Text(
            title,
            style = AppType.title,
            color = BeeTheme.colors.ink,
            modifier = Modifier.weight(1f).padding(start = if (onBack == null) Dimens.Gap * 1.5f else Dimens.Gap),
        )
        action?.invoke()
    }
}

/**
 * Column whose rows can be reordered by long-pressing a drag handle. Rows may differ in
 * height. [onMove] gets the new order when the drag ends.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, handle: Modifier, dragging: Boolean) -> Unit,
) {
    val order = remember { mutableStateListOf<T>() }
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableMapOf<Any, Int>() }
    if (draggingKey == null && order.toList() != items) {
        order.clear()
        order.addAll(items)
    }
    val latestOnMove by rememberUpdatedState(onMove)
    Column(modifier) {
        order.forEach { item ->
            val k = key(item)
            key(k) {
                val dragging = draggingKey == k
                val handle = Modifier.pointerInput(k) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { draggingKey = k; offset = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            offset += amount.y
                            val i = order.indexOfFirst { key(it) == k }
                            if (offset > 0 && i < order.lastIndex) {
                                val h = heights[key(order[i + 1])] ?: 0
                                if (offset > h / 2f) { order.add(i + 1, order.removeAt(i)); offset -= h }
                            } else if (offset < 0 && i > 0) {
                                val h = heights[key(order[i - 1])] ?: 0
                                if (-offset > h / 2f) { order.add(i - 1, order.removeAt(i)); offset += h }
                            }
                        },
                        onDragEnd = { draggingKey = null; offset = 0f; latestOnMove(order.toList()) },
                        onDragCancel = { draggingKey = null; offset = 0f; latestOnMove(order.toList()) },
                    )
                }
                Box(
                    Modifier
                        .onSizeChanged { heights[k] = it.height }
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) offset else 0f }
                        .background(if (dragging) BeeTheme.colors.idleBg else Color.Transparent, RoundedCornerShape(Dimens.SmallCorner)),
                ) { row(item, handle, dragging) }
            }
        }
    }
}

/** The drag handle icon, to be given the handle modifier from [ReorderableColumn]. */
@Composable
fun DragHandle(modifier: Modifier, description: String) {
    Box(
        modifier.size(Dimens.IconTouch).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.DragHandle, contentDescription = null, tint = BeeTheme.colors.inkMuted, modifier = Modifier.size(Dimens.Icon))
    }
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
