package io.github.mecheng70.beetimer.scheduler

import org.junit.Assert.assertEquals

fun manual(name: String, device: Boolean = false) = Step(name, StepKind.MANUAL, deviceBound = device)
fun timed(name: String, seconds: Int, device: Boolean = false) =
    Step(name, StepKind.TIMED, seconds, deviceBound = device)
fun template(vararg steps: Step) = Template("test", "Test", steps.toList())

const val SEC = 1000L

/**
 * The original pan wand order (hold passive after the wand comes out). Frozen here so the
 * scheduler scenario tests keep exercising a device block followed by a passive timed step,
 * independent of preset edits.
 */
val legacyPanWand = Template(
    "legacy",
    "Legacy pan wand",
    listOf(
        Step("Seal hive", StepKind.MANUAL, deviceBound = false),
        Step("Load dose", StepKind.MANUAL, deviceBound = true),
        Step("Heat and vaporize", StepKind.TIMED, 150, deviceBound = true),
        Step("Cool in hive", StepKind.TIMED, 120, deviceBound = true),
        Step("Remove wand", StepKind.MANUAL, deviceBound = true),
        Step("Keep sealed", StepKind.TIMED, 600, deviceBound = false),
        Step("Unseal", StepKind.MANUAL, deviceBound = false),
    ),
)

/** Drives the scheduler like the app would, and tracks the platform alarm per hive. */
class Harness(template: Template = legacyPanWand) {
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
