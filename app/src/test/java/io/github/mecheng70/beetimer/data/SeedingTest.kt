package io.github.mecheng70.beetimer.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.mecheng70.beetimer.scheduler.Presets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression: app startup and the engine's first load both seeded at once and the
 * first build created 6 hives instead of 3.
 */
@RunWith(RobolectricTestRunner::class)
class SeedingTest {
    private fun newDb(): BeeTimerDb =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BeeTimerDb::class.java).build()

    private fun repo(db: BeeTimerDb) = Repository(db) { n -> "Hive $n" }

    @Test
    fun concurrentFirstLaunchSeedingCreatesThreeHivesAndTwoPresets() = runBlocking {
        repeat(ROUNDS) { round ->
            val db = newDb()
            val repo = repo(db)
            coroutineScope {
                (1..SEEDERS).map { async(Dispatchers.IO) { repo.seedIfNeeded() } }.awaitAll()
            }
            assertEquals("round $round hives", listOf("Hive 1", "Hive 2", "Hive 3"), db.hives().all().map { it.label })
            val templates = db.templates().all()
            assertEquals("round $round presets", Presets.all.map { it.id }.sorted(), templates.map { it.id })
            assertEquals(
                "round $round active",
                listOf(Presets.PAN_WAND_ID),
                templates.filter {
                    it.isActive
                }.map { it.id },
            )
            db.close()
        }
    }

    @Test
    fun seedingDoesNotRunAgainAfterHivesAreDeleted() = runBlocking {
        val db = newDb()
        val repo = repo(db)
        repo.seedIfNeeded()
        db.hives().all().forEach { db.hives().delete(it.id) }
        repo.seedIfNeeded()
        assertEquals(0, db.hives().all().size)
        assertEquals(2, db.templates().all().size)
        db.close()
    }

    private companion object {
        const val ROUNDS = 20
        const val SEEDERS = 2
    }
}
