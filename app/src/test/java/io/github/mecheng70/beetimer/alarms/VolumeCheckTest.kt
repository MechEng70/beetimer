package io.github.mecheng70.beetimer.alarms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeCheckTest {
    private class FakeStore : VolumeCheck.Store {
        override var checkedThisSession = false
    }

    @Test
    fun lowVolume_warnsOnFirstStartOnly() {
        val check = VolumeCheck(FakeStore())
        assertTrue(check.onStart(volume = 1, max = 16))
        assertFalse(check.onStart(volume = 1, max = 16))
        assertFalse(check.onStart(volume = 0, max = 16))
    }

    @Test
    fun okOnFirstStart_laterLowStartDoesNotWarn() {
        val check = VolumeCheck(FakeStore())
        assertFalse(check.onStart(volume = 12, max = 16))
        assertFalse(check.onStart(volume = 1, max = 16))
    }

    @Test
    fun newSession_allowsOneMoreWarning() {
        val check = VolumeCheck(FakeStore())
        assertTrue(check.onStart(volume = 1, max = 16))
        check.onNewSession()
        assertTrue(check.onStart(volume = 1, max = 16))
        assertFalse(check.onStart(volume = 1, max = 16))
    }

    @Test
    fun usedUpCheck_survivesARestart() {
        val store = FakeStore()
        assertTrue(VolumeCheck(store).onStart(volume = 1, max = 16))
        assertFalse(VolumeCheck(store).onStart(volume = 1, max = 16))
    }

    @Test
    fun halfOrMore_isNotLow() {
        assertTrue(VolumeCheck.isLow(7, 16))
        assertFalse(VolumeCheck.isLow(8, 16))
        assertFalse(VolumeCheck.isLow(16, 16))
        assertTrue(VolumeCheck.isLow(0, 0))
    }
}
