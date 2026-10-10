package com.daniel.runcoach.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved workout template (plan section 7). No foreign keys; safe to edit/delete without touching history. */
@Entity(tableName = "WorkoutTemplate")
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val json: String,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long?,
)
