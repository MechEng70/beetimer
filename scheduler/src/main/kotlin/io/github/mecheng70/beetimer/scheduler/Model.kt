package io.github.mecheng70.beetimer.scheduler

import kotlinx.serialization.Serializable

// Pure Kotlin, KMP-ready: stdlib and kotlinx.serialization only. Times are epoch ms.

@Serializable
enum class StepKind { TIMED, MANUAL }

@Serializable
data class Step(
    val name: String,
    val kind: StepKind,
    /** TIMED only. Ignored for MANUAL. */
    val durationSeconds: Int? = null,
    /** True while the vaporizer is physically in use on this hive. */
    val deviceBound: Boolean = false,
    val instruction: String = "",
) {
    val durationMs: Long get() = (durationSeconds ?: 0) * MS_PER_SECOND

    private companion object {
        const val MS_PER_SECOND = 1000L
    }
}

/** An ordered list of steps shared by all hives. All edits return a new Template. */
@Serializable
data class Template(
    val id: String,
    val name: String,
    val steps: List<Step>,
) {
    /** Indices of the device-bound steps, or null when there are none. */
    val deviceBlock: IntRange?
        get() {
            val first = steps.indexOfFirst { it.deviceBound }
            if (first < 0) return null
            return first..steps.indexOfLast { it.deviceBound }
        }

    fun addStep(step: Step, at: Int = steps.size): Template {
        require(at in 0..steps.size) { "index $at out of range" }
        return copy(steps = steps.toMutableList().apply { add(at, step) })
    }

    fun removeStep(index: Int): Template {
        require(index in steps.indices) { "index $index out of range" }
        return copy(steps = steps.toMutableList().apply { removeAt(index) })
    }

    fun moveStep(from: Int, to: Int): Template {
        require(from in steps.indices && to in steps.indices) { "move $from -> $to out of range" }
        val list = steps.toMutableList()
        val step = list.removeAt(from)
        list.add(to, step)
        return copy(steps = list)
    }

    fun updateStep(index: Int, step: Step): Template {
        require(index in steps.indices) { "index $index out of range" }
        return copy(steps = steps.toMutableList().apply { set(index, step) })
    }

    fun withDuration(index: Int, seconds: Int): Template =
        updateStep(index, steps[index].copy(durationSeconds = seconds))
}

@Serializable
data class Hive(
    val id: Long,
    /** Free text chosen by the user, for example "Emory". */
    val label: String,
    val note: String? = null,
    val position: Int = 0,
)

@Serializable
enum class RunState { NOT_STARTED, WAITING_FOR_DEVICE, RUNNING, AWAITING_CONFIRM, DONE, CANCELLED }

/** An alarm that needs acknowledging. */
@Serializable
data class Ringing(
    /** The TIMED step whose end caused the alarm. */
    val finishedStepIndex: Int,
    /** When the step ended (epoch ms). */
    val since: Long,
    /** True when the alarm was missed (app dead, phone off) and shown late. */
    val overdue: Boolean,
    /** When the alarm rings again if nobody acknowledges it (re-ring or snooze). */
    val nextRingAt: Long,
)

/**
 * One treatment pass of the template on one hive. A hive with no run in
 * [SchedulerState.runs] is NOT_STARTED.
 */
@Serializable
data class HiveRun(
    val hiveId: Long,
    /** Template snapshot taken at Start. Later template edits never touch it. */
    val steps: List<Step>,
    val currentStepIndex: Int = 0,
    val state: RunState = RunState.NOT_STARTED,
    val stepStartedAt: Long? = null,
    val stepEndsAt: Long? = null,
    val ringing: Ringing? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
) {
    val currentStep: Step? get() = steps.getOrNull(currentStepIndex)
    val isTerminal: Boolean get() = state == RunState.DONE || state == RunState.CANCELLED
    val isActive: Boolean get() = !isTerminal && state != RunState.NOT_STARTED

    /** Holds or wants the device: in a device-bound step and not finished. */
    val inDeviceStep: Boolean get() = !isTerminal && currentStep?.deviceBound == true
}

@Serializable
data class SchedulerState(
    val runs: Map<Long, HiveRun> = emptyMap(),
    /** The hive whose run is in a device-bound step, if any. */
    val lockHolder: Long? = null,
    /** Hives waiting for the device, first come first served. */
    val queue: List<Long> = emptyList(),
    /** Template used by the next Start. Active runs keep their own snapshot. */
    val template: Template,
) {
    fun runFor(hiveId: Long): HiveRun? = runs[hiveId]
    fun stateOf(hiveId: Long): RunState = runs[hiveId]?.state ?: RunState.NOT_STARTED
}
