package com.daniel.runcoach.data

import android.content.Context
import com.daniel.runcoach.engine.voice.VoiceLanguage
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

sealed interface BackupResult {
    data object Success : BackupResult
    data class Failure(val message: String) : BackupResult
}

data class BackupSummary(val sessionCount: Int, val templateCount: Int, val databaseBytes: Long)

/**
 * Backup and restore (plan P3.5): one file holding the database plus the settings. Restoring rejects a file
 * that is not a valid backup with a clear message. Uses streams so the UI can plug in SAF URIs and tests can
 * use plain files.
 */
class BackupRepository(context: Context) {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun summary(): BackupSummary {
        val database = RunDatabase.get(appContext)
        val sessionCount = database.sessionDao().countSessions()
        val templateCount = database.workoutTemplateDao().count()
        val size = appContext.getDatabasePath(DB_NAME).let { if (it.exists()) it.length() else 0L }
        return BackupSummary(sessionCount, templateCount, size)
    }

    suspend fun backupTo(output: OutputStream) {
        val settings = SettingsRepository(appContext).settings.first()
        RunDatabase.close() // no open handles while copying the file
        val databaseFile = appContext.getDatabasePath(DB_NAME)

        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_DB))
            databaseFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(ENTRY_SETTINGS))
            zip.write(json.encodeToString(AppSettings.serializer(), settings).toByteArray())
            zip.closeEntry()
        }
    }

    suspend fun restoreFrom(input: InputStream): BackupResult {
        val entries = try {
            readEntries(input.readBytes())
        } catch (e: Exception) {
            return BackupResult.Failure("File cadangan rusak atau tidak dikenali.")
        }
        val databaseBytes = entries[ENTRY_DB]
            ?: return BackupResult.Failure("File cadangan tidak berisi database.")

        RunDatabase.close()
        val databaseFile = appContext.getDatabasePath(DB_NAME)
        databaseFile.parentFile?.mkdirs()
        databaseFile.outputStream().use { it.write(databaseBytes) }
        File(databaseFile.path + "-wal").delete()
        File(databaseFile.path + "-shm").delete()

        entries[ENTRY_SETTINGS]?.let { bytes ->
            runCatching { json.decodeFromString(AppSettings.serializer(), String(bytes)) }
                .getOrNull()
                ?.let { SettingsRepository(appContext).setAll(it) }
        }
        return BackupResult.Success
    }

    suspend fun deleteAllData() {
        RunDatabase.get(appContext).clearAllTables()
        SettingsRepository(appContext).reset()
    }

    private fun readEntries(bytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                result[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }

    private companion object {
        const val DB_NAME = "runcoach.db"
        const val ENTRY_DB = "runcoach.db"
        const val ENTRY_SETTINGS = "settings.json"
    }
}
