package io.github.mecheng70.beetimer.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.mecheng70.beetimer.MainActivity

/** Thin wrapper over AlarmManager. One pending alarm per hive; scheduling again replaces it. */
class AlarmScheduler(private val context: Context) {
    private val am = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean = am.canScheduleExactAlarms()

    /** Step-end alarm. setAlarmClock fires in Doze and shows the alarm icon. */
    fun scheduleAlarmClock(hiveId: Long, atMs: Long) {
        val show = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(atMs, show), firePending(hiveId))
    }

    /** Snooze and re-ring. Exact, allowed while idle, no status bar alarm icon. */
    fun scheduleExact(hiveId: Long, atMs: Long) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, firePending(hiveId))
    }

    fun cancel(hiveId: Long) {
        am.cancel(firePending(hiveId))
    }

    private fun firePending(hiveId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        hiveId.toInt(),
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_HIVE_ID, hiveId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
