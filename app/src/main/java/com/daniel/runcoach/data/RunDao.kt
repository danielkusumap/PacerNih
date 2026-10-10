package com.daniel.runcoach.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RunDao {

    @Insert
    suspend fun insertTrackPoints(points: List<TrackPointEntity>)

    @Query("SELECT * FROM TrackPoint WHERE sessionId = :sessionId ORDER BY idx")
    suspend fun trackPoints(sessionId: String): List<TrackPointEntity>

    @Query("DELETE FROM TrackPoint WHERE sessionId = :sessionId")
    suspend fun deleteTrackPoints(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: ActiveRunSnapshotEntity)

    @Query("SELECT * FROM ActiveRunSnapshot WHERE id = 1")
    suspend fun snapshot(): ActiveRunSnapshotEntity?

    @Query("DELETE FROM ActiveRunSnapshot WHERE sessionId = :sessionId")
    suspend fun deleteSnapshotFor(sessionId: String)

    @Query("DELETE FROM ActiveRunSnapshot")
    suspend fun clearSnapshot()
}
