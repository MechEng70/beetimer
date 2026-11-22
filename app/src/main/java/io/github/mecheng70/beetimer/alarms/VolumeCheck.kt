package io.github.mecheng70.beetimer.alarms

/**
 * Low alarm volume warning: checked on the first Start of a session, shown at most once per session.
 * The first Start uses up the check even when the volume is fine, so a later Start never warns.
 */
class VolumeCheck(private val store: Store) {
    /** Remembers whether this session's check is used up. Kept across process death. */
    interface Store {
        var checkedThisSession: Boolean
    }

    /** Called when the user taps Start. True when the warning should show now. */
    fun onStart(volume: Int, max: Int): Boolean {
        if (store.checkedThisSession) return false
        store.checkedThisSession = true
        return isLow(volume, max)
    }

    /** A new session gets one check again. */
    fun onNewSession() {
        store.checkedThisSession = false
    }

    companion object {
        /** Under half of the alarm stream's range. */
        fun isLow(volume: Int, max: Int): Boolean = volume * 2 < max.coerceAtLeast(1)
    }
}
