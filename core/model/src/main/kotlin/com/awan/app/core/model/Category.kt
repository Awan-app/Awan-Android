package com.awan.app.core.model

/**
 * What a task *is about* — Study, Work, "Afternoon Work". A [DayZone] is a window of one day handed
 * to a category, so the category is the part that outlives any particular date: a task carries a
 * category, a session carries the zone it was placed in.
 */
data class Category(
    val id: String,
    val name: String,
)
