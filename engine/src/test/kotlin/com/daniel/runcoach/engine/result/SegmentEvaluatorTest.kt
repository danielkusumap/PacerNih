package com.daniel.runcoach.engine.result

import kotlin.test.Test
import kotlin.test.assertEquals

class SegmentEvaluatorTest {

    @Test
    fun `average pace rounded decides pass borderline and fail`() {
        assertEquals(SegmentStatus.PASS, evaluate(avg = 420.4).status)
        assertEquals(SegmentStatus.BORDERLINE, evaluate(avg = 420.5).status)
        assertEquals(SegmentStatus.FAIL, evaluate(avg = 423.0).status)
    }

    @Test
    fun `per-km check fails the segment when one kilometre is too slow`() {
        val pass = SegmentEvaluator.evaluate(
            targetSecPerKm = 420, perKmCheck = true, distanceM = 5000.0, movingSec = 2100.0,
            kmTimesSec = listOf(420.0, 420.0, 420.0, 420.0, 420.0),
        )
        assertEquals(SegmentStatus.PASS, pass.status)

        val fail = SegmentEvaluator.evaluate(
            targetSecPerKm = 420, perKmCheck = true, distanceM = 5000.0, movingSec = 2100.0,
            kmTimesSec = listOf(420.0, 420.0, 425.0, 420.0, 420.0),
        )
        assertEquals(SegmentStatus.FAIL, fail.status)
        assertEquals(SegmentStatus.FAIL, fail.kmStatuses[2].status)
    }

    @Test
    fun `a kilometre inside the margin makes the segment borderline`() {
        val result = SegmentEvaluator.evaluate(
            targetSecPerKm = 420, perKmCheck = true, distanceM = 5000.0, movingSec = 2100.0,
            kmTimesSec = listOf(420.0, 421.0, 420.0, 420.0, 420.0),
        )
        assertEquals(SegmentStatus.BORDERLINE, result.status)
        assertEquals(SegmentStatus.BORDERLINE, result.kmStatuses[1].status)
    }

    @Test
    fun `a short segment is DATA KURANG`() {
        assertEquals(SegmentStatus.NO_DATA, evaluate(avg = 420.0, distanceM = 40.0).status)
    }

    @Test
    fun `no target and skipped are reported plainly`() {
        val noTarget = SegmentEvaluator.evaluate(null, false, 5000.0, 2100.0)
        assertEquals(SegmentStatus.NO_TARGET, noTarget.status)
        // B3: actual pace is reported for every segment with enough distance, even without a target.
        assertEquals(420.0, noTarget.avgPaceSecPerKm!!, 0.001)
        val shortNoTarget = SegmentEvaluator.evaluate(null, false, 40.0, 16.8)
        assertEquals(null, shortNoTarget.avgPaceSecPerKm)
        val skipped = SegmentEvaluator.evaluate(420, false, 5000.0, 2100.0, skipped = true)
        assertEquals(SegmentStatus.SKIPPED, skipped.status)
    }

    @Test
    fun `delta uses the rounded pace`() {
        val result = evaluate(avg = 420.5)
        assertEquals(421L, result.avgPaceRoundedSec)
        assertEquals(1L, result.deltaSec)
    }

    @Test
    fun `R2 range passes inside the band and grades outside it`() {
        val range = com.daniel.runcoach.engine.model.PaceTargetBound.RANGE
        fun ev(avg: Double) = SegmentEvaluator.evaluate(390, perKmCheck = false, distanceM = 1000.0, movingSec = avg, bound = range, slowSecPerKm = 400)
        assertEquals(SegmentStatus.PASS, ev(395.0).status)
        assertEquals(SegmentStatus.PASS, ev(390.0).status)
        assertEquals(SegmentStatus.PASS, ev(400.0).status)
        // margin = min(390 * 6 / 1000, 15) = 2.34 s/km
        assertEquals(SegmentStatus.BORDERLINE, ev(388.0).status)
        assertEquals(SegmentStatus.FAIL, ev(385.0).status)
        assertEquals(SegmentStatus.BORDERLINE, ev(402.0).status)
        assertEquals(SegmentStatus.FAIL, ev(410.0).status)
    }

    private fun evaluate(
        avg: Double,
        distanceM: Double = 1000.0,
        target: Int = 420,
    ): SegmentEvaluation {
        val movingSec = avg * (distanceM / 1000.0)
        return SegmentEvaluator.evaluate(target, perKmCheck = false, distanceM = distanceM, movingSec = movingSec)
    }
}
