package io.github.mecheng70.beetimer.alarms

import androidx.test.core.app.ApplicationProvider
import io.github.mecheng70.beetimer.scheduler.DefaultScheduler
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.Presets
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The alarm's "Then:" line (T-013a): who gets the vaporizer when Done frees it, and their first step. */
@RunWith(RobolectricTestRunner::class)
class AlarmTextTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val scheduler = DefaultScheduler()

    // List order: Emory, Sparky, Herb.
    private val labels = linkedMapOf(EMORY to "Emory", SPARKY to "Sparky", HERB to "Herb")

    private fun SchedulerState.then(vararg events: Event): SchedulerState =
        events.fold(this) { s, e -> scheduler.reduce(s, e, NOW).state }

    /** The step's alarm when Vaporize ends. A fire before then is ignored and re-scheduled by the scheduler. */
    private fun SchedulerState.vaporizeEnds(hive: Long): SchedulerState =
        scheduler.reduce(this, Event.TimerFired(hive), NOW + Presets.GUN_VAPORIZE_S * 1000L).state

    /** Gun type: Seal hive, Load cap and inject (takes), Vaporize 20 s, Withdraw (releases), ... */
    private val gun = SchedulerState(template = Presets.gunType)

    @Test
    fun queueHeadIsNextWithTheStepItWaitsAt() {
        // Emory takes the vaporizer and vaporizes; Sparky queues at Load cap and inject; Vaporize ends, so Emory
        // rings on Withdraw, whose Done frees the vaporizer.
        val s = gun.then(
            Event.Start(EMORY),
            Event.Done(EMORY),
            Event.Done(EMORY),
            Event.Start(SPARKY),
            Event.Done(SPARKY),
        ).vaporizeEnds(EMORY)
        assertEquals(listOf(SPARKY), s.queue)
        val info = AlarmText.info(context, s.runs.getValue(EMORY), s, labels)
        assertEquals("Then: Sparky, load cap and inject", info.thenLine)
        // R12 (Jason): the Then line replaces "Vaporizer frees when you tap Done. Sparky is next."
        assertNull(info.deviceNote)
    }

    @Test
    fun withNoQueueTheNextNotStartedHiveInListOrderIsNext() {
        val s = gun.then(Event.Start(EMORY), Event.Done(EMORY), Event.Done(EMORY)).vaporizeEnds(EMORY)
        val info = AlarmText.info(context, s.runs.getValue(EMORY), s, labels)
        assertEquals("Then: Sparky, seal hive", info.thenLine)
        assertNull(info.deviceNote)
    }

    @Test
    fun withNoOtherHiveTheNoteShowsWithoutAThenLine() {
        // Only Emory: Done still frees the vaporizer, but nobody is next, so the plain note shows instead.
        val s = gun.then(Event.Start(EMORY), Event.Done(EMORY), Event.Done(EMORY)).vaporizeEnds(EMORY)
        val info = AlarmText.info(context, s.runs.getValue(EMORY), s, linkedMapOf(EMORY to "Emory"))
        assertNull(info.thenLine)
        assertEquals("Vaporizer frees when you tap Done.", info.deviceNote)
    }

    @Test
    fun noThenLineWhenDoneDoesNotFreeTheVaporizer() {
        // Emory is on Seal hive: Done there frees nothing.
        val s = gun.then(Event.Start(EMORY))
        val info = AlarmText.info(context, s.runs.getValue(EMORY), s, labels)
        assertNull(info.thenLine)
        assertNull(info.deviceNote)
    }

    private companion object {
        const val EMORY = 1L
        const val SPARKY = 2L
        const val HERB = 3L
        const val NOW = 1_000_000L
    }
}
