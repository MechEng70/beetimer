package io.github.mecheng70.beetimer.ui

import io.github.mecheng70.beetimer.scheduler.HiveRun
import io.github.mecheng70.beetimer.scheduler.RunState

/** What the All tab footer says. Times are epoch ms; the screen formats them as clock times. */
sealed interface Footer {
    /** Every hive is started: "Estimated finish: 9:15 AM". */
    data class Estimated(val at: Long) : Footer

    /** Some hives not started: "Estimated finish: 9:15 AM (2 of 3 hives)". */
    data class EstimatedPartial(val at: Long, val started: Int, val total: Int) : Footer

    /** Every hive's run is complete: "All hives done at 9:15 AM". */
    data class AllDone(val at: Long) : Footer
}

/**
 * Picks the footer for [hiveIds] (the hives on the All tab). A hive counts as started once its run is active
 * or done; a cancelled run is back to not started. [allDoneAt] is the planner's finish for the active runs.
 * Returns null when there is nothing to say (nothing running and not every hive done).
 */
fun footerFor(hiveIds: List<Long>, runs: Map<Long, HiveRun>, allDoneAt: Long?): Footer? {
    val mine = hiveIds.mapNotNull { runs[it] }
    val started = mine.count { it.isActive || it.state == RunState.DONE }
    val total = hiveIds.size
    return when {
        total > 0 && mine.size == total && mine.all { it.state == RunState.DONE } ->
            mine.mapNotNull { it.completedAt }.maxOrNull()?.let { Footer.AllDone(it) }

        allDoneAt == null -> null

        started == total -> Footer.Estimated(allDoneAt)

        else -> Footer.EstimatedPartial(allDoneAt, started, total)
    }
}
