package io.github.mecheng70.beetimer.scheduler

/**
 * Rules that must hold after every event. The debug app logs an ERROR line for each
 * violation (E2E runs fail on any); the property test asserts none.
 */
object Invariants {
    /**
     * @param alarms the platform alarm per hive as the app scheduled it, or null to skip that check.
     * @param forecast the planner output for [state] at [now], or null to skip the firm-time check.
     */
    fun check(
        state: SchedulerState,
        alarms: Map<Long, Long>? = null,
        forecast: Forecast? = null,
        now: Long = Long.MIN_VALUE,
    ): List<String> {
        val v = mutableListOf<String>()
        val runs = state.runs.values

        val usingDevice = runs.filter { it.inDeviceStep && it.state != RunState.WAITING_FOR_DEVICE }
        if (usingDevice.size > 1) v += "more than one run holds the vaporizer: ${usingDevice.map { it.hiveId }}"
        usingDevice.forEach { r ->
            if (state.lockHolder !=
                r.hiveId
            ) {
                v +=
                    "hive ${r.hiveId} is in device step '${r.currentStep?.name}' but does not hold the vaporizer"
            }
        }
        state.lockHolder?.let { h ->
            val r = state.runs[h]
            if (r == null || !r.inDeviceStep ||
                r.state == RunState.WAITING_FOR_DEVICE
            ) {
                v += "lock holder $h is not in a device step"
            }
            if (h in state.queue) v += "lock holder $h is also queued"
        }
        runs.filter { it.state == RunState.WAITING_FOR_DEVICE }.forEach { r ->
            if (r.hiveId !in state.queue) v += "hive ${r.hiveId} is waiting but not in the queue"
        }
        state.queue.forEach { h ->
            if (state.runs[h]?.state != RunState.WAITING_FOR_DEVICE) v += "queued hive $h is not waiting"
        }
        if (state.queue.size != state.queue.toSet().size) v += "duplicate in queue ${state.queue}"

        alarms?.forEach { (h, at) ->
            val r = state.runs[h]
            val timed =
                r != null &&
                    (r.ringing != null || (r.state == RunState.RUNNING && r.currentStep?.kind == StepKind.TIMED))
            if (!timed) v += "alarm at $at scheduled for hive $h with no timed step or ringing alarm"
        }

        forecast?.steps?.groupBy { it.hiveId }?.forEach { (h, planned) ->
            val r = state.runs[h]
            planned.forEachIndexed { i, p ->
                val ends = r?.stepEndsAt
                // A due timer that has not fired yet ends "now"; still no tap before it.
                val allowedFirm = i == 0 && r != null && ends != null && r.ringing == null &&
                    r.state == RunState.RUNNING && p.at == maxOf(ends, now)
                if (!p.estimated && !allowedFirm) v += "hive $h '${p.stepName}' shown firm behind a pending tap"
            }
        }
        return v
    }
}
