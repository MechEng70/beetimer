package io.github.mecheng70.beetimer.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.mecheng70.beetimer.scheduler.Presets
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * R11 T-010: a corrupt row never crashes a read and is quarantined (kept as it was) before anything overwrites
 * it. Before R11 a corrupt step-list row crashed app start, and a corrupt state row was silently lost.
 */
@RunWith(RobolectricTestRunner::class)
class QuarantineTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BeeTimerDb::class.java)
        .allowMainThreadQueries()
        .build()
    private val repo = Repository(db) { n -> "Hive $n" }

    @After
    fun close() = db.close()

    @Test
    fun corruptStateRowLoadsAsNoStateAndIsKeptBeforeTheNextSave() = runBlocking {
        repo.seedIfNeeded()
        db.state().put(StateEntity(json = BAD_JSON))

        assertNull(repo.loadState())
        repo.saveState(SchedulerState(template = repo.activeTemplate()))

        val kept = repo.quarantined().single()
        assertEquals(Repository.SOURCE_STATE, kept.source)
        assertEquals(BAD_JSON, kept.payload)
        assertTrue(kept.error.isNotEmpty())
        assertEquals(SchedulerState(template = repo.activeTemplate()), repo.loadState())
    }

    @Test
    fun corruptStateRowReadTwiceIsKeptOnce() = runBlocking {
        db.state().put(StateEntity(json = BAD_JSON))
        repo.loadState()
        repo.loadState()
        assertEquals(1, repo.quarantined().size)
    }

    @Test
    fun corruptPresetRowIsKeptAndRepairedToItsDefaultStayingActive() = runBlocking {
        repo.seedIfNeeded()
        db.templates().upsert(TemplateEntity(Presets.PAN_WAND_ID, "OAV pan wand", BAD_JSON, isActive = true))

        assertEquals(Presets.panWandStaysIn, repo.activeTemplate())
        val all = repo.allTemplates().associateBy { it.template.id }
        assertEquals(true, all.getValue(Presets.PAN_WAND_ID).isActive)
        assertEquals(Presets.gunType, all.getValue(Presets.GUN_TYPE_ID).template)

        val kept = repo.quarantined().single()
        assertEquals("${Repository.SOURCE_TEMPLATES}${Presets.PAN_WAND_ID}", kept.source)
        assertEquals(BAD_JSON, kept.payload)
    }

    @Test
    fun corruptPresetRowDoesNotBreakTheObservedList() = runBlocking {
        repo.seedIfNeeded()
        db.templates().upsert(TemplateEntity(Presets.GUN_TYPE_ID, "OAV gun type", BAD_JSON, isActive = false))

        val listed = repo.templates.first().associateBy { it.template.id }
        assertEquals(Presets.gunType, listed.getValue(Presets.GUN_TYPE_ID).template)
        assertEquals(1, repo.quarantined().size)
    }

    @Test
    fun corruptRowThatIsNotAPresetIsKeptAndDropped() = runBlocking {
        repo.seedIfNeeded()
        db.templates().upsert(TemplateEntity("custom", "Mine", BAD_JSON, isActive = false))

        assertEquals(Presets.all.map { it.id }.sorted(), repo.allTemplates().map { it.template.id })
        assertEquals("${Repository.SOURCE_TEMPLATES}custom", repo.quarantined().single().source)
    }

    @Test
    fun healthyDataIsUntouched() = runBlocking {
        repo.seedIfNeeded()
        val before = db.templates().all()
        val state = SchedulerState(template = repo.activeTemplate())
        repo.saveState(state)

        assertEquals(state, repo.loadState())
        assertEquals(Presets.panWandStaysIn, repo.activeTemplate())
        assertEquals(before, db.templates().all())
        assertEquals(emptyList<QuarantineEntity>(), repo.quarantined())
    }

    private companion object {
        const val BAD_JSON = "{\"runs\": [not json"
    }
}
