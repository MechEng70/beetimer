package io.github.mecheng70.beetimer.ui

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.github.mecheng70.beetimer.BeeTimerApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Regression: an alarm screen the app opened while on screen closed to the home screen after
 * Done (it runs in its own task). It must go back to the app. Found by the E2E autopilot.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmActivityCloseTest {
    private fun closeWith(returnToApp: Boolean): Pair<AlarmActivity, String?> {
        val app = ApplicationProvider.getApplicationContext<BeeTimerApp>()
        runBlocking { app.engine.ensureLoaded() } // no runs, so nothing rings and the screen closes
        val controller = Robolectric.buildActivity(AlarmActivity::class.java, AlarmActivity.intent(app, 1L, returnToApp)).setup()
        // Let state load and the screen recompose and close (the first test in the JVM is cold).
        repeat(40) {
            if (!controller.get().isFinishing) {
                Thread.sleep(25)
                shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
            }
        }
        val next = shadowOf(controller.get()).nextStartedActivity
        return controller.get() to next?.component?.className
    }

    @Test
    fun openedByTheApp_closesBackToTheApp() {
        val (activity, next) = closeWith(returnToApp = true)
        assertTrue("finishing=${activity.isFinishing} next=$next", activity.isFinishing)
        assertEquals(MainActivity::class.java.name, next)
    }

    @Test
    fun openedFromLockScreenOrNotification_justFinishes() {
        val (activity, next) = closeWith(returnToApp = false)
        assertTrue(activity.isFinishing)
        assertNull(next)
    }
}
