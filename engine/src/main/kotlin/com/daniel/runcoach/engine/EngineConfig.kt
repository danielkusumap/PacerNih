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
    /** No valid sample for this long means "GPS signal lost" (6.1). */
    val gpsLostTimeoutSec: Int = 10,
    /** Seconds of initial countdown before the workout starts (6.1); 0 disables. */
    val startCountdownSec: Int = 5,
    /** Seconds of 3-2-1 countdown before a time-based segment ends (6.1); 0 disables. */
    val segmentCountdownSec: Int = 3,
    /** Time-based segments shorter than this get no countdown (6.1). */
    val minSegmentSecForCountdown: Int = 8,
    /** "Get ready" warning lead time before a time-based segment change (6.1). */
    val preSegmentWarningSec: Int = 10,
    /** Time-based segments shorter than this get no pre-warning (6.1). */
    val preSegmentWarningMinSegmentSec: Int = 30,
    /** Distance cue lead for distance-based segments (6.1); 0 disables. */
    val cueDistanceM: Int = 20,
    /** Assumed GPS distance error used for the BORDERLINE margin (F17). */
    val paceErrorAssumedM: Double = 6.0,
    /** Upper bound for the BORDERLINE margin in seconds per km (F17). */
    val paceMarginCapSec: Double = 15.0,
    /** How long a candidate correction state must persist before it applies (F13). */
    val alertHoldSec: Int = 5,
    /** Repeat the same correction alert while the state persists, this often (F13). */
    val alertRepeatCooldownSec: Int = 25,
    /** No correction alerts within this many seconds of the segment end (F14). */
    val noAlertLastSec: Int = 15,
    /** Margin to leave a correction state, seconds per km (F13). */
    val hysteresisSecPerKm: Int = 3,
    /** Alert gate: minimum distance into the segment before correction alerts (F14). */
    val minSegmentDistForAlertM: Double = 100.0,
    /** Alert gate: minimum time into the segment before correction alerts, seconds (F14). */
    val minSegmentTimeForAlertSec: Int = 20,
    /** Distance-based segments shorter than this get no correction alerts (F14). */
    val noCorrectionBelowSegmentM: Double = 400.0,
    /** Time-based segments shorter than this get no correction alerts, seconds (F14). */
    val noCorrectionBelowSegmentSec: Int = 60,
    /** A countdown tick later than this is dropped instead of played late (6.1). */
    val countdownTickMaxLateMs: Int = 700,
    /** A pace slower than this is shown as `--:--` (6.13). */
    val maxPaceDisplaySec: Double = 1800.0,
)
