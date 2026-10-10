package com.daniel.runcoach.engine.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KmSplitCalculatorTest {

    @Test
    fun `three point one km yields three full km and a partial row`() {
        val points = listOf(
            DistTimePoint(0.0, 0L),
            DistTimePoint(1000.0, 400_000L),
            DistTimePoint(2000.0, 800_000L),
            DistTimePoint(3000.0, 1_203_000L),
            DistTimePoint(3100.0, 1_245_000L),
        )
        val result = KmSplitCalculator.compute(points)
        assertEquals(4, result.splits.size)
        assertEquals(400.0, result.splits[0].paceSecPerKm!!, 1e-9)
        assertEquals(400.0, result.splits[1].paceSecPerKm!!, 1e-9)
        assertEquals(403.0, result.splits[2].paceSecPerKm!!, 1e-9)
        val partial = result.splits[3]
        assertEquals(true, partial.isPartial)
        assertEquals(100.0, partial.distanceM, 1e-9)
        assertEquals(42_000L, partial.durationMs)
        assertEquals(420.0, partial.paceSecPerKm!!, 1e-9)
    }

    @Test
    fun `a split fully inside a target segment is graded`() {
        val points = listOf(
            DistTimePoint(0.0, 0L),
            DistTimePoint(1000.0, 360_000L),
            DistTimePoint(2000.0, 720_000L),
        )
        val ranges = listOf(SegmentRange(0.0, 2000.0, 360))
        val result = KmSplitCalculator.compute(points, ranges)
        assertEquals(SegmentStatus.PASS, result.splits[0].status)
        assertEquals(SegmentStatus.PASS, result.splits[1].status)
        assertEquals(360, result.splits[0].targetSecPerKm)
    }

    @Test
    fun `a split spanning two segments gets no status`() {
        val points = listOf(
            DistTimePoint(0.0, 0L),
            DistTimePoint(1000.0, 360_000L),
            DistTimePoint(2000.0, 720_000L),
        )
        val ranges = listOf(
            SegmentRange(0.0, 1500.0, 360),
            SegmentRange(1500.0, 2000.0, 300),
        )
        val result = KmSplitCalculator.compute(points, ranges)
        assertNull(result.splits[1].status)
    }

    @Test
    fun `fastest full kilometre is highlighted`() {
        val points = listOf(
            DistTimePoint(0.0, 0L),
            DistTimePoint(1000.0, 420_000L),
            DistTimePoint(2000.0, 800_000L),
        )
        assertEquals(2, KmSplitCalculator.compute(points).fastestIndex)
    }

    @Test
    fun `a tiny partial kilometre shows no pace`() {
        val points = listOf(
            DistTimePoint(0.0, 0L),
            DistTimePoint(1000.0, 360_000L),
            DistTimePoint(1020.0, 367_200L),
        )
        val result = KmSplitCalculator.compute(points)
        assertEquals(2, result.splits.size)
        assertNull(result.splits[1].paceSecPerKm)
    }
}
