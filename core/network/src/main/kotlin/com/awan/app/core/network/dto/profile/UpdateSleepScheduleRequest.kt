package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to update sleep schedule.
 * PATCH v1/users/me/preferences/sleep-schedule
 */
@Serializable
data class UpdateSleepScheduleRequest(
    @SerialName("wakeupTime") val wakeupTime: String, // "HH:mm:ss"
    @SerialName("sleepTime") val sleepTime: String    // "HH:mm:ss"
)
