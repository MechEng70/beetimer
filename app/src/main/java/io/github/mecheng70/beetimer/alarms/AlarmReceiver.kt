package io.github.mecheng70.beetimer.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mecheng70.beetimer.R

/** T1b spike: rings a test alarm. T5 routes these to the scheduler engine instead. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        // An exact alarm lets the app start a foreground service from the background.
        RingService.ring(
            context,
            intent.getLongExtra(EXTRA_HIVE_ID, SPIKE_HIVE_ID),
            title = context.getString(R.string.spike_hive_label),
            text = context.getString(R.string.spike_alarm_text),
        )
    }

    companion object {
        const val ACTION_FIRE = "io.github.mecheng70.beetimer.FIRE"
        const val EXTRA_HIVE_ID = "hiveId"
        const val SPIKE_HIVE_ID = 9_999L
    }
}
