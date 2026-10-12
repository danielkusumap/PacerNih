package com.daniel.runcoach.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One saved session (plan section 7). History is a log: everything the session needs is snapshotted here,
 * and the result tables below reference it by a plain indexed [sessionId] with no foreign key.
 */
@Entity(tableName = "Session")
data class SessionEntity(
    @PrimaryKey val id: String,
    val templateId: String?,
    val templateNameSnapshot: String,
    val workoutSnapshotJson: String,
    val name: String,
    val nameIsCustom: Boolean,
    val note: String?,
    val rpe: Int?,
    val excludeFromRecords: Boolean,
    val startedAtEpochMs: Long,
    val zoneId: String,
    val endedAtEpochMs: Long,
    val elapsedMs: Long,
    val movingMs: Long,
    val pausedMs: Long,
    val totalDistanceM: Double,
    val avgPaceSecPerKm: Double?,
    val segPass: Int,
    val segFail: Int,
    val segBorderline: Int,
    val segNoTarget: Int,
    val planCompleted: Boolean,
    val goalReached: Boolean,
    val distanceCalibrationFactorUsed: Double,
    val droppedSampleCount: Int,
    val updatedAt: Long,
    /** True while the session is a draft that has not been saved yet (S06). Excluded from history/records. */
    val pending: Boolean = false,
)

@Entity(tableName = "SegmentResult", indices = [Index("sessionId")])
data class SegmentResultEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sessionId: String,
    val idx: Int,
    val label: String,
    val kind: String,
    val limitType: String,
    val limitValue: Double,
    val targetPace: Int?,
    val distanceM: Double,
    val durationMs: Long,
    val avgPace: Double?,
    val deltaSec: Long?,
    val status: String,
    val kmDetailJson: String?,
    val targetBound: String = "MAX",
    val stopped: Boolean = false,
    /** R2: slow bound for RANGE (fast bound is [targetPace]). */
    val targetPace2: Int? = null,
)

@Entity(tableName = "KmSplit", indices = [Index("sessionId")])
data class KmSplitEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sessionId: String,
    val idx: Int,
    val distanceM: Double,
    val durationMs: Long,
    val paceSecPerKm: Double?,
    val targetPace: Int?,
    val deltaSec: Long?,
    val status: String?,
)

@Entity(tableName = "BestEffort", indices = [Index("sessionId")])
data class BestEffortEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sessionId: String,
    val distanceKey: String,
    val durationMs: Long,
    val startOffsetM: Double,
)
