package com.daniel.runcoach.engine.runner

import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind

/**
 * Events the engine raises while a workout runs (plan 6.7). The runner emits the lifecycle/segment
 * events; the AlertEngine (P0.7) adds the pace-correction events to this same type.
 */
sealed interface AlertEvent {
    data class Countdown(val seconds: Int) : AlertEvent
    data class DistanceCue(val nextKind: SegmentKind) : AlertEvent
    data object WorkoutStarted : AlertEvent
    data class SegmentStart(val index: Int, val total: Int?, val segment: Segment) : AlertEvent
    data class PreSegmentWarning(val nextKind: SegmentKind) : AlertEvent
    data class SegmentEnd(val index: Int, val segment: Segment) : AlertEvent
    data class KmMarker(val km: Int, val timeSec: Double, val paceSecPerKm: Double) : AlertEvent
    data object GoalDistanceReached : AlertEvent
    data object WorkoutPlanCompleted : AlertEvent
    data object GpsLost : AlertEvent
    data object GpsRecovered : AlertEvent
    data object AutoPaused : AlertEvent
    data object AutoResumed : AlertEvent
    data class TooSlow(val paceLiveSecPerKm: Double, val targetSecPerKm: Int, val requiredSecPerKm: Double?) : AlertEvent
    data class TooFast(val paceLiveSecPerKm: Double, val targetSecPerKm: Int) : AlertEvent
    data object BackToStable : AlertEvent
}
