package com.daniel.runcoach.engine.runner

import com.daniel.runcoach.engine.model.Samples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StepCursorTest {

    @Test
    fun `fixed repeat expands to the expected order`() {
        val cursor = StepCursor(Samples.interval().items)
        val labels = generateSequence { cursor.next(0.0) }.map { it.label }.toList()
        assertEquals(
            listOf("Warm up", "Interval", "Recovery", "Interval", "Recovery", "Interval", "Recovery", "Cooldown"),
            labels,
        )
    }

    @Test
    fun `hasNext is false after the last step`() {
        val cursor = StepCursor(Samples.stable5k().items)
        assertTrue(cursor.hasNext(0.0))
        assertEquals("5 km", cursor.next(0.0)?.label)
        assertFalse(cursor.hasNext(0.0))
        assertNull(cursor.next(0.0))
    }

    @Test
    fun `peek does not consume the step`() {
        val cursor = StepCursor(Samples.interval().items)
        assertEquals("Warm up", cursor.peek(0.0)?.label)
        assertEquals("Warm up", cursor.next(0.0)?.label)
        assertEquals("Interval", cursor.peek(0.0)?.label)
    }

    @Test
    fun `distance bounded repeat cycles children until the goal is reached`() {
        val cursor = StepCursor(Samples.runWalk().items)
        assertEquals("Run", cursor.next(0.0)?.label)
        assertEquals("Walk", cursor.next(0.0)?.label)
        assertEquals("Run", cursor.next(1000.0)?.label)
        // Goal reached after a child finishes -> repeat ends, no further steps.
        assertNull(cursor.next(5000.0))
    }
}
