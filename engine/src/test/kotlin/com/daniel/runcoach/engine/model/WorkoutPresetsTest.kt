package com.daniel.runcoach.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkoutPresetsTest {

    @Test
    fun `progressive tempo steps the pace down each kilometre`() {
        val workout = WorkoutPresets.progressiveTempo(kilometers = 5, startPaceSecPerKm = 450, stepSecPerKm = 20)
        val targets = workout.items.filterIsInstance<Segment>().map { it.targetPaceSecPerKm }
        assertEquals(listOf(450, 430, 410, 390, 370), targets)
        assertTrue(WorkoutValidator.validate(workout).isEmpty())
    }

    @Test
    fun `all presets pass validation`() {
        val presets = listOf(
            WorkoutPresets.stableRun(5000, 420),
            WorkoutPresets.progressiveTempo(3, 450, 20, warmUpMeters = 1000, coolDownMeters = 1000),
            WorkoutPresets.interval(
                warmUpMeters = 1500,
                work = Limit.Distance(200),
                workTargetSecPerKm = 390,
                recovery = Limit.Distance(400),
                recoveryTargetSecPerKm = 480,
                repetitions = 3,
                coolDownMeters = 1000,
            ),
            WorkoutPresets.runWalk(runSeconds = 360, walkSeconds = 120, goalDistanceM = 5000),
        )
        presets.forEach { assertTrue(WorkoutValidator.validate(it).isEmpty(), "invalid preset: ${it.name}") }
    }

    @Test
    fun `run walk preset carries the goal distance`() {
        val workout = WorkoutPresets.runWalk(360, 120, 5000)
        assertEquals(5000, workout.goalDistanceM)
        val repeat = workout.items.single() as Repeat
        assertEquals(5000, repeat.untilTotalDistanceM)
    }
}
