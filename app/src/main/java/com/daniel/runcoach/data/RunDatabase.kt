package com.daniel.runcoach.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TrackPointEntity::class, ActiveRunSnapshotEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RunDatabase : RoomDatabase() {

    abstract fun runDao(): RunDao

    companion object {
        @Volatile
        private var instance: RunDatabase? = null

        fun get(context: Context): RunDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, RunDatabase::class.java, "runcoach.db")
                    .build()
                    .also { instance = it }
            }
    }
}
