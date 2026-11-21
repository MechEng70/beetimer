package io.github.mecheng70.beetimer.alarms

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.ui.AlarmActivity

/** Posts and clears the loud, full-screen alarm notification for one hive. */
object RingingNotifier {
    private const val ID_BASE = 10_000

    fun notificationId(hiveId: Long): Int = ID_BASE + hiveId.toInt()

    fun ring(context: Context, hiveId: Long, title: String, text: String, silent: Boolean = false) {
        Channels.ensure(context)
        val fullScreen = PendingIntent.getActivity(
            context,
            notificationId(hiveId),
            AlarmActivity.intent(context, hiveId, title, text),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getBroadcast(
            context,
            notificationId(hiveId),
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_STOP)
                .putExtra(AlarmReceiver.EXTRA_HIVE_ID, hiveId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = Notification.Builder(context, Channels.ALARMS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(silent)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .addAction(Notification.Action.Builder(null, context.getString(R.string.alarm_stop), stop).build())
            .build()
        // Insistent: the sound loops until the notification is cleared.
        if (!silent) n.flags = n.flags or Notification.FLAG_INSISTENT
        context.getSystemService(NotificationManager::class.java).notify(notificationId(hiveId), n)
    }

    fun stop(context: Context, hiveId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(hiveId))
    }
}
