package com.daniel.runcoach.service

/**
 * Hand-off from the service to S06: the id of the finished run. The run is stored as a **draft** (excluded from
 * history and records) until the user saves it in S06, so it survives the app being killed.
 */
object FinishedSessionHolder {
    var sessionId: String? = null

    fun clear() {
        sessionId = null
    }
}
