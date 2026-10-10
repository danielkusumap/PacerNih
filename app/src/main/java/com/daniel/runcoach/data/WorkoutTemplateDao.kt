package com.daniel.runcoach.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Query("SELECT * FROM WorkoutTemplate ORDER BY lastUsedAt IS NULL, lastUsedAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM WorkoutTemplate ORDER BY lastUsedAt IS NULL, lastUsedAt DESC, createdAt DESC")
    suspend fun all(): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM WorkoutTemplate WHERE id = :id")
    suspend fun byId(id: String): WorkoutTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: WorkoutTemplateEntity)

    /** Deletes only if not built in; returns the number of rows removed. */
    @Query("DELETE FROM WorkoutTemplate WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteIfNotBuiltIn(id: String): Int

    @Query("UPDATE WorkoutTemplate SET lastUsedAt = :atMs WHERE id = :id")
    suspend fun markUsed(id: String, atMs: Long)
}
