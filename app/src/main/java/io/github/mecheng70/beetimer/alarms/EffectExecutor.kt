package io.github.mecheng70.beetimer.alarms

import android.content.Context
import io.github.mecheng70.beetimer.scheduler.Effect
import io.github.mecheng70.beetimer.scheduler.SchedulerState

/** Runs scheduler effects. The platform boundary (AlarmManager here, AlarmKit on iOS later). */
fun interface EffectRunner {
    fun execute(effects: List<Effect>, state: SchedulerState, labels: Map<Long, String>)
}

/** Carries out scheduler effects on Android. */
class EffectExecutor(
    private val context: Context,
    private val alarms: AlarmScheduler,
    private val notifier: Notifier,
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
                is Effect.Ring -> {
                    val run = state.runs[effect.hiveId] ?: continue
                    val label = labels[effect.hiveId] ?: continue
                    val info = AlarmText.info(context, run, state, labels)
                    RingService.ring(
                        context,
                        effect.hiveId,
                        AlarmText.notificationTitle(label),
                        AlarmText.notificationBody(context, info),
                    )
                }
                is Effect.StopRing -> RingService.stop(context, effect.hiveId)
                is Effect.NotifyDeviceFree -> labels[effect.hiveId]?.let { notifier.deviceFree(effect.hiveId, it) }
            }
        }
        if (state.lockHolder != null || state.queue.isEmpty()) notifier.cancelDeviceFree()
        notifier.status(state, labels)
    }
}
