package com.awan.app.core.data.sync

/**
 * TTL (Time-To-Live) durations for each resource type.
 * Values are in milliseconds and represent how long cached data
 * remains valid before a fresh API fetch is needed.
 */
object SyncTtl {
    /** Schedule data (tasks, sessions, cached dates): 15 minutes. */
    const val SCHEDULE_TTL_MS = 15 * 60 * 1000L

    /** Goals: 30 minutes. */
    const val GOALS_TTL_MS = 30 * 60 * 1000L

    /** User profile and preferences: 1 hour. */
    const val PROFILE_TTL_MS = 60 * 60 * 1000L

    /** Categories: 1 hour. */
    const val CATEGORIES_TTL_MS = 60 * 60 * 1000L

    /** Templates and zones: 1 hour. */
    const val TEMPLATES_TTL_MS = 60 * 60 * 1000L

    /** Store items and inventory: 1 hour. */
    const val STORE_TTL_MS = 60 * 60 * 1000L

    /** Computes the expiry timestamp given a TTL duration. */
    fun computeExpiry(ttlMs: Long, now: Long = System.currentTimeMillis()): Long = now + ttlMs
}
