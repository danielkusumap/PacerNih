package com.daniel.runcoach.service

import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.engine.result.SessionResultsData

/** A finished run the user has not saved yet (S06 decides). Kept in memory; track points stay in the DB. */
data class PendingSession(
    val entity: SessionEntity,
    val results: SessionResultsData,
)

/**
 * Hand-off from the service to S06. The run is **not** written to the database until the user taps Save; until
 * then the computed session and results live here, and the track points stay in the DB so they can be kept or
 * discarded.
 */
object FinishedSessionHolder {
    var sessionId: String? = null
    var pending: PendingSession? = null

    fun clear() {
        sessionId = null
        pending = null
    }
}
