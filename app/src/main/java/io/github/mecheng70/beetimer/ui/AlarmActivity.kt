package io.github.mecheng70.beetimer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.alarms.AlarmText
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.ui.theme.AlarmColors
import io.github.mecheng70.beetimer.ui.theme.AppType
import io.github.mecheng70.beetimer.ui.theme.BeeTimerTheme
import io.github.mecheng70.beetimer.ui.theme.Dimens

/** Full-screen alarm over the lock screen (mockup 3). Dark amber in both themes. */
class AlarmActivity : ComponentActivity() {
    private var hiveId by mutableLongStateOf(NO_HIVE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        hiveId = intent.getLongExtra(EXTRA_HIVE_ID, NO_HIVE)
        setContent { BeeTimerTheme { AlarmScreen() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        hiveId = intent.getLongExtra(EXTRA_HIVE_ID, NO_HIVE)
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
        LaunchedEffect(run == null) { if (run == null) finish() }
        if (run == null) return
        val labels = hives.associate { it.id to it.label }
        val id = run.hiveId

        Column(
            Modifier.fillMaxSize().background(AlarmColors.Background).safeDrawingPadding().padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(labels[id].orEmpty(), color = AlarmColors.Text, style = AppType.alarmName, textAlign = TextAlign.Center)
            Text(AlarmText.finished(context, run), color = AlarmColors.Accent, style = AppType.alarmFinished, textAlign = TextAlign.Center)
            Text(
                AlarmText.next(context, run),
                color = AlarmColors.Text,
                style = AppType.alarmNext,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Dimens.SectionGap),
            )
            AlarmText.deviceHint(context, run, s, labels)?.let {
                Text(it, color = AlarmColors.Accent, style = AppType.alarmHint, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Dimens.ChipPaddingH))
            }
            Spacer(Modifier.height(Dimens.CardPadding * 2))
            Button(
                onClick = { app.send(Event.Done(id)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.AlarmButton),
                shape = RoundedCornerShape(Dimens.AlarmCorner),
                colors = ButtonDefaults.buttonColors(containerColor = AlarmColors.Text, contentColor = AlarmColors.Background),
            ) { Text(stringResource(R.string.action_done), style = AppType.alarmButton) }
            Spacer(Modifier.height(Dimens.Gap))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                AlarmOutlined(stringResource(R.string.action_add_minute), Modifier.weight(1f)) { app.send(Event.AddMinute(id)) }
                AlarmOutlined(stringResource(R.string.action_snooze), Modifier.weight(1f)) { app.send(Event.Snooze(id)) }
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

    companion object {
        private const val EXTRA_HIVE_ID = "hiveId"
        private const val NO_HIVE = -1L

        fun intent(context: Context, hiveId: Long): Intent =
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .putExtra(EXTRA_HIVE_ID, hiveId)
    }
}
