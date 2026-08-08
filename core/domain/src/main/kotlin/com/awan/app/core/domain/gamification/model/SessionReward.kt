package com.awan.app.core.domain.gamification.model

/**
 * What a session completion actually earned.
 *
 * A session pays out only the first time it is completed, so both halves are nullable: the mapper
 * turns the wire format's `awarded: false` / `updated: false` into `null` here. That way "did the
 * user earn anything?" is a null check no caller can forget, rather than a flag every caller has to
 * remember to read.
 */
data class SessionReward(
    val points: PointsAward? = null,
    val streak: StreakChange? = null,
) {
    val isEmpty: Boolean get() = points == null && streak == null
}

data class PointsAward(
    val amount: Int,
    val oldValue: Int,
    val newValue: Int,
)

data class StreakChange(
    val oldValue: Int,
    val newValue: Int,
    val maxStreakBroken: Boolean,
    val maxStreakNew: Int,
)
