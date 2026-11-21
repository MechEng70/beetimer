package io.github.mecheng70.beetimer

import android.app.Application
import androidx.room.Room
import io.github.mecheng70.beetimer.alarms.AlarmScheduler
import io.github.mecheng70.beetimer.alarms.Channels
import io.github.mecheng70.beetimer.alarms.EffectExecutor
import io.github.mecheng70.beetimer.alarms.Notifier
import io.github.mecheng70.beetimer.alarms.SchedulerEngine
import io.github.mecheng70.beetimer.data.BeeTimerDb
import io.github.mecheng70.beetimer.data.Prefs
import io.github.mecheng70.beetimer.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Wires the app by hand; small enough not to need a DI library. */
class BeeTimerApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val prefs by lazy { Prefs(this) }
    private val db by lazy { Room.databaseBuilder(this, BeeTimerDb::class.java, "beetimer.db").build() }
    val repo by lazy { Repository(db, prefs) { n -> getString(R.string.hive_default_name, n) } }
    val alarms by lazy { AlarmScheduler(this) }
    val engine by lazy { SchedulerEngine(repo, EffectExecutor(this, alarms, Notifier(this))) }

    override fun onCreate() {
        super.onCreate()
        Channels.ensure(this)
        scope.launch {
            repo.seedIfNeeded()
            engine.ensureLoaded()
        }
    }
}
