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

/** Fastest window covering [BestEffortKey.meters], starting at [startOffsetM] from the origin. */
data class BestEffort(val key: BestEffortKey, val durationMs: Long, val startOffsetM: Double)

/**
 * Best effort = the fastest time to cover distance D *anywhere* in the session, not the aligned
 * km splits (plan 6.10). Sliding window over the distance curve with linear interpolation for the
 * window start, O(n) two-pointer. Windows faster than [EngineConfig.recordMinValidPaceSecPerKm]
 * are treated as GPS anomalies and skipped.
 */
object BestEffortCalculator {

    fun compute(points: List<DistTimePoint>, config: EngineConfig = EngineConfig()): List<BestEffort> {
        if (points.size < 2) return emptyList()
        return BestEffortKey.entries.mapNotNull { bestFor(it, points, config) }
    }

    private fun bestFor(
        key: BestEffortKey,
        points: List<DistTimePoint>,
        config: EngineConfig,
    ): BestEffort? {
        val distanceM = key.meters
        if (points.last().cumulativeDistanceM < distanceM) return null

        // A pace faster than the minimum corresponds to a shorter duration than this.
        val minValidMs = (distanceM / 1000.0 * config.recordMinValidPaceSecPerKm * 1000.0).toLong()

        var best: BestEffort? = null
        var startIndex = 0
        for (endIndex in points.indices) {
            val endDistanceM = points[endIndex].cumulativeDistanceM
            if (endDistanceM < distanceM) continue

            val targetStartM = endDistanceM - distanceM
            while (
                startIndex + 1 < endIndex &&
                points[startIndex + 1].cumulativeDistanceM <= targetStartM
            ) {
                startIndex++
            }
            val start = points[startIndex]
            val next = points[startIndex + 1]
            val spanM = next.cumulativeDistanceM - start.cumulativeDistanceM
            val startTimeMs = if (spanM > 0.0) {
                start.elapsedMs +
                    ((targetStartM - start.cumulativeDistanceM) / spanM * (next.elapsedMs - start.elapsedMs)).toLong()
            } else {
                start.elapsedMs
            }

            val durationMs = points[endIndex].elapsedMs - startTimeMs
            if (durationMs < minValidMs) continue
            if (best == null || durationMs < best.durationMs) {
                best = BestEffort(key, durationMs, targetStartM)
            }
        }
        return best
    }
}
