package com.daniel.runcoach.engine.result

import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionResultsCalculatorTest {

    /** A constant 3 m/s track sampled every second. */
    private fun constantTrack(totalM: Int): List<DistTimePoint> =
        (0..(totalM / 3)).map { DistTimePoint((it * 3).toDouble(), it * 1000L) }

    @Test
    fun distanceSegmentIsEvaluatedFromTheCurve() {
        val workout = Workout(
            "w", "w",
            listOf(Segment("Run", SegmentKind.RUN, Limit.Distance(5000), targetPaceSecPerKm = 360)),
        )
        val results = SessionResultsCalculator.compute(workout, constantTrack(6000))
        assertEquals(1, results.segments.size)
        val segment = results.segments.first()
        assertEquals("PASS", segment.status) // 3 m/s = 5:33/km, faster than 6:00
        assertEquals(5000.0, segment.distanceM, 1.0)
        assertEquals(1_666_666.0, segment.durationMs.toDouble(), 2.0)
        assertEquals("distance", segment.limitType)
    }

    @Test
    fun timeSegmentUsesTheTimeLimit() {
        val workout = Workout(
            "w", "w",
            listOf(Segment("Walk", SegmentKind.WALK, Limit.Duration(60), targetPaceSecPerKm = null)),
        )
        val segment = SessionResultsCalculator.compute(workout, constantTrack(6000)).segments.first()
        assertEquals("NO_TARGET", segment.status)
        assertEquals(180.0, segment.distanceM, 1.0) // 60 s * 3 m/s
        assertEquals(60_000L, segment.durationMs)
    }

    @Test
    fun kmSplitsAndBestEffortsAreProduced() {
        val workout = Workout("w", "w", listOf(Segment("Run", SegmentKind.RUN, Limit.Open, null)))
        val results = SessionResultsCalculator.compute(workout, constantTrack(6000))
        assertEquals(6, results.kmSplits.size)
        val k1 = results.bestEfforts.firstOrNull { it.key == BestEffortKey.K1 }
        assertEquals(333_333.0, k1!!.durationMs.toDouble(), 5.0)
    }

    @Test
    fun skippedSegmentIsStoredAsSkippedAndKeepsLaterAlignment() {
        val workout = Workout(
            "w", "w",
            listOf(
                Segment("Run", SegmentKind.RUN, Limit.Distance(1000), targetPaceSecPerKm = null),
                Segment("Run", SegmentKind.RUN, Limit.Distance(1000), targetPaceSecPerKm = null),
            ),
        )
        // The first segment is skipped after 300 m / 100 s; the curve still runs to 1500 m.
        val skipped = mapOf(1 to SkippedSegment(distanceM = 300.0, durationMs = 100_000L))
        val results = SessionResultsCalculator.compute(workout, constantTrack(1500), skipped)
        assertEquals(2, results.segments.size)
        val first = results.segments[0]
        assertEquals("SKIPPED", first.status)
        assertEquals(300.0, first.distanceM, 0.5)
        assertEquals(100_000L, first.durationMs)
        // The second segment starts where the skip happened, so it keeps the real curve (300 m -> 1300 m).
        val second = results.segments[1]
        assertEquals(1000.0, second.distanceM, 1.0)
    }
}
