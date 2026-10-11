package com.daniel.runcoach.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackPointEntity::class,
        ActiveRunSnapshotEntity::class,
        WorkoutTemplateEntity::class,
        SessionEntity::class,
        SegmentResultEntity::class,
        KmSplitEntity::class,
        BestEffortEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class RunDatabase : RoomDatabase() {

    abstract fun runDao(): RunDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var instance: RunDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `WorkoutTemplate` (" +
                        "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `json` TEXT NOT NULL, " +
                        "`isBuiltIn` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, `lastUsedAt` INTEGER, PRIMARY KEY(`id`))",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Session` (" +
                        "`id` TEXT NOT NULL, `templateId` TEXT, `templateNameSnapshot` TEXT NOT NULL, " +
                        "`workoutSnapshotJson` TEXT NOT NULL, `name` TEXT NOT NULL, `nameIsCustom` INTEGER NOT NULL, " +
                        "`note` TEXT, `rpe` INTEGER, `excludeFromRecords` INTEGER NOT NULL, " +
                        "`startedAtEpochMs` INTEGER NOT NULL, `zoneId` TEXT NOT NULL, `endedAtEpochMs` INTEGER NOT NULL, " +
                        "`elapsedMs` INTEGER NOT NULL, `movingMs` INTEGER NOT NULL, `pausedMs` INTEGER NOT NULL, " +
                        "`totalDistanceM` REAL NOT NULL, `avgPaceSecPerKm` REAL, `segPass` INTEGER NOT NULL, " +
                        "`segFail` INTEGER NOT NULL, `segBorderline` INTEGER NOT NULL, `segNoTarget` INTEGER NOT NULL, " +
                        "`planCompleted` INTEGER NOT NULL, `goalReached` INTEGER NOT NULL, " +
                        "`distanceCalibrationFactorUsed` REAL NOT NULL, `droppedSampleCount` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `SegmentResult` (" +
                        "`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` TEXT NOT NULL, `idx` INTEGER NOT NULL, " +
                        "`label` TEXT NOT NULL, `kind` TEXT NOT NULL, `limitType` TEXT NOT NULL, `limitValue` REAL NOT NULL, " +
                        "`targetPace` INTEGER, `distanceM` REAL NOT NULL, `durationMs` INTEGER NOT NULL, `avgPace` REAL, " +
                        "`deltaSec` INTEGER, `status` TEXT NOT NULL, `kmDetailJson` TEXT)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_SegmentResult_sessionId` ON `SegmentResult` (`sessionId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `KmSplit` (" +
                        "`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` TEXT NOT NULL, `idx` INTEGER NOT NULL, " +
                        "`distanceM` REAL NOT NULL, `durationMs` INTEGER NOT NULL, `paceSecPerKm` REAL, `targetPace` INTEGER, " +
                        "`deltaSec` INTEGER, `status` TEXT)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_KmSplit_sessionId` ON `KmSplit` (`sessionId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `BestEffort` (" +
                        "`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` TEXT NOT NULL, " +
                        "`distanceKey` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `startOffsetM` REAL NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_BestEffort_sessionId` ON `BestEffort` (`sessionId`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `Session` ADD COLUMN `pending` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `SegmentResult` ADD COLUMN `targetBound` TEXT NOT NULL DEFAULT 'MAX'")
            }
        }

        fun get(context: Context): RunDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, RunDatabase::class.java, "runcoach.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }

        /** Closes the database and forgets the singleton, so the file can be copied for backup/restore. */
        fun close() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
