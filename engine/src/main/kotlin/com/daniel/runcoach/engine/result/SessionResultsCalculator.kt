package com.daniel.runcoach.engine.result

import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.model.WorkoutExpander
import kotlin.math.floor
import kotlin.math.min

/** One finished segment's stored result (plan section 7 SegmentResult). */
data class SegmentResultData(
    val idx: Int,
    val label: String,
    val kind: String,
    val limitType: String,
    val limitValue: Double,
    val targetPace: Int?,
    val distanceM: Double,
    val durationMs: Long,
    val avgPace: Double?,
    val deltaSec: Long?,
    val status: String,
    val targetBound: com.daniel.runcoach.engine.model.PaceTargetBound = com.daniel.runcoach.engine.model.PaceTargetBound.MAX,
    /** P1: true when the session was stopped during or before this segment. */
    val stopped: Boolean = false,
)

data class SessionResultsData(
    val segments: List<SegmentResultData>,
    val kmSplits: List<KmSplit>,
    val bestEfforts: List<BestEffort>,
)

/** A segment the runner skipped: how much distance/time had accrued before it was skipped (moving clock). */
data class SkippedSegment(val distanceM: Double, val durationMs: Long)

/**
 * Turns a finished session's distance/time curve into stored results (plan 6.8, 6.9, 6.10). Pure. Segment
 * boundaries are found by interpolating the cumulative curve; time-based segments use the time limit.
 *
 * [skipped] maps a 1-based segment index to the amount accrued before the user skipped it, so skipped segments
 * are stored as DILEWATI and later segments keep the right curve alignment.
 */
object SessionResultsCalculator {

    fun compute(
        workout: Workout,
        points: List<DistTimePoint>,
        skipped: Map<Int, SkippedSegment> = emptyMap(),
        stoppedFrom: Int? = null,
    ): SessionResultsData {
        if (points.size < 2) return SessionResultsData(emptyList(), emptyList(), emptyList())
        val steps = runCatching { WorkoutExpander.expandFinite(workout.items) }
            .getOrElse { WorkoutExpander.lazySteps(workout.items).take(500).toList() }
        val totalDistanceM = points.last().cumulativeDistanceM
        val lastTimeMs = points.last().elapsedMs

        val segments = mutableListOf<SegmentResultData>()
        val ranges = mutableListOf<SegmentRange>()
        var startDist = 0.0
        var startTime = points.first().elapsedMs

        steps.forEachIndexed { index, segment ->
            val limit = segment.limit
            var endDist = startDist
            var endTime = startTime
            var distanceM = 0.0
            var durationMs = 0L
            val kmTimes = mutableListOf<Double>()

            val skip = skipped[index + 1]
            if (skip != null) {
                distanceM = skip.distanceM
                durationMs = skip.durationMs
                endDist = startDist + skip.distanceM
                endTime = startTime + skip.durationMs
                segments += SegmentResultData(
                    idx = index + 1,
                    label = segment.label,
                    kind = segment.kind.name,
                    limitType = limitTypeOf(limit),
                    limitValue = limitValueOf(limit),
                    targetPace = segment.targetPaceSecPerKm,
                    distanceM = distanceM,
                    durationMs = durationMs,
                    avgPace = null,
                    deltaSec = null,
                    status = SegmentStatus.SKIPPED.name,
                    targetBound = segment.targetBound,
                    stopped = false,
                )
                startDist = endDist
                startTime = endTime
                return@forEachIndexed
            }

            when (limit) {
                is Limit.Distance -> {
                    val rangeEnd = startDist + limit.meters
                    endDist = min(rangeEnd, totalDistanceM)
                    endTime = timeAtDistance(points, endDist)
                    distanceM = endDist - startDist
                    durationMs = endTime - startTime
                    var boundary = startDist
                    while (boundary + 1000.0 <= endDist + 1e-9) {
                        val nextBoundary = startDist + floor((boundary - startDist) / 1000.0 + 1.0) * 1000.0
                        val tPrev = timeAtDistance(points, boundary)
                        val tNext = timeAtDistance(points, nextBoundary)
                        kmTimes += (tNext - tPrev) / 1000.0
                        boundary = nextBoundary
                    }
                    if (segment.targetPaceSecPerKm != null) {
                        ranges += SegmentRange(startDist, startDist + limit.meters, segment.targetPaceSecPerKm)
                    }
                }

                is Limit.Duration -> {
                    endTime = startTime + limit.seconds * 1000L
                    endDist = distanceAtTime(points, min(endTime, lastTimeMs))
                    distanceM = endDist - startDist
                    durationMs = endTime - startTime
                }

                Limit.Open -> {
                    endDist = totalDistanceM
                    endTime = lastTimeMs
                    distanceM = totalDistanceM - startDist
                    durationMs = lastTimeMs - startTime
                }
            }

            val evaluation = SegmentEvaluator.evaluate(
                targetSecPerKm = segment.targetPaceSecPerKm,
                perKmCheck = segment.perKmCheck,
                distanceM = distanceM,
                movingSec = durationMs / 1000.0,
                kmTimesSec = kmTimes,
                skipped = false,
                bound = segment.targetBound,
            )
            // P1: on Stop, the running segment and every planned segment after it are marked stopped. Target -> FAIL,
            // no target -> NO_TARGET (never failed); Open segments are left as NO_TARGET.
            val stoppedHere = stoppedFrom != null && (index + 1) >= stoppedFrom
            val notStarted = stoppedFrom != null && (index + 1) > stoppedFrom
            if (notStarted) {
                distanceM = 0.0
                durationMs = 0L
                endDist = startDist
                endTime = startTime
            }
            val hasTarget = segment.targetPaceSecPerKm != null && limit != Limit.Open
            val status = when {
                stoppedHere && hasTarget -> SegmentStatus.FAIL.name
                stoppedHere && !hasTarget -> SegmentStatus.NO_TARGET.name
                else -> evaluation.status.name
            }
            segments += SegmentResultData(
                idx = index + 1,
                label = segment.label,
                kind = segment.kind.name,
                limitType = limitTypeOf(limit),
                limitValue = limitValueOf(limit),
                targetPace = segment.targetPaceSecPerKm,
                distanceM = distanceM,
                durationMs = durationMs,
                avgPace = if (notStarted) null else evaluation.avgPaceSecPerKm,
                deltaSec = if (notStarted) null else evaluation.deltaSec,
                status = status,
                targetBound = segment.targetBound,
                stopped = stoppedHere,
            )

            startDist = endDist
            startTime = endTime
        }

        return SessionResultsData(
            segments = segments,
            kmSplits = KmSplitCalculator.compute(points, ranges).splits,
            bestEfforts = BestEffortCalculator.compute(points),
        )
    }

    private fun limitTypeOf(limit: Limit): String = when (limit) {
        is Limit.Distance -> "distance"
        is Limit.Duration -> "duration"
        Limit.Open -> "open"
    }

    private fun limitValueOf(limit: Limit): Double = when (limit) {
        is Limit.Distance -> limit.meters.toDouble()
        is Limit.Duration -> limit.seconds.toDouble()
        Limit.Open -> 0.0
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

    private fun distanceAtTime(points: List<DistTimePoint>, timeMs: Long): Double {
        if (timeMs <= points.first().elapsedMs) return points.first().cumulativeDistanceM
        if (timeMs >= points.last().elapsedMs) return points.last().cumulativeDistanceM
        for (i in 1 until points.size) {
            val after = points[i]
            if (after.elapsedMs >= timeMs) {
                val before = points[i - 1]
                val span = (after.elapsedMs - before.elapsedMs).toDouble()
                if (span <= 0.0) return after.cumulativeDistanceM
                val fraction = (timeMs - before.elapsedMs) / span
                return before.cumulativeDistanceM + fraction * (after.cumulativeDistanceM - before.cumulativeDistanceM)
            }
        }
        return points.last().cumulativeDistanceM
    }
}
