package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.metrics.LivePaceEstimator
import com.daniel.runcoach.engine.metrics.PaceCalculator
import com.daniel.runcoach.engine.result.BestEffortCalculator
import com.daniel.runcoach.engine.result.BestEffortKey
import com.daniel.runcoach.engine.result.DistTimePoint
import com.daniel.runcoach.engine.result.KmSplitCalculator
import com.daniel.runcoach.engine.result.SegmentEvaluator
import com.daniel.runcoach.engine.result.SegmentStatus
import com.daniel.runcoach.engine.roundHalfUp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Formula vectors from RUNCOACH_PLAN 6.14.2, each as one test named after its ID.
 * Grown per task; P0.11 turns this into the full V1–V22 audit.
 */
class FormulaVectorsTest {

    private fun moving(stepM: Double) = FilteredSample(GpsOutcome.MOVING, stepM)

    /** V2: Haversine for 0.0001 degrees of latitude. */
    @Test
    fun v02_haversineTenthMilliDegree() {
        assertEquals(11.12, haversineM(0.0, 0.0, 0.0001, 0.0), 0.01)
    }

    /** V3: boundary interpolation time. */
    @Test
    fun v03_boundaryInterpolation() {
        val tracker = DistanceTracker()
        tracker.addBoundary(1000.0)
        tracker.add(0L, moving(0.0))
        tracker.add(300_000L, moving(995.0))
        val crossings = tracker.add(302_000L, moving(8.0))
        assertEquals(1, crossings.size)
        assertEquals(1000.0, crossings[0].boundaryM)
        assertEquals(301_250L, crossings[0].timeMs)
    }

    /** V4: two boundaries in one sample, and the leftover distance carried forward. */
    @Test
    fun v04_multipleBoundariesAndLeftover() {
        val tracker = DistanceTracker()
        tracker.addBoundary(1010.0)
        tracker.addBoundary(1020.0)
        tracker.add(0L, moving(0.0))
        tracker.add(100_000L, moving(1008.0))
        val crossings = tracker.add(104_000L, moving(16.0))
        assertEquals(listOf(1010.0, 1020.0), crossings.map { it.boundaryM })
        assertEquals(100_500L, crossings[0].timeMs)
        assertEquals(103_000L, crossings[1].timeMs)
        // D1 - last boundary = 1024 - 1020 = 4 m carried into the next segment.
        assertEquals(4.0, tracker.totalDistanceM - 1020.0, 1e-9)
    }

    /** V5: Doppler live pace, time-weighted over five 1 Hz samples. */
    @Test
    fun v05_livePaceDopplerWeighted() {
        val estimator = LivePaceEstimator()
        val speeds = listOf(3.2, 3.1, 3.0, 2.9, 3.0)
        var distanceM = 0.0
        speeds.forEachIndexed { index, speed ->
            estimator.add(index * 1000L, distanceM, speedMps = speed)
            distanceM += speed
        }
        assertEquals(3.04, estimator.liveSpeedMps()!!, 0.001)
        assertEquals(328.95, estimator.livePaceSecPerKm()!!, 0.05)
        assertEquals(329L, roundHalfUp(estimator.livePaceSecPerKm()!!))
    }

    /** V6: projected pace, not too slow at T=420. */
    @Test
    fun v06_projectedPace() {
        assertEquals(416.0, PaceCalculator.projectedPaceSecPerKm(410.0, 170.0, 1000.0, 600.0)!!, 1e-9)
    }

    /** V7: required pace over a 5 km scope. */
    @Test
    fun v07_requiredPace() {
        assertEquals(323.333, PaceCalculator.requiredPaceSecPerKm(330.0, 680.0, 5000.0, 3000.0)!!, 0.01)
    }

    /** V8: required pace when the target is already unreachable -> not realistic. */
    @Test
    fun v08_requiredPaceUnrealistic() {
        assertNull(PaceCalculator.requiredPaceSecPerKm(330.0, 1700.0, 5000.0, 3000.0))
    }

    /** V9: rounded average pace decides the segment status. */
    @Test
    fun v09_segmentRoundedPaceStatus() {
        fun status(avg: Double) =
            SegmentEvaluator.evaluate(420, perKmCheck = false, distanceM = 1000.0, movingSec = avg).status
        assertEquals(SegmentStatus.PASS, status(420.4))
        assertEquals(SegmentStatus.BORDERLINE, status(420.5))
        assertEquals(SegmentStatus.FAIL, status(423.0))
    }

    /** V10: a 200 m segment's BORDERLINE margin. */
    @Test
    fun v10_shortSegmentMargin() {
        fun status(avg: Double) =
            SegmentEvaluator.evaluate(390, perKmCheck = false, distanceM = 200.0, movingSec = avg * 0.2).status
        assertEquals(SegmentStatus.BORDERLINE, status(398.0))
        assertEquals(SegmentStatus.FAIL, status(405.0))
    }

    /** V11: the BORDERLINE margin is capped at 15 s/km. */
    @Test
    fun v11_marginCapped() {
        assertEquals(15.0, SegmentEvaluator.marginSec(420.0, 40.0), 1e-9)
    }

    /** V12: 3.1 km km-split rows, including the trailing partial kilometre. */
    @Test
    fun v12_kmSplits() {
        val points = listOf(
            DistTimePoint(0.0, 0L),
            DistTimePoint(1000.0, 400_000L),
            DistTimePoint(2000.0, 800_000L),
            DistTimePoint(3000.0, 1_203_000L),
            DistTimePoint(3100.0, 1_245_000L),
        )
        val splits = KmSplitCalculator.compute(points).splits
        assertEquals(4, splits.size)
        assertEquals(400.0, splits[0].paceSecPerKm!!, 1e-9)
        assertEquals(400.0, splits[1].paceSecPerKm!!, 1e-9)
        assertEquals(403.0, splits[2].paceSecPerKm!!, 1e-9)
        assertEquals(100.0, splits[3].distanceM, 0.001)
        assertEquals(42_000L, splits[3].durationMs)
        assertEquals(420.0, splits[3].paceSecPerKm!!, 1e-9)
    }

    /** V13: best effort on a constant 3 m/s track. */
    @Test
    fun v13_bestEffortConstantPace() {
        val points = (0..2000 step 100).map { DistTimePoint(it.toDouble(), (it / 3.0 * 1000.0).toLong()) }
        val k1 = BestEffortCalculator.compute(points).first { it.key == BestEffortKey.K1 }
        assertEquals(333333.0, k1.durationMs.toDouble(), 5.0)
        assertEquals(0.0, k1.startOffsetM, 0.5)
    }

    /** V14: the fastest 1 km is found in the middle; the 5 km stays level. */
    @Test
    fun v14_bestEffortFastSegmentInMiddle() {
        val points = mutableListOf(DistTimePoint(0.0, 0L))
        var timeMs = 0.0
        var distanceM = 0.0
        while (distanceM < 10_000.0) {
            val previous = distanceM
            distanceM = minOf(distanceM + 100.0, 10_000.0)
            val paceSecPerKm = if (previous >= 4000.0 && previous < 5000.0) 240.0 else 360.0
            timeMs += (distanceM - previous) * paceSecPerKm
            points += DistTimePoint(distanceM, timeMs.toLong())
        }
        val best = BestEffortCalculator.compute(points)
        val k1 = best.first { it.key == BestEffortKey.K1 }
        // R3: records come from the start, so the first 1 km is the slow one (360 s), not the fast middle km.
        assertEquals(360000.0, k1.durationMs.toDouble(), 50.0)
        assertEquals(0.0, k1.startOffsetM, 1.0)
        val k5 = best.first { it.key == BestEffortKey.K5 }
        assertEquals(1680000.0, k5.durationMs.toDouble(), 200.0)
        assertEquals(0.0, k5.startOffsetM, 1.0)
    }

    /** V15: the only window is impossibly fast, so there is no 1 km record (per-window, D-008). */
    @Test
    fun v15_bestEffortAnomalyWindowDiscarded() {
        val points = listOf(DistTimePoint(0.0, 0L), DistTimePoint(1000.0, 140_000L))
        assertTrue(BestEffortCalculator.compute(points).isEmpty())
    }
}
