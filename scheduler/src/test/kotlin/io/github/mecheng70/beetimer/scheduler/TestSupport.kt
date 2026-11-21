package io.github.mecheng70.beetimer.scheduler

import org.junit.Assert.assertEquals

fun manual(name: String, device: Boolean = false) = Step(name, StepKind.MANUAL, deviceBound = device)
fun timed(name: String, seconds: Int, device: Boolean = false) = Step(name, StepKind.TIMED, seconds, deviceBound = device)
fun template(vararg steps: Step) = Template("test", "Test", steps.toList())

const val SEC = 1000L

/** Drives the scheduler like the app would, and tracks the platform alarm per hive. */
class Harness(template: Template = Presets.panWand) {
    val scheduler: Scheduler = DefaultScheduler()
    var state = SchedulerState(template = template)
    var now = 0L
    val alarms = mutableMapOf<Long, Long>()
    var last: List<Effect> = emptyList()

    fun at(seconds: Long): Harness = apply { now = seconds * SEC }

    fun send(event: Event): List<Effect> {
        if (event is Event.TimerFired) alarms.remove(event.hiveId) // the platform consumed it
        val t = scheduler.reduce(state, event, now)
        return apply(t)
    }

    fun restore(): List<Effect> = apply(scheduler.restore(state, now))

    /** Fire this hive's pending alarm at its due time. */
    fun fire(hiveId: Long): List<Effect> {
        now = alarms.getValue(hiveId)
        return send(Event.TimerFired(hiveId))
    }

    private fun apply(t: Transition): List<Effect> {
        state = t.state
        t.effects.forEach {
            when (it) {
                is Effect.ScheduleAlarm -> alarms[it.hiveId] = it.atMs
                is Effect.CancelAlarm -> alarms.remove(it.hiveId)
                else -> Unit
            }
        }
        last = t.effects
        return t.effects
    }

    fun run(h: Long): HiveRun = state.runs.getValue(h)
    fun stepName(h: Long): String = run(h).currentStep!!.name
    fun assertAt(h: Long, step: String, runState: RunState) {
        assertEquals("hive $h step", step, stepName(h))
        assertEquals("hive $h state", runState, run(h).state)
    }
}
