package com.daniel.runcoach.engine.result

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.roundHalfUp
import kotlin.math.floor

/** A segment's distance span and target, used to grade km splits against the segment they fall in. */
data class SegmentRange(val startM: Double, val endM: Double, val targetSecPerKm: Int?)

/** One row of the historical km split table (plan F18). */
data class KmSplit(
    val index: Int,
    val isPartial: Boolean,
    val distanceM: Double,
    val durationMs: Long,
    val paceSecPerKm: Double?,
    val targetSecPerKm: Int?,
    val deltaSec: Long?,
    val status: SegmentStatus?,
)

data class KmSplits(val splits: List<KmSplit>, val fastestIndex: Int?)

/**
 * Historical km splits computed from the whole-workout distance curve (plan 6.9, F18), not per segment.
 * Each full kilometre is paced from interpolated boundary times; a trailing partial kilometre is shown
 * when at least 10 m remain. A split is graded only when its window lies entirely inside one target segment.
 */
object KmSplitCalculator {

    private const val PARTIAL_MIN_M = 10.0
    private const val PARTIAL_PACE_MIN_M = 50.0

    fun compute(
        points: List<DistTimePoint>,
        ranges: List<SegmentRange> = emptyList(),
        config: EngineConfig = EngineConfig(),
    ): KmSplits {
        if (points.size < 2) return KmSplits(emptyList(), null)
        val totalM = points.last().cumulativeDistanceM
        if (totalM <= 0.0) return KmSplits(emptyList(), null)

        val splits = mutableListOf<KmSplit>()
        val fullKm = floor(totalM / 1000.0).toInt()
        var previousTimeMs = timeAt(points, 0.0)
        for (km in 1..fullKm) {
            val timeMs = timeAt(points, km * 1000.0)
            val durationMs = timeMs - previousTimeMs
            val paceSecPerKm = durationMs / 1000.0
            val graded = grade(km, paceSecPerKm, ranges, config)
            splits += KmSplit(
                index = km,
                isPartial = false,
                distanceM = 1000.0,
                durationMs = durationMs,
                paceSecPerKm = paceSecPerKm,
                targetSecPerKm = graded?.first,
                deltaSec = graded?.second,
                status = graded?.third,
            )
            previousTimeMs = timeMs
        }

        val partialM = totalM - fullKm * 1000.0
        if (partialM >= PARTIAL_MIN_M) {
            val durationMs = points.last().elapsedMs - previousTimeMs
            val paceSecPerKm = if (partialM < PARTIAL_PACE_MIN_M) null else durationMs / 1000.0 / (partialM / 1000.0)
            splits += KmSplit(
                index = fullKm + 1,
                isPartial = true,
                distanceM = partialM,
                durationMs = durationMs,
                paceSecPerKm = paceSecPerKm,
                targetSecPerKm = null,
                deltaSec = null,
                status = null,
            )
        }

        val fastestIndex = splits.filter { !it.isPartial && it.paceSecPerKm != null }
            .minByOrNull { it.paceSecPerKm!! }?.index
        return KmSplits(splits, fastestIndex)
    }

    private fun grade(
        km: Int,
        paceSecPerKm: Double,
        ranges: List<SegmentRange>,
        config: EngineConfig,
    ): Triple<Int, Long, SegmentStatus>? {
        val windowStartM = (km - 1) * 1000.0
        val windowEndM = km * 1000.0
        val range = ranges.firstOrNull { it.startM <= windowStartM && it.endM >= windowEndM } ?: return null
        val target = range.targetSecPerKm ?: return null
        val paceR = roundHalfUp(paceSecPerKm)
        val delta = paceR - target
        val marginKm = SegmentEvaluator.marginKmSec(target.toDouble(), config)
        val status = when {
            delta <= 0L -> SegmentStatus.PASS
            delta <= marginKm -> SegmentStatus.BORDERLINE
            else -> SegmentStatus.FAIL
        }
        return Triple(target, delta, status)
    }

    /** Interpolated time (ms) at distance [distanceM], linear between the two bounding points. */
    private fun timeAt(points: List<DistTimePoint>, distanceM: Double): Long {
        if (distanceM <= points.first().cumulativeDistanceM) return points.first().elapsedMs
        if (distanceM >= points.last().cumulativeDistanceM) return points.last().elapsedMs
        for (i in 1 until points.size) {
            val after = points[i]
            if (after.cumulativeDistanceM >= distanceM) {
                val before = points[i - 1]
                val spanM = after.cumulativeDistanceM - before.cumulativeDistanceM
                if (spanM <= 0.0) return after.elapsedMs
                val fraction = (distanceM - before.cumulativeDistanceM) / spanM
                return before.elapsedMs + (fraction * (after.elapsedMs - before.elapsedMs)).toLong()
            }
        }
        return points.last().elapsedMs
    }
}
