package com.daniel.runcoach.data

import android.content.Context
import com.daniel.runcoach.engine.model.LocationSample
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Reads and writes the in-progress run so it survives a process kill (plan P1.5). Track points are appended
 * in batches (~5 s) by the service; the snapshot JSON is refreshed on the same cadence. All calls are
 * main-safe (Room suspend functions move off the main thread).
 */
class RunPersistence(context: Context) {

    private val appContext = context.applicationContext
    private val dao get() = RunDatabase.get(appContext).runDao()

    suspend fun saveSnapshot(snapshot: RunSnapshot, savedAtMs: Long) {
        dao.upsertSnapshot(
            ActiveRunSnapshotEntity(
                id = 1,
                sessionId = snapshot.sessionId,
                runnerStateJson = json.encodeToString(snapshot),
                savedAt = savedAtMs,
            ),
        )
    }

    suspend fun loadSnapshot(): RunSnapshot? =
        dao.snapshot()?.let { runCatching { json.decodeFromString<RunSnapshot>(it.runnerStateJson) }.getOrNull() }

    suspend fun appendTrackPoints(points: List<TrackPointEntity>) {
        if (points.isNotEmpty()) dao.insertTrackPoints(points)
    }

    suspend fun loadTrackPoints(sessionId: String): List<TrackPointEntity> = dao.trackPoints(sessionId)

    suspend fun clear(sessionId: String) {
        dao.deleteTrackPoints(sessionId)
        dao.deleteSnapshotFor(sessionId)
    }

    /** Removes only the active-run snapshot, keeping the track points for history (used at Stop, P2.5). */
    suspend fun clearActiveSnapshot(sessionId: String) {
        dao.deleteSnapshotFor(sessionId)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun toEntity(sample: LocationSample, sessionId: String, idx: Int, segmentIdx: Int) = TrackPointEntity(
            sessionId = sessionId,
            idx = idx,
            timeMs = sample.timeNanos / 1_000_000L,
            lat = sample.lat,
            lon = sample.lon,
            accuracyM = sample.accuracyM,
            speedMps = sample.speedMps,
            segmentIdx = segmentIdx,
        )

        fun toSample(entity: TrackPointEntity) = LocationSample(
            timeNanos = entity.timeMs * 1_000_000L,
            lat = entity.lat,
            lon = entity.lon,
            accuracyM = entity.accuracyM ?: 5.0,
            speedMps = entity.speedMps,
        )
    }
}
