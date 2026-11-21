package io.github.mecheng70.beetimer.alarms

import android.content.Context
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.scheduler.HiveRun
import io.github.mecheng70.beetimer.scheduler.RunState
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.scheduler.StepKind

/** Alarm wording shared by the ringing notification and the alarm screen. */
object AlarmText {
    fun finished(context: Context, run: HiveRun): String {
        val ringing = run.ringing ?: return ""
        val name = run.steps.getOrNull(ringing.finishedStepIndex)?.name.orEmpty()
        val res = if (ringing.overdue) R.string.alarm_finished_overdue else R.string.alarm_finished
        return context.getString(res, name)
    }

    fun next(context: Context, run: HiveRun): String {
        val ringing = run.ringing
        val current = run.currentStep
        return when {
            run.state == RunState.DONE -> context.getString(R.string.alarm_all_done)
            run.state == RunState.WAITING_FOR_DEVICE && current != null ->
                context.getString(R.string.alarm_next_waiting, current.name)
            ringing != null && run.currentStepIndex == ringing.finishedStepIndex -> {
                val nextStep = run.steps.getOrNull(run.currentStepIndex + 1)
                if (nextStep == null) context.getString(R.string.alarm_all_done)
                else context.getString(R.string.alarm_next_timed, nextStep.name)
            }
            current != null -> context.getString(R.string.alarm_next, current.instruction.ifBlank { current.name })
            else -> ""
        }
    }

    /** "Device frees when you tap Done. Herb is next." when Done on this step releases the device. */
    fun deviceHint(context: Context, run: HiveRun, state: SchedulerState, labels: Map<Long, String>): String? {
        val step = run.currentStep ?: return null
        val lastDevice = run.steps.indexOfLast { it.deviceBound }
        val doneReleases = run.state == RunState.AWAITING_CONFIRM && step.deviceBound &&
            run.currentStepIndex == lastDevice && step.kind == StepKind.MANUAL
        if (!doneReleases) return null
        val nextLabel = state.queue.firstOrNull()?.let { labels[it] }
        return if (nextLabel != null) context.getString(R.string.alarm_device_frees_next, nextLabel)
        else context.getString(R.string.alarm_device_frees)
    }

    fun body(context: Context, run: HiveRun): String = listOf(finished(context, run), next(context, run))
        .filter { it.isNotBlank() }
        .joinToString(". ") { it.trimEnd('.') } + "."
}
