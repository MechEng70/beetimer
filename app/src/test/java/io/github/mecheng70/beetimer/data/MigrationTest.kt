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

/** Migrations keep hives and the active choice, and leave both presets in the current model. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), BeeTimerDb::class.java)

    private val name = "migration-test.db"

    @Test
    fun migrate2To3_rewritesBothPresetsToTheMarkerModel() = runBlocking {
        val v2 = "migration-test-v2.db"
        helper.createDatabase(v2, 2).apply {
            val oldPan = """[{"name":"Remove wand and unseal","kind":"MANUAL","deviceBound":true}]"""
            val oldGun = """[{"name":"Withdraw","kind":"MANUAL","deviceBound":true}]"""
            execSQL(
                "INSERT INTO templates (id, name, stepsJson, isActive) VALUES ('oav-pan-wand', 'OAV pan wand', '$oldPan', 1)",
            )
            execSQL(
                "INSERT INTO templates (id, name, stepsJson, isActive) VALUES ('oav-gun-type', 'OAV gun type', '$oldGun', 0)",
            )
            execSQL("INSERT INTO hives (id, label, note, position) VALUES (3, 'Herb', 'fence', 0)")
            close()
        }
        helper.runMigrationsAndValidate(v2, 3, true, MIGRATION_2_3).close()
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), BeeTimerDb::class.java, v2)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        val repo = Repository(db) { n -> "Hive $n" }
        val templates = repo.allTemplates().associateBy { it.template.id }
        assertEquals(Presets.panWandStaysIn, templates.getValue(Presets.PAN_WAND_ID).template)
        assertEquals(true, templates.getValue(Presets.PAN_WAND_ID).isActive)
        assertEquals(Presets.gunType, templates.getValue(Presets.GUN_TYPE_ID).template)
        assertEquals(listOf("Herb"), repo.hiveList().map { it.label })
        db.close()
    }

    @Test
    fun migrate1To2_reseedsPanWandOnly() = runBlocking {
        helper.createDatabase(name, 1).apply {
            val oldPan = """[{"name":"Seal hive","kind":"MANUAL"},{"name":"Unseal","kind":"MANUAL"}]"""
            val gun = """[{"name":"Custom gun","kind":"MANUAL"}]"""
            execSQL(
                "INSERT INTO templates (id, name, stepsJson, isActive) VALUES ('oav-pan-wand', 'Old pan', '$oldPan', 0)",
            )
            execSQL(
                "INSERT INTO templates (id, name, stepsJson, isActive) VALUES ('oav-gun-type', 'OAV gun type', '$gun', 1)",
            )
            execSQL("INSERT INTO hives (id, label, note, position) VALUES (7, 'Emory', NULL, 0)")
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).close()

        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), BeeTimerDb::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        val repo = Repository(db) { n -> "Hive $n" }
        val templates = repo.allTemplates()
        val pan = templates.first { it.template.id == Presets.PAN_WAND_ID }
        assertEquals(Presets.panWand, pan.template)
        assertEquals(false, pan.isActive)
        val gun = templates.first { it.template.id == Presets.GUN_TYPE_ID }
        assertEquals("v2 -> v3 moves gun type to the marker model too", Presets.gunType, gun.template)
        assertEquals(true, gun.isActive)
        assertEquals(listOf("Emory"), repo.hiveList().map { it.label })
        db.close()
    }
}
