package io.github.mecheng70.beetimer.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import io.github.mecheng70.beetimer.alarms.AlarmInfo
import io.github.mecheng70.beetimer.ui.theme.BeeTimerTheme
import io.github.mecheng70.beetimer.ui.theme.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Alarm screen readability (T-013a, Jason 2026-11-23): at font scale 1.15 and 1.3 on a phone-size screen,
 * everything shows, the next action is never cut off (two lines at most), and the information never reaches
 * the buttons.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AlarmLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    private val full = AlarmInfo(
        finished = "Remove wand finished (overdue)",
        finishedStep = "Remove wand",
        nextAction = "Load cap and inject a new dose",
        timerLine = "10:00 timer starts when you tap Done",
        // R12 (Jason): the Then line replaces the device note; they never show together.
        deviceNote = null,
        thenLine = "Then: Sparky, load dose",
    )

    private fun show(fontScale: Float, info: AlarmInfo = full, label: String = "Emory") {
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                BeeTimerTheme(ThemeMode.LIGHT) { AlarmContent(label, info, {}, {}, {}) }
            }
        }
    }

    private fun check(fontScale: Float) {
        show(fontScale)
        listOf(
            "Emory",
            "Remove wand finished (overdue)",
            "Next for Emory",
            "10:00 timer starts when you tap Done",
            "Then: Sparky, load dose",
        ).forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        val next = compose.onNodeWithTag("alarm-next").fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        next.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        val layout = layouts.single()
        assertFalse("next action cut off at $fontScale", layout.hasVisualOverflow)
        assertTrue("next action over 2 lines at $fontScale", layout.lineCount <= 2)

        val info = compose.onNodeWithTag("alarm-info").getBoundsInRoot()
        val done = compose.onNodeWithTag("alarm-done").getBoundsInRoot()
        val lastText = compose.onNodeWithText("Then: Sparky, load dose").getBoundsInRoot()
        assertTrue("info runs into Done at $fontScale", info.bottom <= done.top)
        assertTrue("last line runs past the info block at $fontScale", lastText.bottom <= info.bottom)
    }

    @Test
    fun fitsAtFontScale115() = check(1.15f)

    @Test
    fun fitsAtFontScale130() = check(1.3f)

    @Test
    fun noteWithoutThenLineFitsAtFontScale130() {
        // Done frees the vaporizer but no hive is next: the plain note shows in place of the Then line.
        show(1.3f, full.copy(thenLine = null, deviceNote = "Vaporizer frees when you tap Done."))
        compose.onNodeWithTag("alarm-then").assertDoesNotExist()
        val note = compose.onNodeWithText("Vaporizer frees when you tap Done.").assertIsDisplayed().getBoundsInRoot()
        assertTrue(
            "note runs past the info block",
            note.bottom <= compose.onNodeWithTag("alarm-info").getBoundsInRoot().bottom,
        )
    }

    @Test
    fun thenLineOnlyWhenGiven() {
        show(1.15f, full.copy(thenLine = null, deviceNote = null))
        compose.onNodeWithTag("alarm-then").assertDoesNotExist()
    }
}
