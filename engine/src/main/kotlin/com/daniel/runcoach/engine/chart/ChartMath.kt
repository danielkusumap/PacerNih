package com.daniel.runcoach.engine.chart

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** A point on the cumulative distance/moving-time curve. A null [timeMs] marks a gap (pause or lost samples). */
data class DistTimePoint(val distanceM: Double, val timeMs: Long?)

/** One pace bucket. [paceSecPerKm] is null when the curve has no span for that bucket (a gap). */
data class PaceBucket(val startM: Double, val endM: Double, val paceSecPerKm: Double?)

/**
 * Pure data helpers for the charts (DESIGN.md 6.2). No Android, so they are unit-testable offline. The UI
 * layer only turns the results into Canvas drawings.
 */
object ChartMath {

    /**
     * Pace per fixed distance bucket (CH1). Walks the cumulative curve and interpolates the time at each bucket
     * edge; a bucket that the curve does not fully span becomes null so the line shows a gap.
     */
    fun bucketByDistance(points: List<DistTimePoint>, bucketM: Double = 100.0): List<PaceBucket> {
        if (points.size < 2 || bucketM <= 0.0) return emptyList()
        val lastD = points.last().distanceM
        val buckets = mutableListOf<PaceBucket>()
        var start = 0.0
        while (start < lastD) {
            val end = minOf(start + bucketM, lastD)
            val span = end - start
            val t0 = timeAt(points, start)
            val t1 = timeAt(points, end)
            val pace = if (t0 != null && t1 != null && t1 > t0 && span > 0.0) {
                ((t1 - t0) / 1000.0) / (span / 1000.0)
            } else {
                null
            }
            buckets += PaceBucket(start, end, pace)
            start = start + bucketM
        }
        return buckets
    }

    /** Time at a cumulative distance, linearly interpolated. Null when outside the curve or inside a gap. */
    fun timeAt(points: List<DistTimePoint>, distanceM: Double): Double? {
        if (points.isEmpty()) return null
        if (distanceM < points.first().distanceM || distanceM > points.last().distanceM) return null
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            if (distanceM in a.distanceM..b.distanceM) {
                val ta = a.timeMs ?: return null
                val tb = b.timeMs ?: return null
                val span = b.distanceM - a.distanceM
                if (span <= 0.0) return tb.toDouble()
                val f = (distanceM - a.distanceM) / span
                return ta + f * (tb - ta)
            }
        }
        return points.last().timeMs?.toDouble()
    }

    /** Reversed pace axis bounds: faster (smaller) pace is up. Minimum width 60 s/km (DESIGN 6.2). */
    fun paceAxisBounds(paceValues: List<Double>): Pair<Double, Double> {
        val values = paceValues.filter { it.isFinite() && it > 0.0 }
        if (values.isEmpty()) return 300.0 to 360.0
        val fastest = values.min()
        val slowest = values.max()
        var min = floor(fastest / 10.0) * 10.0 - 10.0
        var max = ceil(slowest / 10.0) * 10.0 + 10.0
        if (max - min < 60.0) {
            val mid = (min + max) / 2.0
            min = mid - 30.0
            max = mid + 30.0
        }
        return min to max
    }

    /** 3-4 pace ticks on the smallest step from {10, 15, 30, 60} seconds (DESIGN 6.2). */
    fun paceTicks(min: Double, max: Double): List<Double> {
        val steps = listOf(10.0, 15.0, 30.0, 60.0)
        val chosen = steps.firstOrNull { step ->
            val first = ceil(min / step) * step
            var count = 0
            var t = first
            while (t <= max + 1e-9) {
                count++
                t += step
            }
            count in 3..4
        } ?: 60.0
        val ticks = mutableListOf<Double>()
        var t = ceil(min / chosen) * chosen
        while (t <= max + 1e-9) {
            ticks += t
            t += chosen
        }
        return ticks
    }

    /** 0..1 position on a reversed pace axis: the fastest pace maps to 1 (top), the slowest to 0. */
    fun invertPaceFraction(pace: Double, minPace: Double, maxPace: Double): Double {
        val span = (maxPace - minPace).takeIf { abs(it) > 1e-9 } ?: return 0.5
        return ((maxPace - pace) / span).coerceIn(0.0, 1.0)
    }

    /** Keeps at most [maxPoints] by merging adjacent entries (CH1: at most 200 points). */
    fun downsample(points: List<DistTimePoint>, maxPoints: Int = 200): List<DistTimePoint> {
        if (maxPoints < 2 || points.size <= maxPoints) return points
        val result = ArrayList<DistTimePoint>(maxPoints)
        val factor = ceil(points.size.toDouble() / maxPoints).toInt()
        var i = 0
        while (i < points.size) {
            val chunk = points.subList(i, minOf(i + factor, points.size))
            val d = chunk.map { it.distanceM }.average()
            val times = chunk.map { it.timeMs }
            val t = if (times.all { it != null }) times.filterNotNull().average().toLong() else null
            result += DistTimePoint(d, t)
            i += factor
        }
        if (result.last() != points.last()) result[result.size - 1] = points.last()
        return result
    }
}
