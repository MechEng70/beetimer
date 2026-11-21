package io.github.mecheng70.beetimer.alarms

import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryTest {
    @Test
    fun foreground_opensAlarmScreenAndRingsQuietly() {
        assertEquals(Delivery.Ring(openAlarmScreen = true, quietNotification = true), Delivery.ring(foreground = true))
    }

    @Test
    fun background_usesTheFullScreenNotificationAsBefore() {
        assertEquals(Delivery.Ring(openAlarmScreen = false, quietNotification = false), Delivery.ring(foreground = false))
    }

    @Test
    fun vaporizerFree_isASnackbarInTheAppAndANotificationOutside() {
        assertEquals(Delivery.DeviceFree.SNACKBAR, Delivery.deviceFree(foreground = true))
        assertEquals(Delivery.DeviceFree.NOTIFICATION, Delivery.deviceFree(foreground = false))
    }
}
