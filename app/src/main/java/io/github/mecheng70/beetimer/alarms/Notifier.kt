package io.github.mecheng70.beetimer.alarms

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.scheduler.HiveRun
import io.github.mecheng70.beetimer.scheduler.RunState
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.ui.AlarmActivity
import io.github.mecheng70.beetimer.ui.MainActivity
import java.text.DateFormat
import java.util.Date

/** Builds and posts every notification except the ring service's own lifecycle. */
class Notifier(private val context: Context) {
    private val nm = context.getSystemService(NotificationManager::class.java)
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)

    /** Loud alarm notification for one hive: full-screen intent plus Done, +1 min, Snooze. */
    fun ringNotification(hiveId: Long, title: String, text: String): Notification {
        Channels.ensure(context)
        val fullScreen = PendingIntent.getActivity(
            context,
            requestCode(hiveId, RC_ALARM_SCREEN),
            AlarmActivity.intent(context, hiveId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(context, Channels.ALARMS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(action(hiveId, ActionReceiver.ACTION_DONE, R.string.action_done))
            .addAction(action(hiveId, ActionReceiver.ACTION_ADD_MINUTE, R.string.action_add_minute))
            .addAction(action(hiveId, ActionReceiver.ACTION_SNOOZE, R.string.action_snooze))
            .build()
    }

    fun postRing(hiveId: Long, notification: Notification) = nm.notify(ringId(hiveId), notification)

    fun cancelRing(hiveId: Long) = nm.cancel(ringId(hiveId))

    fun deviceFree(hiveId: Long, label: String) {
        Channels.ensure(context)
        val n = Notification.Builder(context, Channels.DEVICE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.device_notif_title, label))
            .setContentText(context.getString(R.string.device_notif_text))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .addAction(action(hiveId, ActionReceiver.ACTION_START, R.string.action_start))
            .build()
        nm.notify(DEVICE_FREE_ID, n)
    }

    fun cancelDeviceFree() = nm.cancel(DEVICE_FREE_ID)

    /** One line per active hive. Removed when nothing is active. */
    fun status(state: SchedulerState, labels: Map<Long, String>) {
        val active = state.runs.values
            .filter { (it.isActive || it.ringing != null) && labels.containsKey(it.hiveId) }
            .sortedBy { it.hiveId }
        if (active.isEmpty()) {
            nm.cancel(STATUS_ID)
            return
        }
        Channels.ensure(context)
        val lines = active.map { line(it, labels.getValue(it.hiveId)) }
        val soonest = active.filter { it.state == RunState.RUNNING }.mapNotNull { it.stepEndsAt }.minOrNull()
        val title = context.resources.getQuantityString(R.plurals.status_title, active.size, active.size)
        val builder = Notification.Builder(context, Channels.STATUS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(Notification.InboxStyle().also { s -> lines.forEach { s.addLine(it) } })
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp())
        if (soonest != null) {
            builder.setWhen(soonest).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
        }
        nm.notify(STATUS_ID, builder.build())
    }

    private fun line(run: HiveRun, label: String): String {
        val step = run.currentStep?.name.orEmpty()
        val ringing = run.ringing
        val ends = run.stepEndsAt
        return when {
            ringing != null -> context.getString(
                R.string.status_line_ringing,
                label,
                run.steps.getOrNull(ringing.finishedStepIndex)?.name.orEmpty(),
            )
            run.state == RunState.WAITING_FOR_DEVICE -> context.getString(R.string.status_line_waiting, label)
            run.state == RunState.RUNNING && ends != null ->
                context.getString(R.string.status_line_ends, label, step, timeFormat.format(Date(ends)))
            else -> context.getString(R.string.status_line_done, label, step)
        }
    }

    private fun action(hiveId: Long, action: String, label: Int): Notification.Action {
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(hiveId, action.hashCode()),
            ActionReceiver.intent(context, action, hiveId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(null, context.getString(label), pi).build()
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun requestCode(hiveId: Long, salt: Int): Int = (hiveId.toInt() * PRIME) xor salt

    companion object {
        private const val STATUS_ID = 1
        private const val DEVICE_FREE_ID = 2
        private const val RING_ID_BASE = 10_000
        private const val RC_ALARM_SCREEN = 7
        private const val PRIME = 31

        fun ringId(hiveId: Long): Int = RING_ID_BASE + hiveId.toInt()
    }
}
