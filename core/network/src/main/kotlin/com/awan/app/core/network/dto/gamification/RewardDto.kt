package com.awan.app.core.network.dto.gamification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The reward block returned alongside a session completion.
 *
 * `awarded` / `updated` are the authoritative flags: re-completing a session still returns a full
 * block, with the flag false and the values unchanged. Callers must never infer an award from the
 * amounts alone — the mapper collapses an unawarded block to `null` so nothing downstream can.
 */
@Serializable
data class RewardDto(
    @SerialName("points") val points: PointsRewardDto? = null,
    @SerialName("streak") val streak: StreakRewardDto? = null,
)

@Serializable
data class PointsRewardDto(
    @SerialName("awarded") val awarded: Boolean = false,
    @SerialName("amount") val amount: Int = 0,
    @SerialName("oldValue") val oldValue: Int = 0,
    @SerialName("newValue") val newValue: Int = 0,
)

@Serializable
data class StreakRewardDto(
    @SerialName("updated") val updated: Boolean = false,
    @SerialName("oldValue") val oldValue: Int = 0,
    @SerialName("newValue") val newValue: Int = 0,
    @SerialName("maxStreakBroken") val maxStreakBroken: Boolean = false,
    @SerialName("maxStreakOld") val maxStreakOld: Int = 0,
    @SerialName("maxStreakNew") val maxStreakNew: Int = 0,
)
