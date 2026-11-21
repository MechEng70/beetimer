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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.ui.theme.Dimens
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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

@OptIn(ExperimentalComposeUiApi::class)
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
                Box(
                    Modifier.fillMaxSize().background(c.page).safeDrawingPadding()
                        .semantics { testTagsAsResourceId = true },
                ) {
                    Root()
                    DeviceFreeSnackbar(Modifier.align(Alignment.BottomCenter))
                }
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

    /**
     * "Vaporizer free: Herb is next" while the app is on screen, at the bottom so it never
     * covers the top bar or the vaporizer bar's buttons. Start hands the vaporizer over.
     */
    @Composable
    private fun DeviceFreeSnackbar(modifier: Modifier) {
        val app = app
        val host = remember { SnackbarHostState() }
        val message = stringResource(R.string.device_free_next)
        val start = stringResource(R.string.action_start)
        LaunchedEffect(Unit) {
            app.deviceFreeInApp.collect { (id, label) ->
                host.currentSnackbarData?.dismiss()
                val result = host.showSnackbar(message.replace("%1\$s", label), actionLabel = start, duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) app.send(Event.Start(id))
            }
        }
        val c = BeeTheme.colors
        SnackbarHost(host, modifier.padding(Dimens.PageMargin).testTag("snackbar-device-free")) { data ->
            Snackbar(data, containerColor = c.ink, contentColor = c.card, actionColor = c.deviceStrong)
        }
    }
}
