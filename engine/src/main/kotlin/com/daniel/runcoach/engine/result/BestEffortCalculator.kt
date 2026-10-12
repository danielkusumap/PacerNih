package com.daniel.runcoach.engine.result

import com.daniel.runcoach.engine.EngineConfig

/** The distances a personal record is tracked for (plan 6.10). */
enum class BestEffortKey(val meters: Double) {
    K1(1000.0),
    K5(5000.0),
    K10(10000.0),
    HM(21097.5),
    FM(42195.0),
}

/**
 * One point on the run's cumulative-distance/moving-time curve. [elapsedMs] must be *moving* time
 * (pauses excluded); the calculator never sees wall-clock time.
 */
data class DistTimePoint(val cumulativeDistanceM: Double, val elapsedMs: Long)

/** Time taken to cover [BestEffortKey.meters] from the start of the session ([startOffsetM] is always 0, R3). */
data class BestEffort(val key: BestEffortKey, val durationMs: Long, val startOffsetM: Double)

/**
 * R3: a record is the time to cover distance D **from the start** of the session (the first D km), not the
 * fastest window anywhere. O(n) interpolation of the time at cumulative distance D. A duration faster than
 * [EngineConfig.recordMinValidPaceSecPerKm] is a GPS anomaly and yields no record for D.
 */
object BestEffortCalculator {

    fun compute(points: List<DistTimePoint>, config: EngineConfig = EngineConfig()): List<BestEffort> {
        if (points.size < 2) return emptyList()
        return BestEffortKey.entries.mapNotNull { bestFor(it, points, config) }
    }

    private fun bestFor(key: BestEffortKey, points: List<DistTimePoint>, config: EngineConfig): BestEffort? {
        val distanceM = key.meters
        if (points.last().cumulativeDistanceM < distanceM) return null

        val startMs = points.first().elapsedMs
        val durationMs = timeAtDistance(points, distanceM) - startMs
        val minValidMs = (distanceM / 1000.0 * config.recordMinValidPaceSecPerKm * 1000.0).toLong()
        if (durationMs < minValidMs) return null
        return BestEffort(key, durationMs, 0.0)
    }

    private fun timeAtDistance(points: List<DistTimePoint>, distanceM: Double): Long {
        if (distanceM <= points.first().cumulativeDistanceM) return points.first().elapsedMs
        if (distanceM >= points.last().cumulativeDistanceM) return points.last().elapsedMs
        for (i in 1 until points.size) {
            val after = points[i]
            if (after.cumulativeDistanceM >= distanceM) {
                val before = points[i - 1]
                val span = after.cumulativeDistanceM - before.cumulativeDistanceM
                if (span <= 0.0) return after.elapsedMs
                val fraction = (distanceM - before.cumulativeDistanceM) / span
                return before.elapsedMs + (fraction * (after.elapsedMs - before.elapsedMs)).toLong()
            }
        }
        return points.last().elapsedMs
    }
}
