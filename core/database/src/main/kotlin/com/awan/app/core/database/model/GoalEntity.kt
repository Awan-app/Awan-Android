package com.awan.app.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local representation of a user goal.
 *
 * Maps to [GoalInfoResponse] from the Awan API.
 * Tasks belonging to a goal are stored in [TaskEntity] with a [TaskEntity.goalId]
 * foreign key; they are NOT embedded here to keep the goal row lightweight.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    /**
     * Goal lifecycle status.
     * One of: `ACTIVE`, `ACHIEVED`.
     */
    val status: String,
    /** Optional target-completion date stored as `YYYY-MM-DD` string. */
    val targetDate: String?,
    /** ISO-8601 creation instant stored as a String (e.g. "2026-07-19T10:30:00Z"). */
    val createdAt: String,
    /**
     * Whether this is the special system-managed Inbox goal.
     * The Inbox goal cannot be renamed or deleted.
     */
    val isInbox: Boolean,
)
