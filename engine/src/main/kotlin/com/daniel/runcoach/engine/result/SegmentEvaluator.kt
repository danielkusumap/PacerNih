package com.daniel.runcoach.engine.result

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.roundHalfUp
import kotlin.math.max
import kotlin.math.min

/** Outcome of one finished segment (plan F17). */
enum class SegmentStatus { PASS, FAIL, BORDERLINE, NO_TARGET, NO_DATA, SKIPPED }

/** Evaluation of one full kilometre inside a target segment. */
data class KmStatus(
    val index: Int,
    val paceSecPerKm: Double,
    val paceRoundedSec: Long,
    val deltaSec: Long,
    val status: SegmentStatus,
)

/** Full evaluation of a segment. Paces are raw; `...RoundedSec` and `deltaSec` use the rounded pace. */
data class SegmentEvaluation(
    val status: SegmentStatus,
    val distanceM: Double,
    val movingMs: Long,
    val avgPaceSecPerKm: Double?,
    val avgPaceRoundedSec: Long?,
    val deltaSec: Long?,
    val marginSec: Double?,
    val kmStatuses: List<KmStatus>,
    val extended: Boolean,
)

/**
 * Grades a finished segment against its target pace (plan 6.8, F17). Pass/fail uses the pace rounded to
 * the nearest second, so the screen never shows "7:00" while reporting FAIL. A short segment (under
 * [EngineConfig.minPaceDistanceM]) is DATA KURANG; a skipped segment is DILEWATI; no target is NO_TARGET.
 */
object SegmentEvaluator {

    /** Capped GPS-error margin for the average pace: `min(T * PACE_ERROR_ASSUMED_M / Ds, cap)` (F17 / V11). */
    fun marginSec(targetSecPerKm: Double, distanceM: Double, config: EngineConfig = EngineConfig()): Double =
        min(targetSecPerKm * config.paceErrorAssumedM / distanceM, config.paceMarginCapSec)

    /** Capped GPS-error margin per full kilometre (F17). */
    fun marginKmSec(targetSecPerKm: Double, config: EngineConfig = EngineConfig()): Double =
        min(targetSecPerKm * config.paceErrorAssumedM / 1000.0, config.paceMarginCapSec)

    fun evaluate(
        targetSecPerKm: Int?,
        perKmCheck: Boolean,
        distanceM: Double,
        movingSec: Double,
        kmTimesSec: List<Double> = emptyList(),
        skipped: Boolean = false,
        extended: Boolean = false,
        bound: com.daniel.runcoach.engine.model.PaceTargetBound = com.daniel.runcoach.engine.model.PaceTargetBound.MAX,
        config: EngineConfig = EngineConfig(),
    ): SegmentEvaluation {
        val movingMs = roundHalfUp(movingSec * 1000.0)
        fun plain(status: SegmentStatus) =
            SegmentEvaluation(status, distanceM, movingMs, null, null, null, null, emptyList(), extended)

        if (skipped) return plain(SegmentStatus.SKIPPED)
        if (targetSecPerKm == null) return plain(SegmentStatus.NO_TARGET)
        if (distanceM < config.minPaceDistanceM) return plain(SegmentStatus.NO_DATA)

        val avg = movingSec / (distanceM / 1000.0)
        val avgR = roundHalfUp(avg)
        val deltaSec = avgR - targetSecPerKm
        val marginAvg = marginSec(targetSecPerKm.toDouble(), distanceM, config)
        val marginKm = marginKmSec(targetSecPerKm.toDouble(), config)

        fun ok(delta: Long): Boolean = if (bound == com.daniel.runcoach.engine.model.PaceTargetBound.MIN) delta >= 0L else delta <= 0L
        fun withinAvg(delta: Long): Boolean = if (bound == com.daniel.runcoach.engine.model.PaceTargetBound.MIN) -delta <= marginAvg else delta <= marginAvg
        fun withinKm(delta: Long): Boolean = if (bound == com.daniel.runcoach.engine.model.PaceTargetBound.MIN) -delta <= marginKm else delta <= marginKm

        val kmStatuses = if (perKmCheck) {
            kmTimesSec.mapIndexed { index, kmTimeSec ->
                val kmR = roundHalfUp(kmTimeSec)
                val kmDelta = kmR - targetSecPerKm
                val status = when {
                    ok(kmDelta) -> SegmentStatus.PASS
                    withinKm(kmDelta) -> SegmentStatus.BORDERLINE
                    else -> SegmentStatus.FAIL
                }
                KmStatus(index + 1, kmTimeSec, kmR, kmDelta, status)
            }
        } else {
            emptyList()
        }

        val status = when {
            ok(deltaSec) && kmStatuses.all { ok(it.deltaSec) } -> SegmentStatus.PASS
            withinAvg(deltaSec) && kmStatuses.all { withinKm(it.deltaSec) } -> SegmentStatus.BORDERLINE
            else -> SegmentStatus.FAIL
        }
        return SegmentEvaluation(
            status = status,
            distanceM = distanceM,
            movingMs = movingMs,
            avgPaceSecPerKm = avg,
            avgPaceRoundedSec = avgR,
            deltaSec = deltaSec,
            marginSec = marginAvg,
            kmStatuses = kmStatuses,
            extended = extended,
        )
    }
}
