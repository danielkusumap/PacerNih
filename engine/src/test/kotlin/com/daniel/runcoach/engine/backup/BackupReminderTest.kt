package com.daniel.runcoach.engine.backup

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** P3.8 AC: the reminder stays silent when off and fires exactly once the interval has elapsed. */
class BackupReminderTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_700_000_000_000L

    @Test
    fun offNeverReminds() {
        assertFalse(BackupReminder.isDue(null, now, reminderDays = 0))
        assertFalse(BackupReminder.isDue(now - 400 * day, now, reminderDays = 0))
    }

    @Test
    fun firesOnlyAfterTheInterval() {
        val last = now - 14 * day
        assertFalse(BackupReminder.isDue(last + 1, now, reminderDays = 14))
        assertTrue(BackupReminder.isDue(last, now, reminderDays = 14))
        assertTrue(BackupReminder.isDue(last - day, now, reminderDays = 14))
    }

    @Test
    fun noBackupYetIsDueWhenEnabled() {
        assertTrue(BackupReminder.isDue(null, now, reminderDays = 30))
    }
}
