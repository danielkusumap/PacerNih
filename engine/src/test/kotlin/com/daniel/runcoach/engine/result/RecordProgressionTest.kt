package com.daniel.runcoach.engine.result

import kotlin.test.Test
import kotlin.test.assertEquals

class RecordProgressionTest {

    private fun best(key: BestEffortKey, ms: Long) = BestEffort(key, ms, 0.0)

    @Test
    fun recordsAreSetWhenBeatenOverTime() {
        val sessions = listOf(
            SessionEfforts("a", 1_000, listOf(best(BestEffortKey.K1, 400_000))),
            SessionEfforts("b", 2_000, listOf(best(BestEffortKey.K1, 390_000))),
            SessionEfforts("c", 3_000, listOf(best(BestEffortKey.K1, 395_000))), // slower, no record
        )
        val events = RecordProgression.compute(sessions, BestEffortKey.K1)
        assertEquals(listOf("a", "b"), events.map { it.sessionId })
        assertEquals(390_000L, events.last().durationMs)
    }

    @Test
    fun excludedSessionsAreSkipped() {
        val sessions = listOf(
            SessionEfforts("a", 1_000, listOf(best(BestEffortKey.K5, 1_500_000))),
            SessionEfforts("b", 2_000, listOf(best(BestEffortKey.K5, 1_400_000)), excluded = true),
        )
        val events = RecordProgression.compute(sessions, BestEffortKey.K5)
        assertEquals(listOf("a"), events.map { it.sessionId })
    }

    @Test
    fun deletingTheRecordHolderLowersTheCurrentRecord() {
        // Current record = min over remaining sessions; recomputing from the (fewer) sessions gives the next.
        val withHolder = listOf(
            SessionEfforts("a", 1_000, listOf(best(BestEffortKey.K1, 400_000))),
            SessionEfforts("b", 2_000, listOf(best(BestEffortKey.K1, 390_000))),
        )
        assertEquals(390_000L, RecordProgression.compute(withHolder, BestEffortKey.K1).last().durationMs)
        val afterDelete = withHolder.filterNot { it.sessionId == "b" }
        assertEquals(400_000L, RecordProgression.compute(afterDelete, BestEffortKey.K1).last().durationMs)
    }

    @Test
    fun recentListsLatestEvents() {
        val sessions = (1..6).map { index ->
            SessionEfforts("s$index", index * 1000L, listOf(best(BestEffortKey.K1, 500_000L - index * 1000L)))
        }
        val recent = RecordProgression.recent(sessions, limit = 5)
        assertEquals(5, recent.size)
        assertEquals("s6", recent.first().sessionId)
    }
}
