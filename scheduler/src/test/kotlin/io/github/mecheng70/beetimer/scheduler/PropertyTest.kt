package io.github.mecheng70.beetimer.scheduler

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Randomized check of the device lock: 1,000 events over 6 hives per seed.
 * After every event the invariants below must hold.
 */
class PropertyTest {
    private val hives = (1L..6L).toList()

    @Test
    fun lockIsNeverHeldByTwoRuns_panWand() = runSeeds(Presets.panWand)

    @Test
    fun lockIsNeverHeldByTwoRuns_gunType() = runSeeds(Presets.gunType)

    @Test
    fun lockIsNeverHeldByTwoRuns_panWandComesOut() = runSeeds(Presets.panWandComesOut)

    @Test
    fun lockIsNeverHeldByTwoRuns_shortTemplate() = runSeeds(
        template(
            timed("Wait", 5),
            manual("Load", device = true),
            timed("Heat", 3, device = true),
            timed("Cool", 2, device = true),
            timed("Sealed", 4),
            manual("Open"),
        ),
    )

    private fun runSeeds(tpl: Template) {
        for (seed in 1..20) runOne(tpl, seed)
    }

    private fun runOne(tpl: Template, seed: Int) {
        val rnd = Random(seed)
        val t = Harness(tpl)
        repeat(EVENTS) { i ->
            t.now += rnd.nextLong(0, 90) * SEC
            val h = hives.random(rnd)
            val roll = rnd.nextInt(100)
            val label: String
            when {
                roll < 20 -> {
                    label = "Start($h)"
                    t.send(Event.Start(h))
                }

                roll < 40 -> {
                    label = "Done($h)"
                    t.send(Event.Done(h))
                }

                roll < 55 -> {
                    // Fire the earliest pending alarm at its due time (or now, if already due).
                    val due = t.alarms.minByOrNull { it.value }
                    if (due != null) {
                        t.now = maxOf(t.now, due.value)
                        label = "fire(${due.key})"
                        t.send(Event.TimerFired(due.key))
                    } else {
                        label = "TimerFired($h) early"
                        t.send(Event.TimerFired(h))
                    }
                }

                roll < 62 -> {
                    label = "Skip($h)"
                    t.send(Event.Skip(h))
                }

                roll < 68 -> {
                    label = "AddMinute($h)"
                    t.send(Event.AddMinute(h))
                }

                roll < 73 -> {
                    label = "Snooze($h)"
                    t.send(Event.Snooze(h))
                }

                roll < 80 -> {
                    label = "Cancel($h)"
                    t.send(Event.Cancel(h))
                }

                roll < 83 -> {
                    label = "QueueSkipHead"
                    t.send(Event.QueueSkipHead)
                }

                roll < 85 -> {
                    label = "SkipToBack($h)"
                    t.send(Event.SkipToBack(h))
                }

                roll < 88 -> {
                    label = "QueueRemoveHead"
                    t.send(Event.QueueRemoveHead)
                }

                roll < 89 -> {
                    label = "NewSession"
                    t.send(Event.NewSession)
                }

                roll < 92 -> {
                    label = "Template edit"
                    t.send(
                        Event.TemplateChanged(
                            tpl.withDuration(
                                tpl.steps.indexOfFirst {
                                    it.kind ==
                                        StepKind.TIMED
                                },
                                rnd.nextInt(1, 60),
                            ),
                        ),
                    )
                }

                else -> {
                    label = "reboot"
                    t.alarms.clear()
                    t.restore()
                }
            }
            check(t, "seed $seed, event $i: $label")
        }
    }

    private fun check(t: Harness, where: String) {
        val s = t.state
        val runs = s.runs.values

        // 1. At most one run is working in a device step, and it is the lock holder.
        val usingDevice = runs.filter { it.inDeviceStep && it.state != RunState.WAITING_FOR_DEVICE }
        if (usingDevice.size > 1) fail("$where: two runs in device steps: ${usingDevice.map { it.hiveId }}")
        usingDevice.firstOrNull()?.let { assertEquals("$where: holder", it.hiveId, s.lockHolder) }

        // 2. The holder exists, is active, and is in a device step.
        s.lockHolder?.let { h ->
            val run = s.runs[h] ?: return fail("$where: holder $h has no run")
            assertTrue("$where: holder $h not in a device step (${run.state})", run.inDeviceStep)
            assertTrue("$where: holder $h is waiting", run.state != RunState.WAITING_FOR_DEVICE)
            assertTrue("$where: holder $h also queued", h !in s.queue)
        }

        // 3. The queue is exactly the waiting runs, no duplicates.
        assertEquals("$where: duplicate in queue", s.queue.size, s.queue.toSet().size)
        val waiting = runs.filter { it.state == RunState.WAITING_FOR_DEVICE }.map { it.hiveId }.toSet()
        assertEquals("$where: queue vs waiting runs", waiting, s.queue.toSet())
        s.queue.forEach { h -> assertTrue("$where: queued $h not at a device step", s.runs.getValue(h).inDeviceStep) }

        // 4. Each hive's platform alarm matches what its run needs.
        for (h in hives) {
            val want = s.runs[h]?.let(::desiredAlarm)
            assertEquals("$where: alarm for hive $h", want, t.alarms[h])
        }

        // 5. The shared invariant checker (also run by debug builds after every event) agrees.
        val forecast = Planner().forecast(s, t.now)
        val problems = Invariants.check(s, t.alarms, forecast, t.now)
        assertTrue("$where: ${problems.joinToString()}", problems.isEmpty())

        // 6. Running timed steps have an end time; terminal runs have no alarm and no lock.
        runs.forEach { r ->
            if (r.state == RunState.RUNNING) assertTrue("$where: running without end", r.stepEndsAt != null)
            if (r.isTerminal) assertTrue("$where: terminal ${r.hiveId} holds lock", s.lockHolder != r.hiveId)
        }
    }

    private companion object {
        const val EVENTS = 1_000
    }
}
