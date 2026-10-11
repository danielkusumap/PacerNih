package com.daniel.runcoach.data

import android.content.Context
import com.daniel.runcoach.engine.naming.DefaultSessionNamer
import com.daniel.runcoach.engine.result.BestEffort
import com.daniel.runcoach.engine.result.SegmentResultData
import com.daniel.runcoach.engine.result.SessionResultsData
import com.daniel.runcoach.engine.voice.VoiceLanguage
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes saved sessions (plan P2.5). Sessions are appended by the service the moment Stop is
 * pressed; the S06 form only updates the name/note/RPE afterwards. Deletion is one transaction across all
 * related tables (no foreign keys) — see [SessionDao.deleteSessionCascade].
 */
class SessionRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao get() = RunDatabase.get(appContext).sessionDao()
    private val runDao get() = RunDatabase.get(appContext).runDao()

    val sessions: Flow<List<SessionEntity>> = dao.observeSessions()

    suspend fun saveSession(session: SessionEntity, results: SessionResultsData) {
        dao.saveSessionBundle(
            session = session,
            segments = results.segments.map { it.toEntity(session.id) },
            splits = results.kmSplits.map { it.toEntity(session.id) },
            bestEfforts = results.bestEfforts.map { it.toEntity(session.id) },
        )
    }

    /** Writes the finished run as a draft (excluded from history/records) until the user saves it in S06. */
    suspend fun saveDraft(session: SessionEntity, results: SessionResultsData) =
        saveSession(session.copy(pending = true), results)

    suspend fun pendingDraft(): SessionEntity? = dao.pendingSession()

    /** Applies the S06 form and promotes the draft to a saved session. */
    suspend fun finalizeDraft(id: String, name: String, note: String?, rpe: Int?, excludeFromRecords: Boolean, language: VoiceLanguage, nowMs: Long): Boolean {
        val session = dao.session(id) ?: return false
        val trimmed = name.trim()
        val resolvedName = if (trimmed.isEmpty()) {
            DefaultSessionNamer.name(session.startedAtEpochMs, session.zoneId, language)
        } else {
            trimmed
        }
        dao.finalizeDraft(id, resolvedName, trimmed.isNotEmpty(), note?.take(MAX_NOTE), rpe, excludeFromRecords, nowMs)
        return true
    }

    suspend fun get(id: String): SessionEntity? = dao.session(id)

    suspend fun latest(): SessionEntity? = dao.latestSession()

    suspend fun allSessions(): List<SessionEntity> = dao.allSessions()

    suspend fun segments(id: String): List<SegmentResultEntity> = dao.segments(id)

    suspend fun splits(id: String): List<KmSplitEntity> = dao.splits(id)

    suspend fun bestEfforts(id: String): List<BestEffortEntity> = dao.bestEfforts(id)

    suspend fun trackPoints(id: String): List<TrackPointEntity> = runDao.trackPoints(id)

    suspend fun delete(id: String) = dao.deleteSessionCascade(id)

    /**
     * Applies the S06 form. An empty name falls back to the default for the session's start hour and language,
     * stored as final text; a name at or under [MAX_NOTE] characters is required for the note.
     */
    suspend fun updateForm(id: String, name: String, note: String?, rpe: Int?, excludeFromRecords: Boolean, language: VoiceLanguage, nowMs: Long): Boolean {
        val session = dao.session(id) ?: return false
        val trimmed = name.trim()
        val resolvedName = if (trimmed.isEmpty()) {
            DefaultSessionNamer.name(session.startedAtEpochMs, session.zoneId, language)
        } else {
            trimmed
        }
        val cleanNote = note?.take(MAX_NOTE)
        dao.updateForm(id, resolvedName, trimmed.isNotEmpty(), cleanNote, rpe, excludeFromRecords, nowMs)
        return true
    }

    /** Best efforts from this session that beat every other counted session (record badge). */
    suspend fun newRecords(id: String): List<BestEffortEntity> =
        dao.bestEfforts(id).filter { effort ->
            val previous = dao.bestDurationExcluding(effort.distanceKey, id)
            previous == null || effort.durationMs < previous
        }

    companion object {
        const val MAX_NOTE = 500
        fun noteWithinLimit(note: String?): Boolean = (note?.length ?: 0) <= MAX_NOTE
    }
}

private fun SegmentResultData.toEntity(sessionId: String) = SegmentResultEntity(
    sessionId = sessionId,
    idx = idx,
    label = label,
    kind = kind,
    limitType = limitType,
    limitValue = limitValue,
    targetPace = targetPace,
    distanceM = distanceM,
    durationMs = durationMs,
    avgPace = avgPace,
    deltaSec = deltaSec,
    status = status,
    kmDetailJson = null,
    targetBound = targetBound.name,
    stopped = stopped,
)

private fun com.daniel.runcoach.engine.result.KmSplit.toEntity(sessionId: String) = KmSplitEntity(
    sessionId = sessionId,
    idx = index,
    distanceM = distanceM,
    durationMs = durationMs,
    paceSecPerKm = paceSecPerKm,
    targetPace = targetSecPerKm,
    deltaSec = deltaSec,
    status = status?.name,
)

private fun BestEffort.toEntity(sessionId: String) = BestEffortEntity(
    sessionId = sessionId,
    distanceKey = key.name,
    durationMs = durationMs,
    startOffsetM = startOffsetM,
)
