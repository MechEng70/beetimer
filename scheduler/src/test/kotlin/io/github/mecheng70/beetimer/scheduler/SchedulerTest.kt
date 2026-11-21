package io.github.mecheng70.beetimer.scheduler

import io.github.mecheng70.beetimer.scheduler.Effect.CancelAlarm
import io.github.mecheng70.beetimer.scheduler.Effect.NotifyDeviceFree
import io.github.mecheng70.beetimer.scheduler.Effect.Ring
import io.github.mecheng70.beetimer.scheduler.Effect.ScheduleAlarm
import io.github.mecheng70.beetimer.scheduler.Effect.StopRing
import io.github.mecheng70.beetimer.scheduler.Event.AddMinute
import io.github.mecheng70.beetimer.scheduler.Event.Cancel
import io.github.mecheng70.beetimer.scheduler.Event.Done
import io.github.mecheng70.beetimer.scheduler.Event.QueueRemoveHead
import io.github.mecheng70.beetimer.scheduler.Event.QueueSkipHead
import io.github.mecheng70.beetimer.scheduler.Event.Skip
import io.github.mecheng70.beetimer.scheduler.Event.Snooze
import io.github.mecheng70.beetimer.scheduler.Event.Start
import io.github.mecheng70.beetimer.scheduler.Event.TimerFired
import io.github.mecheng70.beetimer.scheduler.RunState.AWAITING_CONFIRM
import io.github.mecheng70.beetimer.scheduler.RunState.CANCELLED
import io.github.mecheng70.beetimer.scheduler.RunState.DONE
import io.github.mecheng70.beetimer.scheduler.RunState.NOT_STARTED
import io.github.mecheng70.beetimer.scheduler.RunState.RUNNING
import io.github.mecheng70.beetimer.scheduler.RunState.WAITING_FOR_DEVICE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {
    private val a = 1L
    private val b = 2L
    private val c = 3L

    /** Takes hive [h] from Start through its device block on the pan wand preset. */
    private fun Harness.panWandThroughDeviceBlock(h: Long) {
        send(Done(h)) // Load dose done
        assertAt(h, "Heat and vaporize", RUNNING)
        fire(h)
        assertAt(h, "Heat and vaporize", AWAITING_CONFIRM) // timed -> timed waits for you
        send(Done(h))
        assertAt(h, "Cool in hive", RUNNING)
        fire(h)
        assertAt(h, "Remove wand", AWAITING_CONFIRM) // timed -> manual moves on and rings
        send(Done(h))
    }

    @Test
    fun twoHivesStartedTogether_secondWaitsForReleaseAndStart() {
        val t = Harness()
        t.send(Start(a))
        t.send(Start(b))
        t.assertAt(a, "Seal hive", AWAITING_CONFIRM)
        t.assertAt(b, "Seal hive", AWAITING_CONFIRM)
        assertNull(t.state.lockHolder)

        t.at(10).send(Done(a))
        t.assertAt(a, "Load dose", AWAITING_CONFIRM)
        assertEquals(a, t.state.lockHolder)

        t.at(20).send(Done(b))
        t.assertAt(b, "Load dose", WAITING_FOR_DEVICE)
        assertEquals(listOf(b), t.state.queue)

        // Start does nothing while the device is busy.
        t.send(Start(b))
        t.assertAt(b, "Load dose", WAITING_FOR_DEVICE)

        t.at(30)
        t.panWandThroughDeviceBlock(a)
        t.assertAt(a, "Keep sealed", RUNNING)
        assertNull(t.state.lockHolder)
        assertTrue(t.last.contains(NotifyDeviceFree(b)))
        // Handoff is not automatic.
        t.assertAt(b, "Load dose", WAITING_FOR_DEVICE)

        t.send(Start(b))
        t.assertAt(b, "Load dose", AWAITING_CONFIRM)
        assertEquals(b, t.state.lockHolder)
        assertTrue(t.state.queue.isEmpty())
    }

    @Test
    fun passiveOverlap_sealedHiveDoesNotHoldDevice() {
        val t = Harness()
        t.send(Start(a))
        t.send(Done(a))
        t.panWandThroughDeviceBlock(a)
        t.assertAt(a, "Keep sealed", RUNNING)

        t.send(Start(b))
        t.send(Done(b))
        assertEquals(b, t.state.lockHolder)
        t.send(Done(b))
        t.assertAt(b, "Heat and vaporize", RUNNING)
        t.assertAt(a, "Keep sealed", RUNNING)
        assertFalse(t.run(a).inDeviceStep)
    }

    @Test
    fun cancelWhileHoldingLock_releasesAndAlertsQueueHead() {
        val t = Harness()
        listOf(a, b, c).forEach { t.send(Start(it)); t.send(Done(it)) }
        assertEquals(a, t.state.lockHolder)
        assertEquals(listOf(b, c), t.state.queue)

        val fx = t.send(Cancel(a))
        assertEquals(NOT_STARTED, t.state.stateOf(a))
        assertNull(t.state.lockHolder)
        assertEquals(listOf(NotifyDeviceFree(b)), fx.filterIsInstance<NotifyDeviceFree>())
        assertEquals(listOf(b, c), t.state.queue)
    }

    @Test
    fun cancelWhileRunningTimedStep_cancelsItsAlarm() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a))
        assertNotNull(t.alarms[a])
        val fx = t.send(Cancel(a))
        assertTrue(fx.contains(CancelAlarm(a)))
        assertNull(t.alarms[a])
    }

    @Test
    fun cancelWhileQueued_leavesLockAlone() {
        val t = Harness()
        listOf(a, b, c).forEach { t.send(Start(it)); t.send(Done(it)) }
        val fx = t.send(Cancel(b))
        assertEquals(NOT_STARTED, t.state.stateOf(b))
        assertEquals(a, t.state.lockHolder)
        assertEquals(listOf(c), t.state.queue)
        assertTrue(fx.none { it is NotifyDeviceFree })
    }

    @Test
    fun cancelQueueHeadWhileDeviceFree_alertsNextHead() {
        val t = Harness()
        listOf(a, b, c).forEach { t.send(Start(it)); t.send(Done(it)) }
        t.send(Cancel(a)) // b is told the device is free
        val fx = t.send(Cancel(b))
        assertEquals(listOf(NotifyDeviceFree(c)), fx.filterIsInstance<NotifyDeviceFree>())
    }

    @Test
    fun deviceReservedForQueueHead_newcomerCannotJumpAhead() {
        val t = Harness()
        listOf(a, b).forEach { t.send(Start(it)); t.send(Done(it)) }
        t.send(Cancel(a)) // device free, reserved for b
        t.send(Start(c)); t.send(Done(c))
        t.assertAt(c, "Load dose", WAITING_FOR_DEVICE)
        assertEquals(listOf(b, c), t.state.queue)
        assertNull(t.state.lockHolder)
    }

    @Test
    fun timedToTimed_nextTimerStartsWhenAcknowledged() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a))
        val fx = t.fire(a) // Heat ends at 150 s
        assertTrue(fx.contains(Ring(a, 2, overdue = false)))
        t.at(170).send(Done(a)) // power off took 20 s
        assertEquals(170 * SEC, t.run(a).stepStartedAt)
        assertEquals((170 + 120) * SEC, t.run(a).stepEndsAt)
        assertEquals((170 + 120) * SEC, t.alarms[a])
    }

    @Test
    fun timedToManual_movesOnAndAlarmDoneCompletesManualStep() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a))
        t.fire(a); t.send(Done(a))
        val fx = t.fire(a) // Cool ends
        assertTrue(fx.contains(Ring(a, 3, overdue = false)))
        t.assertAt(a, "Remove wand", AWAITING_CONFIRM)
        assertEquals(3, t.run(a).ringing!!.finishedStepIndex)

        val done = t.send(Done(a))
        assertTrue(done.contains(StopRing(a)))
        t.assertAt(a, "Keep sealed", RUNNING)
        assertNull(t.state.lockHolder)
    }

    @Test
    fun addMinute_onRunningTimedStepExtendsAndReschedules() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a))
        t.at(50).send(AddMinute(a))
        assertEquals(210 * SEC, t.run(a).stepEndsAt)
        assertEquals(210 * SEC, t.alarms[a])
    }

    @Test
    fun addMinute_whileRinging_reopensFinishedStep() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a))
        t.fire(a); t.send(Done(a)); t.fire(a) // now on Remove wand, ringing for Cool
        val fx = t.send(AddMinute(a))
        assertTrue(fx.contains(StopRing(a)))
        t.assertAt(a, "Cool in hive", RUNNING)
        assertEquals(t.now + 60 * SEC, t.run(a).stepEndsAt)
        assertEquals(a, t.state.lockHolder)
        assertNull(t.run(a).ringing)
    }

    @Test
    fun addMinute_whileRinging_releasesQueueSpotTakenByAdvance() {
        // Wait step then device step: the advance queued the hive; +1 min takes it back out.
        val tpl = template(timed("Wait", 10), manual("Use device", device = true), manual("Finish"))
        val t = Harness(tpl)
        t.send(Start(b)); t.send(Start(a))
        t.send(Skip(b)) // b takes the device
        t.fire(a)
        t.assertAt(a, "Use device", WAITING_FOR_DEVICE)
        assertEquals(listOf(a), t.state.queue)
        t.send(AddMinute(a))
        t.assertAt(a, "Wait", RUNNING)
        assertTrue(t.state.queue.isEmpty())
        assertEquals(b, t.state.lockHolder)
    }

    @Test
    fun snooze_silencesAndRingsAgainIn30s() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a))
        t.fire(a)
        val fx = t.at(160).send(Snooze(a))
        assertTrue(fx.contains(StopRing(a)))
        assertEquals(190 * SEC, t.alarms[a])
        t.assertAt(a, "Heat and vaporize", AWAITING_CONFIRM)
        val again = t.fire(a)
        assertTrue(again.contains(Ring(a, 2, overdue = false)))
    }

    @Test
    fun unacknowledgedAlarm_ringsAgainEveryCycle() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a))
        t.fire(a) // at 150 s
        assertEquals(150 * SEC + SchedulerTiming.RING_CYCLE_MS, t.alarms[a])
        val fx = t.fire(a)
        assertTrue(fx.contains(Ring(a, 2, overdue = false)))
        assertEquals(150 * SEC + 2 * SchedulerTiming.RING_CYCLE_MS, t.alarms[a])
    }

    @Test
    fun earlyTimerFired_reschedulesWithoutAdvancing() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a))
        val fx = t.at(100).send(TimerFired(a))
        assertEquals(listOf(ScheduleAlarm(a, 150 * SEC)), fx)
        t.assertAt(a, "Heat and vaporize", RUNNING)
    }

    @Test
    fun lateTimerFired_isMarkedOverdue() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a))
        val fx = t.at(150 + 61).send(TimerFired(a))
        assertTrue(fx.contains(Ring(a, 2, overdue = true)))
    }

    @Test
    fun skip_lastDeviceStepReleasesLockAndAlertsHead() {
        val t = Harness()
        listOf(a, b).forEach { t.send(Start(it)); t.send(Done(it)) }
        t.send(Skip(a)) // Load dose
        t.send(Skip(a)) // Heat
        t.send(Skip(a)) // Cool
        t.assertAt(a, "Remove wand", AWAITING_CONFIRM)
        val fx = t.send(Skip(a))
        t.assertAt(a, "Keep sealed", RUNNING)
        assertTrue(fx.contains(NotifyDeviceFree(b)))
        assertNull(t.state.lockHolder)
    }

    @Test
    fun skip_whileWaitingForDeviceDoesNothing() {
        val t = Harness()
        listOf(a, b).forEach { t.send(Start(it)); t.send(Done(it)) }
        val before = t.state
        t.send(Skip(b))
        assertEquals(before, t.state)
    }

    @Test
    fun lastStepTimed_ringsAndDoneClearsIt() {
        val t = Harness(template(manual("Seal"), timed("Wait", 30)))
        t.send(Start(a)); t.send(Done(a))
        t.fire(a)
        assertEquals(DONE, t.run(a).state)
        assertNotNull(t.run(a).ringing)
        val fx = t.send(Done(a))
        assertTrue(fx.contains(StopRing(a)))
        assertNull(t.run(a).ringing)
        assertNull(t.alarms[a])
    }

    @Test
    fun queueSkipHead_movesHeadToBackAndAlertsNewHead() {
        val t = Harness()
        listOf(a, b, c).forEach { t.send(Start(it)); t.send(Done(it)) }
        t.send(Cancel(a))
        val fx = t.send(QueueSkipHead)
        assertEquals(listOf(c, b), t.state.queue)
        assertEquals(listOf(NotifyDeviceFree(c)), fx.filterIsInstance<NotifyDeviceFree>())
        t.send(Start(b)) // no longer head
        t.assertAt(b, "Load dose", WAITING_FOR_DEVICE)
        t.send(Start(c))
        assertEquals(c, t.state.lockHolder)
    }

    @Test
    fun queueRemoveHead_resetsThatHive() {
        val t = Harness()
        listOf(a, b, c).forEach { t.send(Start(it)); t.send(Done(it)) }
        t.send(Cancel(a))
        val fx = t.send(QueueRemoveHead)
        assertEquals(NOT_STARTED, t.state.stateOf(b))
        assertEquals(listOf(c), t.state.queue)
        assertTrue(fx.contains(NotifyDeviceFree(c)))
    }

    @Test
    fun templateEditWhileActive_runsKeepTheirSnapshot() {
        val t = Harness(legacyPanWand)
        t.send(Start(a))
        t.send(Event.TemplateChanged(Presets.gunType))
        assertEquals(legacyPanWand.steps, t.run(a).steps)
        t.send(Done(a))
        t.assertAt(a, "Load dose", AWAITING_CONFIRM)
        t.send(Start(b))
        assertEquals(Presets.gunType.steps, t.run(b).steps)
    }

    @Test
    fun cancel_returnsHiveToNotStartedWithNoRunLeft() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a)) // heating with the device
        t.send(Cancel(a))
        assertFalse(t.state.runs.containsKey(a))
        assertEquals(NOT_STARTED, t.state.stateOf(a))
        assertNull(t.alarms[a])
        assertNull(t.state.lockHolder)
    }

    @Test
    fun restore_dropsRunsSavedAsCancelledByOlderBuilds() {
        val t = Harness()
        t.send(Start(a))
        val legacy = t.state.copy(runs = mapOf(a to t.run(a).copy(state = CANCELLED, completedAt = 0)))
        val restored = t.scheduler.restore(legacy, 10 * SEC).state
        assertEquals(NOT_STARTED, restored.stateOf(a))
    }

    @Test
    fun restartAfterCancel_startsFreshRun() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Cancel(a))
        t.at(5).send(Start(a))
        t.assertAt(a, "Seal hive", AWAITING_CONFIRM)
        assertEquals(5 * SEC, t.run(a).startedAt)
    }

    @Test
    fun newSession_clearsRunsAndAlarms() {
        val t = Harness()
        t.send(Start(a)); t.send(Done(a)); t.send(Done(a))
        t.send(Start(b)); t.send(Done(b))
        t.fire(a)
        val fx = t.send(Event.NewSession)
        assertTrue(fx.contains(StopRing(a)))
        assertTrue(t.state.runs.isEmpty())
        assertTrue(t.state.queue.isEmpty())
        assertNull(t.state.lockHolder)
        assertTrue(t.alarms.isEmpty())
    }

    @Test
    fun panWandPreset_sealedHoldKeepsWandAndLastDoneReleasesIt() {
        val t = Harness(Presets.panWand)
        listOf(a, b).forEach { t.send(Start(it)); t.send(Done(it)) }
        t.send(Done(a)) // Load dose
        t.fire(a); t.send(Done(a)) // Heat -> Cool
        t.fire(a); t.send(Done(a)) // Cool -> Keep sealed
        t.assertAt(a, "Keep sealed", RUNNING)
        assertEquals("the sealed hold keeps the wand", a, t.state.lockHolder)
        t.fire(a)
        t.assertAt(a, "Remove wand and unseal", AWAITING_CONFIRM)
        assertEquals(a, t.state.lockHolder)
        val fx = t.send(Done(a))
        assertEquals(DONE, t.run(a).state)
        assertNull(t.state.lockHolder)
        assertTrue(fx.contains(NotifyDeviceFree(b)))
    }

    @Test
    fun templateWithNoDeviceSteps_neverTakesLock() {
        val t = Harness(template(manual("Seal"), timed("Wait", 10), manual("Open")))
        t.send(Start(a)); t.send(Start(b))
        t.send(Done(a)); t.send(Done(b))
        t.assertAt(a, "Wait", RUNNING)
        t.assertAt(b, "Wait", RUNNING)
        assertNull(t.state.lockHolder)
    }
}
