package io.github.mecheng70.beetimer.alarms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import io.github.mecheng70.beetimer.R

object Channels {
    // Channel settings are fixed once created, so the id carries a version.
    // v1 played sound from the channel; OxygenOS cut it to one buzz. From v2 the
    // channel is silent and RingService plays sound and vibration itself.
    const val ALARMS = "alarms_v2"
    const val STATUS = "status_v1"
    const val DEVICE = "device_v1"
    private val OBSOLETE = listOf("alarms_v1")

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        OBSOLETE.forEach { nm.deleteNotificationChannel(it) }
        val alarms = NotificationChannel(
            ALARMS,
            context.getString(R.string.channel_alarms_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarms_desc)
            setSound(null, null)
            enableVibration(false)
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
        // Default notification sound and vibration: a one-off heads-up, not a looping alarm.
        val device = NotificationChannel(
            DEVICE,
            context.getString(R.string.channel_device_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_device_desc)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannels(listOf(alarms, status, device))
    }
}
