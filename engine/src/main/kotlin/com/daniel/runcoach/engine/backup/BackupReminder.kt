package com.daniel.runcoach.engine.backup

/**
 * Backup reminder rule (plan S13, P3.8). Pure so the UI and the tests share one definition of "overdue".
 *
 * The reminder is off when [reminderDays] is 0. With no backup yet it is always due; otherwise it is due once
 * the interval has fully elapsed since the last backup.
 */
object BackupReminder {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun isDue(lastBackupEpochMs: Long?, nowEpochMs: Long, reminderDays: Int): Boolean {
        if (reminderDays <= 0) return false
        val last = lastBackupEpochMs ?: return true
        return nowEpochMs - last >= reminderDays * DAY_MS
    }
}
