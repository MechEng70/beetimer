package io.github.mecheng70.beetimer

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.mecheng70.beetimer.alarms.AlarmReceiver
import io.github.mecheng70.beetimer.alarms.AlarmScheduler
import io.github.mecheng70.beetimer.alarms.Channels
import io.github.mecheng70.beetimer.ui.theme.BeeTimerTheme
import io.github.mecheng70.beetimer.ui.theme.Dimens
import java.text.DateFormat
import java.util.Date

/** T1b spike screen: check permissions and schedule one test alarm. Replaced by the real UI in T7. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Channels.ensure(this)
        setContent {
            BeeTimerTheme {
                Surface(Modifier.fillMaxSize()) { SpikeScreen() }
            }
        }
    }

    @Composable
    private fun SpikeScreen() {
        val scheduler = remember { AlarmScheduler(this) }
        var refresh by remember { mutableIntStateOf(0) }
        var scheduledFor by remember { mutableStateOf<String?>(null) }
        LifecycleResumeEffect(Unit) {
            refresh++
            onPauseOrDispose { }
        }
        val notifLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

        // Reading refresh re-evaluates the checks below on resume and after a permission result.
        // POST_NOTIFICATIONS exists from Android 13; below that notifications are on by default.
        val notifOk = refresh >= 0 && (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )
        val exactOk = scheduler.canScheduleExact()
        val fsiOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        val yes = stringResource(R.string.perm_allowed)
        val no = stringResource(R.string.perm_not_allowed)
        val buttonMod = Modifier.fillMaxWidth().heightIn(min = Dimens.TouchMin)

        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(Dimens.Gutter),
            verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
        ) {
            Text(stringResource(R.string.spike_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.perm_notifications, if (notifOk) yes else no))
            Text(stringResource(R.string.perm_exact, if (exactOk) yes else no))
            Text(stringResource(R.string.perm_fsi, if (fsiOk) yes else no))
            if (!notifOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                OutlinedButton(
                    onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = buttonMod,
                ) { Text(stringResource(R.string.perm_allow_notifications)) }
            }
            if (!exactOk) {
                OutlinedButton(
                    onClick = { openSetting(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM) },
                    modifier = buttonMod,
                ) { Text(stringResource(R.string.perm_open_exact)) }
            }
            if (!fsiOk) {
                Text(stringResource(R.string.perm_fsi_explain))
                OutlinedButton(
                    onClick = { openSetting(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT) },
                    modifier = buttonMod,
                ) { Text(stringResource(R.string.perm_open_fsi)) }
            }
            SPIKE_DELAYS_S.forEach { secs ->
                Button(
                    onClick = {
                        val at = System.currentTimeMillis() + secs * MS_PER_S
                        scheduler.scheduleAlarmClock(AlarmReceiver.SPIKE_HIVE_ID, at)
                        scheduledFor = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(at))
                    },
                    enabled = exactOk,
                    modifier = buttonMod,
                ) { Text(stringResource(R.string.spike_test_in, secs)) }
            }
            scheduledFor?.let { Text(stringResource(R.string.spike_set_for, it)) }
        }
    }

    private fun openSetting(action: String) {
        startActivity(Intent(action, "package:$packageName".toUri()))
    }

    private companion object {
        val SPIKE_DELAYS_S = listOf(60, 10)
        const val MS_PER_S = 1000L
    }
}
