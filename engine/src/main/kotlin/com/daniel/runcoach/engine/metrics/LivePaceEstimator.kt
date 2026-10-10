package com.daniel.runcoach.engine.metrics

import com.daniel.runcoach.engine.EngineConfig

/**
 * Rolling live-pace estimate over the last [EngineConfig.liveWindowSec] seconds of moving time (plan F8).
 *
 * Method A (Doppler): time-weighted average of `speedMps` over samples in the window that report a speed
 * and have no worse than 1.0 m/s speed accuracy, needing at least [EngineConfig.liveMinSamples] samples.
 * Each sample is weighted by the time it represents (trapezoidal), so uniformly spaced samples weigh
 * equally — this is what makes vector V5's five 1 Hz samples average to their plain mean.
 *
 * Method B (fallback): straight-line speed from the cumulative-distance curve over the window, using
 * interpolation, once at least [EngineConfig.liveFallbackMinSec] seconds of data exist.
 *
 * A resulting speed below [EngineConfig.stationarySpeedMps] counts as stopped (null pace). Stateful,
 * single-use per run, not thread safe.
 */
class LivePaceEstimator(private val config: EngineConfig = EngineConfig()) {

    private data class Entry(
        val mtMs: Long,
        val cumulativeDistanceM: Double,
        val speedMps: Double?,
        val speedAccuracyMps: Double?,
    )

    private val entries = ArrayDeque<Entry>()

    /** Feeds one sample on the moving clock. [speedAccuracyMps] is the reported speed accuracy, if any. */
    fun add(
        mtMs: Long,
        cumulativeDistanceM: Double,
        speedMps: Double? = null,
        speedAccuracyMps: Double? = null,
    ) {
        entries.addLast(Entry(mtMs, cumulativeDistanceM, speedMps, speedAccuracyMps))
        // Keep one sample before the window so the fallback can interpolate the window start.
        val cutoffMs = mtMs - config.liveWindowSec * 1000L
        while (entries.size > 2 && entries.first().mtMs < cutoffMs) entries.removeFirst()
    }

    fun reset() = entries.clear()

    /** Live pace in seconds per kilometre, or null when stopped / not enough data. */
    fun livePaceSecPerKm(): Double? {
        val speed = liveSpeedMps() ?: return null
        if (speed < config.stationarySpeedMps) return null
        return 1000.0 / speed
    }

    fun liveSpeedMps(): Double? {
        if (entries.size < 2) return null
        val window = entries.toList()
        val nowMs = window.last().mtMs
        val windowStartMs = nowMs - config.liveWindowSec * 1000L

        var weightedSpeed = 0.0
        var totalWeight = 0.0
        var count = 0
        for (i in window.indices) {
            val entry = window[i]
            if (entry.mtMs < windowStartMs) continue
            val speed = entry.speedMps ?: continue
            val accuracy = entry.speedAccuracyMps
            if (accuracy != null && accuracy > SPEED_ACCURACY_LIMIT_MPS) continue
            val weightSec = timeWeightSec(window, i)
            if (weightSec <= 0.0) continue
            weightedSpeed += speed * weightSec
            totalWeight += weightSec
            count++
        }
        if (count >= config.liveMinSamples && totalWeight > 0.0) {
            return weightedSpeed / totalWeight
        }

        // Method B: distance over the window (clamped to the data actually available).
        val firstMs = window.first().mtMs
        val availableMs = nowMs - firstMs
        if (availableMs < config.liveFallbackMinSec * 1000L) return null
        val spanMs = minOf(config.liveWindowSec * 1000L, availableMs)
        val distanceThenM = distanceAt(window, nowMs - spanMs) ?: return null
        val distanceNowM = window.last().cumulativeDistanceM
        return (distanceNowM - distanceThenM) / (spanMs / 1000.0)
    }

    /** Seconds a sample represents: half the gap between its neighbours, edge samples use their one side. */
    private fun timeWeightSec(window: List<Entry>, index: Int): Double {
        val previous = window.getOrNull(index - 1)
        val next = window.getOrNull(index + 1)
        return when {
            previous != null && next != null -> (next.mtMs - previous.mtMs) / 2000.0
            next != null -> (next.mtMs - window[index].mtMs) / 1000.0
            previous != null -> (window[index].mtMs - previous.mtMs) / 1000.0
            else -> 0.0
        }
    }

    private fun distanceAt(window: List<Entry>, mtMs: Long): Double? {
        if (window.isEmpty()) return null
        if (mtMs <= window.first().mtMs) return window.first().cumulativeDistanceM
        if (mtMs >= window.last().mtMs) return window.last().cumulativeDistanceM
        for (i in 1 until window.size) {
            val after = window[i]
            if (after.mtMs >= mtMs) {
                val before = window[i - 1]
                val spanMs = (after.mtMs - before.mtMs).toDouble()
                if (spanMs <= 0.0) return after.cumulativeDistanceM
                val fraction = (mtMs - before.mtMs) / spanMs
                return before.cumulativeDistanceM +
                    fraction * (after.cumulativeDistanceM - before.cumulativeDistanceM)
            }
        }
        return window.last().cumulativeDistanceM
    }

    private companion object {
        const val SPEED_ACCURACY_LIMIT_MPS = 1.0
    }
}
