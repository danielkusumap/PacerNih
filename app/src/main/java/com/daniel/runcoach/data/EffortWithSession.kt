package com.daniel.runcoach.data

/** Best effort joined with its session (plan P2.9). */
data class EffortWithSession(
    val sessionId: String,
    val distanceKey: String,
    val durationMs: Long,
    val startOffsetM: Double,
    val startedAtEpochMs: Long,
    val sessionName: String,
)
