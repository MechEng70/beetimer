package io.github.mecheng70.beetimer.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.mecheng70.beetimer.scheduler.Presets
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** v1 -> v2 reseeds the pan wand preset and leaves hives, gun type and the active choice alone. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), BeeTimerDb::class.java)

    private val name = "migration-test.db"

    @Test
    fun migrate1To2_reseedsPanWandOnly() = runBlocking {
        helper.createDatabase(name, 1).apply {
            val oldPan = """[{"name":"Seal hive","kind":"MANUAL"},{"name":"Unseal","kind":"MANUAL"}]"""
            val gun = """[{"name":"Custom gun","kind":"MANUAL"}]"""
            execSQL("INSERT INTO templates (id, name, stepsJson, isActive) VALUES ('oav-pan-wand', 'Old pan', '$oldPan', 0)")
            execSQL("INSERT INTO templates (id, name, stepsJson, isActive) VALUES ('oav-gun-type', 'OAV gun type', '$gun', 1)")
            execSQL("INSERT INTO hives (id, label, note, position) VALUES (7, 'Emory', NULL, 0)")
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).close()

        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), BeeTimerDb::class.java, name)
            .addMigrations(MIGRATION_1_2).build()
        val repo = Repository(db) { n -> "Hive $n" }
        val templates = repo.allTemplates()
        val pan = templates.first { it.template.id == Presets.PAN_WAND_ID }
        assertEquals(Presets.panWand, pan.template)
        assertEquals(false, pan.isActive)
        val gun = templates.first { it.template.id == Presets.GUN_TYPE_ID }
        assertEquals(listOf("Custom gun"), gun.template.steps.map { it.name })
        assertEquals(true, gun.isActive)
        assertEquals(listOf("Emory"), repo.hiveList().map { it.label })
        db.close()
    }
}
