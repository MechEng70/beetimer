package io.github.mecheng70.beetimer.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.mecheng70.beetimer.scheduler.Presets
import io.github.mecheng70.beetimer.scheduler.Step
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Entity(tableName = "hives")
data class HiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val note: String?,
    val position: Int,
)

/** A saved template. Steps are stored as JSON (kotlinx.serialization). */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val stepsJson: String,
    val isActive: Boolean,
)

/** The whole scheduler state as one JSON row (id = 1), written once per dispatch. */
@Entity(tableName = "scheduler_state")
data class StateEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
)

@Dao
interface HiveDao {
    @Query("SELECT * FROM hives ORDER BY position, id")
    fun observe(): Flow<List<HiveEntity>>

    @Query("SELECT * FROM hives ORDER BY position, id")
    suspend fun all(): List<HiveEntity>

    @Insert
    suspend fun insert(hive: HiveEntity): Long

    @Update
    suspend fun update(hives: List<HiveEntity>)

    @Query("DELETE FROM hives WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY id")
    fun observe(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates ORDER BY id")
    suspend fun all(): List<TemplateEntity>

    @Upsert
    suspend fun upsert(template: TemplateEntity)

    @Query("UPDATE templates SET isActive = (id = :id)")
    suspend fun setActive(id: String)
}

@Dao
interface StateDao {
    @Query("SELECT * FROM scheduler_state WHERE id = 1")
    suspend fun get(): StateEntity?

    @Upsert
    suspend fun put(state: StateEntity)
}

@Database(
    entities = [HiveEntity::class, TemplateEntity::class, StateEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class BeeTimerDb : RoomDatabase() {
    abstract fun hives(): HiveDao
    abstract fun templates(): TemplateDao
    abstract fun state(): StateDao
}

/**
 * v1 -> v2: no table changes. Reseeds the pan wand preset with the corrected order
 * (the wand stays in through the sealed hold, so the hold holds the device). The gun type
 * preset and hives are untouched. Active runs keep their own snapshot in scheduler_state.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val steps = Json.encodeToString(ListSerializer(Step.serializer()), Presets.panWand.steps)
        db.execSQL(
            "UPDATE templates SET name = ?, stepsJson = ? WHERE id = ?",
            arrayOf<Any>(Presets.panWand.name, steps, Presets.PAN_WAND_ID),
        )
    }
}

/**
 * v2 -> v3: no table changes. Presets move to the take/release marker model (Device is
 * derived from one take step and one release step). Rewrites both preset rows with the new
 * defaults (pan wand: wand stays in). Active runs keep their own snapshot.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val json = Json { encodeDefaults = true }
        listOf(Presets.panWandStaysIn, Presets.gunType).forEach { preset ->
            db.execSQL(
                "UPDATE templates SET name = ?, stepsJson = ? WHERE id = ?",
                arrayOf<Any>(preset.name, json.encodeToString(ListSerializer(Step.serializer()), preset.steps), preset.id),
            )
        }
    }
}
