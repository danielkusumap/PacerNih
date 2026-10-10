package com.daniel.runcoach.data

import kotlinx.serialization.Serializable

/**
 * Everything needed to offer D04 ("resume session") after a process death: the workout and the run's start
 * info. The GPS points live in the `TrackPoint` table; resume replays them into a fresh engine, which is
 * deterministic, so distance and segment progress are reconstructed (see DECISIONS D-023).
 */
@Serializable
data class RunSnapshot(
    val sessionId: String,
    val workoutJson: String,
    val startedAtElapsedMs: Long,
    val startedAtEpochMs: Long,
    val zoneId: String,
)
