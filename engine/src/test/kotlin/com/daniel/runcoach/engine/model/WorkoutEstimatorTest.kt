package com.daniel.runcoach.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkoutEstimatorTest {

    @Test
    fun `fully known workout has exact distance and duration`() {
        val estimate = WorkoutEstimator.estimate(Samples.stable5k())
        assertEquals(5000.0, estimate.distanceM!!, 1e-9)
        assertFalse(estimate.distanceIsAtLeast)
        assertEquals(2100.0, estimate.durationSec!!, 1e-9)
        assertFalse(estimate.durationIsAtLeast)
    }

    @Test
    fun `repeats multiply into the estimate`() {
        val estimate = WorkoutEstimator.estimate(Samples.interval())
        assertEquals(4300.0, estimate.distanceM!!, 1e-9)
        assertEquals(1866.0, estimate.durationSec!!, 1e-9)
        assertFalse(estimate.distanceIsAtLeast)
    }

    @Test
    fun `distance bounded repeat and open segment are lower bounds`() {
        val runWalk = WorkoutEstimator.estimate(Samples.runWalk())
        assertEquals(5000.0, runWalk.distanceM!!, 1e-9)
        assertTrue(runWalk.distanceIsAtLeast)
        assertTrue(runWalk.durationIsAtLeast)

        val free = WorkoutEstimator.estimate(Samples.freeRun())
        assertTrue(free.distanceIsAtLeast)
        assertTrue(free.durationIsAtLeast)
    }
}
