package com.awan.app.core.domain.gamification.model

/**
 * A reward worth celebrating on screen, whatever produced it — a completed session or a wheel spin.
 *
 * These are emitted by the data layer rather than by whichever ViewModel happened to trigger the
 * earning, so the celebration plays over any screen the user is on when it lands.
 */
enum class RewardSource {
    SESSION_COMPLETION,
    DAILY_WHEEL,
    OTHER,
}

sealed interface RewardEvent {

    data class Points(
        val amount: Int,
        val newTotal: Int,
        val comboCount: Int = 1,
        val source: RewardSource = RewardSource.SESSION_COMPLETION,
    ) : RewardEvent

    data class Streak(
        val oldValue: Int,
        val newValue: Int,
        val maxStreakBroken: Boolean,
        val maxStreakNew: Int,
    ) : RewardEvent

    data class Item(
        val name: String,
        val imageUrl: String?,
    ) : RewardEvent
}
