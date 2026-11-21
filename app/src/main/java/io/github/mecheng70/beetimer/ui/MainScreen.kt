package io.github.mecheng70.beetimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.Hive
import io.github.mecheng70.beetimer.scheduler.HiveRun
import io.github.mecheng70.beetimer.scheduler.RunState
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.scheduler.StepKind
import io.github.mecheng70.beetimer.ui.theme.Dimens
import io.github.mecheng70.beetimer.ui.theme.LocalStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSettings: () -> Unit) {
    val app = LocalContext.current.app
    val state by app.engine.state.collectAsStateWithLifecycle()
    val hives by app.repo.hives.collectAsStateWithLifecycle(emptyList())
    val now by rememberNow()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val s = state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { TextButton(onClick = onSettings) { Text(stringResource(R.string.action_settings)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = Dimens.Gutter)) {
            if (s == null) return@Column
            val labels = hives.associate { it.id to it.label }
            DeviceStatusBar(s, labels, now, onStart = { app.send(Event.Start(it)) })
            if (hives.isEmpty()) {
                Spacer(Modifier.height(Dimens.Gutter))
                Text(stringResource(R.string.no_hives), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            val selected = tab.coerceIn(0, hives.size)
            PrimaryScrollableTabRow(selectedTabIndex = selected, edgePadding = 0.dp) {
                Tab(selected = selected == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_all)) })
                hives.forEachIndexed { i, h ->
                    Tab(selected = selected == i + 1, onClick = { tab = i + 1 }, text = { Text(h.label) })
                }
            }
            if (selected == 0) {
                AllTab(hives, s, now, onSelect = { tab = hives.indexOf(it) + 1 })
            } else {
                HiveTab(hives[selected - 1], s, labels, now)
            }
        }
    }
}

@Composable
private fun DeviceStatusBar(s: SchedulerState, labels: Map<Long, String>, now: Long, onStart: (Long) -> Unit) {
    val holder = s.lockHolder?.let { s.runs[it] }
    val head = s.queue.firstOrNull()
    val tone = if (holder != null) Tone.DEVICE else Tone.IDLE
    val (bg, fg) = toneColors(tone)
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(Dimens.Corner), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            val line = when {
                holder != null -> {
                    val label = labels[holder.hiveId].orEmpty()
                    val step = holder.currentStep?.name.orEmpty()
                    val ends = holder.stepEndsAt
                    if (holder.state == RunState.RUNNING && ends != null) {
                        stringResource(R.string.device_busy_timed, label, step, formatCountdown(ends - now))
                    } else {
                        stringResource(R.string.device_busy, label, step)
                    }
                }
                head != null -> stringResource(R.string.device_free_next, labels[head].orEmpty())
                else -> stringResource(R.string.device_free)
            }
            Text(line, style = MaterialTheme.typography.titleMedium)
            val waiting = if (holder != null) s.queue else s.queue.drop(1)
            if (waiting.isNotEmpty()) {
                Text(
                    stringResource(R.string.device_next, waiting.joinToString(", ") { labels[it].orEmpty() }),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (holder == null && head != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton(stringResource(R.string.action_start), onClick = { onStart(head) }, modifier = Modifier.weight(1f))
                    val app = LocalContext.current.app
                    if (s.queue.size > 1) {
                        BigOutlinedButton(stringResource(R.string.action_queue_skip), onClick = { app.send(Event.QueueSkipHead) }, modifier = Modifier.weight(1f))
                    }
                    BigOutlinedButton(stringResource(R.string.action_queue_remove), onClick = { app.send(Event.QueueRemoveHead) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AllTab(hives: List<Hive>, s: SchedulerState, now: Long, onSelect: (Hive) -> Unit) {
    val app = LocalContext.current.app
    LazyColumn(Modifier.fillMaxSize()) {
        items(hives, key = { it.id }) { hive ->
            val run = s.runs[hive.id]
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    TextButton(onClick = { onSelect(hive) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(hive.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(subtitle(run), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RowAction(hive.id, run, s, now, onEvent = { app.send(it) })
            }
            HorizontalDivider()
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

/** The one thing you can do now for this hive, or its countdown. */
@Composable
private fun RowAction(hiveId: Long, run: HiveRun?, s: SchedulerState, now: Long, onEvent: (Event) -> Unit) {
    val headReady = s.lockHolder == null && s.queue.firstOrNull() == hiveId
    val ends = run?.stepEndsAt
    when {
        run == null || run.state == RunState.CANCELLED ->
            BigButton(stringResource(R.string.action_start), onClick = { onEvent(Event.Start(hiveId)) })
        run.ringing != null || run.state == RunState.AWAITING_CONFIRM ->
            BigButton(stringResource(R.string.action_done), onClick = { onEvent(Event.Done(hiveId)) })
        run.state == RunState.WAITING_FOR_DEVICE && headReady ->
            BigButton(stringResource(R.string.action_start), onClick = { onEvent(Event.Start(hiveId)) })
        run.state == RunState.WAITING_FOR_DEVICE -> Pill(stringResource(R.string.state_queued), Tone.IDLE)
        run.state == RunState.RUNNING && ends != null ->
            Pill(formatCountdown(ends - now), toneOf(run))
        run.state == RunState.DONE -> Pill(stringResource(R.string.state_done), Tone.IDLE)
    }
}

@Composable
private fun HiveTab(hive: Hive, s: SchedulerState, labels: Map<Long, String>, now: Long) {
    val app = LocalContext.current.app
    val run = s.runs[hive.id]
    val steps = run?.steps ?: s.template.steps
    val status = LocalStatusColors.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header: step, big countdown, instruction.
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val ringing = run?.ringing
            val current = run?.currentStep
            val ends = run?.stepEndsAt
            if (run != null && current != null && !run.isTerminal) {
                Text(
                    stringResource(R.string.hive_step_of, run.currentStepIndex + 1, run.steps.size, current.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val big = when {
                run == null || run.state == RunState.CANCELLED -> stringResource(R.string.hive_ready)
                run.state == RunState.DONE -> stringResource(R.string.hive_all_done)
                ringing != null && run.currentStepIndex == ringing.finishedStepIndex ->
                    stringResource(R.string.state_finished, run.steps[ringing.finishedStepIndex].name)
                run.state == RunState.RUNNING && ends != null -> formatCountdown(ends - now)
                run.state == RunState.WAITING_FOR_DEVICE ->
                    if (s.lockHolder == null && s.queue.firstOrNull() == hive.id) stringResource(R.string.hive_waiting_head)
                    else stringResource(R.string.hive_waiting)
                else -> stringResource(R.string.hive_tap_done)
            }
            val isCountdown = run?.state == RunState.RUNNING && ringing == null
            Text(
                big,
                style = if (isCountdown) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            if (ringing?.overdue == true) Pill(stringResource(R.string.pill_overdue), Tone.DEVICE)
            if (current != null && run.isActive && current.instruction.isNotBlank()) {
                Text(current.instruction, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        }

        // Step list.
        steps.forEachIndexed { i, step ->
            val isCurrent = run != null && !run.isTerminal && i == run.currentStepIndex
            val isDone = run != null && (i < run.currentStepIndex || run.state == RunState.DONE)
            val rowBg = if (isCurrent && step.deviceBound) status.deviceBg else if (isCurrent) status.passiveBg else null
            Row(
                Modifier.fillMaxWidth()
                    .then(if (rowBg != null) Modifier.background(rowBg, RoundedCornerShape(Dimens.Corner)) else Modifier)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (isDone) "✓" else if (isCurrent) "▶" else "○",
                    color = if (isDone) status.done else MaterialTheme.colorScheme.onSurface,
                )
                val name = if (step.kind == StepKind.TIMED) {
                    stringResource(R.string.step_with_duration, step.name, formatDuration(step.durationSeconds ?: 0))
                } else {
                    step.name
                }
                Text(
                    name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                if (step.deviceBound) Pill(stringResource(R.string.pill_device), Tone.DEVICE)
                else if (step.kind == StepKind.TIMED) Pill(stringResource(R.string.pill_passive), Tone.PASSIVE)
            }
        }

        Spacer(Modifier.height(Dimens.Gap))
        HiveActions(hive.id, run, s, onEvent = { app.send(it) })
        Spacer(Modifier.height(Dimens.Gutter))
    }
}

@Composable
private fun HiveActions(hiveId: Long, run: HiveRun?, s: SchedulerState, onEvent: (Event) -> Unit) {
    val full = Modifier.fillMaxWidth()
    val danger = LocalStatusColors.current.danger
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            run == null || run.isTerminal && run.ringing == null -> {
                if (run?.state != RunState.DONE) BigButton(stringResource(R.string.action_start), { onEvent(Event.Start(hiveId)) }, full)
                return@Column
            }
            run.state == RunState.WAITING_FOR_DEVICE -> {
                val ready = s.lockHolder == null && s.queue.firstOrNull() == hiveId
                BigButton(stringResource(R.string.action_start), { onEvent(Event.Start(hiveId)) }, full, enabled = ready)
                if (run.ringing != null) BigOutlinedButton(stringResource(R.string.action_done), { onEvent(Event.Done(hiveId)) }, full)
            }
            else -> {
                if (run.ringing != null || run.state == RunState.AWAITING_CONFIRM) {
                    BigButton(stringResource(R.string.action_done), { onEvent(Event.Done(hiveId)) }, full)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (run.ringing != null || run.state == RunState.RUNNING) {
                        BigOutlinedButton(stringResource(R.string.action_add_minute), { onEvent(Event.AddMinute(hiveId)) }, Modifier.weight(1f))
                    }
                    if (run.state == RunState.RUNNING || run.state == RunState.AWAITING_CONFIRM) {
                        BigOutlinedButton(stringResource(R.string.action_skip), { onEvent(Event.Skip(hiveId)) }, Modifier.weight(1f))
                    }
                }
                if (run.ringing != null) BigOutlinedButton(stringResource(R.string.action_snooze), { onEvent(Event.Snooze(hiveId)) }, full)
            }
        }
        if (!run.isTerminal) {
            BigOutlinedButton(stringResource(R.string.action_cancel_run), { onEvent(Event.Cancel(hiveId)) }, full, color = danger)
        }
    }
}
