package com.awan.app.core.network.dto.gamification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GamificationProgressDto(
    @SerialName("points") val points: Int = 0,
    @SerialName("streak") val streak: Int = 0,
    @SerialName("maxStreak") val maxStreak: Int = 0,
)
