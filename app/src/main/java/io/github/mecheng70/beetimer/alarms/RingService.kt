package io.github.mecheng70.beetimer.alarms

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.ui.AlarmActivity

/**
 * Foreground service that runs only while an alarm rings.
 *
 * Type systemExempted: Android allows it for apps holding USE_EXACT_ALARM that keep an
 * alarm going in the background. Needed because notification sound and vibration alone
 * stopped after one buzz on OxygenOS (T1b phone test, 2026-11-21).
 */
class RingService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var sound: AlarmSound
    private val capRunnable = Runnable { stopRinging() }

    override fun onCreate() {
        super.onCreate()
        sound = AlarmSound(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> {
                val hiveId = intent.getLongExtra(EXTRA_HIVE_ID, 0L)
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                val notification = buildNotification(hiveId, title, text)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                sound.start()
                handler.removeCallbacks(capRunnable)
                handler.postDelayed(capRunnable, RING_CAP_MS)
            }
            else -> stopRinging()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(capRunnable)
        sound.stop()
        super.onDestroy()
    }

    private fun stopRinging() {
        handler.removeCallbacks(capRunnable)
        sound.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(hiveId: Long, title: String, text: String): Notification {
        Channels.ensure(this)
        val fullScreen = PendingIntent.getActivity(
            this,
            hiveId.toInt(),
            AlarmActivity.intent(this, hiveId, title, text),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            hiveId.toInt(),
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, Channels.ALARMS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(Notification.Action.Builder(null, getString(R.string.alarm_stop), stop).build())
            .build()
    }

    companion object {
        private const val ACTION_RING = "io.github.mecheng70.beetimer.RING"
        private const val ACTION_STOP = "io.github.mecheng70.beetimer.STOP_RING"
        private const val EXTRA_HIVE_ID = "hiveId"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TEXT = "text"
        private const val NOTIFICATION_ID = 10_000

        /** Ring at most 2 minutes per alarm (SPEC). T6 schedules the repeat. */
        private const val RING_CAP_MS = 2 * 60 * 1000L

        fun ring(context: Context, hiveId: Long, title: String, text: String) {
            context.startForegroundService(
                Intent(context, RingService::class.java)
                    .setAction(ACTION_RING)
                    .putExtra(EXTRA_HIVE_ID, hiveId)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_TEXT, text),
            )
        }

        fun stopIntent(context: Context): Intent =
            Intent(context, RingService::class.java).setAction(ACTION_STOP)

        fun stop(context: Context) {
            context.stopService(Intent(context, RingService::class.java))
        }
    }
}
