package io.github.mecheng70.beetimer.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.mecheng70.beetimer.alarms.EffectRunner
import io.github.mecheng70.beetimer.alarms.SchedulerEngine
import io.github.mecheng70.beetimer.scheduler.Event
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HivesAndSessionTest {
    private lateinit var db: BeeTimerDb
    private lateinit var repo: Repository
    private var now = 1_000_000L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BeeTimerDb::class.java).build()
        repo = Repository(db) { n -> "Hive $n" }
        repo.seedIfNeeded()
    }

    @After
    fun tearDown() = db.close()

    private fun engine() = SchedulerEngine(repo, EffectRunner { _, _, _ -> }, clock = { now })

    @Test
    fun hiveNamesPersistAcrossSessions() = runBlocking {
        val ids = repo.hiveList().map { it.id }
        repo.updateHive(ids[0], "Emory", null)
        repo.updateHive(ids[1], "Herb", "by the fence")
        repo.updateHive(ids[2], "Sparky", null)
        val engine = engine()
        engine.dispatch(Event.Start(ids[0]))
        engine.dispatch(Event.Done(ids[0]))
        engine.dispatch(Event.NewSession)
        assertTrue(engine.state.value!!.runs.isEmpty())
        val hives = repo.hiveList()
        assertEquals(listOf("Emory", "Herb", "Sparky"), hives.map { it.label })
        assertEquals("by the fence", hives[1].note)
        // A fresh engine (app restart) sees the same names and no runs.
        val reloaded = engine()
        reloaded.ensureLoaded()
        assertTrue(reloaded.state.value!!.runs.isEmpty())
        assertEquals(listOf("Emory", "Herb", "Sparky"), repo.hiveList().map { it.label })
    }

    @Test
    fun addHiveStopsAtFive() = runBlocking {
        assertEquals(3, repo.hiveList().size)
        val fourth = repo.addHive()
        val fifth = repo.addHive()
        val sixth = repo.addHive()
        assertEquals(listOf("Hive 4", "Hive 5"), repo.hiveList().filter { it.id == fourth || it.id == fifth }.map { it.label })
        assertNull(sixth)
        assertEquals(Repository.MAX_HIVES, repo.hiveList().size)
    }

    @Test
    fun reorderPersistsListOrder() = runBlocking {
        val ids = repo.hiveList().map { it.id }
        repo.reorderHives(listOf(ids[2], ids[0], ids[1]))
        assertEquals(listOf(ids[2], ids[0], ids[1]), repo.hiveList().map { it.id })
    }
}
