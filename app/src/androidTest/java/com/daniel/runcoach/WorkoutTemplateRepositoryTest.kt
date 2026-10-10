package com.daniel.runcoach

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.data.WorkoutTemplateRepository
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented checks for the template repository (plan P2.1): CRUD, duplicate, built-in protection. */
@RunWith(AndroidJUnit4::class)
class WorkoutTemplateRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun builtInExistsAndCannotBeDeletedOrEdited() = runBlocking {
        val repo = WorkoutTemplateRepository(context)
        repo.ensureBuiltIn()

        val free = repo.byId(WorkoutTemplateRepository.FREE_RUN_ID)
        assertNotNull("built-in present", free)
        assertTrue(free!!.isBuiltIn)

        assertFalse("built-in not deletable", repo.delete(WorkoutTemplateRepository.FREE_RUN_ID))

        val edited = repo.update(WorkoutTemplateRepository.freeRunWorkout().copy(name = "changed"))
        assertFalse("built-in not editable", edited)
    }

    @Test
    fun createUpdateDuplicateAndDelete() = runBlocking {
        val repo = WorkoutTemplateRepository(context)
        val id = "test-${System.nanoTime()}"
        val workout = Workout(
            id = id,
            name = "My 5k",
            items = listOf(Segment("Run", SegmentKind.RUN, Limit.Distance(5000), targetPaceSecPerKm = 420)),
        )

        repo.create(workout)
        val created = repo.byId(id)
        assertNotNull(created)
        assertEquals("My 5k", created!!.name)

        assertTrue(repo.update(workout.copy(name = "My 5k edited")))
        assertEquals("My 5k edited", repo.byId(id)!!.name)

        val copy = repo.duplicate(id)
        assertNotNull(copy)
        assertTrue(copy!!.id != id)

        assertTrue(repo.delete(id))
        assertNull(repo.byId(id))
        assertTrue(repo.delete(copy.id))
    }
}
