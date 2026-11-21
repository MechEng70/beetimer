package io.github.mecheng70.beetimer.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mecheng70.beetimer.R

/** T1b spike: rings a test alarm. T5 routes these to the scheduler engine instead. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val hiveId = intent.getLongExtra(EXTRA_HIVE_ID, SPIKE_HIVE_ID)
        when (intent.action) {
            ACTION_FIRE -> RingingNotifier.ring(
                context,
                hiveId,
                title = context.getString(R.string.spike_hive_label),
                text = context.getString(R.string.spike_alarm_text),
            )
            ACTION_STOP -> RingingNotifier.stop(context, hiveId)
        }
    }

    companion object {
        const val ACTION_FIRE = "io.github.mecheng70.beetimer.FIRE"
        const val ACTION_STOP = "io.github.mecheng70.beetimer.STOP"
        const val EXTRA_HIVE_ID = "hiveId"
        const val SPIKE_HIVE_ID = 9_999L
    }
}
