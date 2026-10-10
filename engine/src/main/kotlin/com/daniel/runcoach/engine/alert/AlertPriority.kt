package com.daniel.runcoach.engine.alert

import com.daniel.runcoach.engine.runner.AlertEvent

/**
 * Priority order for the single spoken-alert queue (plan 6.7). Lower rank wins. Countdown is highest and
 * preempts whatever is speaking; a lifecycle/segment event (e.g. SegmentEnd) beats a pace correction.
 */
object AlertPriority {

    fun rank(event: AlertEvent): Int = when (event) {
        is AlertEvent.Countdown -> 0
        is AlertEvent.WorkoutStarted,
        is AlertEvent.SegmentStart,
        is AlertEvent.SegmentEnd,
        is AlertEvent.GoalDistanceReached,
        is AlertEvent.WorkoutPlanCompleted,
        -> 1

        is AlertEvent.PreSegmentWarning, is AlertEvent.DistanceCue -> 2
        is AlertEvent.GpsLost, is AlertEvent.GpsRecovered,
        is AlertEvent.AutoPaused, is AlertEvent.AutoResumed,
        -> 3

        is AlertEvent.TooSlow, is AlertEvent.TooFast -> 4
        is AlertEvent.BackToStable -> 5
        is AlertEvent.KmMarker -> 6
    }

    /** The event that should speak first, or null when nothing is pending. */
    fun pick(events: List<AlertEvent>): AlertEvent? = events.minByOrNull { rank(it) }
}
