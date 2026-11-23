package io.github.mecheng70.beetimer.ui

import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.ui.theme.AppType
import io.github.mecheng70.beetimer.ui.theme.TABULAR_DIGITS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Playbook layout rule (R11 T-011): a ticking countdown keeps its width, so nothing next to it moves. Measured with
 * the bundled Inter Bold in Robolectric's native graphics.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabularDigitsTest {
    private fun paint(features: String?) = Paint().apply {
        typeface = ResourcesCompat.getFont(ApplicationProvider.getApplicationContext(), R.font.inter_bold)
        textSize = TEXT_PX
        fontFeatureSettings = features
    }

    @Test
    fun countdownDigitsAllHaveTheSameWidth() {
        val p = paint(TABULAR_DIGITS)
        val widths = listOf("1:11", "0:00", "8:88", "~11:11", "~44:44").map { it.length to p.measureText(it) }
        widths.groupBy({ it.first }, { it.second }).forEach { (_, same) ->
            same.forEach { assertEquals(same.first(), it, 0.01f) }
        }
    }

    @Test
    fun withoutTabularDigitsTheWidthChanges() {
        // Shows the feature is what keeps the width: Inter's default digits are proportional.
        val p = paint(null)
        assertNotEquals(p.measureText("1:11"), p.measureText("0:00"), 0.01f)
    }

    @Test
    fun everyTickingTextStyleUsesTabularDigits() {
        listOf(AppType.timeChip, AppType.countdown, AppType.rowNext).forEach {
            assertEquals(TABULAR_DIGITS, it.fontFeatureSettings)
        }
    }

    private companion object {
        const val TEXT_PX = 60f
    }
}
