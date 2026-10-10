package com.daniel.runcoach.engine.result

/** One session's best efforts, ordered in time, for record progression (plan F22). */
data class SessionEfforts(
    val sessionId: String,
    val atEpochMs: Long,
    val efforts: List<BestEffort>,
    val excluded: Boolean = false,
)

/** A record was set at [atEpochMs] by [sessionId] with [durationMs]. */
data class RecordEvent(
    val key: BestEffortKey,
    val sessionId: String,
    val durationMs: Long,
    val atEpochMs: Long,
)

/**
 * Record progression (plan F22): ordered by time, list when each distance record is first beaten. Current
 * record = the minimum over all counted sessions. Excluded sessions are skipped.
 */
object RecordProgression {

    fun compute(sessions: List<SessionEfforts>, key: BestEffortKey): List<RecordEvent> {
        val ordered = sessions.filterNot { it.excluded }.sortedBy { it.atEpochMs }
        var best = Long.MAX_VALUE
        val events = mutableListOf<RecordEvent>()
        ordered.forEach { session ->
            val effort = session.efforts.firstOrNull { it.key == key } ?: return@forEach
            if (effort.durationMs < best) {
                best = effort.durationMs
                events += RecordEvent(key, session.sessionId, effort.durationMs, session.atEpochMs)
            }
        }
        return events
    }

    /** New records across all keys, in the order they were set (for the "recent records" list). */
    fun recent(sessions: List<SessionEfforts>, limit: Int = 5): List<RecordEvent> {
        val all = BestEffortKey.entries.flatMap { compute(sessions, it) }.sortedBy { it.atEpochMs }
        return all.takeLast(limit).reversed()
    }
}
