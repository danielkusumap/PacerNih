package com.daniel.runcoach.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Insert
    suspend fun insertSegments(segments: List<SegmentResultEntity>)

    @Insert
    suspend fun insertSplits(splits: List<KmSplitEntity>)

    @Insert
    suspend fun insertBestEfforts(efforts: List<BestEffortEntity>)

    @Query("SELECT * FROM Session WHERE id = :id")
    suspend fun session(id: String): SessionEntity?

    @Query("SELECT * FROM Session ORDER BY startedAtEpochMs DESC LIMIT 1")
    suspend fun latestSession(): SessionEntity?

    @Query("SELECT * FROM Session ORDER BY startedAtEpochMs DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM Session ORDER BY startedAtEpochMs DESC")
    suspend fun allSessions(): List<SessionEntity>

    @Query("SELECT * FROM SegmentResult WHERE sessionId = :sessionId ORDER BY idx")
    suspend fun segments(sessionId: String): List<SegmentResultEntity>

    @Query("SELECT * FROM KmSplit WHERE sessionId = :sessionId ORDER BY idx")
    suspend fun splits(sessionId: String): List<KmSplitEntity>

    @Query("SELECT * FROM BestEffort WHERE sessionId = :sessionId")
    suspend fun bestEfforts(sessionId: String): List<BestEffortEntity>

    @Query("UPDATE Session SET name = :name, nameIsCustom = :custom, note = :note, rpe = :rpe, excludeFromRecords = :exclude, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateForm(id: String, name: String, custom: Boolean, note: String?, rpe: Int?, exclude: Boolean, updatedAt: Long)

    /** Best time for a distance across all other sessions that are not excluded (plan 6.10). */
    @Query("SELECT MIN(BestEffort.durationMs) FROM BestEffort JOIN Session ON Session.id = BestEffort.sessionId WHERE BestEffort.distanceKey = :key AND Session.excludeFromRecords = 0 AND BestEffort.sessionId != :excludeSessionId")
    suspend fun bestDurationExcluding(key: String, excludeSessionId: String): Long?

    @Query(
        "SELECT * FROM BestEffort WHERE distanceKey = :key AND sessionId IN " +
            "(SELECT id FROM Session WHERE excludeFromRecords = 0) ORDER BY durationMs ASC LIMIT 1",
    )
    suspend fun bestEffort(key: String): BestEffortEntity?

    @Query(
        "SELECT * FROM BestEffort WHERE distanceKey = :key AND sessionId IN " +
            "(SELECT id FROM Session WHERE excludeFromRecords = 0) ORDER BY durationMs ASC LIMIT :limit",
    )
    suspend fun topEfforts(key: String, limit: Int): List<BestEffortEntity>

    @Query(
        "SELECT BestEffort.sessionId AS sessionId, BestEffort.distanceKey AS distanceKey, " +
            "BestEffort.durationMs AS durationMs, BestEffort.startOffsetM AS startOffsetM, " +
            "Session.startedAtEpochMs AS startedAtEpochMs, Session.name AS sessionName " +
            "FROM BestEffort JOIN Session ON Session.id = BestEffort.sessionId " +
            "WHERE Session.excludeFromRecords = 0 ORDER BY Session.startedAtEpochMs ASC",
    )
    suspend fun allEfforts(): List<EffortWithSession>

    @Query("DELETE FROM Session WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM SegmentResult WHERE sessionId = :sessionId")
    suspend fun deleteSegments(sessionId: String)

    @Query("DELETE FROM KmSplit WHERE sessionId = :sessionId")
    suspend fun deleteSplits(sessionId: String)

    @Query("DELETE FROM BestEffort WHERE sessionId = :sessionId")
    suspend fun deleteBestEfforts(sessionId: String)

    @Query("DELETE FROM TrackPoint WHERE sessionId = :sessionId")
    suspend fun deleteTrackPoints(sessionId: String)

    /** No foreign keys, so all related rows are removed together (plan section 7). */
    @Transaction
    suspend fun deleteSessionCascade(id: String) {
        deleteSegments(id)
        deleteSplits(id)
        deleteBestEfforts(id)
        deleteTrackPoints(id)
        deleteSession(id)
    }

    @Transaction
    suspend fun saveSessionBundle(
        session: SessionEntity,
        segments: List<SegmentResultEntity>,
        splits: List<KmSplitEntity>,
        bestEfforts: List<BestEffortEntity>,
    ) {
        insertSession(session)
        insertSegments(segments)
        insertSplits(splits)
        insertBestEfforts(bestEfforts)
    }
}
