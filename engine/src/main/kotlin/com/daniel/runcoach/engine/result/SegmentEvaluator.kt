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
        slowSecPerKm: Int? = null,
        config: EngineConfig = EngineConfig(),
    ): SegmentEvaluation {
        val movingMs = roundHalfUp(movingSec * 1000.0)
        fun plain(status: SegmentStatus) =
            SegmentEvaluation(status, distanceM, movingMs, null, null, null, null, emptyList(), extended)

        if (skipped) return plain(SegmentStatus.SKIPPED)
        if (targetSecPerKm == null) {
            // B3: actual pace is stored for every segment with enough distance, even without a target.
            val avg = if (distanceM >= config.minPaceDistanceM) movingSec / (distanceM / 1000.0) else null
            return SegmentEvaluation(
                status = SegmentStatus.NO_TARGET,
                distanceM = distanceM,
                movingMs = movingMs,
                avgPaceSecPerKm = avg,
                avgPaceRoundedSec = avg?.let { roundHalfUp(it) },
                deltaSec = null,
                marginSec = null,
                kmStatuses = emptyList(),
                extended = extended,
            )
        }
        if (distanceM < config.minPaceDistanceM) return plain(SegmentStatus.NO_DATA)

        // R2: range mode keeps the pace inside [fast, slow]; PASS inside, BORDERLINE within the GPS margin, else FAIL.
        if (bound == com.daniel.runcoach.engine.model.PaceTargetBound.RANGE && slowSecPerKm != null) {
            val fast = targetSecPerKm.toLong()
            val slow = slowSecPerKm.toLong()
            val avgR = roundHalfUp(movingSec / (distanceM / 1000.0))
            val marginAvg = marginSec(targetSecPerKm.toDouble(), distanceM, config)
            val marginKm = marginKmSec(targetSecPerKm.toDouble(), config)
            fun excessRounded(value: Long): Long = when {
                value < fast -> fast - value
                value > slow -> value - slow
                else -> 0L
            }
            val kmStatuses = if (perKmCheck) {
                kmTimesSec.mapIndexed { index, kmTimeSec ->
                    val kmR = roundHalfUp(kmTimeSec)
                    val kmDelta = when {
                        kmR < fast -> kmR - fast
                        kmR > slow -> kmR - slow
                        else -> 0L
                    }
                    val status = when {
                        excessRounded(kmR) == 0L -> SegmentStatus.PASS
                        excessRounded(kmR).toDouble() <= marginKm -> SegmentStatus.BORDERLINE
                        else -> SegmentStatus.FAIL
                    }
                    KmStatus(index + 1, kmTimeSec, kmR, kmDelta, status)
                }
            } else {
                emptyList()
            }
            val status = when {
                excessRounded(avgR) == 0L && kmStatuses.all { it.status == SegmentStatus.PASS } -> SegmentStatus.PASS
                excessRounded(avgR).toDouble() <= marginAvg && kmStatuses.none { it.status == SegmentStatus.FAIL } -> SegmentStatus.BORDERLINE
                else -> SegmentStatus.FAIL
            }
            val deltaSec = when {
                avgR < fast -> avgR - fast
                avgR > slow -> avgR - slow
                else -> 0L
            }
            return SegmentEvaluation(
                status = status,
                distanceM = distanceM,
                movingMs = movingMs,
                avgPaceSecPerKm = movingSec / (distanceM / 1000.0),
                avgPaceRoundedSec = avgR,
                deltaSec = deltaSec,
                marginSec = marginAvg,
                kmStatuses = kmStatuses,
                extended = extended,
            )
        }

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
