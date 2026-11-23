package io.github.mecheng70.beetimer

import android.app.Application
import android.media.AudioManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.Room
import io.github.mecheng70.beetimer.alarms.AlarmScheduler
import io.github.mecheng70.beetimer.alarms.Channels
import io.github.mecheng70.beetimer.alarms.EffectExecutor
import io.github.mecheng70.beetimer.alarms.Notifier
import io.github.mecheng70.beetimer.alarms.RingService
import io.github.mecheng70.beetimer.alarms.SchedulerEngine
import io.github.mecheng70.beetimer.alarms.VolumeCheck
import io.github.mecheng70.beetimer.data.BeeTimerDb
import io.github.mecheng70.beetimer.data.MIGRATION_1_2
import io.github.mecheng70.beetimer.data.MIGRATION_2_3
import io.github.mecheng70.beetimer.data.MIGRATION_3_4
import io.github.mecheng70.beetimer.data.Prefs
import io.github.mecheng70.beetimer.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** Wires the app by hand; small enough not to need a DI library. */
class BeeTimerApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val prefs by lazy { Prefs(this) }
    private val db by lazy {
        Room.databaseBuilder(
            this,
            BeeTimerDb::class.java,
            "beetimer.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }
    val repo by lazy { Repository(db) { n -> getString(R.string.hive_default_name, n) } }
    val alarms by lazy { AlarmScheduler(this) }

    /** "Vaporizer free" messages for the snackbar while the app is on screen. */
    val deviceFreeInApp = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 4)

    /** "Alarm volume is low" snackbar, from the first Start of a session. */
    val volumeLowInApp = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val volumeCheck by lazy {
        VolumeCheck(object : VolumeCheck.Store {
            override var checkedThisSession: Boolean
                get() = prefs.volumeChecked
                set(v) {
                    prefs.volumeChecked = v
                }
        })
    }

    /** Alarm stream volume and its maximum. Alarms ring on this stream. */
    fun alarmVolume(): Pair<Int, Int> {
        val audio = getSystemService(AudioManager::class.java)
        return audio.getStreamVolume(AudioManager.STREAM_ALARM) to audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    }

    val engine by lazy {
        SchedulerEngine(
            repo,
            EffectExecutor(
                context = this,
                alarms = alarms,
                notifier = Notifier(this),
                isForeground = {
                    ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                },
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
