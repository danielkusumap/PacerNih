package com.daniel.runcoach.data

import android.content.Context
import com.daniel.runcoach.engine.result.BestEffort
import com.daniel.runcoach.engine.result.BestEffortKey
import com.daniel.runcoach.engine.result.RecordEvent
import com.daniel.runcoach.engine.result.RecordProgression
import com.daniel.runcoach.engine.result.SessionEfforts

/** A record row for display: time, where it started, and which session set it. */
data class RecordRow(
    val key: BestEffortKey,
    val durationMs: Long,
    val startOffsetM: Double,
    val sessionId: String,
    val startedAtEpochMs: Long,
    val sessionName: String,
)

/** One point of a distance's record series (best effort per session over time). */
data class RecordSeriesPoint(val atEpochMs: Long, val durationMs: Long, val isRecord: Boolean)

/**
 * Personal records (plan P2.9), derived from the stored `BestEffort` table. Current records are the minimum
 * per distance among sessions not excluded; deleting a session therefore lowers the record to the next best
 * without rescanning any track. Progression comes from the pure [RecordProgression].
 */
class RecordRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao get() = RunDatabase.get(appContext).sessionDao()

    suspend fun currentRecords(): Map<BestEffortKey, RecordRow?> =
        BestEffortKey.entries.associateWith { key ->
            dao.bestEffort(key.name)?.let { toRow(key, it) }
        }

    suspend fun topN(key: BestEffortKey, n: Int = 10): List<RecordRow> =
        dao.topEfforts(key.name, n).mapNotNull { toRow(key, it) }

    suspend fun recentRecords(limit: Int = 5): List<RecordRow> {
        val sessions = sessionEfforts()
        return RecordProgression.recent(sessions, limit).mapNotNull { toRow(it) }
    }

    suspend fun series(key: BestEffortKey): List<RecordSeriesPoint> {
        val sessions = sessionEfforts()
        val recordEvents = RecordProgression.compute(sessions, key).associateBy { it.sessionId }
        return sessions
            .filterNot { it.excluded }
            .mapNotNull { session ->
                session.efforts.firstOrNull { it.key == key }?.let { effort ->
                    RecordSeriesPoint(session.atEpochMs, effort.durationMs, recordEvents.containsKey(session.sessionId))
                }
            }
            .sortedBy { it.atEpochMs }
    }

    private suspend fun sessionEfforts(): List<SessionEfforts> {
        val bySession = dao.allEfforts().groupBy { it.sessionId }
        return bySession.map { (sessionId, rows) ->
            SessionEfforts(
                sessionId = sessionId,
                atEpochMs = rows.first().startedAtEpochMs,
                efforts = rows.map { BestEffort(BestEffortKey.valueOf(it.distanceKey), it.durationMs, it.startOffsetM) },
            )
        }
    }

    private suspend fun toRow(key: BestEffortKey, effort: BestEffortEntity): RecordRow? {
        val session = dao.session(effort.sessionId) ?: return null
        return RecordRow(key, effort.durationMs, effort.startOffsetM, session.id, session.startedAtEpochMs, session.name)
    }

    private suspend fun toRow(event: RecordEvent): RecordRow? {
        val row = dao.allEfforts().firstOrNull { it.sessionId == event.sessionId && it.distanceKey == event.key.name } ?: return null
        return RecordRow(event.key, event.durationMs, row.startOffsetM, event.sessionId, event.atEpochMs, row.sessionName)
    }
}
