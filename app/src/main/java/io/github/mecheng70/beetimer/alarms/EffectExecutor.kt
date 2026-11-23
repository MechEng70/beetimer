package io.github.mecheng70.beetimer.alarms

import android.content.Context
import io.github.mecheng70.beetimer.scheduler.Effect
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.ui.AlarmActivity

/** Runs scheduler effects. The platform boundary (AlarmManager here, AlarmKit on iOS later). */
fun interface EffectRunner {
    fun execute(effects: List<Effect>, state: SchedulerState, labels: Map<Long, String>)
}

/** Carries out scheduler effects on Android. */
class EffectExecutor(
    private val context: Context,
    private val alarms: AlarmScheduler,
    private val notifier: Notifier,
    /** True while an app activity is on screen. */
    private val isForeground: () -> Boolean,
    /** Shows "Vaporizer free: <label> is next" in the app (snackbar). */
    private val showInApp: (hiveId: Long, label: String) -> Unit,
) : EffectRunner {
    override fun execute(effects: List<Effect>, state: SchedulerState, labels: Map<Long, String>) {
        for (effect in effects) {
            when (effect) {
                is Effect.ScheduleAlarm ->
                    if (state.runs[effect.hiveId]?.ringing != null) {
                        alarms.scheduleExact(effect.hiveId, effect.atMs)
                    } else {
                        alarms.scheduleAlarmClock(effect.hiveId, effect.atMs)
                    }

                is Effect.CancelAlarm -> alarms.cancel(effect.hiveId)

                is Effect.Ring -> ring(effect.hiveId, state, labels)

                is Effect.StopRing -> RingService.stop(context, effect.hiveId)

                is Effect.NotifyDeviceFree -> labels[effect.hiveId]?.let { deviceFree(effect.hiveId, it) }
            }
        }
        if (state.lockHolder != null || state.queue.isEmpty()) notifier.cancelDeviceFree()
        notifier.status(state, labels)
    }

    private fun ring(hiveId: Long, state: SchedulerState, labels: Map<Long, String>) {
        val run = state.runs[hiveId]
        val label = labels[hiveId]
        if (run == null || label == null) return
        val info = AlarmText.info(context, run, state, labels)
        val how = Delivery.ring(isForeground())
        RingService.ring(
            context,
            hiveId,
            AlarmText.notificationTitle(label),
            AlarmText.notificationBody(context, info),
            quiet = how.quietNotification,
        )
        if (how.openAlarmScreen) {
            runCatching {
                context.startActivity(AlarmActivity.intent(context, hiveId, returnToApp = true))
            }
        }
    }

    private fun deviceFree(hiveId: Long, label: String) {
        when (Delivery.deviceFree(isForeground())) {
            Delivery.DeviceFree.SNACKBAR -> showInApp(hiveId, label)
            Delivery.DeviceFree.NOTIFICATION -> notifier.deviceFree(hiveId, label)
        }
    }
}
