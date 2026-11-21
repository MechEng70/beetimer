package io.github.mecheng70.beetimer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.alarms.RingService
import io.github.mecheng70.beetimer.ui.theme.AlarmColors
import io.github.mecheng70.beetimer.ui.theme.BeeTimerTheme
import io.github.mecheng70.beetimer.ui.theme.Dimens

/** Full-screen alarm shown over the lock screen. T1b spike version: one Stop button. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render()
    }

    private fun render() {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        setContent {
            BeeTimerTheme {
                AlarmScreen(title = title, text = text, onStop = {
                    RingService.stop(this)
                    finish()
                })
            }
        }
    }

    companion object {
        private const val EXTRA_HIVE_ID = "hiveId"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TEXT = "text"

        fun intent(context: Context, hiveId: Long, title: String, text: String): Intent =
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .putExtra(EXTRA_HIVE_ID, hiveId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
    }
}

@Composable
private fun AlarmScreen(title: String, text: String, onStop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmColors.Background)
            .safeDrawingPadding()
            .padding(Dimens.Gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            color = AlarmColors.Text,
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.Gutter))
        Text(
            text,
            color = AlarmColors.Accent,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.TouchAlarm))
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.TouchAlarm),
            shape = RoundedCornerShape(Dimens.Corner),
            colors = ButtonDefaults.buttonColors(
                containerColor = AlarmColors.Text,
                contentColor = AlarmColors.Background,
            ),
        ) {
            Text(stringResource(R.string.alarm_stop), style = MaterialTheme.typography.titleLarge)
        }
    }
}
