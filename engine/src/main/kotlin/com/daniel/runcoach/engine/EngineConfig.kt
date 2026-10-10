package com.daniel.runcoach.engine

/**
 * Single home for every tunable threshold (plan rule #5). Values are the defaults from
 * section 6.1 of RUNCOACH_PLAN.md. This file grows as tasks need constants; nothing numeric
 * is allowed to live in the algorithms themselves.
 *
 * Note: 6.1 values are provisional until section 6.14 (Formula Catalog) lands — see
 * docs/OPEN_QUESTIONS.md Q-003.
 */
data class EngineConfig(
    /** Samples with a horizontal accuracy worse than this are dropped. */
    val maxAccuracyM: Double = 20.0,
    /** Horizontal accuracy at or better than this counts as a "GPS ready" fix. */
    val goodFixAccuracyM: Double = 12.0,
    /** Samples whose implied speed from the last valid sample exceeds this are dropped. */
    val maxSpeedMps: Double = 9.0,
    /** Mean Earth radius in metres, used by the Haversine formula. */
    val earthRadiusM: Double = 6_371_008.8,
    /** Best efforts faster than this pace are treated as GPS anomalies and discarded. */
    val recordMinValidPaceSecPerKm: Double = 150.0,
    /** Below this speed (with displacement under [stationaryMaxDispM]) a sample is treated as jitter (F3.4). */
    val stationarySpeedMps: Double = 0.3,
    /** Max straight-line displacement still considered jitter while stationary (F3.4). */
    val stationaryMaxDispM: Double = 5.0,
    /** Consecutive speed rejections before the reference is reset instead of stalling (F3.7). */
    val speedRejectResetCount: Int = 3,
    /** Lower bound for the distance calibration factor (F4). */
    val calibrationMin: Double = 0.95,
    /** Upper bound for the distance calibration factor (F4). */
    val calibrationMax: Double = 1.05,
    /** Live pace averaging window in seconds (F8). */
    val liveWindowSec: Int = 15,
    /** Minimum Doppler samples in the window for method A (F8). */
    val liveMinSamples: Int = 5,
    /** Minimum seconds of data before the distance fallback (method B) may be used (F8). */
    val liveFallbackMinSec: Int = 8,
    /** Minimum distance before an average pace is reported (F9). */
    val minPaceDistanceM: Double = 50.0,
)
