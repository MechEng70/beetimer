package io.github.mecheng70.beetimer.data

import android.content.Context
import androidx.core.content.edit
import io.github.mecheng70.beetimer.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small per-device UI settings. Scheduler data lives in Room. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(sp.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeMode.SYSTEM),
    )
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(sp.getBoolean(KEY_SCREEN_ON, true))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    var batteryAsked: Boolean
        get() = sp.getBoolean(KEY_BATTERY_ASKED, false)
        set(v) = sp.edit { putBoolean(KEY_BATTERY_ASKED, v) }

    var permsShown: Boolean
        get() = sp.getBoolean(KEY_PERMS_SHOWN, false)
        set(v) = sp.edit { putBoolean(KEY_PERMS_SHOWN, v) }

    /** True once this session's low alarm volume check has run (first Start). Cleared by New session. */
    var volumeChecked: Boolean
        get() = sp.getBoolean(KEY_VOLUME_CHECKED, false)
        set(v) = sp.edit { putBoolean(KEY_VOLUME_CHECKED, v) }

    fun setTheme(mode: ThemeMode) {
        sp.edit { putString(KEY_THEME, mode.name) }
        _theme.value = mode
    }

    fun setKeepScreenOn(on: Boolean) {
        sp.edit { putBoolean(KEY_SCREEN_ON, on) }
        _keepScreenOn.value = on
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_SCREEN_ON = "keepScreenOn"
        const val KEY_BATTERY_ASKED = "batteryAsked"
        const val KEY_PERMS_SHOWN = "permsShown"
        const val KEY_VOLUME_CHECKED = "volumeChecked"
    }
}
