package com.awan.app.core.domain.gamification.model

/**
 * The daily wheel's layout. [segments] arrive in wheel order and that order is stable across calls,
 * so wedge *i* is always the same wedge and the layout can be measured once.
 */
data class WheelConfig(
    val segments: List<WheelSegment>,
    val claimedToday: Boolean,
)

data class WheelSegment(
    val id: String,
    val coins: Int,
    val isItem: Boolean,
)

/**
 * The outcome of a spin, decided entirely by the server. [segmentId] says which wedge to land on;
 * [item] is non-null only when an item was actually won.
 */
data class WheelSpinResult(
    val segmentId: String,
    val coins: Int,
    val newBalance: Int,
    val item: WonItem?,
)

data class WonItem(
    val id: String,
    val name: String,
    val imageUrl: String?,
)
