package io.github.mecheng70.beetimer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import io.github.mecheng70.beetimer.scheduler.DefaultScheduler
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.Planner
import io.github.mecheng70.beetimer.scheduler.Presets
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.ui.theme.BeeTimerTheme
import io.github.mecheng70.beetimer.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Playbook layout rule (Jason, 2026-11-23): nothing above or at a tapped control may move. The vaporizer bar is
 * always two lines and Coming up always three rows, whatever happens, so the hive rows under them stay put.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-xxhdpi")
class FixedLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    private val scheduler = DefaultScheduler()
    private val planner = Planner()
    private val labels = mapOf(EMORY to "Emory", SPARKY to "Sparky", HERB to "Herb")

    private fun SchedulerState.then(vararg events: Event): SchedulerState =
        events.fold(this) { s, e -> scheduler.reduce(s, e, NOW).state }

    private val idle = SchedulerState(template = Presets.panWandStaysIn)

    // Emory holds the vaporizer (Seal hive done, now on Load dose).
    private val holding = idle.then(Event.Start(EMORY), Event.Done(EMORY))

    // Sparky waits behind Emory.
    private val queued = holding.then(Event.Start(SPARKY), Event.Done(SPARKY))

    // Emory cancelled: the vaporizer is free and Sparky is next.
    private val freeForSparky = queued.then(Event.Cancel(EMORY))

    private fun height(tag: String): Dp = compose.onNodeWithTag(tag).getBoundsInRoot().let { it.bottom - it.top }

    @Test
    fun vaporizerBarKeepsItsHeightThroughEveryState() {
        var state by mutableStateOf(idle)
        var nextUp by mutableStateOf<Long?>(null)
        compose.setContent {
            BeeTimerTheme(ThemeMode.LIGHT) {
                DeviceStatusBar(state, labels, NOW, nextUp, planner.forecast(state, NOW))
            }
        }
        val heights = mutableListOf<Dp>()
        fun measure(s: SchedulerState, next: Long?) {
            state = s
            nextUp = next
            compose.waitForIdle()
            heights += height("vaporizer-bar")
        }
        measure(idle, null)
        measure(idle, EMORY)
        measure(holding, SPARKY)
        measure(queued, SPARKY)
        measure(freeForSparky, SPARKY)
        compose.onNodeWithTag("vaporizer-bar-next").assertTextEquals("Next: Sparky. Tap Start on its row.")
        compose.onNodeWithText("Start").assertDoesNotExist()
        compose.onNodeWithText("Skip to back").assertDoesNotExist()
        assertEquals("bar heights $heights", 1, heights.distinct().size)
    }

    @Test
    fun comingUpKeepsThreeRowsFromFirstLaunch() {
        var state by mutableStateOf(idle)
        compose.setContent {
            BeeTimerTheme(ThemeMode.LIGHT) {
                ComingUp(planner.forecast(state, NOW).comingUp(COMING_UP_COUNT), labels)
            }
        }
        compose.onNodeWithText("Start a hive to see what's next").assertExists()
        val heights = mutableListOf(height("coming-up"))
        listOf(holding, queued, freeForSparky, freeForSparky.then(Event.Start(SPARKY))).forEach {
            state = it
            compose.waitForIdle()
            heights += height("coming-up")
        }
        compose.onNodeWithText("Start a hive to see what's next").assertDoesNotExist()
        assertEquals("coming up heights $heights", 1, heights.distinct().size)
    }

    private companion object {
        const val EMORY = 1L
        const val SPARKY = 2L
        const val HERB = 3L
        const val NOW = 1_000_000L
    }
}
