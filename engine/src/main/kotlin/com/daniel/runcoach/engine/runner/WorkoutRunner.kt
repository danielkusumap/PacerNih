package com.daniel.runcoach.engine.runner

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.gps.DistanceTracker
import com.daniel.runcoach.engine.gps.GpsOutcome
import com.daniel.runcoach.engine.gps.SampleFilter
import com.daniel.runcoach.engine.metrics.LivePaceEstimator
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.LocationSample
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.model.WorkoutExpander
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Runs a workout as a state machine (plan 6.5, 6.6, F5, F6, F15, F27).
 *
 * All time comes from the caller as a monotonic millisecond clock (`onTick`) and sample timestamps;
 * the engine never reads the system clock. Distance-boundary interpolation uses the moving clock
 * (F5) supplied to [DistanceTracker]. The runner never stops by itself: after the last planned step it
 * announces [AlertEvent.WorkoutPlanCompleted] and keeps recording an open segment until [onStop].
 *
 * Not thread safe; drive it from one coroutine.
 */
class WorkoutRunner(
    private val workout: Workout,
    private val config: EngineConfig = EngineConfig(),
    calibrationFactor: Double = 1.0,
) {

    private val _state = MutableStateFlow(RunnerState(stepCount = WorkoutExpander.stepCountOrNull(workout.items)))
    val state: StateFlow<RunnerState> = _state

    private val _events = MutableSharedFlow<AlertEvent>(extraBufferCapacity = 256)
    val events: Flow<AlertEvent> = _events

    /** Every event raised so far, in order. Used by tests and by the in-app debug log (S15). */
    val emittedEvents: List<AlertEvent> get() = eventLog
    private val eventLog = mutableListOf<AlertEvent>()

    private val filter = SampleFilter(config)
    private val tracker = DistanceTracker(config, calibrationFactor)
    private val livePace = LivePaceEstimator(config)
    private val cursor = StepCursor(workout.items)

    private var phase = RunnerPhase.IDLE
    private var current: Segment? = null
    private var stepIndex = 0
    private var segmentStartTotalM = 0.0
    private var segmentStartMtMs = 0L
    private var extendedLimitM = 0.0
    private var extendedLimitMs = 0L

    private var tStartRunningMs: Long? = null
    private var pauseStartedMs: Long? = null
    private var pausedMsTotal = 0L

    private var countdownStartMs = 0L
    private var countdownEndMs = 0L
    private val countdownEmitted = mutableSetOf<Int>()
    private val segmentCountdownEmitted = mutableSetOf<Int>()
    private var preWarningEmitted = false
    private var distanceCueEmitted = false
    private var registeredSegmentBoundary: Double? = null

    private var kmReached = 0
    private var goalReached = false
    private var planCompleted = false
    private var gpsLost = false
    private var lastValidSampleMs: Long? = null

    // --- inputs -------------------------------------------------------------

    /** Starts the run: begins the initial countdown, or running directly when it is disabled. */
    fun onStart(nowMs: Long) {
        if (phase != RunnerPhase.IDLE && phase != RunnerPhase.WAITING_GPS) return
        val seconds = config.startCountdownSec
        if (seconds > 0) {
            phase = RunnerPhase.COUNTDOWN
            countdownStartMs = nowMs
            countdownEndMs = nowMs + seconds * 1000L
            countdownEmitted.clear()
            emit(AlertEvent.Countdown(seconds))
            countdownEmitted.add(seconds)
        } else {
            beginRunning(nowMs)
        }
        updateState(nowMs)
    }

    fun onCancelCountdown() {
        if (phase == RunnerPhase.COUNTDOWN) {
            phase = RunnerPhase.WAITING_GPS
            countdownEmitted.clear()
        }
    }

    fun onTick(nowMs: Long) {
        when (phase) {
            RunnerPhase.COUNTDOWN -> handleCountdownTick(nowMs)
            RunnerPhase.RUNNING -> handleRunningTick(nowMs)
            else -> {}
        }
    }

    fun onSample(sample: LocationSample) {
        if (phase != RunnerPhase.RUNNING) return
        val filtered = filter.accept(sample)
        if (filtered.outcome == GpsOutcome.REJECTED) return

        val sampleMs = sample.timeNanos / 1_000_000L
        lastValidSampleMs = sampleMs
        if (gpsLost) {
            gpsLost = false
            emit(AlertEvent.GpsRecovered)
        }

        val movingMs = movingMsAt(sampleMs)
        registerSegmentBoundary()
        val crossings = tracker.add(movingMs, filtered)
        livePace.add(movingMs, tracker.totalDistanceM, sample.speedMps, sample.speedAccuracyMps)

        emitKmMarkers(movingMs)
        checkGoal()

        for (crossing in crossings) {
            if (crossing.boundaryM == registeredSegmentBoundary) {
                finishSegment(crossing.boundaryM, crossing.timeMs)
            }
        }
        if (phase == RunnerPhase.RUNNING) maybeDistanceCue()
        updateState(sampleMs)
    }

    fun onPause(nowMs: Long) {
        if (phase != RunnerPhase.RUNNING) return
        pauseStartedMs = nowMs
        phase = RunnerPhase.PAUSED_MANUAL
        updateState(nowMs)
    }

    fun onResume(nowMs: Long) {
        if (phase != RunnerPhase.PAUSED_MANUAL && phase != RunnerPhase.PAUSED_AUTO) return
        pausedMsTotal += nowMs - (pauseStartedMs ?: nowMs)
        pauseStartedMs = null
        phase = RunnerPhase.RUNNING
        updateState(nowMs)
    }

    /** Skips the current segment: it counts nothing more, and the next one starts at zero progress. */
    fun onSkipSegment(nowMs: Long) {
        if (phase != RunnerPhase.RUNNING && phase != RunnerPhase.PAUSED_MANUAL) return
        val segment = current ?: return
        emit(AlertEvent.SegmentEnd(stepIndex, segment))
        advanceSegment(tracker.totalDistanceM, movingMsAt(nowMs))
        updateState(nowMs)
    }

    /** Extends the current segment by metres and/or seconds (plan D07 / F27). */
    fun onExtendSegment(deltaMeters: Double = 0.0, deltaSeconds: Int = 0) {
        extendedLimitM += deltaMeters
        extendedLimitMs += deltaSeconds * 1000L
        registeredSegmentBoundary = null
    }

    fun onStop(nowMs: Long) {
        phase = RunnerPhase.STOPPED
        updateState(nowMs)
    }

    // --- internals ----------------------------------------------------------

    private fun beginRunning(nowMs: Long) {
        tStartRunningMs = nowMs
        phase = RunnerPhase.RUNNING
        emit(AlertEvent.WorkoutStarted)
        advanceSegment(0.0, 0L)
    }

    private fun advanceSegment(nextStartTotalM: Double, nextStartMtMs: Long) {
        val next = cursor.next(tracker.totalDistanceM)
        segmentCountdownEmitted.clear()
        preWarningEmitted = false
        distanceCueEmitted = false
        registeredSegmentBoundary = null
        extendedLimitM = 0.0
        extendedLimitMs = 0L
        if (next == null) {
            planCompleted = true
            stepIndex = 0
            current = Segment("Lari bebas", SegmentKind.RUN, Limit.Open, targetPaceSecPerKm = null)
            emit(AlertEvent.WorkoutPlanCompleted)
        } else {
            stepIndex++
            current = next
            segmentStartTotalM = nextStartTotalM
            segmentStartMtMs = nextStartMtMs
            emit(AlertEvent.SegmentStart(stepIndex, _state.value.stepCount, next))
        }
    }

    private fun finishSegment(boundaryTotalM: Double, boundaryMtMs: Long) {
        val segment = current ?: return
        emit(AlertEvent.SegmentEnd(stepIndex, segment))
        advanceSegment(boundaryTotalM, boundaryMtMs)
    }

    private fun handleCountdownTick(nowMs: Long) {
        val seconds = config.startCountdownSec
        val elapsed = nowMs - countdownStartMs
        for (k in seconds downTo 1) {
            if (elapsed >= (seconds - k) * 1000L && countdownEmitted.add(k)) {
                emit(AlertEvent.Countdown(k))
            }
        }
        if (nowMs >= countdownEndMs) beginRunning(nowMs)
        updateState(nowMs)
    }

    private fun handleRunningTick(nowMs: Long) {
        if (!gpsLost) {
            val last = lastValidSampleMs
            if (last != null && nowMs - last > config.gpsLostTimeoutSec * 1000L) {
                gpsLost = true
                emit(AlertEvent.GpsLost)
            }
        }
        val segment = current
        val limit = segment?.limit
        if (limit is Limit.Duration) {
            val limitMs = limit.seconds * 1000L + extendedLimitMs
            val segmentMovingMs = movingMsAt(nowMs) - segmentStartMtMs
            val remainingMs = limitMs - segmentMovingMs
            emitSegmentAdvanceCues(limitMs, remainingMs)
            if (segmentMovingMs >= limitMs) {
                finishSegment(tracker.totalDistanceM, segmentStartMtMs + limitMs)
            }
        }
        updateState(nowMs)
    }

    private fun emitSegmentAdvanceCues(limitMs: Long, remainingMs: Long) {
        val hasNext = cursor.hasNext(tracker.totalDistanceM)
        if (!hasNext) return
        if (limitMs >= config.preSegmentWarningMinSegmentSec * 1000L && !preWarningEmitted &&
            remainingMs <= config.preSegmentWarningSec * 1000L
        ) {
            preWarningEmitted = true
            val nextKind = cursor.peek(tracker.totalDistanceM)?.kind ?: current?.kind ?: SegmentKind.RUN
            emit(AlertEvent.PreSegmentWarning(nextKind))
        }
        val countdown = config.segmentCountdownSec
        if (countdown > 0 && limitMs >= config.minSegmentSecForCountdown * 1000L) {
            for (k in countdown downTo 1) {
                if (remainingMs <= k * 1000L && segmentCountdownEmitted.add(k)) {
                    emit(AlertEvent.Countdown(k))
                }
            }
        }
    }

    private fun registerSegmentBoundary() {
        val segment = current ?: return
        val limit = segment.limit
        if (limit !is Limit.Distance) return
        val boundary = segmentStartTotalM + limit.meters + extendedLimitM
        if (boundary > tracker.totalDistanceM && registeredSegmentBoundary != boundary) {
            tracker.addBoundary(boundary)
            registeredSegmentBoundary = boundary
        }
    }

    private fun maybeDistanceCue() {
        val segment = current ?: return
        val limit = segment.limit
        if (limit !is Limit.Distance) return
        val cue = config.cueDistanceM
        if (cue <= 0 || distanceCueEmitted) return
        val limitM = limit.meters + extendedLimitM
        if (limitM < 2.0 * cue) return
        if (!cursor.hasNext(tracker.totalDistanceM)) return
        val doneM = tracker.totalDistanceM - segmentStartTotalM
        if (limitM - doneM <= cue) {
            distanceCueEmitted = true
            val nextKind = cursor.peek(tracker.totalDistanceM)?.kind ?: SegmentKind.RUN
            emit(AlertEvent.DistanceCue(nextKind))
        }
    }

    private fun emitKmMarkers(movingMs: Long) {
        val km = floor(tracker.totalDistanceM / 1000.0).toInt()
        while (kmReached < km) {
            kmReached++
            val timeSec = movingMs / 1000.0
            val paceSecPerKm = if (tracker.totalDistanceM > 0.0) movingMs / tracker.totalDistanceM else 0.0
            emit(AlertEvent.KmMarker(kmReached, timeSec, paceSecPerKm))
        }
    }

    private fun checkGoal() {
        val goal = workout.goalDistanceM
        if (!goalReached && goal != null && tracker.totalDistanceM >= goal) {
            goalReached = true
            emit(AlertEvent.GoalDistanceReached)
        }
    }

    private fun movingMsAt(nowMs: Long): Long {
        val start = tStartRunningMs ?: return 0L
        return nowMs - start - pausedMsTotal
    }

    private fun emit(event: AlertEvent) {
        eventLog += event
        _events.tryEmit(event)
    }

    private fun updateState(nowMs: Long) {
        val movingMs = movingMsAt(nowMs)
        val elapsedMs = tStartRunningMs?.let { nowMs - it } ?: 0L
        val segment = current
        val segmentDistanceM = (tracker.totalDistanceM - segmentStartTotalM).coerceAtLeast(0.0)
        val segmentMovingMs = (movingMs - segmentStartMtMs).coerceAtLeast(0L)
        val remainingM = when (val limit = segment?.limit) {
            is Limit.Distance -> (limit.meters + extendedLimitM - segmentDistanceM).coerceAtLeast(0.0)
            else -> 0.0
        }
        val remainingMs = when (val limit = segment?.limit) {
            is Limit.Duration -> (limit.seconds * 1000L + extendedLimitMs - segmentMovingMs).coerceAtLeast(0L)
            else -> 0L
        }
        val countdownSec = if (phase == RunnerPhase.COUNTDOWN) {
            ceil((countdownEndMs - nowMs) / 1000.0).toInt().coerceAtLeast(0)
        } else {
            null
        }
        _state.value = RunnerState(
            phase = phase,
            stepIndex = stepIndex,
            stepCount = _state.value.stepCount,
            segment = segment,
            segmentDistanceM = segmentDistanceM,
            segmentMovingMs = segmentMovingMs,
            segmentRemainingM = remainingM,
            segmentRemainingMs = remainingMs,
            totalDistanceM = tracker.totalDistanceM,
            movingMs = movingMs,
            elapsedMs = elapsedMs,
            pausedMs = pausedMsTotal,
            paceLiveSecPerKm = if (phase == RunnerPhase.RUNNING) livePace.livePaceSecPerKm() else null,
            countdownSec = countdownSec,
            planCompleted = planCompleted,
            goalReached = goalReached,
            gpsLost = gpsLost,
        )
    }
}
