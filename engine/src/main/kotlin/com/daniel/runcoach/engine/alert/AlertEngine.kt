package com.daniel.runcoach.engine.alert

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.runner.AlertEvent

/** Correction state of the runner's current segment (plan F13). */
enum class CorrectionState { STABLE, TOO_SLOW, TOO_FAST }

/** Everything the alert engine needs for one evaluation (F13/F14). One field per gate or formula input. */
data class CorrectionInput(
    val segmentTargetSecPerKm: Int?,
    val fastMarginSec: Int?,
    val segmentOpen: Boolean,
    val timeBased: Boolean,
    val segmentLengthM: Double,
    val segmentLengthSec: Double,
    val segmentDistanceM: Double,
    val segmentMovingSec: Double,
    val scopeDistM: Double,
    val remainingSegmentSec: Double,
    val paceLiveSecPerKm: Double?,
    val projectedSecPerKm: Double?,
    val requiredSecPerKm: Double?,
    val running: Boolean,
    val gpsLost: Boolean,
)

/**
 * Decides the pace-correction state and raises the matching alert, following the gates (F14), hysteresis,
 * hold, repeat cooldown, and back-to-stable rules (F13) from the plan.
 *
 * Pure and deterministic: call [evaluate] on every tick with the current metrics and the monotonic clock;
 * it returns an [AlertEvent] when one should be spoken, else null. Stateful and single-use per run.
 */
class AlertEngine(private val config: EngineConfig = EngineConfig()) {

    private var state = CorrectionState.STABLE
    private var pending: CorrectionState? = null
    private var pendingSinceMs = 0L
    private var lastAlertMs = 0L
    private var alertedNonStable = false

    fun state(): CorrectionState = state

    fun reset() {
        state = CorrectionState.STABLE
        pending = null
        alertedNonStable = false
        lastAlertMs = 0L
    }

    fun evaluate(input: CorrectionInput, nowMs: Long): AlertEvent? {
        if (!gatesPass(input)) {
            reset()
            return null
        }
        val target = input.segmentTargetSecPerKm!!
        val candidate = classify(input, target)

        if (candidate != state) {
            if (pending != candidate) {
                pending = candidate
                pendingSinceMs = nowMs
            }
            if (nowMs - pendingSinceMs < config.alertHoldSec * 1000L) return null
            val previous = state
            state = candidate
            pending = null
            return when (candidate) {
                CorrectionState.TOO_SLOW -> {
                    lastAlertMs = nowMs
                    alertedNonStable = true
                    AlertEvent.TooSlow(input.paceLiveSecPerKm!!, target, input.requiredSecPerKm)
                }

                CorrectionState.TOO_FAST -> {
                    lastAlertMs = nowMs
                    alertedNonStable = true
                    AlertEvent.TooFast(input.paceLiveSecPerKm!!, target)
                }

                CorrectionState.STABLE -> {
                    val wasAlerted = alertedNonStable || previous != CorrectionState.STABLE
                    alertedNonStable = false
                    if (wasAlerted) AlertEvent.BackToStable else null
                }
            }
        }

        pending = null
        if (state != CorrectionState.STABLE && alertedNonStable &&
            nowMs - lastAlertMs >= config.alertRepeatCooldownSec * 1000L
        ) {
            lastAlertMs = nowMs
            return when (state) {
                CorrectionState.TOO_SLOW -> AlertEvent.TooSlow(input.paceLiveSecPerKm!!, target, input.requiredSecPerKm)
                CorrectionState.TOO_FAST -> AlertEvent.TooFast(input.paceLiveSecPerKm!!, target)
                CorrectionState.STABLE -> null
            }
        }
        return null
    }

    private fun classify(input: CorrectionInput, target: Int): CorrectionState {
        val paceLive = input.paceLiveSecPerKm!!
        val hysteresis = config.hysteresisSecPerKm
        if (state == CorrectionState.TOO_SLOW) {
            val stillSlow = if (input.timeBased) {
                paceLive > target - hysteresis
            } else {
                (input.projectedSecPerKm ?: Double.MAX_VALUE) > target - hysteresis
            }
            if (stillSlow) return CorrectionState.TOO_SLOW
        }
        if (state == CorrectionState.TOO_FAST) {
            val margin = input.fastMarginSec
            if (margin != null && paceLive >= target - margin + hysteresis) return CorrectionState.TOO_FAST
        }

        val tooSlow = if (input.timeBased) {
            paceLive > target
        } else {
            val projected = input.projectedSecPerKm
            projected != null && projected > target
        }
        if (tooSlow) return CorrectionState.TOO_SLOW

        val margin = input.fastMarginSec
        if (margin != null && paceLive < target - margin) return CorrectionState.TOO_FAST
        return CorrectionState.STABLE
    }

    private fun gatesPass(input: CorrectionInput): Boolean {
        if (!input.running || input.gpsLost) return false
        if (input.segmentTargetSecPerKm == null || input.segmentOpen) return false
        val lengthOk = if (input.timeBased) {
            input.segmentLengthSec >= config.noCorrectionBelowSegmentSec
        } else {
            input.segmentLengthM >= config.noCorrectionBelowSegmentM
        }
        if (!lengthOk) return false
        if (input.segmentDistanceM < config.minSegmentDistForAlertM) return false
        if (input.segmentMovingSec < config.minSegmentTimeForAlertSec) return false
        if (input.scopeDistM < config.noCorrectionBelowSegmentM) return false
        if (input.remainingSegmentSec <= config.noAlertLastSec) return false
        if (input.paceLiveSecPerKm == null) return false
        return true
    }
}
