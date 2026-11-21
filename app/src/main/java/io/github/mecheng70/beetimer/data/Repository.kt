package io.github.mecheng70.beetimer.data

import androidx.room.withTransaction
import io.github.mecheng70.beetimer.scheduler.Hive
import io.github.mecheng70.beetimer.scheduler.Presets
import io.github.mecheng70.beetimer.scheduler.SchedulerState
import io.github.mecheng70.beetimer.scheduler.Step
import io.github.mecheng70.beetimer.scheduler.Template
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

data class StoredTemplate(val template: Template, val isActive: Boolean)

class Repository(
    private val db: BeeTimerDb,
    private val prefs: Prefs,
    private val defaultHiveName: (Int) -> String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val stepsSerializer = ListSerializer(Step.serializer())

    val hives: Flow<List<Hive>> = db.hives().observe().map { list -> list.map { it.toHive() } }

    val templates: Flow<List<StoredTemplate>> = db.templates().observe().map { list -> list.map { it.toStored() } }

    private val seedLock = Mutex()

    /**
     * First run: both presets (pan wand active) and three hives. Locked because app
     * startup and the engine's first load both call it (regression: 6 hives seeded).
     */
    suspend fun seedIfNeeded() = seedLock.withLock {
        if (db.templates().all().isEmpty()) {
            Presets.all.forEach { db.templates().upsert(it.toEntity(isActive = it.id == Presets.PAN_WAND_ID)) }
        }
        if (!prefs.seeded) {
            if (db.hives().all().isEmpty()) addHives(DEFAULT_HIVES)
            prefs.seeded = true
        }
    }

    suspend fun hiveList(): List<Hive> = db.hives().all().map { it.toHive() }

    suspend fun activeTemplate(): Template {
        seedIfNeeded()
        val all = db.templates().all()
        return (all.firstOrNull { it.isActive } ?: all.first()).toStored().template
    }

    suspend fun loadState(): SchedulerState? =
        db.state().get()?.let { runCatching { json.decodeFromString(SchedulerState.serializer(), it.json) }.getOrNull() }

    suspend fun saveState(state: SchedulerState) {
        db.state().put(StateEntity(json = json.encodeToString(SchedulerState.serializer(), state)))
    }

    suspend fun addHives(count: Int) {
        db.withTransaction {
            val existing = db.hives().all()
            var position = (existing.maxOfOrNull { it.position } ?: -1) + 1
            var number = existing.size + 1
            val names = existing.map { it.label }.toSet()
            repeat(count) {
                while (defaultHiveName(number) in names) number++
                db.hives().insert(HiveEntity(label = defaultHiveName(number), note = null, position = position))
                position++
                number++
            }
        }
    }

    suspend fun updateHive(id: Long, label: String, note: String?) {
        val all = db.hives().all()
        val hive = all.firstOrNull { it.id == id } ?: return
        db.hives().update(listOf(hive.copy(label = label.trim().ifEmpty { hive.label }, note = note?.trim()?.ifEmpty { null })))
    }

    suspend fun moveHive(id: Long, delta: Int) {
        db.withTransaction {
            val list = db.hives().all().toMutableList()
            val i = list.indexOfFirst { it.id == id }
            val j = i + delta
            if (i < 0 || j !in list.indices) return@withTransaction
            list.add(j, list.removeAt(i))
            db.hives().update(list.mapIndexed { index, h -> h.copy(position = index) })
        }
    }

    suspend fun deleteHive(id: Long) = db.hives().delete(id)

    suspend fun saveTemplate(template: Template) {
        val active = db.templates().all().firstOrNull { it.id == template.id }?.isActive ?: false
        db.templates().upsert(template.toEntity(active))
    }

    suspend fun setActiveTemplate(id: String) = db.templates().setActive(id)

    private fun HiveEntity.toHive() = Hive(id = id, label = label, note = note, position = position)

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

    private companion object {
        const val DEFAULT_HIVES = 3
    }
}
