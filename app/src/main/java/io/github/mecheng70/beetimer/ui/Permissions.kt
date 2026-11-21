package io.github.mecheng70.beetimer.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.ui.theme.Dimens

object Permissions {
    fun notifications(c: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun exactAlarms(c: Context): Boolean = c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun fullScreen(c: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        c.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun battery(c: Context): Boolean = c.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(c.packageName)

    fun anyRequiredMissing(c: Context): Boolean = !notifications(c) || !exactAlarms(c) || !fullScreen(c)

    fun open(c: Context, action: String) {
        runCatching { c.startActivity(Intent(action, "package:${c.packageName}".toUri())) }
            .onFailure { c.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${c.packageName}".toUri())) }
    }
}

@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    @Suppress("UNUSED_EXPRESSION")
    refresh // re-read the checks below on every resume

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(Dimens.Gutter).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
    ) {
        Text(stringResource(R.string.perms_title), style = MaterialTheme.typography.headlineMedium)
        PermRow(stringResource(R.string.perm_notifications), null, Permissions.notifications(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        PermRow(stringResource(R.string.perm_exact), null, Permissions.exactAlarms(context)) {
            Permissions.open(context, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        }
        PermRow(stringResource(R.string.perm_fsi), stringResource(R.string.perm_fsi_explain), Permissions.fullScreen(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Permissions.open(context, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            }
        }
        PermRow(stringResource(R.string.perm_battery), stringResource(R.string.perm_battery_explain), Permissions.battery(context)) {
            if (!app.prefs.batteryAsked) {
                app.prefs.batteryAsked = true
                @Suppress("BatteryLife") // sideloaded timer app; asked once with an explanation
                Permissions.open(context, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            } else {
                Permissions.open(context, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            }
        }
        Text(stringResource(R.string.perms_sound_note), style = MaterialTheme.typography.bodyMedium)
        BigButton(stringResource(R.string.perms_continue), onContinue, Modifier.fillMaxWidth())
    }
}

@Composable
private fun PermRow(title: String, explain: String?, on: Boolean, onAllow: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(if (on) R.string.perm_on else R.string.perm_off),
                    color = if (on) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
            }
            if (!on) BigOutlinedButton(stringResource(R.string.perm_allow), onAllow)
        }
        if (!on && explain != null) Text(explain, style = MaterialTheme.typography.bodyMedium)
    }
}
