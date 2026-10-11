package com.daniel.runcoach.engine.result

import com.daniel.runcoach.engine.model.PaceTargetBound
import kotlin.test.Test
import kotlin.test.assertEquals

/** Target pace can be a ceiling (MAX, run) or a floor (MIN, warm-up/cool-down/recovery). */
class TargetBoundTest {

    private fun eval(target: Int, distanceM: Double, movingSec: Double, bound: PaceTargetBound) =
        SegmentEvaluator.evaluate(
            targetSecPerKm = target,
            perKmCheck = false,
            distanceM = distanceM,
            movingSec = movingSec,
            bound = bound,
        )

    @Test
    fun maxBoundPassesWhenFaster() {
        // 1000 m in 400 s (6:40) against a 7:00 ceiling -> pass.
        assertEquals(SegmentStatus.PASS, eval(420, 1000.0, 400.0, PaceTargetBound.MAX).status)
    }

    @Test
    fun maxBoundFailsWhenSlower() {
        // 1000 m in 460 s (7:40) against a 7:00 ceiling -> fail (beyond the margin).
        assertEquals(SegmentStatus.FAIL, eval(420, 1000.0, 460.0, PaceTargetBound.MAX).status)
    }

    @Test
    fun minBoundFailsWhenTooFast() {
        // 1000 m in 480 s (8:00) against an 8:25 floor -> too fast, fail.
        assertEquals(SegmentStatus.FAIL, eval(505, 1000.0, 480.0, PaceTargetBound.MIN).status)
    }

    @Test
    fun minBoundBorderlineJustInsideTheMargin() {
        // 8:23 against an 8:25 floor -> only 2 s fast, within the GPS margin -> borderline.
        assertEquals(SegmentStatus.BORDERLINE, eval(505, 1000.0, 503.0, PaceTargetBound.MIN).status)
    }

    @Test
    fun minBoundPassesWhenSlower() {
        // 8:40 against an 8:25 floor -> slower is fine -> pass.
        assertEquals(SegmentStatus.PASS, eval(505, 1000.0, 520.0, PaceTargetBound.MIN).status)
    }
}
