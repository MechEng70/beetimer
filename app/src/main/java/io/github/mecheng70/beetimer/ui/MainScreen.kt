package io.github.mecheng70.beetimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.Hive
import io.github.mecheng70.beetimer.scheduler.HiveRun
import io.github.mecheng70.beetimer.scheduler.RunState
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.scheduler.StepKind
import io.github.mecheng70.beetimer.ui.theme.AppType
import io.github.mecheng70.beetimer.ui.theme.BeeTheme
import io.github.mecheng70.beetimer.ui.theme.Dimens

/** Placeholder swapped for the bold countdown in the status line. */
private const val COUNTDOWN_MARK = "\u0001"

@Composable
fun MainScreen(onSettings: () -> Unit) {
    val app = LocalContext.current.app
    val state by app.engine.state.collectAsStateWithLifecycle()
    val hives by app.repo.hives.collectAsStateWithLifecycle(emptyList())
    val now by rememberNow()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val s = state

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.app_name), action = stringResource(R.string.action_settings), onAction = onSettings)
        AppCard(Modifier.fillMaxWidth().padding(horizontal = Dimens.PageMargin).padding(bottom = Dimens.PageMargin)) {
            if (s == null) return@AppCard
            val labels = hives.associate { it.id to it.label }
            DeviceStatusBar(s, labels, now)
            if (hives.isEmpty()) {
                VSpace(Dimens.SectionGap)
                Text(stringResource(R.string.no_hives), style = AppType.body, color = BeeTheme.colors.inkMuted)
                return@AppCard
            }
            val selected = tab.coerceIn(0, hives.size)
            TabStrip(
                labels = listOf(stringResource(R.string.tab_all)) + hives.map { it.label },
                selected = selected,
                onSelect = { tab = it },
                modifier = Modifier.padding(vertical = Dimens.RowPadding),
            )
            if (selected == 0) {
                AllTab(hives, s, now, onSelect = { tab = hives.indexOf(it) + 1 })
            } else {
                HiveTab(hives[selected - 1], s, now)
            }
        }
    }
}

/** App name (or Back) on the left, one text action on the right, on the page background. */
@Composable
fun ScreenHeader(title: String, action: String? = null, onAction: () -> Unit = {}, onBack: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.PageMargin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) TextAction(stringResource(R.string.action_back), onBack)
        Text(
            title,
            style = AppType.title,
            color = BeeTheme.colors.ink,
            modifier = Modifier.weight(1f).padding(horizontal = if (onBack == null) Dimens.Gap else 0.dp),
        )
        if (action != null) TextAction(action, onAction)
    }
}

@Composable
private fun DeviceStatusBar(s: SchedulerState, labels: Map<Long, String>, now: Long) {
    val app = LocalContext.current.app
    val holder = s.lockHolder?.let { s.runs[it] }
    val head = s.queue.firstOrNull()
    val (bg, fg) = toneColors(if (holder != null) Tone.DEVICE else Tone.IDLE)
    Column(
        Modifier.fillMaxWidth()
            .background(bg, RoundedCornerShape(Dimens.SmallCorner))
            .padding(horizontal = Dimens.StatusPaddingH, vertical = Dimens.StatusPaddingV),
    ) {
        val ends = holder?.stepEndsAt
        val line = when {
            holder != null && holder.state == RunState.RUNNING && ends != null -> {
                val template = stringResource(
                    R.string.device_busy_timed,
                    labels[holder.hiveId].orEmpty(),
                    holder.currentStep?.name.orEmpty().midSentence(),
                    COUNTDOWN_MARK,
                )
                buildAnnotatedString {
                    append(template.substringBefore(COUNTDOWN_MARK))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formatCountdown(ends - now)) }
                    append(template.substringAfter(COUNTDOWN_MARK))
                }
            }
            holder != null -> buildAnnotatedString {
                append(stringResource(R.string.device_busy, labels[holder.hiveId].orEmpty(), holder.currentStep?.name.orEmpty().midSentence()))
            }
            head != null -> buildAnnotatedString { append(stringResource(R.string.device_free_next, labels[head].orEmpty())) }
            else -> buildAnnotatedString { append(stringResource(R.string.device_free)) }
        }
        Text(line, style = AppType.status, color = fg)
        val waiting = if (holder != null) s.queue else s.queue.drop(1)
        if (waiting.isNotEmpty()) {
            Text(stringResource(R.string.device_next, waiting.joinToString(", ") { labels[it].orEmpty() }), style = AppType.statusSub, color = fg)
        }
        if (holder == null && head != null) {
            VSpace(Dimens.Gap)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                AppButton(stringResource(R.string.action_start), { app.send(Event.Start(head)) }, kind = ButtonKind.FILLED)
                if (s.queue.size > 1) AppButton(stringResource(R.string.action_queue_skip), { app.send(Event.QueueSkipHead) })
                AppButton(stringResource(R.string.action_queue_remove), { app.send(Event.QueueRemoveHead) })
            }
        }
    }
}

@Composable
private fun AllTab(hives: List<Hive>, s: SchedulerState, now: Long, onSelect: (Hive) -> Unit) {
    val app = LocalContext.current.app
    val c = BeeTheme.colors
    Column(Modifier.fillMaxWidth()) {
        hives.forEachIndexed { i, hive ->
            val run = s.runs[hive.id]
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(hive) }.padding(vertical = Dimens.RowPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(hive.label, style = AppType.hiveName, color = c.ink)
                    Text(subtitle(run), style = AppType.body, color = c.inkMuted)
                }
                RowAction(hive.id, run, s, now, onEvent = { app.send(it) })
            }
            if (i < hives.lastIndex) RowDivider()
        }
    }
}

@Composable
private fun subtitle(run: HiveRun?): String {
    val ringing = run?.ringing
    return when {
        run == null -> stringResource(R.string.state_not_started)
        run.state == RunState.CANCELLED -> stringResource(R.string.state_cancelled)
        ringing != null && run.currentStepIndex == ringing.finishedStepIndex ->
            stringResource(R.string.state_finished, run.steps[ringing.finishedStepIndex].name)
        run.state == RunState.DONE -> stringResource(R.string.state_done)
        run.state == RunState.WAITING_FOR_DEVICE -> stringResource(R.string.state_waiting)
        else -> run.currentStep?.name.orEmpty()
    }
}

/** The one thing you can do now for this hive, or its countdown chip. */
@Composable
private fun RowAction(hiveId: Long, run: HiveRun?, s: SchedulerState, now: Long, onEvent: (Event) -> Unit) {
    val headReady = s.lockHolder == null && s.queue.firstOrNull() == hiveId
    val ends = run?.stepEndsAt
    when {
        run == null || run.state == RunState.CANCELLED ->
            AppButton(stringResource(R.string.action_start), { onEvent(Event.Start(hiveId)) })
        run.ringing != null || run.state == RunState.AWAITING_CONFIRM ->
            AppButton(stringResource(R.string.action_done), { onEvent(Event.Done(hiveId)) })
        run.state == RunState.WAITING_FOR_DEVICE && headReady ->
            AppButton(stringResource(R.string.action_start), { onEvent(Event.Start(hiveId)) })
        run.state == RunState.WAITING_FOR_DEVICE -> Chip(stringResource(R.string.state_queued), Tone.IDLE)
        run.state == RunState.RUNNING && ends != null -> Chip(formatCountdown(ends - now), toneOf(run))
        run.state == RunState.DONE -> Chip(stringResource(R.string.state_done), Tone.IDLE)
    }
}

@Composable
private fun HiveTab(hive: Hive, s: SchedulerState, now: Long) {
    val app = LocalContext.current.app
    val c = BeeTheme.colors
    val run = s.runs[hive.id]
    val steps = run?.steps ?: s.template.steps
    Column(Modifier.fillMaxWidth()) {
        // Header: step, big countdown, instruction.
        Column(
            Modifier.fillMaxWidth().padding(top = Dimens.Gap, bottom = Dimens.SectionGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val ringing = run?.ringing
            val current = run?.currentStep
            val ends = run?.stepEndsAt
            if (run != null && current != null && !run.isTerminal) {
                Text(
                    stringResource(R.string.hive_step_of, run.currentStepIndex + 1, run.steps.size, current.name.midSentence()),
                    style = AppType.label,
                    color = c.inkMuted,
                )
            }
            val counting = run != null && ringing == null && run.state == RunState.RUNNING && ends != null
            if (counting && ends != null) {
                Text(formatCountdown(ends - now), style = AppType.countdown, color = c.ink)
            } else {
                val line = when {
                    run == null || run.state == RunState.CANCELLED -> stringResource(R.string.hive_ready)
                    run.state == RunState.DONE && ringing == null -> stringResource(R.string.hive_all_done)
                    ringing != null -> stringResource(R.string.state_finished, run.steps[ringing.finishedStepIndex].name)
                    run.state == RunState.WAITING_FOR_DEVICE ->
                        if (s.lockHolder == null && s.queue.firstOrNull() == hive.id) stringResource(R.string.hive_waiting_head)
                        else stringResource(R.string.hive_waiting)
                    else -> stringResource(R.string.hive_tap_done)
                }
                Text(line, style = AppType.stateLine, color = c.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = Dimens.Gap))
            }
            if (ringing?.overdue == true) Chip(stringResource(R.string.pill_overdue), Tone.DEVICE, strong = true)
            if (current != null && run.isActive && current.instruction.isNotBlank()) {
                Text(current.instruction, style = AppType.label, color = c.inkMuted, textAlign = TextAlign.Center)
            }
        }

        // Step list.
        steps.forEachIndexed { i, step ->
            val isCurrent = run != null && !run.isTerminal && i == run.currentStepIndex
            val isDone = run != null && (i < run.currentStepIndex || run.state == RunState.DONE)
            val tone = if (step.deviceBound) Tone.DEVICE else Tone.PASSIVE
            val (rowBg, rowFg) = toneColors(tone)
            Row(
                Modifier.fillMaxWidth()
                    .then(if (isCurrent) Modifier.background(rowBg, RoundedCornerShape(Dimens.SmallCorner)).padding(horizontal = Dimens.Gap) else Modifier)
                    .padding(vertical = Dimens.RowPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when {
                        isDone -> "✓"
                        isCurrent -> "▶"
                        else -> "○"
                    },
                    style = AppType.body,
                    color = when {
                        isDone -> c.done
                        isCurrent -> rowFg
                        else -> c.ink
                    },
                    modifier = Modifier.width(Dimens.StepIcon),
                )
                val name = if (step.kind == StepKind.TIMED) {
                    stringResource(R.string.step_with_duration, step.name, formatDuration(step.durationSeconds ?: 0))
                } else {
                    step.name
                }
                Text(
                    name,
                    style = AppType.body,
                    color = when {
                        isCurrent -> rowFg
                        isDone -> c.inkMuted
                        else -> c.ink
                    },
                    modifier = Modifier.weight(1f).padding(start = Dimens.Gap),
                )
                if (step.deviceBound) {
                    Chip(stringResource(R.string.pill_device), Tone.DEVICE, strong = isCurrent)
                } else if (step.kind == StepKind.TIMED) {
                    Chip(stringResource(R.string.pill_passive), Tone.PASSIVE)
                }
            }
            if (!isCurrent && i < steps.lastIndex) RowDivider()
        }

        VSpace(Dimens.RowPadding)
        HiveActions(hive.id, run, s, onEvent = { app.send(it) })
        VSpace(Dimens.Gap)
    }
}

@Composable
private fun HiveActions(hiveId: Long, run: HiveRun?, s: SchedulerState, onEvent: (Event) -> Unit) {
    val full = Modifier.fillMaxWidth()
    val glove = Dimens.GloveButton
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
        if (run == null || run.isTerminal && run.ringing == null) {
            if (run?.state != RunState.DONE) {
                AppButton(stringResource(R.string.action_start), { onEvent(Event.Start(hiveId)) }, full, kind = ButtonKind.FILLED)
            }
            return@Column
        }
        if (run.state == RunState.WAITING_FOR_DEVICE) {
            val ready = s.lockHolder == null && s.queue.firstOrNull() == hiveId
            AppButton(stringResource(R.string.action_start), { onEvent(Event.Start(hiveId)) }, full, kind = ButtonKind.FILLED, enabled = ready)
            if (run.ringing != null) AppButton(stringResource(R.string.action_done), { onEvent(Event.Done(hiveId)) }, full)
        } else {
            val ringing = run.ringing != null
            val canConfirm = ringing || run.state == RunState.AWAITING_CONFIRM
            val canExtend = ringing || run.state == RunState.RUNNING
            val canSkip = run.state == RunState.RUNNING || run.state == RunState.AWAITING_CONFIRM
            val buttons = buildList<@Composable (Modifier) -> Unit> {
                if (canConfirm) add { m -> AppButton(stringResource(R.string.action_done), { onEvent(Event.Done(hiveId)) }, m, ButtonKind.FILLED, minHeight = glove, style = AppType.gloveButton) }
                if (canExtend) add { m -> AppButton(stringResource(R.string.action_add_minute), { onEvent(Event.AddMinute(hiveId)) }, m, minHeight = glove, style = AppType.gloveButton) }
                if (ringing) add { m -> AppButton(stringResource(R.string.action_snooze), { onEvent(Event.Snooze(hiveId)) }, m, minHeight = glove, style = AppType.gloveButton) }
                if (canSkip) add { m -> AppButton(stringResource(R.string.action_skip), { onEvent(Event.Skip(hiveId)) }, m, minHeight = glove, style = AppType.gloveButton) }
            }
            buttons.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                    pair.forEach { it(Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (!run.isTerminal) {
            AppButton(stringResource(R.string.action_cancel_run), { onEvent(Event.Cancel(hiveId)) }, full, ButtonKind.DANGER)
        }
    }
}
