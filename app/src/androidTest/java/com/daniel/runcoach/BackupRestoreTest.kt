package com.daniel.runcoach

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.data.AppTheme
import com.daniel.runcoach.data.BackupRepository
import com.daniel.runcoach.data.BackupResult
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.data.SettingsRepository
import com.daniel.runcoach.data.WorkoutTemplateRepository
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** P3.5 AC: backup then restore returns history, templates and settings; a corrupt file is rejected. */
@RunWith(AndroidJUnit4::class)
class BackupRestoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun backupRestoreRoundTripAndCorruptRejection() {
        runBlocking {
        val sessions = SessionRepository(context)
        val templates = WorkoutTemplateRepository(context)
        val settings = SettingsRepository(context)
        val backup = BackupRepository(context)
        // Clean slate.
        sessions.allSessions().forEach { sessions.delete(it.id) }
        templates.all().filterNot { it.isBuiltIn }.forEach { templates.delete(it.id) }

        // Seed data.
        val sessionId = "bk-${System.nanoTime()}"
        sessions.saveSession(SessionFixtures.session(sessionId, "Backup Test"), SessionFixtures.results())
        templates.create(
            Workout("bk-t", "Backup Template", listOf(Segment("Run", SegmentKind.RUN, Limit.Distance(1000), 420))),
        )
        settings.setTheme(AppTheme.DARK)

        // Backup to a temp file.
        val file = File(context.cacheDir, "backup-test.zip")
        file.outputStream().use { backup.backupTo(it) }
        assertTrue(file.length() > 0)

        // Wipe, then restore.
        backup.deleteAllData()
        assertNull(sessions.get(sessionId))

        val result = file.inputStream().use { backup.restoreFrom(it) }
        assertTrue("restore success", result is BackupResult.Success)
        assertNotNull("session restored", sessions.get(sessionId))
        assertTrue("template restored", templates.all().any { it.name == "Backup Template" })
        assertEquals(AppTheme.DARK, settings.settings.first().theme)

        // Corrupt file rejected with a message, data intact.
        val corrupt = backup.restoreFrom(ByteArrayInputStream("not a zip".toByteArray()))
        assertTrue(corrupt is BackupResult.Failure)
        assertNotNull(sessions.get(sessionId))

        // Cleanup.
        sessions.delete(sessionId)
        templates.all().filterNot { it.isBuiltIn }.forEach { templates.delete(it.id) }
        settings.setTheme(AppTheme.SYSTEM)
        }
    }
}
