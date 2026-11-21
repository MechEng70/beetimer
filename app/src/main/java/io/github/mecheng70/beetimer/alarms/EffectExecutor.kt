package io.github.mecheng70.beetimer.alarms

import android.content.Context
import io.github.mecheng70.beetimer.scheduler.Effect
import io.github.mecheng70.beetimer.scheduler.SchedulerState

/** Carries out scheduler effects on Android. The platform boundary (AlarmKit on iOS later). */
class EffectExecutor(
    private val context: Context,
    private val alarms: AlarmScheduler,
    private val notifier: Notifier,
) {
    fun execute(effects: List<Effect>, state: SchedulerState, labels: Map<Long, String>) {
        for (effect in effects) {
            when (effect) {
                is Effect.ScheduleAlarm ->
                    if (state.runs[effect.hiveId]?.ringing != null) {
                        alarms.scheduleExact(effect.hiveId, effect.atMs)
                    } else {
                        alarms.scheduleAlarmClock(effect.hiveId, effect.atMs)
                    }
                is Effect.CancelAlarm -> alarms.cancel(effect.hiveId)
                is Effect.Ring -> {
                    val run = state.runs[effect.hiveId] ?: continue
                    val label = labels[effect.hiveId] ?: continue
                    RingService.ring(context, effect.hiveId, label, AlarmText.body(context, run))
                }
                is Effect.StopRing -> RingService.stop(context, effect.hiveId)
                is Effect.NotifyDeviceFree -> labels[effect.hiveId]?.let { notifier.deviceFree(effect.hiveId, it) }
            }
        }
        if (state.lockHolder != null || state.queue.isEmpty()) notifier.cancelDeviceFree()
        notifier.status(state, labels)
    }
}
