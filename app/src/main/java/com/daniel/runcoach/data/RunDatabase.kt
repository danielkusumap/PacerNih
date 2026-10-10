package com.daniel.runcoach.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TrackPointEntity::class, ActiveRunSnapshotEntity::class, WorkoutTemplateEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class RunDatabase : RoomDatabase() {

    abstract fun runDao(): RunDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao

    companion object {
        @Volatile
        private var instance: RunDatabase? = null

        /** Adds the WorkoutTemplate table (plan section 7). Explicit migration, no destructive fallback. */
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

        fun get(context: Context): RunDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, RunDatabase::class.java, "runcoach.db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
