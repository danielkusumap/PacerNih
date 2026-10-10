package com.daniel.runcoach.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One logged GPS point (plan section 7). Keyed only by [sessionId] with an index — no foreign key and no
 * cascade; session deletion is a single transactional repository call.
 */
@Entity(tableName = "TrackPoint", indices = [Index("sessionId")])
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val sessionId: String,
    val idx: Int,
    val timeMs: Long,
    val lat: Double,
    val lon: Double,
    val accuracyM: Double?,
    val speedMps: Double?,
    val segmentIdx: Int,
)

/** The single in-progress run, so an interrupted session can be offered for resume (D04). */
@Entity(tableName = "ActiveRunSnapshot")
data class ActiveRunSnapshotEntity(
    @PrimaryKey val id: Int = 1,
    val sessionId: String,
    val runnerStateJson: String,
    val savedAt: Long,
)
