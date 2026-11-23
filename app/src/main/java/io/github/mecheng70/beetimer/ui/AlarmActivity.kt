package io.github.mecheng70.beetimer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.mecheng70.beetimer.DevTools
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.alarms.AlarmText
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.ui.theme.AlarmColors
import io.github.mecheng70.beetimer.ui.theme.AppType
import io.github.mecheng70.beetimer.ui.theme.BeeTimerTheme
import io.github.mecheng70.beetimer.ui.theme.Dimens
import kotlinx.coroutines.launch

/** Full-screen alarm over the lock screen (mockup 3). Dark amber in both themes. */
@OptIn(ExperimentalComposeUiApi::class)
class AlarmActivity : ComponentActivity() {
    private var hiveId by mutableLongStateOf(NO_HIVE)

    /** Set once any open came from the app while it was on screen. */
    private var returnToApp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        hiveId = intent.getLongExtra(EXTRA_HIVE_ID, NO_HIVE)
        returnToApp = intent.getBooleanExtra(EXTRA_RETURN_TO_APP, false)
        // Close when nothing rings any more (watched here rather than in Compose, so it does
        // not depend on recomposition).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.engine.state.collect { s -> if (s != null && s.runs.values.none { it.ringing != null }) close() }
            }
        }
        setContent {
            BeeTimerTheme { Box(Modifier.semantics { testTagsAsResourceId = true }) { AlarmScreen() } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        hiveId = intent.getLongExtra(EXTRA_HIVE_ID, NO_HIVE)
        // A later in-app open adds return-to-app; a notification tap never takes it away.
        returnToApp = returnToApp || intent.getBooleanExtra(EXTRA_RETURN_TO_APP, false)
    }

    @Composable
    private fun AlarmScreen() {
        val context = LocalContext.current
        val app = context.app
        val state by app.engine.state.collectAsStateWithLifecycle()
        val hives by app.repo.hives.collectAsStateWithLifecycle(emptyList())
        val s = state ?: return
        // Show this hive while it rings; then any other ringing hive; then close.
        val run = s.runs[hiveId]?.takeIf { it.ringing != null }
            ?: s.runs.values.sortedBy { it.hiveId }.firstOrNull { it.ringing != null }
        if (run == null) return
        val labels = hives.associate { it.id to it.label }
        val id = run.hiveId

        val info = AlarmText.info(context, run, s, labels)
        LaunchedEffect(id, labels[id]) { labels[id]?.let { DevTools.alarmShown(it) } }
        Column(
            Modifier.fillMaxSize().background(AlarmColors.Background).safeDrawingPadding().padding(Dimens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Information fills the space above the buttons, spread evenly.
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        labels[id].orEmpty(),
                        color = AlarmColors.Text,
                        style = AppType.alarmName,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.testTag("alarm-hive"),
                    )
                    Text(
                        info.finished,
                        color = AlarmColors.Accent,
                        style = AppType.alarmFinished,
                        textAlign = TextAlign.Center,
                    )
                }
                HorizontalDivider(thickness = Dimens.Border, color = AlarmColors.Border)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
                ) {
                    Text(
                        stringResource(R.string.alarm_next_label),
                        color = AlarmColors.Accent,
                        style = AppType.alarmLabel,
                    )
                    Text(
                        info.nextAction,
                        color = AlarmColors.Text,
                        style = AppType.alarmNext,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    info.timerLine?.let {
                        Text(it, color = AlarmColors.Text, style = AppType.alarmTimer, textAlign = TextAlign.Center)
                    }
                    info.deviceNote?.let {
                        Text(it, color = AlarmColors.Accent, style = AppType.alarmNote, textAlign = TextAlign.Center)
                    }
                }
            }
            Button(
                onClick = { app.send(Event.Done(id)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.AlarmButton).testTag("alarm-done"),
                shape = RoundedCornerShape(Dimens.AlarmCorner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlarmColors.Text,
                    contentColor = AlarmColors.Background,
                ),
            ) { Text(stringResource(R.string.action_done), style = AppType.alarmButton) }
            Spacer(Modifier.height(Dimens.Gap))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                AlarmOutlined(
                    stringResource(R.string.action_add_minute),
                    Modifier.weight(1f).testTag("alarm-add-minute"),
                ) {
                    app.send(Event.AddMinute(id))
                }
                AlarmOutlined(stringResource(R.string.action_snooze), Modifier.weight(1f).testTag("alarm-snooze")) {
                    app.send(Event.Snooze(id))
                }
            }
        }
    }

    @Composable
    private fun AlarmOutlined(text: String, modifier: Modifier, onClick: () -> Unit) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = Dimens.AlarmButton),
            shape = RoundedCornerShape(Dimens.AlarmCorner),
            border = BorderStroke(Dimens.Border, AlarmColors.Border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlarmColors.Text),
        ) { Text(text, style = AppType.alarmButton) }
    }

    /**
     * Nothing rings any more. Opened by the app while it was on screen: go back to the app
     * (this activity runs in its own task, so finishing alone can land on the home screen).
     * Opened from the lock screen or a notification: just finish, as before.
     */
    private fun close() {
        if (isFinishing) return
        if (returnToApp) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    ),
            )
        }
        finish()
    }

    companion object {
        private const val EXTRA_RETURN_TO_APP = "returnToApp"
        private const val EXTRA_HIVE_ID = "hiveId"
        private const val NO_HIVE = -1L

        /** [returnToApp]: opened by the app while on screen; closing goes back to the app. */
        fun intent(context: Context, hiveId: Long, returnToApp: Boolean = false): Intent =
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .putExtra(EXTRA_HIVE_ID, hiveId)
                .putExtra(EXTRA_RETURN_TO_APP, returnToApp)
    }
}
