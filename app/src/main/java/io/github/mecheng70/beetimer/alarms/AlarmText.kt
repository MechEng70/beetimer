package io.github.mecheng70.beetimer.alarms

import android.content.Context
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.scheduler.HiveRun
import io.github.mecheng70.beetimer.scheduler.RunState
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.scheduler.StepKind
import io.github.mecheng70.beetimer.ui.formatDuration

/** What an alarm says, shared by the alarm screen and the ringing notification. */
data class AlarmInfo(
    /** "Cool in hive finished" (with "(overdue)" when late). */
    val finished: String,
    val finishedStep: String,
    /** The next thing to do, on its own line: a step name. */
    val nextAction: String,
    /** "10:00 timer starts when you tap Done" when the next step is timed and waits for Done. */
    val timerLine: String?,
    /** "Vaporizer frees when you tap Done. Sparky is next." when Done releases the vaporizer. */
    val deviceNote: String?,
)

object AlarmText {
    fun info(context: Context, run: HiveRun, state: SchedulerState, labels: Map<Long, String>): AlarmInfo {
        val ringing = run.ringing
        val finishedStep = ringing?.let { run.steps.getOrNull(it.finishedStepIndex)?.name }.orEmpty()
        val finished = context.getString(
            if (ringing?.overdue == true) R.string.alarm_finished_overdue else R.string.alarm_finished,
            finishedStep,
        )
        val current = run.currentStep
        var timerLine: String? = null
        val nextAction = when {
            run.state == RunState.DONE -> context.getString(R.string.alarm_all_done)

            run.state == RunState.WAITING_FOR_DEVICE && current != null -> context.getString(
                R.string.alarm_waiting,
                current.name,
            )

            ringing != null && run.currentStepIndex == ringing.finishedStepIndex -> {
                val next = run.steps.getOrNull(run.currentStepIndex + 1)
                if (next?.kind == StepKind.TIMED) {
                    timerLine =
                        context.getString(R.string.alarm_timer_starts, formatDuration(next.durationSeconds ?: 0))
                }
                next?.name ?: context.getString(R.string.alarm_all_done)
            }

            current != null -> current.name

            else -> ""
        }
        return AlarmInfo(finished, finishedStep, nextAction, timerLine, deviceNote(context, run, state, labels))
    }

    /** Heads-up title: the hive name only, so it never truncates. */
    fun notificationTitle(label: String): String = label

    /** Heads-up body: "Heat and vaporize finished. Next: Cool in hive". */
    fun notificationBody(context: Context, info: AlarmInfo): String =
        context.getString(R.string.alarm_notif_body, info.finished, info.nextAction)

    private fun deviceNote(context: Context, run: HiveRun, state: SchedulerState, labels: Map<Long, String>): String? {
        val step = run.currentStep
        val lastDevice = run.steps.indexOfLast { it.deviceBound }
        val doneReleases = step != null && run.state == RunState.AWAITING_CONFIRM && step.deviceBound &&
            run.currentStepIndex == lastDevice && step.kind == StepKind.MANUAL
        if (!doneReleases) return null
        val nextLabel = state.queue.firstOrNull()?.let { labels[it] }
        return if (nextLabel != null) {
            context.getString(R.string.alarm_device_frees_next, nextLabel)
        } else {
            context.getString(R.string.alarm_device_frees)
        }
    }
}
