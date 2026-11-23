package io.github.mecheng70.beetimer.alarms

import android.content.Context
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.scheduler.HiveRun
import io.github.mecheng70.beetimer.scheduler.Planner
import io.github.mecheng70.beetimer.scheduler.RunState
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.scheduler.StepKind
import io.github.mecheng70.beetimer.ui.formatDuration
import io.github.mecheng70.beetimer.ui.midSentence

/** What an alarm says, shared by the alarm screen and the ringing notification. */
data class AlarmInfo(
    /** "Cool in hive finished" (with "(overdue)" when late). */
    val finished: String,
    val finishedStep: String,
    /** The next thing to do, on its own line: a step name. */
    val nextAction: String,
    /** "10:00 timer starts when you tap Done" when the next step is timed and waits for Done. */
    val timerLine: String?,
    /** "Vaporizer frees when you tap Done. Hive 2 is next." when Done releases the vaporizer. */
    val deviceNote: String?,
    /** "Then: Hive 2, load dose": who gets the vaporizer when Done frees it, and the step they start on. */
    val thenLine: String? = null,
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
        val (note, then) = releaseLines(context, run, state, labels)
        return AlarmInfo(finished, finishedStep, nextAction, timerLine, note, then)
    }

    /** Heads-up title: the hive name only, so it never truncates. */
    fun notificationTitle(label: String): String = label

    /** Heads-up body: "Heat and vaporize finished. Next: Cool in hive". */
    fun notificationBody(context: Context, info: AlarmInfo): String =
        context.getString(R.string.alarm_notif_body, info.finished, info.nextAction)

    /** The device note and the "Then:" line, both only when Done on this alarm frees the vaporizer. */
    private fun releaseLines(
        context: Context,
        run: HiveRun,
        state: SchedulerState,
        labels: Map<Long, String>,
    ): Pair<String?, String?> = if (doneReleases(run)) {
        deviceNote(context, state, labels) to thenLine(context, run, state, labels)
    } else {
        null to null
    }

    /** Done on this alarm completes the run's last vaporizer step, so it frees the vaporizer. */
    private fun doneReleases(run: HiveRun): Boolean {
        val step = run.currentStep
        val lastDevice = run.steps.indexOfLast { it.deviceBound }
        return step != null && run.state == RunState.AWAITING_CONFIRM && step.deviceBound &&
            run.currentStepIndex == lastDevice && step.kind == StepKind.MANUAL
    }

    /**
     * The hive that gets the vaporizer next (the queue head, else the next not-started hive in list order, as on
     * the vaporizer bar) and the step it starts on: where a queued hive waits, or a new run's first step.
     * [labels] is in list order.
     */
    private fun thenLine(context: Context, run: HiveRun, state: SchedulerState, labels: Map<Long, String>): String? {
        val order = labels.keys.filter { it != run.hiveId }
        val other = Planner().nextUp(state.copy(queue = state.queue - run.hiveId), order)
        val step = other?.let { state.runs[it]?.currentStep ?: state.template.steps.firstOrNull() }
        val label = other?.let { labels[it] }
        return if (label != null && step != null) {
            context.getString(R.string.alarm_then, label, step.name.midSentence())
        } else {
            null
        }
    }

    private fun deviceNote(context: Context, state: SchedulerState, labels: Map<Long, String>): String {
        val nextLabel = state.queue.firstOrNull()?.let { labels[it] }
        return if (nextLabel != null) {
            context.getString(R.string.alarm_device_frees_next, nextLabel)
        } else {
            context.getString(R.string.alarm_device_frees)
        }
    }
}
