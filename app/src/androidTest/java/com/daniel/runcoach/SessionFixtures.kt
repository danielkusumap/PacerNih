package com.daniel.runcoach

import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.engine.result.BestEffort
import com.daniel.runcoach.engine.result.BestEffortKey
import com.daniel.runcoach.engine.result.KmSplit
import com.daniel.runcoach.engine.result.SegmentResultData
import com.daniel.runcoach.engine.result.SegmentStatus
import com.daniel.runcoach.engine.result.SessionResultsData

/** Shared fixtures for session-related instrumented tests. */
object SessionFixtures {

    fun results(): SessionResultsData = resultsWithK1(420_000L)

    fun resultsWithK1(k1Ms: Long): SessionResultsData = SessionResultsData(
        segments = listOf(
            SegmentResultData(1, "Run", "RUN", "distance", 5000.0, 420, 5000.0, 2_100_000L, 420.0, 0L, "PASS"),
        ),
        kmSplits = listOf(KmSplit(1, false, 1000.0, k1Ms, k1Ms / 1000.0, 420, 0L, SegmentStatus.PASS)),
        bestEfforts = listOf(BestEffort(BestEffortKey.K1, k1Ms, 0.0)),
    )

    fun session(
        id: String,
        name: String,
        exclude: Boolean = false,
        startedAt: Long = System.currentTimeMillis(),
    ) = SessionEntity(
        id = id,
        templateId = null,
        templateNameSnapshot = "Template",
        workoutSnapshotJson = "{}",
        name = name,
        nameIsCustom = true,
        note = null,
        rpe = null,
        excludeFromRecords = exclude,
        startedAtEpochMs = startedAt,
        zoneId = "Asia/Jakarta",
        endedAtEpochMs = startedAt + 2_100_000L,
        elapsedMs = 2_100_000L,
        movingMs = 2_100_000L,
        pausedMs = 0L,
        totalDistanceM = 5000.0,
        avgPaceSecPerKm = 420.0,
        segPass = 1,
        segFail = 0,
        segBorderline = 0,
        segNoTarget = 0,
        planCompleted = true,
        goalReached = false,
        distanceCalibrationFactorUsed = 1.0,
        droppedSampleCount = 0,
        updatedAt = startedAt,
    )
}
