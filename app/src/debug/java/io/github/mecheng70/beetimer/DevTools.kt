package io.github.mecheng70.beetimer

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mecheng70.beetimer.scheduler.Effect
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.Invariants
import io.github.mecheng70.beetimer.scheduler.Planner
import io.github.mecheng70.beetimer.scheduler.RunState
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.scheduler.Transition
import io.github.mecheng70.beetimer.ui.AppButton
import io.github.mecheng70.beetimer.ui.AppCard
import io.github.mecheng70.beetimer.ui.app
import io.github.mecheng70.beetimer.ui.send
import io.github.mecheng70.beetimer.ui.theme.AppType
import io.github.mecheng70.beetimer.ui.theme.BeeTheme
import io.github.mecheng70.beetimer.ui.theme.Dimens

/**
 * Debug-build tools (this file is in src/debug; release has a no-op twin):
 * - Developer card in Settings: time scale 1x / 10x / 60x.
 * - After every scheduler commit: invariant check (logcat ERROR "BeeTimerInvariant" on any
 *   violation) and an event log ("BeeTimerE2E") for end-to-end runs.
 */
object DevTools {
    private const val TAG_INVARIANT = "BeeTimerInvariant"
    private const val TAG_E2E = "BeeTimerE2E"
    private val SCALES = listOf(1, 10, 60)

    /** Platform alarms as scheduled through effects in this process. */
    private val alarms = mutableMapOf<Long, Long>()
    private val planner = Planner()

    @Composable
    fun DeveloperCard() {
        val app = LocalContext.current.app
        val state by app.engine.state.collectAsStateWithLifecycle()
        val scale = state?.timeScale ?: 1
        AppCard(Modifier.fillMaxWidth()) {
            Text(
                "Developer (debug build only)",
                style = AppType.section,
                color = BeeTheme.colors.ink,
                modifier = Modifier.padding(bottom = Dimens.Gap),
            )
            Text(
                "Time scale: divides timed steps, +1 min, Snooze and planner estimates.",
                style = AppType.label,
                color = BeeTheme.colors.inkMuted,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Gap),
                modifier = Modifier.padding(top = Dimens.Gap),
            ) {
                SCALES.forEach { s ->
                    AppButton(
                        "${s}x",
                        {
                            app.send(Event.TimeScaleChanged(s))
                        },
                        Modifier.weight(1f).testTag("dev-scale-$s"),
                        selected =
                            s == scale,
                    )
                }
            }
        }
    }

    /** The alarm screen is showing [label]'s alarm. */
    fun alarmShown(label: String) = Log.i(TAG_E2E, "ALARMSCREEN t=${System.currentTimeMillis()} hive=$label")

    fun afterCommit(event: String, before: SchedulerState?, t: Transition, labels: Map<Long, String>, now: Long) {
        val s = t.state
        fun name(id: Long) = labels[id] ?: "hive$id"
        t.effects.forEach {
            when (it) {
                is Effect.ScheduleAlarm -> alarms[it.hiveId] = it.atMs
                is Effect.CancelAlarm -> alarms.remove(it.hiveId)
                else -> Unit
            }
        }
        if (event.startsWith("TimerFired")) {
            // The platform consumed that alarm unless an effect just set a new one.
            val id = event.substringAfter("hiveId=").substringBefore(")").toLongOrNull()
            if (id != null && t.effects.none { e -> e is Effect.ScheduleAlarm && e.hiveId == id }) alarms.remove(id)
        }
        Log.i(TAG_E2E, "EVENT t=$now scale=${s.timeScale} $event")
        for ((id, run) in s.runs) {
            val prior = before?.runs?.get(id)
            val entered = prior == null || prior.currentStepIndex != run.currentStepIndex ||
                (prior.state == RunState.WAITING_FOR_DEVICE && run.state != RunState.WAITING_FOR_DEVICE)
            if (run.state == RunState.DONE &&
                prior?.state != RunState.DONE
            ) {
                Log.i(TAG_E2E, "DONE t=$now hive=${name(id)}")
            } else if (run.state == RunState.WAITING_FOR_DEVICE && prior?.state != RunState.WAITING_FOR_DEVICE) {
                Log.i(TAG_E2E, "WAIT t=$now hive=${name(id)} step=${run.currentStep?.name}")
            } else if (entered && run.isActive) {
                Log.i(
                    TAG_E2E,
                    "STEP t=$now hive=${name(
                        id,
                    )} idx=${run.currentStepIndex} step=${run.currentStep?.name} device=${run.currentStep?.deviceBound}",
                )
            }
            if (run.ringing != null &&
                prior?.ringing == null
            ) {
                Log.i(
                    TAG_E2E,
                    "RING t=$now hive=${name(id)} finished=${run.steps[run.ringing!!.finishedStepIndex].name}",
                )
            }
        }
        before?.runs?.keys?.filter { it !in s.runs }?.forEach { Log.i(TAG_E2E, "NOTSTARTED t=$now hive=${name(it)}") }
        if (before == null || before.lockHolder != s.lockHolder || before.queue != s.queue) {
            Log.i(TAG_E2E, "LOCK t=$now holder=${s.lockHolder?.let(::name) ?: "none"} queue=${s.queue.map(::name)}")
        }
        val forecast = planner.forecast(s, now)
        Log.i(
            TAG_E2E,
            "FORECAST t=$now done=${forecast.allDoneAt} " +
                "dones=${forecast.hives.entries.joinToString(",") { "${name(it.key)}@${it.value.doneAt}" }} " +
                forecast.steps.joinToString(";") {
                    "${name(it.hiveId)}|${it.stepName}|${it.at}|${if (it.estimated) "~" else "="}"
                },
        )
        Invariants.check(s, alarms, forecast, now).forEach { Log.e(TAG_INVARIANT, "VIOLATION after $event: $it") }
    }
}
