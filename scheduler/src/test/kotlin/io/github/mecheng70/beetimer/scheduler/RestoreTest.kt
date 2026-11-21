package io.github.mecheng70.beetimer.scheduler

import io.github.mecheng70.beetimer.scheduler.Effect.NotifyDeviceFree
import io.github.mecheng70.beetimer.scheduler.Effect.Ring
import io.github.mecheng70.beetimer.scheduler.Effect.ScheduleAlarm
import io.github.mecheng70.beetimer.scheduler.Event.Cancel
import io.github.mecheng70.beetimer.scheduler.Event.Done
import io.github.mecheng70.beetimer.scheduler.Event.Start
import io.github.mecheng70.beetimer.scheduler.RunState.AWAITING_CONFIRM
import io.github.mecheng70.beetimer.scheduler.RunState.RUNNING
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreTest {
    private val a = 1L
    private val b = 2L

    /** Persist, "reboot" (lose every platform alarm), and restore at [atSeconds]. */
    private fun Harness.rebootAt(atSeconds: Long): List<Effect> {
        val json = Json.encodeToString(SchedulerState.serializer(), state)
        state = Json.decodeFromString(SchedulerState.serializer(), json)
        alarms.clear()
        at(atSeconds)
        return restore()
    }

    private fun Harness.heating(h: Long) {
        send(Start(h)); send(Done(h)); send(Done(h)) // Heat runs 0..150 s
    }

    @Test
    fun beforeStepEnd_keepsStepAndSchedulesFutureAlarm() {
        val t = Harness()
        t.heating(a)
        val fx = t.rebootAt(100)
        t.assertAt(a, "Heat and vaporize", RUNNING)
        assertEquals(listOf(ScheduleAlarm(a, 150 * SEC)), fx)
    }

    @Test
    fun afterTimedStepEnd_nextTimed_ringsOverdueAndWaits() {
        val t = Harness()
        t.heating(a)
        val fx = t.rebootAt(400)
        t.assertAt(a, "Heat and vaporize", AWAITING_CONFIRM)
        assertTrue(t.run(a).ringing!!.overdue)
        assertEquals(150 * SEC, t.run(a).ringing!!.since)
        assertTrue(fx.contains(Ring(a, 2, overdue = true)))
        assertEquals(listOf(ScheduleAlarm(a, 400 * SEC + SchedulerTiming.RING_CYCLE_MS)), fx.filterIsInstance<ScheduleAlarm>())
    }

    @Test
    fun afterTimedStepEnd_nextManual_movesToManualStepAndRingsOverdue() {
        val t = Harness()
        t.heating(a)
        t.fire(a); t.send(Done(a)) // Cool runs 150..270 s
        val fx = t.rebootAt(1000)
        t.assertAt(a, "Remove wand", AWAITING_CONFIRM)
        assertEquals(a, t.state.lockHolder)
        assertTrue(fx.contains(Ring(a, 3, overdue = true)))
    }

    @Test
    fun missedAlarmsNeverCascadeThroughSeveralSteps() {
        val t = Harness()
        t.heating(a)
        t.rebootAt(100_000) // Heat, Cool, and Keep sealed would all be long over
        t.assertAt(a, "Heat and vaporize", AWAITING_CONFIRM)
    }

    @Test
    fun onlyFutureAlarmsAreScheduled() {
        val t = Harness()
        t.heating(a)
        t.send(Start(b)) // b on Seal hive, no alarm
        val fx = t.rebootAt(5000)
        fx.filterIsInstance<ScheduleAlarm>().forEach { assertTrue("$it is in the past", it.atMs > t.now) }
        assertEquals(setOf(a), t.alarms.keys)
    }

    @Test
    fun ringingAtReboot_ringsAgain() {
        val t = Harness()
        t.heating(a)
        t.fire(a) // ringing, not acknowledged
        val fx = t.rebootAt(200)
        assertTrue(fx.contains(Ring(a, 2, overdue = false)))
        assertEquals(200 * SEC + SchedulerTiming.RING_CYCLE_MS, t.alarms[a])
    }

    @Test
    fun deviceFreeWithQueue_remindsHead() {
        val t = Harness()
        listOf(a, b).forEach { t.send(Start(it)); t.send(Done(it)) }
        t.send(Cancel(a))
        val fx = t.rebootAt(60)
        assertEquals(listOf(NotifyDeviceFree(b)), fx.filterIsInstance<NotifyDeviceFree>())
    }

    @Test
    fun restoreIsStableWhenNothingIsDue() {
        val t = Harness()
        t.heating(a)
        t.rebootAt(10)
        val before = t.state
        t.rebootAt(20)
        assertEquals(before, t.state)
    }
}
