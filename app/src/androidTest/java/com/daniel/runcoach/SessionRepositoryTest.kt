package com.daniel.runcoach

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.data.SettingsRepository
import com.daniel.runcoach.engine.gps.CalibrationCalculator
import com.daniel.runcoach.engine.result.BestEffort
import com.daniel.runcoach.engine.result.BestEffortKey
import com.daniel.runcoach.engine.result.KmSplit
import com.daniel.runcoach.engine.result.SegmentResultData
import com.daniel.runcoach.engine.result.SegmentStatus
import com.daniel.runcoach.engine.result.SessionResultsData
import com.daniel.runcoach.engine.voice.VoiceLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** P2.5 AC: session saved with results, default name on empty, note limit, and orphan-free delete. */
@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository = SessionRepository(context)

    private fun sampleResults() = SessionResultsData(
        segments = listOf(SegmentResultData(1, "Run", "RUN", "distance", 5000.0, 420, 5000.0, 2_100_000L, 420.0, 0L, "PASS")),
        kmSplits = listOf(KmSplit(1, false, 1000.0, 420_000L, 420.0, 420, 0L, SegmentStatus.PASS)),
        bestEfforts = listOf(BestEffort(BestEffortKey.K1, 420_000L, 0.0)),
    )

    private fun sampleSession(id: String) = SessionEntity(
        id = id,
        templateId = null,
        templateNameSnapshot = "Test",
        workoutSnapshotJson = "{}",
        name = "Seed",
        nameIsCustom = false,
        note = null,
        rpe = null,
        excludeFromRecords = false,
        startedAtEpochMs = System.currentTimeMillis(),
        zoneId = "Asia/Jakarta",
        endedAtEpochMs = System.currentTimeMillis(),
        elapsedMs = 2_100_000L,
        movingMs = 2_100_000L,
        pausedMs = 0L,
        totalDistanceM = 5000.0,
        avgPaceSecPerKm = 420.0,
        segPass = 1,
        segFail = 0,
        segBorderline = 0,
        segNoTarget = 0,
        planCompleted = true,
        goalReached = false,
        distanceCalibrationFactorUsed = 1.0,
        droppedSampleCount = 0,
        updatedAt = System.currentTimeMillis(),
    )

    @Test
    fun saveUpdateDefaultNameAndDeleteWithoutOrphans() = runBlocking {
        val id = "sess-${System.nanoTime()}"
        repository.saveSession(sampleSession(id), sampleResults())

        assertNotNull(repository.get(id))
        assertEquals(1, repository.segments(id).size)
        assertEquals(1, repository.splits(id).size)
        assertEquals(1, repository.bestEfforts(id).size)

        // Empty name -> default, not custom.
        repository.updateForm(id, "", null, null, false, VoiceLanguage.ID, System.currentTimeMillis())
        val renamed = repository.get(id)!!
        assertFalse(renamed.nameIsCustom)
        assertTrue(renamed.name.isNotBlank())

        // Note over 500 is clamped.
        repository.updateForm(id, "My run", "x".repeat(600), 5, false, VoiceLanguage.ID, System.currentTimeMillis())
        assertEquals(500, repository.get(id)!!.note!!.length)

        repository.delete(id)
        assertNull(repository.get(id))
        assertTrue(repository.segments(id).isEmpty())
        assertTrue(repository.splits(id).isEmpty())
        assertTrue(repository.bestEfforts(id).isEmpty())
    }

    @Test
    fun noteLimitIsFiveHundred() {
        assertTrue(SessionRepository.noteWithinLimit("a".repeat(500)))
        assertFalse(SessionRepository.noteWithinLimit("a".repeat(501)))
    }

    /** P3.7 AC: the new factor applies to the next session only; a saved session keeps the factor it used. */
    @Test
    fun calibrationDoesNotChangeOldSessions() {
        runBlocking {
            val settings = SettingsRepository(context)
            val id = "cal-${System.nanoTime()}"
            repository.saveSession(sampleSession(id).copy(distanceCalibrationFactorUsed = 1.02), sampleResults())

            val factor = CalibrationCalculator.factorFor(5100.0, 5000.0, 1.02)
            settings.setCalibrationFactor(factor)
            assertEquals(factor, settings.settings.first().calibrationFactor, 1e-9)

            assertEquals(1.02, repository.get(id)!!.distanceCalibrationFactorUsed, 1e-9)

            settings.setCalibrationFactor(1.0)
            repository.delete(id)
        }
    }
}
