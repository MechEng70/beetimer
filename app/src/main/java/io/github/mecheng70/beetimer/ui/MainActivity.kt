package io.github.mecheng70.beetimer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import io.github.mecheng70.beetimer.ui.theme.BeeTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mecheng70.beetimer.ui.theme.BeeTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = app
        setContent {
            val theme by app.prefs.theme.collectAsStateWithLifecycle()
            BeeTimerTheme(theme) {
                val c = BeeTheme.colors
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !c.isDark
                        isAppearanceLightNavigationBars = !c.isDark
                    }
                }
                Box(Modifier.fillMaxSize().background(c.page).safeDrawingPadding()) { Root() }
            }
        }
    }

    @Composable
    private fun Root() {
        val app = app
        var screen by rememberSaveable {
            mutableStateOf(if (!app.prefs.permsShown || Permissions.anyRequiredMissing(this)) Screen.PERMISSIONS else Screen.MAIN)
        }
        val state by app.engine.state.collectAsStateWithLifecycle()
        val keepOn by app.prefs.keepScreenOn.collectAsStateWithLifecycle()
        val anyActive = state?.runs?.values?.any { it.isActive } == true
        val view = LocalView.current
        DisposableEffect(keepOn, anyActive) {
            view.keepScreenOn = keepOn && anyActive
            onDispose { view.keepScreenOn = false }
        }
        BackHandler(enabled = screen != Screen.MAIN) { screen = Screen.MAIN }
        when (screen) {
            Screen.MAIN -> MainScreen(onSettings = { screen = Screen.SETTINGS })
            Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.MAIN }, onAlerts = { screen = Screen.PERMISSIONS })
            Screen.PERMISSIONS -> PermissionsScreen(onContinue = {
                app.prefs.permsShown = true
                screen = Screen.MAIN
            })
        }
    }

    private enum class Screen { MAIN, SETTINGS, PERMISSIONS }
}
