package com.daniel.runcoach.engine.result

/**
 * Per-distance split times for one session: the time to cover D **from the start** (R3 semantics). Used by the
 * session detail to show "time and average pace per distance" for the milestones a session actually reached.
 */
object MilestoneTimes {

    /** Milestones shown in the session detail (metres). */
    val DISTANCES_M: List<Double> = listOf(1000.0, 5000.0, 10000.0, 15000.0, 21097.5, 30000.0, 42195.0)

    data class Milestone(val meters: Double, val durationMs: Long, val paceSecPerKm: Double)

    fun compute(points: List<DistTimePoint>, distances: List<Double> = DISTANCES_M): List<Milestone> {
        if (points.size < 2) return emptyList()
        val total = points.last().cumulativeDistanceM
        val startMs = points.first().elapsedMs
        return distances.filter { it <= total + 1e-6 }.map { d ->
            val durationMs = BestEffortCalculator.timeAtDistance(points, d) - startMs
            Milestone(d, durationMs, durationMs / 1000.0 / (d / 1000.0))
        }
    }
}
