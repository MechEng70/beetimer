package io.github.mecheng70.beetimer.scheduler

/** How long the planner assumes things take that only a person can finish. */
object PlanningEstimates {
    /** A MANUAL step, from when it starts to when Done is tapped. */
    const val MANUAL_STEP_MS = 60_000L

    /** Moving the wand to the next hive and tapping Start. */
    const val HANDOFF_MS = 60_000L
}

/** A step that will start at [at]. [estimated] when it depends on a person (manual step or queued start). */
data class PlannedStep(
    val hiveId: Long,
    val stepIndex: Int,
    val stepName: String,
    val at: Long,
    val estimated: Boolean,
    val deviceBound: Boolean,
)

data class HiveForecast(
    /** The next step this hive will start. */
    val next: PlannedStep?,
    /** When this hive next gets the device (queued hives). */
    val deviceStart: PlannedStep?,
    val doneAt: Long?,
    val doneEstimated: Boolean,
)

data class Forecast(
    /** Every future step start, in time order. */
    val steps: List<PlannedStep>,
    val hives: Map<Long, HiveForecast>,
    /** When the last started hive finishes, or null if none is active. */
    val allDoneAt: Long?,
    val allDoneEstimated: Boolean,
    /** When the current holder releases the device, or null if nobody holds it. */
    val deviceFreeAt: Long? = null,
    val deviceFreeEstimated: Boolean = false,
) {
    fun comingUp(count: Int = 5): List<PlannedStep> = steps.take(count)
}

/**
 * Projects the started hives forward by running the real scheduler with a simulated person:
 * timers fire on time, a finished timed step is acknowledged at once, manual steps take
 * [PlanningEstimates.MANUAL_STEP_MS], and the queue head starts [PlanningEstimates.HANDOFF_MS]
 * after the device frees. Not-started hives are not projected (Jason, 2026-11-21).
 * Call it when the state changes, not on clock ticks, so estimates hold still while a
 * person works; the UI shows "Now" for a time that has passed.
 * Pure: same input, same output.
 */
class Planner(private val scheduler: Scheduler = DefaultScheduler()) {

    /** The hive the device should go to next: the queue head, else the first not-started hive in list order. */
    fun nextUp(state: SchedulerState, hiveOrder: List<Long>): Long? =
        state.queue.firstOrNull() ?: hiveOrder.firstOrNull { notStarted(state, it) }

    fun forecast(state: SchedulerState, now: Long): Forecast {
        var s = state
        var t = now
        var lockFreedAt = if (s.lockHolder == null) Long.MIN_VALUE else now
        val estimated = mutableSetOf<Long>()
        val steps = mutableListOf<PlannedStep>()
        val doneAt = mutableMapOf<Long, Long>()
        val involved = state.runs.values.filter { it.isActive }.map { it.hiveId }.toSet()
        val holder = state.lockHolder
        var freeAt: Long? = null
        var freeEstimated = false

        for (i in 0 until MAX_ACTIONS) {
            val action = nextAction(s, t, lockFreedAt) ?: break
            t = maxOf(t, action.at)
            val before = s
            s = scheduler.reduce(s, action.event, t).state
            if (s == before) break
            if (action.estimate) estimated += action.hiveId
            if (before.lockHolder != null && s.lockHolder == null) lockFreedAt = t
            if (holder != null && freeAt == null && s.lockHolder != holder) {
                freeAt = t
                freeEstimated = holder in estimated
            }
            for ((id, after) in s.runs) {
                val prior = before.runs[id]
                if (after.state == RunState.DONE && prior?.state != RunState.DONE) doneAt[id] = t
                if (!after.isActive || after.state == RunState.WAITING_FOR_DEVICE) continue
                val entered = prior == null || prior.isTerminal || prior.state == RunState.WAITING_FOR_DEVICE ||
                    prior.currentStepIndex != after.currentStepIndex
                if (entered) {
                    val step = after.steps[after.currentStepIndex]
                    steps += PlannedStep(id, after.currentStepIndex, step.name, t, id in estimated, step.deviceBound)
                }
            }
        }

        val ordered = steps.sortedBy { it.at }
        val hives = involved.associateWith { id ->
            val mine = ordered.filter { it.hiveId == id }
            HiveForecast(
                next = mine.firstOrNull(),
                deviceStart = mine.firstOrNull { it.deviceBound },
                doneAt = doneAt[id],
                doneEstimated = id in estimated,
            )
        }
        val allDone = if (involved.isNotEmpty() && involved.all { it in doneAt }) involved.maxOf { doneAt.getValue(it) } else null
        return Forecast(ordered, hives, allDone, involved.any { it in estimated }, freeAt, freeEstimated)
    }

    private data class Action(
        val at: Long,
        val event: Event,
        val hiveId: Long,
        val estimate: Boolean,
        val rank: Int,
    )

    private fun nextAction(s: SchedulerState, t: Long, lockFreedAt: Long): Action? {
        val candidates = mutableListOf<Action>()
        for (run in s.runs.values) {
            val ringing = run.ringing
            val h = run.hiveId
            when {
                ringing != null && run.state == RunState.AWAITING_CONFIRM ->
                    if (run.currentStepIndex == ringing.finishedStepIndex) {
                        candidates += Action(maxOf(t, ringing.since), Event.Done(h), h, estimate = false, rank = 1)
                    } else {
                        candidates += Action(maxOf(t, ringing.since + PlanningEstimates.MANUAL_STEP_MS), Event.Done(h), h, estimate = true, rank = 1)
                    }
                ringing != null -> Unit
                run.state == RunState.RUNNING && run.stepEndsAt != null ->
                    candidates += Action(maxOf(t, run.stepEndsAt), Event.TimerFired(h), h, estimate = false, rank = 0)
                run.state == RunState.AWAITING_CONFIRM ->
                    candidates += Action(
                        maxOf(t, (run.stepStartedAt ?: t) + PlanningEstimates.MANUAL_STEP_MS),
                        Event.Done(h), h, estimate = true, rank = 1,
                    )
            }
        }
        val head = s.queue.firstOrNull()
        if (s.lockHolder == null && head != null) {
            val at = if (lockFreedAt == Long.MIN_VALUE) t else maxOf(t, lockFreedAt + PlanningEstimates.HANDOFF_MS)
            candidates += Action(at, Event.Start(head), head, estimate = true, rank = 2)
        }
        return candidates.minWithOrNull(compareBy({ it.at }, { it.rank }, { it.hiveId }))
    }

    private fun notStarted(state: SchedulerState, hiveId: Long): Boolean {
        val run = state.runs[hiveId]
        return run == null || run.state == RunState.CANCELLED
    }

    private companion object {
        const val MAX_ACTIONS = 500
    }
}
