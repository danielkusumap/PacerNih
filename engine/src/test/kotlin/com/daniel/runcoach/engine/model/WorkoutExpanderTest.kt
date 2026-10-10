package com.daniel.runcoach.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WorkoutExpanderTest {

    @Test
    fun `interval expands to warmup, three work recovery pairs and cooldown`() {
        val labels = WorkoutExpander.expandFinite(Samples.interval().items).map { it.label }
        assertEquals(
            listOf("Warm up", "Interval", "Recovery", "Interval", "Recovery", "Interval", "Recovery", "Cooldown"),
            labels,
        )
    }

    @Test
    fun `step count reflects repeat expansion`() {
        assertEquals(8, WorkoutExpander.stepCountOrNull(Samples.interval().items))
        assertEquals(1, WorkoutExpander.stepCountOrNull(Samples.stable5k().items))
        assertEquals(5, WorkoutExpander.stepCountOrNull(Samples.tempo().items))
    }

    @Test
    fun `distance bounded repeat has no fixed step count`() {
        assertNull(WorkoutExpander.stepCountOrNull(Samples.runWalk().items))
    }

    @Test
    fun `finite expansion rejects a distance bounded repeat`() {
        assertFailsWith<IllegalStateException> {
            WorkoutExpander.expandFinite(Samples.runWalk().items)
        }
    }

    @Test
    fun `lazy steps yields a distance bounded repeat cyclically`() {
        val labels = WorkoutExpander.lazySteps(Samples.runWalk().items).take(5).map { it.label }.toList()
        assertEquals(listOf("Run", "Walk", "Run", "Walk", "Run"), labels)
    }

    @Test
    fun `lazy steps stops after a finite repeat`() {
        val labels = WorkoutExpander.lazySteps(Samples.interval().items).map { it.label }.toList()
        assertEquals(8, labels.size)
        assertEquals("Cooldown", labels.last())
    }
}
