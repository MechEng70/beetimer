package io.github.mecheng70.beetimer.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mecheng70.beetimer.BeeTimerApp
import io.github.mecheng70.beetimer.scheduler.Event
import kotlinx.coroutines.launch

/** Runs [block] on the app scope while keeping the broadcast alive. */
private fun BroadcastReceiver.async(context: Context, block: suspend (BeeTimerApp) -> Unit) {
    val pending = goAsync()
    val app = context.applicationContext as BeeTimerApp
    app.scope.launch {
        try {
            block(app)
        } finally {
            pending.finish()
        }
    }
}

/** A hive's alarm fired: step end, re-ring or snooze. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val hiveId = intent.getLongExtra(EXTRA_HIVE_ID, -1L)
        if (hiveId < 0) return
        async(context) { it.engine.dispatch(Event.TimerFired(hiveId)) }
    }

    companion object {
        const val ACTION_FIRE = "io.github.mecheng70.beetimer.FIRE"
        const val EXTRA_HIVE_ID = "hiveId"
    }
}

/** Buttons on notifications: Done, +1 min, Snooze, Start. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val hiveId = intent.getLongExtra(EXTRA_HIVE_ID, -1L)
        if (hiveId < 0) return
        val event = when (intent.action) {
            ACTION_DONE -> Event.Done(hiveId)
            ACTION_ADD_MINUTE -> Event.AddMinute(hiveId)
            ACTION_SNOOZE -> Event.Snooze(hiveId)
            ACTION_START -> Event.Start(hiveId)
            else -> return
        }
        async(context) { it.engine.dispatch(event) }
    }

    companion object {
        const val ACTION_DONE = "io.github.mecheng70.beetimer.DONE"
        const val ACTION_ADD_MINUTE = "io.github.mecheng70.beetimer.ADD_MINUTE"
        const val ACTION_SNOOZE = "io.github.mecheng70.beetimer.SNOOZE"
        const val ACTION_START = "io.github.mecheng70.beetimer.START"
        private const val EXTRA_HIVE_ID = "hiveId"

        fun intent(context: Context, action: String, hiveId: Long): Intent =
            Intent(context, ActionReceiver::class.java).setAction(action).putExtra(EXTRA_HIVE_ID, hiveId)
    }
}

/** Reboot, app update, or clock change: reload state and reschedule through restore. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> async(context) { it.engine.restoreNow() }
        }
    }
}
