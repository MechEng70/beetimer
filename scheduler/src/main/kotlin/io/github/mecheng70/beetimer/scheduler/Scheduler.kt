package io.github.mecheng70.beetimer.scheduler

/** Something the user did or a timer that fired. */
sealed interface Event {
    data class Start(val hiveId: Long) : Event
    data class Done(val hiveId: Long) : Event
    data class TimerFired(val hiveId: Long) : Event
    data class AddMinute(val hiveId: Long) : Event
    data class Snooze(val hiveId: Long) : Event
    data class Skip(val hiveId: Long) : Event
    data class Cancel(val hiveId: Long) : Event
    data object QueueSkipHead : Event
    data object QueueRemoveHead : Event
    data object NewSession : Event
    data class TemplateChanged(val template: Template) : Event
}

/** Work for the platform layer. The scheduler never does I/O itself. */
sealed interface Effect {
    /** Replace this hive's single pending alarm. */
    data class ScheduleAlarm(val hiveId: Long, val atMs: Long) : Effect
    data class CancelAlarm(val hiveId: Long) : Effect

    /** Start the loud alarm for this hive. The run's [HiveRun.ringing] says why. */
    data class Ring(val hiveId: Long, val finishedStepIndex: Int, val overdue: Boolean) : Effect
    data class StopRing(val hiveId: Long) : Effect

    /** "Device free: <label> is next". */
    data class NotifyDeviceFree(val hiveId: Long) : Effect
}

data class Transition(val state: SchedulerState, val effects: List<Effect>)

/** Pure treatment scheduler: `(state, event, now) -> (newState, effects)`. */
interface Scheduler {
    fun reduce(state: SchedulerState, event: Event, now: Long): Transition

    /**
     * Bring persisted state up to [now] after process death, reboot or a clock change.
     * A timed step that ended while nobody was watching rings now, marked overdue.
     * Only future alarms are scheduled.
     */
    fun restore(state: SchedulerState, now: Long): Transition
}

object SchedulerTiming {
    const val ADD_MINUTE_MS = 60_000L
    const val SNOOZE_MS = 30_000L

    /** Ring for 2 minutes, stay quiet for 2, then ring again. */
    const val RING_CYCLE_MS = 4 * 60_000L

    /** An alarm handled more than this late is shown as overdue. */
    const val OVERDUE_AFTER_MS = 60_000L
}

class DefaultScheduler : Scheduler {
    override fun reduce(state: SchedulerState, event: Event, now: Long): Transition {
        val r = Reduction(state, now)
        when (event) {
            is Event.Start -> r.start(event.hiveId)
            is Event.Done -> r.done(event.hiveId)
            is Event.TimerFired -> r.timerFired(event.hiveId)
            is Event.AddMinute -> r.addMinute(event.hiveId)
            is Event.Snooze -> r.snooze(event.hiveId)
            is Event.Skip -> r.skip(event.hiveId)
            is Event.Cancel -> r.cancel(event.hiveId)
            Event.QueueSkipHead -> r.queueSkipHead()
            Event.QueueRemoveHead -> r.queueRemoveHead()
            Event.NewSession -> r.newSession()
            is Event.TemplateChanged -> r.s = r.s.copy(template = event.template)
        }
        return r.finish(state)
    }

    override fun restore(state: SchedulerState, now: Long): Transition {
        val r = Reduction(state, now)
        r.restore()
        // After a restart nothing is scheduled, so schedule every pending alarm.
        val effects = r.effects.filterNot { it is Effect.ScheduleAlarm || it is Effect.CancelAlarm }.toMutableList()
        r.s.runs.values.sortedBy { it.hiveId }.forEach { run ->
            desiredAlarm(run)?.let { if (it > now) effects += Effect.ScheduleAlarm(run.hiveId, it) }
        }
        return Transition(r.s, effects)
    }
}

/** When this run's single alarm should fire, or null for none. */
internal fun desiredAlarm(run: HiveRun): Long? = when {
    run.ringing != null -> run.ringing.nextRingAt
    run.state == RunState.RUNNING -> run.stepEndsAt
    else -> null
}

/** One reduction. Mutable inside, pure from the outside. */
private class Reduction(var s: SchedulerState, val now: Long) {
    val effects = mutableListOf<Effect>()

    fun finish(before: SchedulerState): Transition {
        // Alarm effects come from the difference in each run's desired alarm.
        val ids = (before.runs.keys + s.runs.keys).sorted()
        for (id in ids) {
            val old = before.runs[id]?.let(::desiredAlarm)
            val new = s.runs[id]?.let(::desiredAlarm)
            if (old != new) effects += if (new != null) Effect.ScheduleAlarm(id, new) else Effect.CancelAlarm(id)
        }
        return Transition(s, effects.toList())
    }

    // ---- events ----

    fun start(h: Long) {
        val run = s.runs[h]
        when {
            run == null || run.isTerminal -> {
                if (s.template.steps.isEmpty()) return
                if (run?.ringing != null) effects += Effect.StopRing(h)
                val fresh = HiveRun(hiveId = h, steps = s.template.steps, startedAt = now)
                put(enterStep(fresh, 0))
            }
            run.state == RunState.WAITING_FOR_DEVICE && deviceAvailableFor(h) ->
                put(enterStep(clearRinging(run), run.currentStepIndex))
        }
    }

    fun done(h: Long) {
        val run = s.runs[h] ?: return
        when {
            run.state == RunState.AWAITING_CONFIRM ->
                put(enterStep(clearRinging(run), run.currentStepIndex + 1))
            run.ringing != null -> put(clearRinging(run))
        }
    }

    fun timerFired(h: Long) {
        val run = s.runs[h] ?: return
        val ringing = run.ringing
        val endsAt = run.stepEndsAt
        when {
            ringing != null ->
                if (now >= ringing.nextRingAt) {
                    put(run.copy(ringing = ringing.copy(nextRingAt = now + SchedulerTiming.RING_CYCLE_MS)))
                    effects += Effect.Ring(h, ringing.finishedStepIndex, ringing.overdue)
                } else {
                    // Fired early: the platform consumed the alarm, so set it again.
                    effects += Effect.ScheduleAlarm(h, ringing.nextRingAt)
                }
            run.state == RunState.RUNNING && endsAt != null ->
                if (now >= endsAt) {
                    finishTimed(run, overdue = now - endsAt > SchedulerTiming.OVERDUE_AFTER_MS)
                } else {
                    effects += Effect.ScheduleAlarm(h, endsAt)
                }
        }
    }

    fun addMinute(h: Long) {
        val run = s.runs[h] ?: return
        val ringing = run.ringing
        when {
            ringing != null -> reopen(run, ringing)
            run.state == RunState.RUNNING && run.stepEndsAt != null ->
                put(run.copy(stepEndsAt = run.stepEndsAt + SchedulerTiming.ADD_MINUTE_MS))
        }
    }

    fun snooze(h: Long) {
        val run = s.runs[h] ?: return
        val ringing = run.ringing ?: return
        put(run.copy(ringing = ringing.copy(nextRingAt = now + SchedulerTiming.SNOOZE_MS)))
        effects += Effect.StopRing(h)
    }

    fun skip(h: Long) {
        val run = s.runs[h] ?: return
        if (run.state != RunState.RUNNING && run.state != RunState.AWAITING_CONFIRM) return
        put(enterStep(clearRinging(run), run.currentStepIndex + 1))
    }

    fun cancel(h: Long) {
        val run = s.runs[h] ?: return
        if (run.isTerminal) return
        releaseLock(h)
        dequeue(h)
        put(
            clearRinging(run).copy(
                state = RunState.CANCELLED,
                stepEndsAt = null,
                completedAt = now,
            ),
        )
    }

    fun queueSkipHead() {
        if (s.queue.size < 2) return
        val head = s.queue.first()
        s = s.copy(queue = s.queue.drop(1) + head)
        if (s.lockHolder == null) effects += Effect.NotifyDeviceFree(s.queue.first())
    }

    fun queueRemoveHead() {
        val head = s.queue.firstOrNull() ?: return
        val run = s.runs[head]
        if (run?.ringing != null) effects += Effect.StopRing(head)
        dequeue(head)
        // Reset: the hive goes back to Not started.
        s = s.copy(runs = s.runs - head)
    }

    fun newSession() {
        s.runs.values.filter { it.ringing != null }.sortedBy { it.hiveId }
            .forEach { effects += Effect.StopRing(it.hiveId) }
        s = s.copy(runs = emptyMap(), lockHolder = null, queue = emptyList())
    }

    fun restore() {
        s.runs.values.sortedBy { it.hiveId }.forEach { stale ->
            val run = s.runs.getValue(stale.hiveId)
            val ringing = run.ringing
            val endsAt = run.stepEndsAt
            when {
                // Ring again only if its next ring is due; a snoozed or quiet alarm waits.
                ringing != null && now >= ringing.nextRingAt -> {
                    put(run.copy(ringing = ringing.copy(nextRingAt = now + SchedulerTiming.RING_CYCLE_MS)))
                    effects += Effect.Ring(run.hiveId, ringing.finishedStepIndex, ringing.overdue)
                }
                // The process often starts because this very alarm fired, so on time is not overdue.
                ringing == null && run.state == RunState.RUNNING && endsAt != null && endsAt <= now ->
                    finishTimed(run, overdue = now - endsAt > SchedulerTiming.OVERDUE_AFTER_MS)
            }
        }
        val head = s.queue.firstOrNull()
        if (s.lockHolder == null && head != null && effects.none { it == Effect.NotifyDeviceFree(head) }) {
            effects += Effect.NotifyDeviceFree(head)
        }
    }

    // ---- step mechanics ----

    /** A TIMED step reached its end. */
    private fun finishTimed(run: HiveRun, overdue: Boolean) {
        val finished = run.currentStepIndex
        val next = finished + 1
        val ringing = Ringing(
            finishedStepIndex = finished,
            since = run.stepEndsAt ?: now,
            overdue = overdue,
            nextRingAt = now + SchedulerTiming.RING_CYCLE_MS,
        )
        val nextIsTimed = run.steps.getOrNull(next)?.kind == StepKind.TIMED
        val after = if (nextIsTimed) {
            // Wait for acknowledgement so the next timer starts when you act.
            run.copy(state = RunState.AWAITING_CONFIRM)
        } else {
            // Next is MANUAL (or the end): move on now; Done completes the manual step.
            enterStep(run, next)
        }
        put(after.copy(ringing = ringing))
        effects += Effect.Ring(run.hiveId, finished, overdue)
    }

    /** +1 min on a ringing alarm: give the finished step 60 more seconds. */
    private fun reopen(run: HiveRun, ringing: Ringing) {
        val h = run.hiveId
        val idx = ringing.finishedStepIndex
        val step = run.steps[idx]
        if (step.deviceBound && s.lockHolder != h && !deviceAvailableFor(h)) return
        if (!step.deviceBound) releaseLock(h)
        if (step.deviceBound && s.lockHolder != h) s = s.copy(lockHolder = h)
        s = s.copy(queue = s.queue - h)
        put(
            run.copy(
                currentStepIndex = idx,
                state = RunState.RUNNING,
                stepStartedAt = now,
                stepEndsAt = now + SchedulerTiming.ADD_MINUTE_MS,
                ringing = null,
                completedAt = null,
            ),
        )
        effects += Effect.StopRing(h)
    }

    /** Move a run into step [index], taking, keeping or releasing the device as needed. */
    private fun enterStep(run: HiveRun, index: Int): HiveRun {
        val h = run.hiveId
        if (index >= run.steps.size) {
            releaseLock(h)
            dequeue(h)
            return run.copy(
                currentStepIndex = run.steps.lastIndex,
                state = RunState.DONE,
                stepStartedAt = null,
                stepEndsAt = null,
                completedAt = now,
            )
        }
        val step = run.steps[index]
        if (step.deviceBound) {
            if (s.lockHolder != h) {
                if (!deviceAvailableFor(h)) {
                    if (h !in s.queue) s = s.copy(queue = s.queue + h)
                    return run.copy(
                        currentStepIndex = index,
                        state = RunState.WAITING_FOR_DEVICE,
                        stepStartedAt = null,
                        stepEndsAt = null,
                    )
                }
                s = s.copy(lockHolder = h, queue = s.queue - h)
            }
        } else {
            releaseLock(h)
        }
        return when (step.kind) {
            StepKind.TIMED -> run.copy(
                currentStepIndex = index,
                state = RunState.RUNNING,
                stepStartedAt = now,
                stepEndsAt = now + step.durationMs,
            )
            StepKind.MANUAL -> run.copy(
                currentStepIndex = index,
                state = RunState.AWAITING_CONFIRM,
                stepStartedAt = now,
                stepEndsAt = null,
            )
        }
    }

    /**
     * The device is free for [h] if [h] holds it, or nobody holds it and the queue
     * is empty or [h] is at its head (the lock is reserved for the queue head).
     */
    private fun deviceAvailableFor(h: Long): Boolean =
        s.lockHolder == h || (s.lockHolder == null && (s.queue.isEmpty() || s.queue.first() == h))

    private fun releaseLock(h: Long) {
        if (s.lockHolder != h) return
        s = s.copy(lockHolder = null)
        s.queue.firstOrNull()?.let { effects += Effect.NotifyDeviceFree(it) }
    }

    private fun dequeue(h: Long) {
        if (h !in s.queue) return
        val wasHead = s.queue.first() == h
        s = s.copy(queue = s.queue - h)
        if (wasHead && s.lockHolder == null) s.queue.firstOrNull()?.let { effects += Effect.NotifyDeviceFree(it) }
    }

    private fun clearRinging(run: HiveRun): HiveRun {
        if (run.ringing == null) return run
        effects += Effect.StopRing(run.hiveId)
        return run.copy(ringing = null)
    }

    private fun put(run: HiveRun) {
        s = s.copy(runs = s.runs + (run.hiveId to run))
    }
}
