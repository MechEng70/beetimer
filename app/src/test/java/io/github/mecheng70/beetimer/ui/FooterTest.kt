package io.github.mecheng70.beetimer.ui

import io.github.mecheng70.beetimer.scheduler.HiveRun
import io.github.mecheng70.beetimer.scheduler.RunState
import io.github.mecheng70.beetimer.scheduler.Step
import io.github.mecheng70.beetimer.scheduler.StepKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FooterTest {
    private val steps = listOf(Step("Unseal", StepKind.MANUAL))
    private val hives = listOf(1L, 2L, 3L)

    private fun run(id: Long, state: RunState, completedAt: Long? = null) =
        HiveRun(hiveId = id, steps = steps, state = state, completedAt = completedAt)

    @Test
    fun allStarted_isEstimated() {
        val runs = mapOf(
            1L to run(1, RunState.RUNNING),
            2L to run(2, RunState.WAITING_FOR_DEVICE),
            3L to run(3, RunState.DONE, completedAt = 50),
        )
        assertEquals(Footer.Estimated(900), footerFor(hives, runs, allDoneAt = 900))
    }

    @Test
    fun someNotStarted_countsStartedOfTotal() {
        val runs = mapOf(1L to run(1, RunState.RUNNING), 2L to run(2, RunState.DONE, completedAt = 50))
        assertEquals(Footer.EstimatedPartial(900, started = 2, total = 3), footerFor(hives, runs, allDoneAt = 900))
    }

    @Test
    fun cancelledRun_countsAsNotStarted() {
        val runs = mapOf(1L to run(1, RunState.RUNNING), 2L to run(2, RunState.CANCELLED))
        assertEquals(Footer.EstimatedPartial(900, started = 1, total = 3), footerFor(hives, runs, allDoneAt = 900))
    }

    @Test
    fun everyRunDone_isAllDoneAtLatestCompletion() {
        val runs = hives.associateWith { run(it, RunState.DONE, completedAt = it * 100) }
        assertEquals(Footer.AllDone(300), footerFor(hives, runs, allDoneAt = null))
    }

    @Test
    fun nothingRunning_andNotAllDone_showsNothing() {
        assertNull(footerFor(hives, emptyMap(), allDoneAt = null))
        val runs = mapOf(1L to run(1, RunState.DONE, completedAt = 50))
        assertNull(footerFor(hives, runs, allDoneAt = null))
    }
}
