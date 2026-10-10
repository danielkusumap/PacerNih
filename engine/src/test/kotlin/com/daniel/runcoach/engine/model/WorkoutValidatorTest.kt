package com.daniel.runcoach.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkoutValidatorTest {

    private fun codes(workout: Workout) = WorkoutValidator.validate(workout).map { it.code }

    private fun workout(vararg items: Block, goalDistanceM: Int? = null) =
        Workout(id = "w", name = "w", items = items.toList(), goalDistanceM = goalDistanceM)

    private fun run(limit: Limit, pace: Int? = 420) =
        Segment("Run", SegmentKind.RUN, limit, pace)

    @Test
    fun `all five samples validate`() {
        for (sample in Samples.all()) {
            assertTrue(WorkoutValidator.validate(sample).isEmpty(), "invalid: ${sample.id}")
        }
    }

    @Test
    fun `empty workout is rejected`() {
        assertEquals(listOf(WorkoutErrorCode.WORKOUT_EMPTY), codes(workout()))
    }

    @Test
    fun `repeat without a bound is rejected`() {
        val w = workout(Repeat(children = listOf(run(Limit.Distance(100)))))
        assertTrue(WorkoutErrorCode.REPEAT_BOUND_REQUIRED in codes(w))
    }

    @Test
    fun `repeat with both bounds is rejected`() {
        val w = workout(Repeat(times = 3, untilTotalDistanceM = 500, children = listOf(run(Limit.Distance(100)))))
        assertTrue(WorkoutErrorCode.REPEAT_BOUND_CONFLICT in codes(w))
    }

    @Test
    fun `repeat with zero times is rejected`() {
        val w = workout(Repeat(times = 0, children = listOf(run(Limit.Distance(100)))))
        assertTrue(WorkoutErrorCode.REPEAT_TIMES_NOT_POSITIVE in codes(w))
    }

    @Test
    fun `repeat with non positive distance bound is rejected`() {
        val w = workout(Repeat(untilTotalDistanceM = 0, children = listOf(run(Limit.Distance(100)))))
        assertTrue(WorkoutErrorCode.REPEAT_UNTIL_NOT_POSITIVE in codes(w))
    }

    @Test
    fun `repeat with no children is rejected`() {
        val w = workout(Repeat(times = 2, children = emptyList()))
        assertTrue(WorkoutErrorCode.REPEAT_CHILDREN_EMPTY in codes(w))
    }

    @Test
    fun `open inside a repeat is rejected`() {
        val w = workout(Repeat(times = 2, children = listOf(run(Limit.Open))))
        assertTrue(WorkoutErrorCode.OPEN_IN_REPEAT in codes(w))
    }

    @Test
    fun `open before the last segment is rejected`() {
        val w = workout(run(Limit.Open), run(Limit.Distance(100)))
        assertTrue(WorkoutErrorCode.OPEN_NOT_LAST in codes(w))
    }

    @Test
    fun `open as the last segment is accepted`() {
        val w = workout(run(Limit.Distance(100)), run(Limit.Open))
        assertTrue(WorkoutValidator.validate(w).isEmpty())
    }

    @Test
    fun `non positive distance is rejected`() {
        val w = workout(run(Limit.Distance(0)))
        assertTrue(WorkoutErrorCode.DISTANCE_NOT_POSITIVE in codes(w))
    }

    @Test
    fun `non positive duration is rejected`() {
        val w = workout(run(Limit.Duration(-5)))
        assertTrue(WorkoutErrorCode.DURATION_NOT_POSITIVE in codes(w))
    }

    @Test
    fun `target pace faster than the allowed range is rejected`() {
        val w = workout(run(Limit.Distance(1000), pace = 100))
        assertTrue(WorkoutErrorCode.TARGET_PACE_OUT_OF_RANGE in codes(w))
    }

    @Test
    fun `target pace slower than the allowed range is rejected`() {
        val w = workout(run(Limit.Distance(1000), pace = 1300))
        assertTrue(WorkoutErrorCode.TARGET_PACE_OUT_OF_RANGE in codes(w))
    }

    @Test
    fun `non positive goal distance is rejected`() {
        val w = workout(run(Limit.Distance(1000)), goalDistanceM = 0)
        assertTrue(WorkoutErrorCode.GOAL_DISTANCE_NOT_POSITIVE in codes(w))
    }

    @Test
    fun `errors point at the offending path`() {
        val w = workout(Repeat(times = 2, children = listOf(run(Limit.Distance(0)))))
        val error = WorkoutValidator.validate(w).first { it.code == WorkoutErrorCode.DISTANCE_NOT_POSITIVE }
        assertEquals("items[0].children[0].limit", error.path)
    }
}
