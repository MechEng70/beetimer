package io.github.mecheng70.beetimer.data

import androidx.room.withTransaction
import io.github.mecheng70.beetimer.scheduler.Hive
import io.github.mecheng70.beetimer.scheduler.Presets
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.scheduler.Step
import io.github.mecheng70.beetimer.scheduler.Template
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

data class StoredTemplate(val template: Template, val isActive: Boolean)

class Repository(private val db: BeeTimerDb, private val defaultHiveName: (Int) -> String) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val stepsSerializer = ListSerializer(Step.serializer())

    val hives: Flow<List<Hive>> = db.hives().observe().map { list -> list.map { it.toHive() } }

    val templates: Flow<List<StoredTemplate>> = db.templates().observe().map { readTemplates(it) }

    /**
     * First run: both presets (pan wand active) and three hives, in one transaction and
     * only while the templates table is empty. Room runs write transactions one at a time,
     * so a second caller sees the seeded rows and does nothing (regression: two startup
     * callers raced and seeded 6 hives; see SeedingTest).
     */
    suspend fun seedIfNeeded() {
        db.withTransaction {
            if (db.templates().all().isNotEmpty()) return@withTransaction
            Presets.all.forEach { db.templates().upsert(it.toEntity(isActive = it.id == Presets.PAN_WAND_ID)) }
            insertHives(DEFAULT_HIVES)
        }
    }

    suspend fun hiveList(): List<Hive> = db.hives().all().map { it.toHive() }

    suspend fun allTemplates(): List<StoredTemplate> = readTemplates(db.templates().all())

    suspend fun activeTemplate(): Template {
        seedIfNeeded()
        val all = readTemplates(db.templates().all())
        return (all.firstOrNull { it.isActive } ?: all.first()).template
    }

    /**
     * The saved scheduler state, or null if there is none. A row that can't be read is quarantined first and then
     * treated as no state, so runs start over, as before R11, but the data is kept.
     */
    suspend fun loadState(): SchedulerState? {
        val row = db.state().get() ?: return null
        return runCatching { json.decodeFromString(SchedulerState.serializer(), row.json) }
            .getOrElse { e ->
                quarantine(SOURCE_STATE, row.json, e)
                null
            }
    }

    /** Rows that couldn't be read, oldest first. Kept for export; the app doesn't show them. */
    suspend fun quarantined(): List<QuarantineEntity> = db.quarantine().all()

    suspend fun saveState(state: SchedulerState) {
        db.state().put(StateEntity(json = json.encodeToString(SchedulerState.serializer(), state)))
    }

    /** Adds one default-named hive and returns its id, or null at [MAX_HIVES]. */
    suspend fun addHive(): Long? = db.withTransaction {
        if (db.hives().all().size >= MAX_HIVES) null else insertHives(1).firstOrNull()
    }

    /** Saves the list order, which is also the treatment order. */
    suspend fun reorderHives(ids: List<Long>) {
        db.withTransaction {
            val byId = db.hives().all().associateBy { it.id }
            db.hives().update(ids.mapIndexedNotNull { i, id -> byId[id]?.copy(position = i) })
        }
    }

    /** Call inside a transaction. Returns the new ids. */
    private suspend fun insertHives(count: Int): List<Long> {
        val existing = db.hives().all()
        var position = (existing.maxOfOrNull { it.position } ?: -1) + 1
        var number = existing.size + 1
        val names = existing.map { it.label }.toSet()
        return List(count) {
            while (defaultHiveName(number) in names) number++
            val id = db.hives().insert(HiveEntity(label = defaultHiveName(number), note = null, position = position))
            position++
            number++
            id
        }
    }

    suspend fun updateHive(id: Long, label: String, note: String?) {
        val all = db.hives().all()
        val hive = all.firstOrNull { it.id == id } ?: return
        db.hives().update(
            listOf(
                hive.copy(
                    label = label.trim().ifEmpty {
                        hive.label
                    },
                    note = note?.trim()?.ifEmpty { null },
                ),
            ),
        )
    }

    suspend fun deleteHive(id: Long) = db.hives().delete(id)

    suspend fun saveTemplate(template: Template) {
        val active = db.templates().all().firstOrNull { it.id == template.id }?.isActive ?: false
        db.templates().upsert(template.toEntity(active))
    }

    suspend fun setActiveTemplate(id: String) = db.templates().setActive(id)

    private fun HiveEntity.toHive() = Hive(id = id, label = label, note = note, position = position)

    /**
     * Decodes template rows. A row that can't be read is quarantined, then repaired to its preset's default steps
     * (keeping which one is active), or dropped if it isn't a preset. Healthy rows are untouched.
     */
    private suspend fun readTemplates(rows: List<TemplateEntity>): List<StoredTemplate> = rows.mapNotNull { row ->
        runCatching { row.toStored() }.getOrElse { e -> repairTemplate(row, e) }
    }

    private suspend fun repairTemplate(row: TemplateEntity, e: Throwable): StoredTemplate? {
        val preset = Presets.all.firstOrNull { it.id == row.id }
        db.withTransaction {
            // Another reader may have repaired it already.
            if (db.templates().byId(row.id)?.stepsJson == row.stepsJson) {
                quarantine("$SOURCE_TEMPLATES${row.id}", row.stepsJson, e)
                if (preset != null) {
                    db.templates().upsert(preset.toEntity(row.isActive))
                } else {
                    db.templates().delete(row.id)
                }
            }
        }
        return preset?.let { StoredTemplate(it, row.isActive) }
    }

    /** Keeps [payload] as it was, once per source and payload. */
    private suspend fun quarantine(source: String, payload: String, e: Throwable) {
        db.withTransaction {
            if (db.quarantine().count(source, payload) == 0) {
                val error = e.toString().take(MAX_ERROR_CHARS)
                db.quarantine().insert(
                    QuarantineEntity(
                        source = source,
                        payload = payload,
                        error = error,
                        at = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    private fun TemplateEntity.toStored() = StoredTemplate(
        template = Template(id = id, name = name, steps = json.decodeFromString(stepsSerializer, stepsJson)),
        isActive = isActive,
    )

    private fun Template.toEntity(isActive: Boolean) = TemplateEntity(
        id = id,
        name = name,
        stepsJson = json.encodeToString(stepsSerializer, steps),
        isActive = isActive,
    )

    companion object {
        const val DEFAULT_HIVES = 3

        /** Hives per session (one vaporizer; Jason, 2026-11-21). */
        const val MAX_HIVES = 5

        const val SOURCE_STATE = "scheduler_state"
        const val SOURCE_TEMPLATES = "templates:"
        private const val MAX_ERROR_CHARS = 500
    }
}
