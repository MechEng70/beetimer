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
import kotlinx.coroutines.flow.Flow

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
    version = 1,
    exportSchema = true,
)
abstract class BeeTimerDb : RoomDatabase() {
    abstract fun hives(): HiveDao
    abstract fun templates(): TemplateDao
    abstract fun state(): StateDao
}
