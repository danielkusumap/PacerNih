package com.daniel.runcoach.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkoutSerializationTest {

    @Test
    fun `all five sample workouts survive a json round trip`() {
        for (workout in Samples.all()) {
            val decoded = WorkoutJson.decode(WorkoutJson.encode(workout))
            assertEquals(workout, decoded, "round trip failed for ${workout.id}")
        }
    }

    @Test
    fun `nested repeat and limits keep their types after decoding`() {
        val decoded = WorkoutJson.decode(WorkoutJson.encode(Samples.interval()))
        val repeat = decoded.items[1]
        assertTrue(repeat is Repeat, "expected a Repeat, got $repeat")
        assertEquals(390, repeat.children[0].targetPaceSecPerKm)
        assertTrue(repeat.children[0].limit is Limit.Distance)
    }

    @Test
    fun `open limit round trips as Open`() {
        val decoded = WorkoutJson.decode(WorkoutJson.encode(Samples.freeRun()))
        assertEquals(Limit.Open, decoded.items[0].let { (it as Segment).limit })
    }

    @Test
    fun `duration limit round trips`() {
        val decoded = WorkoutJson.decode(WorkoutJson.encode(Samples.runWalk()))
        val segment = ((decoded.items[0] as Repeat).children[0])
        assertEquals(Limit.Duration(360), segment.limit)
    }

    @Test
    fun `default fast margin and per km check are applied`() {
        val segment = (Samples.stable5k().items[0] as Segment)
        assertEquals(20, segment.fastMarginSec)
        assertTrue(segment.perKmCheck)
    }
}
