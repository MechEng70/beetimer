package io.github.mecheng70.beetimer.ui

import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.mecheng70.beetimer.scheduler.Forecast
import io.github.mecheng70.beetimer.scheduler.Planner
import io.github.mecheng70.beetimer.ui.theme.AppIcons
import java.util.Date
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.testTag
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
private const val HALF = 0.5f

@Composable
fun MainScreen(onSettings: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val state by app.engine.state.collectAsStateWithLifecycle()
    val hives by app.repo.hives.collectAsStateWithLifecycle(emptyList())
    val now by rememberNow()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val planner = remember { Planner() }
    val s = state

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppTopBar(stringResource(R.string.app_name)) {
            IconAction(AppIcons.Settings, stringResource(R.string.action_settings), onSettings)
        }
        if (s == null) return@Column
        val order = hives.map { it.id }
        val anyActive = s.runs.values.any { it.isActive }
        if (anyActive) VolumeBanner()
        AppCard(Modifier.fillMaxWidth().padding(horizontal = Dimens.PageMargin).padding(bottom = Dimens.PageMargin)) {
            val labels = hives.associate { it.id to it.label }
            // Recomputed only when the state changes (taps, step ends, queue changes), never on
            // clock ticks, so estimates hold still; the countdowns below tick from these times.
            val forecast = remember(s) { planner.forecast(s, System.currentTimeMillis()) }
            DeviceStatusBar(s, labels, now, planner.nextUp(s, order), forecast)
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
                AllTab(hives, s, forecast, now, onSelect = { tab = hives.indexOf(it) + 1 })
            } else {
                HiveTab(hives[selected - 1], s, now)
            }
        }
    }
}

/** Shown while a run is active and the alarm stream is under half volume. */
@Composable
private fun VolumeBanner() {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    var low by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        low = audio.getStreamVolume(AudioManager.STREAM_ALARM).toFloat() / max < HALF
        onPauseOrDispose { }
    }
    if (!low) return
    val (bg, fg) = toneColors(Tone.DEVICE)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.PageMargin).padding(bottom = Dimens.Gap)
            .background(bg, RoundedCornerShape(Dimens.SmallCorner))
            .padding(horizontal = Dimens.StatusPaddingH, vertical = Dimens.Gap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Gap),
    ) {
        Text(stringResource(R.string.volume_low), style = AppType.status, color = fg, maxLines = 1, modifier = Modifier.weight(1f))
        AppButton(stringResource(R.string.volume_open), {
            runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }
            // Re-checked on resume.
        })
    }
}

/** Clock time for the footer, following the phone's 12/24-hour setting: "1:05 PM" or "13:05". */
@Composable
private fun rememberClock(): (Long) -> String {
    val context = LocalContext.current
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    return remember(is24) {
        val locale = java.util.Locale.getDefault()
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, if (is24) "Hm" else "hma")
        val format = java.text.SimpleDateFormat(pattern, locale)
        val clock: (Long) -> String = { ms -> format.format(Date(ms)) }
        clock
    }
}

@Composable
private fun estimate(text: String, estimated: Boolean): String =
    if (estimated) stringResource(R.string.estimate_prefix) + text else text

/** Time left until a projected moment: "2:10", "~14:09" when it depends on a manual tap, or [nowText] once passed. */
@Composable
private fun remaining(at: Long, estimated: Boolean, now: Long, nowText: String): String =
    if (at <= now) nowText else estimate(formatCountdown(at - now), estimated)

/** A clock time for the footer: "~12:49 PM", or [nowText] once passed. */
@Composable
private fun projectedClock(at: Long, estimated: Boolean, now: Long, clock: (Long) -> String, nowText: String): String =
    if (at <= now) nowText else estimate(clock(at), estimated)

@Composable
private fun DeviceStatusBar(s: SchedulerState, labels: Map<Long, String>, now: Long, nextUp: Long?, forecast: Forecast) {
    val app = LocalContext.current.app
    val holder = s.lockHolder?.let { s.runs[it] }
    val head = s.queue.firstOrNull()
    val (bg, fg) = toneColors(if (holder != null) Tone.DEVICE else Tone.IDLE)
    Column(
        Modifier.fillMaxWidth()
            .background(bg, RoundedCornerShape(Dimens.SmallCorner))
            .padding(horizontal = Dimens.StatusPaddingH, vertical = Dimens.StatusPaddingV),
    ) {
        val freeAt = forecast.deviceFreeAt
        val line = when {
            holder != null && freeAt != null -> {
                val label = labels[holder.hiveId].orEmpty()
                if (freeAt <= now) {
                    AnnotatedString(stringResource(R.string.device_free_now, label))
                } else {
                    val template = stringResource(R.string.device_free_in, label, COUNTDOWN_MARK)
                    buildAnnotatedString {
                        append(template.substringBefore(COUNTDOWN_MARK))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(remaining(freeAt, forecast.deviceFreeEstimated, now, ""))
                        }
                        append(template.substringAfter(COUNTDOWN_MARK))
                    }
                }
            }
            holder != null -> AnnotatedString(
                stringResource(R.string.device_busy, labels[holder.hiveId].orEmpty(), holder.currentStep?.name.orEmpty().midSentence()),
            )
            head != null -> AnnotatedString(stringResource(R.string.device_free_next, labels[head].orEmpty()))
            else -> AnnotatedString(stringResource(R.string.device_free))
        }
        Text(line, style = AppType.status, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val showNext = nextUp != null && nextUp != holder?.hiveId && !(holder == null && head != null)
        if (showNext) {
            Text(stringResource(R.string.device_next, labels[nextUp].orEmpty()), style = AppType.statusSub, color = fg, maxLines = 1)
        }
        if (holder == null && head != null) {
            VSpace(Dimens.Gap)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                AppButton(stringResource(R.string.action_start), { app.send(Event.Start(head)) }, Modifier.testTag("bar-start"), kind = ButtonKind.FILLED)
                if (s.queue.size > 1) AppButton(stringResource(R.string.action_queue_skip), { app.send(Event.QueueSkipHead) }, Modifier.testTag("bar-skip"))
                AppButton(stringResource(R.string.action_queue_remove), { app.send(Event.QueueRemoveHead) }, Modifier.testTag("bar-remove"))
            }
        }
    }
}

@Composable
private fun AllTab(hives: List<Hive>, s: SchedulerState, forecast: Forecast, now: Long, onSelect: (Hive) -> Unit) {
    val app = LocalContext.current.app
    val c = BeeTheme.colors
    val fullClock = rememberClock()
    val labels = hives.associate { it.id to it.label }

    val upcoming = forecast.comingUp()
    if (upcoming.isNotEmpty()) {
        Text(stringResource(R.string.coming_up), style = AppType.section, color = c.ink, modifier = Modifier.padding(bottom = Dimens.ChipPaddingV))
        upcoming.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = Dimens.ChipPaddingV), horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                Text(remaining(p.at, p.estimated, now, stringResource(R.string.time_now)), style = AppType.comingTime, color = c.ink, modifier = Modifier.width(Dimens.ComingTimeWidth))
                Text(
                    labels[p.hiveId].orEmpty(),
                    style = AppType.section,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(Dimens.ComingHiveWidth),
                )
                Text(p.stepName, style = AppType.body, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
        VSpace(Dimens.Gap)
        RowDivider()
    }

    ReorderableColumn(
        items = hives,
        key = { it.id },
        onMove = { moved -> app.scope.launch { app.repo.reorderHives(moved.map { it.id }) } },
    ) { hive, handle, _ ->
        val run = s.runs[hive.id]
        val plan = forecast.hives[hive.id]
        Column {
            Row(
                Modifier.fillMaxWidth().testTag("row-${hive.label}").clickable { onSelect(hive) }.padding(vertical = Dimens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DragHandle(handle.testTag("drag-${hive.label}"), stringResource(R.string.action_reorder, hive.label))
                Column(Modifier.weight(1f)) {
                    Text(hive.label, style = AppType.hiveName, color = c.ink)
                    Text(subtitle(run), style = AppType.body, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val second = when {
                        run?.state == RunState.WAITING_FOR_DEVICE -> plan?.deviceStart?.let {
                            if (it.at <= now) stringResource(R.string.hive_starts_now)
                            else stringResource(R.string.hive_starts_in, remaining(it.at, it.estimated, now, ""))
                        }
                        run != null && run.isActive -> plan?.next?.let {
                            if (it.at <= now) stringResource(R.string.hive_next_now, it.stepName)
                            else stringResource(R.string.hive_next_in, it.stepName, remaining(it.at, it.estimated, now, ""))
                        }
                        else -> null
                    }
                    if (second != null) Text(second, style = AppType.label, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.testTag("row-action-${hive.label}")) {
                    RowAction(hive.id, run, s, now, onEvent = { app.send(it) })
                }
            }
            if (hive != hives.last()) RowDivider()
        }
    }

    forecast.allDoneAt?.let {
        VSpace(Dimens.Gap)
        RowDivider()
        Text(
            stringResource(R.string.all_done_at, projectedClock(it, forecast.allDoneEstimated, now, fullClock, stringResource(R.string.time_now_inline))),
            style = AppType.label,
            color = c.inkMuted,
            modifier = Modifier.padding(top = Dimens.RowPadding),
        )
    }
}

@Composable
private fun subtitle(run: HiveRun?): String {
    val ringing = run?.ringing
    return when {
        run == null -> stringResource(R.string.state_not_started)
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
        run.state == RunState.RUNNING && ends != null -> Chip(formatCountdown(ends - now), toneOf(run), kind = ChipKind.TIME)
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
                AppButton(stringResource(R.string.action_start), { onEvent(Event.Start(hiveId)) }, full.testTag("hive-start"), kind = ButtonKind.FILLED)
            }
            return@Column
        }
        if (run.state == RunState.WAITING_FOR_DEVICE) {
            val ready = s.lockHolder == null && s.queue.firstOrNull() == hiveId
            AppButton(stringResource(R.string.action_start), { onEvent(Event.Start(hiveId)) }, full.testTag("hive-start"), kind = ButtonKind.FILLED, enabled = ready)
            if (run.ringing != null) AppButton(stringResource(R.string.action_done), { onEvent(Event.Done(hiveId)) }, full)
        } else {
            val ringing = run.ringing != null
            val canConfirm = ringing || run.state == RunState.AWAITING_CONFIRM
            val canExtend = ringing || run.state == RunState.RUNNING
            val canSkip = run.state == RunState.RUNNING || run.state == RunState.AWAITING_CONFIRM
            val buttons = buildList<@Composable (Modifier) -> Unit> {
                if (canConfirm) add { m -> AppButton(stringResource(R.string.action_done), { onEvent(Event.Done(hiveId)) }, m.testTag("hive-done"), ButtonKind.FILLED, minHeight = glove, style = AppType.gloveButton) }
                if (canExtend) add { m -> AppButton(stringResource(R.string.action_add_minute), { onEvent(Event.AddMinute(hiveId)) }, m.testTag("hive-add-minute"), minHeight = glove, style = AppType.gloveButton) }
                if (ringing) add { m -> AppButton(stringResource(R.string.action_snooze), { onEvent(Event.Snooze(hiveId)) }, m.testTag("hive-snooze"), minHeight = glove, style = AppType.gloveButton) }
                if (canSkip) add { m -> AppButton(stringResource(R.string.action_skip), { onEvent(Event.Skip(hiveId)) }, m.testTag("hive-skip"), minHeight = glove, style = AppType.gloveButton) }
            }
            buttons.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                    pair.forEach { it(Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (!run.isTerminal) {
            AppButton(stringResource(R.string.action_cancel_run), { onEvent(Event.Cancel(hiveId)) }, full.testTag("hive-cancel"), ButtonKind.DANGER)
        }
    }
}
