package io.github.mecheng70.beetimer.scheduler

import io.github.mecheng70.beetimer.scheduler.Event.Done
import io.github.mecheng70.beetimer.scheduler.Event.Start
import io.github.mecheng70.beetimer.scheduler.PlanningEstimates.HANDOFF_MS
import io.github.mecheng70.beetimer.scheduler.PlanningEstimates.MANUAL_STEP_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {
    private val a = 1L
    private val b = 2L
    private val c = 3L
    private val planner = Planner()

    /** Seal hive, Load dose (device), Heat 100 s (device), Hold 300 s (passive), Open. */
    private val tpl = template(
        manual("Seal"),
        manual("Load", device = true),
        timed("Heat", 100, device = true),
        timed("Hold", 300),
        manual("Open"),
    )

    @Test
    fun runningTimedStep_nextStepTimeIsFirm() {
        val h = Harness(tpl)
        h.send(Start(a)); h.send(Done(a)); h.send(Done(a)) // Heat runs 0..100 s
        val f = planner.forecast(h.state, h.now)
        val next = f.hives.getValue(a).next!!
        assertEquals("Hold", next.stepName)
        assertEquals(100 * SEC, next.at)
        assertFalse(next.estimated)
    }

    @Test
    fun stepsAfterAManualStepAreEstimated() {
        val h = Harness(tpl)
        h.send(Start(a)) // on Seal (manual)
        val f = planner.forecast(h.state, h.now)
        val load = f.steps.first { it.stepName == "Load" }
        assertEquals(MANUAL_STEP_MS, load.at)
        assertTrue(load.estimated)
        assertTrue(f.steps.filter { it.hiveId == a }.all { it.estimated })
    }

    @Test
    fun queuedHive_startsAfterHolderReleasesPlusHandoff() {
        val h = Harness(tpl)
        listOf(a, b).forEach { h.send(Start(it)); h.send(Done(it)) }
        h.send(Done(a)) // a heats 0..100 s; b queued
        val f = planner.forecast(h.state, h.now)
        val bStart = f.hives.getValue(b).deviceStart!!
        assertEquals("Load", bStart.stepName)
        assertEquals(100 * SEC + HANDOFF_MS, bStart.at)
        assertTrue(bStart.estimated)
    }

    @Test
    fun comingUp_isTimeOrderedAcrossHivesAndLimitedToFive() {
        val h = Harness(tpl)
        listOf(a, b).forEach { h.send(Start(it)); h.send(Done(it)) }
        h.send(Done(a))
        val f = planner.forecast(h.state, h.now)
        val up = f.comingUp()
        assertEquals(5, up.size)
        assertEquals(up.sortedBy { it.at }, up)
        assertEquals(setOf(a, b), up.map { it.hiveId }.toSet())
    }

    @Test
    fun notStartedHivesAreNotProjected() {
        val h = Harness(tpl)
        h.send(Start(a)); h.send(Done(a)); h.send(Done(a))
        val f = planner.forecast(h.state, h.now)
        assertEquals(setOf(a), f.hives.keys)
        assertTrue(f.steps.all { it.hiveId == a })
    }

    @Test
    fun estimatesComeFromTheStateNotTheClock() {
        // Recomputed only when the state changes: the same state gives the same times, so
        // they hold still while a person works (the UI shows "Now" once a time passes).
        val h = Harness(tpl)
        h.send(Start(a)) // Seal (manual) started at 0
        val first = planner.forecast(h.state, h.now)
        assertEquals(first, planner.forecast(h.state, h.now))
        assertEquals(MANUAL_STEP_MS, first.steps.first().at)
    }

    @Test
    fun allDone_isTheLastFinishAndEstimatedWhenManualStepsRemain() {
        val h = Harness(tpl)
        h.send(Start(a)); h.send(Done(a)); h.send(Done(a))
        val f = planner.forecast(h.state, h.now)
        // Heat ends 100 s, Hold ends 400 s, Open takes one manual estimate.
        assertEquals(400 * SEC + MANUAL_STEP_MS, f.allDoneAt)
        assertTrue(f.allDoneEstimated)
    }

    @Test
    fun nothingActiveOrPending_hasNoFinishTime() {
        val h = Harness(tpl)
        h.send(Start(a)); h.send(Event.Cancel(a))
        val f = planner.forecast(h.state, h.now)
        assertNull(f.allDoneAt)
        assertTrue(f.steps.isEmpty())
    }

    @Test
    fun nextUp_isQueueHeadElseFirstNotStartedInListOrder() {
        val h = Harness(tpl)
        assertEquals(b, planner.nextUp(h.state, listOf(b, a)))
        h.send(Start(a)); h.send(Done(a)) // a holds the device
        h.send(Start(c)); h.send(Done(c)) // c queued
        assertEquals(c, planner.nextUp(h.state, listOf(b, a, c)))
    }

    @Test
    fun forecastNeverChangesTheRealState() {
        val h = Harness(tpl)
        listOf(a, b).forEach { h.send(Start(it)); h.send(Done(it)) }
        val before = h.state
        planner.forecast(h.state, h.now)
        assertEquals(before, h.state)
    }

    @Test
    fun forecastTerminatesForEveryPropertyTestState() {
        // Reuse the randomized sessions: the planner must finish and stay time-ordered.
        val rnd = kotlin.random.Random(7)
        val h = Harness(Presets.panWand)
        val hives = (1L..5L).toList()
        repeat(300) {
            h.now += rnd.nextLong(0, 60) * SEC
            val id = hives.random(rnd)
            when (rnd.nextInt(5)) {
                0 -> h.send(Start(id))
                1 -> h.send(Done(id))
                2 -> h.alarms.minByOrNull { it.value }?.let { h.now = maxOf(h.now, it.value); h.send(Event.TimerFired(it.key)) }
                3 -> h.send(Event.Skip(id))
                else -> h.send(Event.Cancel(id))
            }
            val f = planner.forecast(h.state, h.now)
            assertEquals(f.steps.sortedBy { it.at }, f.steps)
            assertTrue(f.steps.all { it.at >= h.now })
        }
    }
}
