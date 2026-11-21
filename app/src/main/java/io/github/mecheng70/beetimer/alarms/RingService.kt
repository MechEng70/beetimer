package io.github.mecheng70.beetimer.alarms

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

/**
 * Foreground service that runs only while an alarm rings. Plays sound and vibration
 * for every ringing hive (one loop, however many hives) and keeps each hive's alarm
 * notification posted.
 *
 * Type systemExempted: Android allows it for apps holding USE_EXACT_ALARM that keep an
 * alarm going in the background. Needed because notification sound and vibration alone
 * stopped after one buzz on OxygenOS (T1b phone test, 2026-11-21).
 */
class RingService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var sound: AlarmSound
    private lateinit var notifier: Notifier
    private val ringing = LinkedHashMap<Long, Pair<String, String>>()
    private val quiet = mutableSetOf<Long>()
    private var foregroundId: Long? = null
    private val capRunnable = Runnable { quiet() }

    override fun onCreate() {
        super.onCreate()
        sound = AlarmSound(this)
        notifier = Notifier(this)
        running = true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hiveId = intent?.getLongExtra(EXTRA_HIVE_ID, NO_HIVE) ?: NO_HIVE
        when (intent?.action) {
            ACTION_RING -> {
                ringing[hiveId] = intent.getStringExtra(EXTRA_TITLE).orEmpty() to intent.getStringExtra(EXTRA_TEXT).orEmpty()
                if (intent.getBooleanExtra(EXTRA_QUIET, false)) quiet += hiveId else quiet -= hiveId
                promote(hiveId)
                sound.start()
                handler.removeCallbacks(capRunnable)
                handler.postDelayed(capRunnable, RING_CAP_MS)
            }
            ACTION_UNQUIET -> if (quiet.isNotEmpty()) {
                // The app left the screen while ringing: back to the alarm channel with the
                // full-screen intent, so the lock screen and the notification bring the alarm back.
                quiet.clear()
                foregroundId?.let { promote(it) }
            }
            ACTION_STOP -> {
                ringing.remove(hiveId)
                quiet -= hiveId
                notifier.cancelRing(hiveId)
                if (ringing.isEmpty()) {
                    shutDown(removeNotification = true)
                } else if (foregroundId == hiveId) {
                    promote(ringing.keys.last())
                }
            }
            else -> if (ringing.isEmpty()) shutDown(removeNotification = true)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(capRunnable)
        sound.stop()
        running = false
        super.onDestroy()
    }

    /** Make [hiveId]'s notification the foreground one; keep the others posted. */
    private fun promote(hiveId: Long) {
        val (title, text) = ringing.getValue(hiveId)
        val n = notifier.ringNotification(hiveId, title, text, quiet = hiveId in quiet)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Notifier.ringId(hiveId), n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(Notifier.ringId(hiveId), n)
        }
        foregroundId = hiveId
        ringing.forEach { (id, t) -> if (id != hiveId) notifier.postRing(id, notifier.ringNotification(id, t.first, t.second, quiet = id in quiet)) }
    }

    /** Two minutes of ringing are up: go quiet but leave the alarms on screen. */
    private fun quiet() {
        Log.i(TAG, "Ring cap reached for ${ringing.keys}")
        shutDown(removeNotification = false)
    }

    private fun shutDown(removeNotification: Boolean) {
        handler.removeCallbacks(capRunnable)
        sound.stop()
        stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        ringing.clear()
        quiet.clear()
        foregroundId = null
        stopSelf()
    }

    companion object {
        private const val TAG = "RingService"
        private const val ACTION_RING = "io.github.mecheng70.beetimer.RING"
        private const val ACTION_STOP = "io.github.mecheng70.beetimer.STOP_RING"
        private const val ACTION_UNQUIET = "io.github.mecheng70.beetimer.UNQUIET_RING"
        private const val EXTRA_HIVE_ID = "hiveId"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_QUIET = "quiet"
        private const val NO_HIVE = -1L

        /** Ring at most 2 minutes per alarm (SPEC). The scheduler rings again later. */
        private const val RING_CAP_MS = 2 * 60 * 1000L

        @Volatile
        var running = false
            private set

        /** [quiet]: the app is on screen and shows the alarm screen itself; no heads-up. */
        fun ring(context: Context, hiveId: Long, title: String, text: String, quiet: Boolean = false) {
            runCatching {
                context.startForegroundService(
                    Intent(context, RingService::class.java)
                        .setAction(ACTION_RING)
                        .putExtra(EXTRA_HIVE_ID, hiveId)
                        .putExtra(EXTRA_TITLE, title)
                        .putExtra(EXTRA_TEXT, text)
                        .putExtra(EXTRA_QUIET, quiet),
                )
            }.onFailure { Log.e(TAG, "Could not start ring service", it) }
        }

        /** Called when the app goes to the background. */
        fun unquiet(context: Context) {
            if (!running) return
            runCatching { context.startService(Intent(context, RingService::class.java).setAction(ACTION_UNQUIET)) }
                .onFailure { Log.w(TAG, "Could not reach ring service", it) }
        }

        fun stop(context: Context, hiveId: Long) {
            Notifier(context).cancelRing(hiveId)
            if (!running) return
            runCatching {
                context.startService(
                    Intent(context, RingService::class.java).setAction(ACTION_STOP).putExtra(EXTRA_HIVE_ID, hiveId),
                )
            }.onFailure { Log.w(TAG, "Could not reach ring service", it) }
        }
    }
}
