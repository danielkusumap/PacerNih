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
)
