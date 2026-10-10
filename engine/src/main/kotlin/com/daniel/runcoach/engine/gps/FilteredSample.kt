package com.daniel.runcoach.engine.gps

/** What the sample filter decided about a raw GPS fix (plan F3). */
enum class GpsOutcome {
    /** Reference advanced; [FilteredSample.stepM] metres were earned (0 for the first sample). */
    MOVING,

    /** Valid for GPS readiness and live pace, but no distance and the reference does not move (F3.4). */
    STATIONARY,

    /** Reference was reset to this sample after repeated speed rejections; no distance (F3.7). */
    RESET,

    /** Dropped (accuracy, non-advancing timestamp, or impossible speed). */
    REJECTED,
}

/** Filter output: the outcome plus the raw, uncalibrated distance earned since the reference. */
data class FilteredSample(val outcome: GpsOutcome, val stepM: Double)
