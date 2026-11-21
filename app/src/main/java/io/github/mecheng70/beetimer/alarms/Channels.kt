package io.github.mecheng70.beetimer.alarms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import io.github.mecheng70.beetimer.R

object Channels {
    // Channel sound and importance are fixed once created, so the id carries a version.
    const val ALARMS = "alarms_v1"
    const val STATUS = "status_v1"

    private val VIBRATION = longArrayOf(0, 800, 400, 800, 400, 800)

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarms = NotificationChannel(
            ALARMS,
            context.getString(R.string.channel_alarms_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarms_desc)
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = VIBRATION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val status = NotificationChannel(
            STATUS,
            context.getString(R.string.channel_status_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_status_desc)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannels(listOf(alarms, status))
    }
}
