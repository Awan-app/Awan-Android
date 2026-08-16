package com.awan.app.core.database.model

/**
 * A session joined with its parent task's title.
 *
 * Sessions carry no title of their own, and the notification scheduler needs one for every session
 * in its window — reading them back through `TaskDao.getTask` would be an N+1 on a path that runs on
 * every write to the sessions table.
 */
data class UpcomingSessionRow(
    val id: String,
    val taskId: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val zoneId: String?,
)
