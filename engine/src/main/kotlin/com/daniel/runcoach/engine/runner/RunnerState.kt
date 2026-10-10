package com.daniel.runcoach.engine.runner

import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind

/** Phase of the run state machine (plan 6.5). */
enum class RunnerPhase { IDLE, WAITING_GPS, COUNTDOWN, RUNNING, PAUSED_MANUAL, PAUSED_AUTO, STOPPED }

/** Immutable snapshot of the runner, exposed as a `StateFlow` (plan 6.5). All times are milliseconds. */
data class RunnerState(
    val phase: RunnerPhase = RunnerPhase.IDLE,
    val stepIndex: Int = 0,
    val stepCount: Int? = null,
    val segment: Segment? = null,
    val nextSegment: Segment? = null,
    val segmentDistanceM: Double = 0.0,
    val segmentMovingMs: Long = 0L,
    val segmentRemainingM: Double = 0.0,
    val segmentRemainingMs: Long = 0L,
    val totalDistanceM: Double = 0.0,
    val movingMs: Long = 0L,
    val elapsedMs: Long = 0L,
    val pausedMs: Long = 0L,
    val paceLiveSecPerKm: Double? = null,
    val countdownSec: Int? = null,
    val segmentCountdownSec: Int? = null,
    val distanceCueActive: Boolean = false,
    val distanceCueNextKind: SegmentKind? = null,
    val planCompleted: Boolean = false,
    val goalReached: Boolean = false,
    val gpsLost: Boolean = false,
    val droppedSampleCount: Int = 0,
)
