package com.daniel.runcoach.engine.model

/** Rough estimate of a workout shown in the editor (plan F24). `...IsAtLeast` flags mean the real value
 *  is unknown (time-based or open segments, or a distance-bounded repeat) and should be shown with "≥". */
data class WorkoutEstimate(
    val distanceM: Double?,
    val distanceIsAtLeast: Boolean,
    val durationSec: Double?,
    val durationIsAtLeast: Boolean,
)

/** Editor estimate per plan F24: sum segment limits; distance/duration only when fully known. */
object WorkoutEstimator {

    fun estimate(workout: Workout): WorkoutEstimate {
        var distanceM = 0.0
        var distanceLower = false
        var durationSec = 0.0
        var durationLower = false

        fun addSegment(segment: Segment) {
            when (val limit = segment.limit) {
                is Limit.Distance -> {
                    distanceM += limit.meters
                    val target = segment.targetPaceSecPerKm
                    if (target != null) {
                        durationSec += limit.meters / 1000.0 * target
                    } else {
                        durationLower = true
                    }
                }

                is Limit.Duration -> {
                    distanceLower = true
                    durationSec += limit.seconds
                }

                Limit.Open -> {
                    distanceLower = true
                    durationLower = true
                }
            }
        }

        for (item in workout.items) {
            when (item) {
                is Segment -> addSegment(item)
                is Repeat -> {
                    val times = item.times
                    if (times != null) {
                        repeat(times) { item.children.forEach(::addSegment) }
                    } else {
                        // Distance-bounded: the goal is the distance; duration is a lower bound.
                        distanceM += item.untilTotalDistanceM ?: 0
                        distanceLower = true
                        durationLower = true
                    }
                }
            }
        }

        return WorkoutEstimate(
            distanceM = distanceM.takeIf { it > 0.0 },
            distanceIsAtLeast = distanceLower,
            durationSec = durationSec.takeIf { it > 0.0 },
            durationIsAtLeast = durationLower,
        )
    }
}
