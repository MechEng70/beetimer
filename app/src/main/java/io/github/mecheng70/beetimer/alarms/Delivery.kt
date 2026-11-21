package io.github.mecheng70.beetimer.alarms

/**
 * How an alarm or "vaporizer free" reaches the user, by whether the app is on screen.
 * Foreground: Android shows a full-screen intent as a heads-up while the phone is in use,
 * and heads-ups cover the top bar and the vaporizer bar's buttons, so the app shows the
 * alarm screen itself and a snackbar instead. Background and lock screen: notifications.
 */
object Delivery {
    data class Ring(
        /** Open the alarm screen directly. */
        val openAlarmScreen: Boolean,
        /** Post the ring service's notification on the quiet channel (no heads-up). */
        val quietNotification: Boolean,
    )

    enum class DeviceFree { SNACKBAR, NOTIFICATION }

    fun ring(foreground: Boolean) = Ring(openAlarmScreen = foreground, quietNotification = foreground)

    fun deviceFree(foreground: Boolean) = if (foreground) DeviceFree.SNACKBAR else DeviceFree.NOTIFICATION
}
