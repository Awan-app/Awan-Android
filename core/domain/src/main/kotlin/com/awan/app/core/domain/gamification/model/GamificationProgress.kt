package com.awan.app.core.domain.gamification.model

/**
 * The user's wallet and streak standing. The server owns every one of these numbers — the client
 * never computes them, it only reads them back after a session completion or a wheel spin.
 */
data class GamificationProgress(
    val points: Int = 0,
    val streak: Int = 0,
    val maxStreak: Int = 0,
)
