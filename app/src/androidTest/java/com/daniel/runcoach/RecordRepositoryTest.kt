package com.daniel.runcoach

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.data.RecordRepository
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.engine.result.BestEffortKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** P2.9 AC: records follow stored efforts, deletion lowers them without rescanning, excluded sessions ignored. */
@RunWith(AndroidJUnit4::class)
class RecordRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sessions = SessionRepository(context)
    private val records = RecordRepository(context)

    @Before
    fun clean() {
        runBlocking { sessions.allSessions().forEach { sessions.delete(it.id) } }
    }

    @Test
    fun deletionLowersTheRecordAndUntraveledDistanceIsNull() = runBlocking {
        val idA = "ra-${System.nanoTime()}"
        val idB = "rb-${System.nanoTime()}"
        sessions.saveSession(SessionFixtures.session(idA, "A", startedAt = 1_000), SessionFixtures.resultsWithK1(400_000))
        sessions.saveSession(SessionFixtures.session(idB, "B", startedAt = 2_000), SessionFixtures.resultsWithK1(390_000))

        var current = records.currentRecords()
        assertEquals(390_000L, current[BestEffortKey.K1]!!.durationMs)
        assertEquals(idB, current[BestEffortKey.K1]!!.sessionId)

        // Deleting the holder lowers the record to the next best, without rescanning any track.
        sessions.delete(idB)
        current = records.currentRecords()
        assertEquals(400_000L, current[BestEffortKey.K1]!!.durationMs)
        assertEquals(idA, current[BestEffortKey.K1]!!.sessionId)

        // A distance never covered shows no record.
        assertNull(current[BestEffortKey.K5])

        sessions.delete(idA)
    }

    @Test
    fun excludedSessionIsNotCounted() = runBlocking {
        val idA = "rc-${System.nanoTime()}"
        val idB = "rd-${System.nanoTime()}"
        sessions.saveSession(SessionFixtures.session(idA, "A", startedAt = 1_000), SessionFixtures.resultsWithK1(500_000))
        sessions.saveSession(
            SessionFixtures.session(idB, "B", exclude = true, startedAt = 2_000),
            SessionFixtures.resultsWithK1(450_000),
        )

        val current = records.currentRecords()
        assertEquals(500_000L, current[BestEffortKey.K1]!!.durationMs)
        assertEquals(idA, current[BestEffortKey.K1]!!.sessionId)

        sessions.delete(idA)
        sessions.delete(idB)
    }
}
