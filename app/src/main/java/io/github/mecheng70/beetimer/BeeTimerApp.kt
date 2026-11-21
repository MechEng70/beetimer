package io.github.mecheng70.beetimer

import android.app.Application
import androidx.room.Room
import io.github.mecheng70.beetimer.alarms.AlarmScheduler
import io.github.mecheng70.beetimer.alarms.Channels
import io.github.mecheng70.beetimer.alarms.EffectExecutor
import io.github.mecheng70.beetimer.alarms.Notifier
import io.github.mecheng70.beetimer.alarms.SchedulerEngine
import io.github.mecheng70.beetimer.data.BeeTimerDb
import io.github.mecheng70.beetimer.data.MIGRATION_1_2
import io.github.mecheng70.beetimer.data.MIGRATION_2_3
import io.github.mecheng70.beetimer.data.Prefs
import io.github.mecheng70.beetimer.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.github.mecheng70.beetimer.alarms.RingService
import androidx.lifecycle.ProcessLifecycleOwner

/** Wires the app by hand; small enough not to need a DI library. */
class BeeTimerApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val prefs by lazy { Prefs(this) }
    private val db by lazy { Room.databaseBuilder(this, BeeTimerDb::class.java, "beetimer.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build() }
    val repo by lazy { Repository(db) { n -> getString(R.string.hive_default_name, n) } }
    val alarms by lazy { AlarmScheduler(this) }
    /** "Vaporizer free" messages for the snackbar while the app is on screen. */
    val deviceFreeInApp = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 4)

    val engine by lazy {
        SchedulerEngine(
            repo,
            EffectExecutor(
                context = this,
                alarms = alarms,
                notifier = Notifier(this),
                isForeground = { ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) },
                showInApp = { id, label -> deviceFreeInApp.tryEmit(id to label) },
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
        Channels.ensure(this)
        // Leaving the screen while an alarm rings (Home, lock): restore the loud notification.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = RingService.unquiet(this@BeeTimerApp)
        })
        scope.launch {
            repo.seedIfNeeded()
            engine.ensureLoaded()
        }
    }
}
