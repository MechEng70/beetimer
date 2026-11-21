package io.github.mecheng70.beetimer.alarms

import io.github.mecheng70.beetimer.DevTools
import io.github.mecheng70.beetimer.data.Repository
import io.github.mecheng70.beetimer.scheduler.DefaultScheduler
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.Scheduler
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.scheduler.Transition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single place events enter the scheduler: load, reduce, persist, then run effects.
 * No scheduling logic lives here.
 */
class SchedulerEngine(
    private val repo: Repository,
    private val executor: EffectRunner,
    private val scheduler: Scheduler = DefaultScheduler(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<SchedulerState?>(null)
    val state: StateFlow<SchedulerState?> = _state.asStateFlow()

    /** Loads state once per process and restores it to now (reschedules alarms). */
    suspend fun ensureLoaded() {
        mutex.withLock { loadLocked() }
    }

    /** Boot, time or timezone change: restore again even if already loaded. */
    suspend fun restoreNow() {
        mutex.withLock {
            val wasLoaded = _state.value != null
            val s = loadLocked()
            if (wasLoaded) commit(scheduler.restore(s, clock()), "Restore")
        }
    }

    suspend fun dispatch(event: Event) {
        mutex.withLock {
            val s = loadLocked()
            commit(scheduler.reduce(s, event, clock()), event.toString())
        }
    }

    private suspend fun loadLocked(): SchedulerState {
        _state.value?.let { return it }
        // The templates table is the source of truth for the next Start (a migration may have
        // reseeded a preset); active runs keep their own snapshot.
        val active = repo.activeTemplate()
        val saved = repo.loadState()?.copy(template = active) ?: SchedulerState(template = active)
        val t = scheduler.restore(saved, clock())
        commit(t, "Load")
        return t.state
    }

    private suspend fun commit(t: Transition, cause: String) {
        val before = _state.value
        repo.saveState(t.state)
        _state.value = t.state
        val labels = repo.hiveList().associate { it.id to it.label }
        executor.execute(t.effects, t.state, labels)
        // Debug builds: invariant check and event log. A no-op in release.
        DevTools.afterCommit(cause, before, t, labels, clock())
    }
}
