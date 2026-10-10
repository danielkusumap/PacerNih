package com.daniel.runcoach.data

import android.content.Context
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.model.WorkoutJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** A workout template with its decoded [Workout] (plan P2.1). */
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val workout: Workout,
    val isBuiltIn: Boolean,
    val lastUsedAt: Long?,
)

/**
 * CRUD for workout templates (plan P2.1). Templates are independent of history: deleting one never affects
 * past sessions. The built-in "Lari Bebas" cannot be edited or deleted.
 */
class WorkoutTemplateRepository(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val dao = RunDatabase.get(context).workoutTemplateDao()

    val templates: Flow<List<WorkoutTemplate>> =
        dao.observeAll().map { list -> list.mapNotNull { it.toDomain() } }

    suspend fun all(): List<WorkoutTemplate> = dao.all().mapNotNull { it.toDomain() }

    suspend fun byId(id: String): WorkoutTemplate? = dao.byId(id)?.toDomain()

    suspend fun create(workout: Workout): WorkoutTemplate {
        val id = workout.id.ifBlank { UUID.randomUUID().toString() }
        return upsertNew(workout.copy(id = id), workout.name)
    }

    /** Updates an existing, non-built-in template. Returns false when absent or built in. */
    suspend fun update(workout: Workout): Boolean {
        val existing = dao.byId(workout.id) ?: return false
        if (existing.isBuiltIn) return false
        dao.upsert(existing.copy(name = workout.name, json = WorkoutJson.encode(workout), updatedAt = clock()))
        return true
    }

    suspend fun duplicate(id: String): WorkoutTemplate? {
        val source = dao.byId(id) ?: return null
        val workout = runCatching { WorkoutJson.decode(source.json) }.getOrNull() ?: return null
        val newId = UUID.randomUUID().toString()
        val newName = "${source.name} (copy)"
        return upsertNew(workout.copy(id = newId, name = newName), newName)
    }

    /** Deletes a template unless it is built in. Returns true when a row was removed. */
    suspend fun delete(id: String): Boolean = dao.deleteIfNotBuiltIn(id) > 0

    suspend fun markUsed(id: String, atMs: Long = clock()) = dao.markUsed(id, atMs)

    /** Seeds the built-in "Lari Bebas" template if it is missing (plan section 5). */
    suspend fun ensureBuiltIn() {
        if (dao.byId(FREE_RUN_ID) == null) {
            val now = clock()
            dao.upsert(
                WorkoutTemplateEntity(
                    id = FREE_RUN_ID,
                    name = "Lari Bebas",
                    json = WorkoutJson.encode(freeRunWorkout()),
                    isBuiltIn = true,
                    createdAt = now,
                    updatedAt = now,
                    lastUsedAt = null,
                ),
            )
        }
    }

    private suspend fun upsertNew(workout: Workout, name: String): WorkoutTemplate {
        val now = clock()
        val entity = WorkoutTemplateEntity(
            id = workout.id,
            name = name,
            json = WorkoutJson.encode(workout.copy(name = name)),
            isBuiltIn = false,
            createdAt = now,
            updatedAt = now,
            lastUsedAt = null,
        )
        dao.upsert(entity)
        return checkNotNull(entity.toDomain())
    }

    private fun WorkoutTemplateEntity.toDomain(): WorkoutTemplate? =
        runCatching { WorkoutJson.decode(json) }.getOrNull()?.let { WorkoutTemplate(id, name, it, isBuiltIn, lastUsedAt) }

    companion object {
        const val FREE_RUN_ID = "free"

        fun freeRunWorkout() = Workout(
            id = FREE_RUN_ID,
            name = "Lari Bebas",
            items = listOf(Segment("Lari Bebas", SegmentKind.RUN, Limit.Open, targetPaceSecPerKm = null)),
        )
    }
}
