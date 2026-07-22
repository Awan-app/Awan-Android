package com.awan.app.core.network.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to update user's name.
 * PATCH v1/users/me/profile/name
 */
@Serializable
data class UpdateNameRequest(
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String
)

/**
 * Request to update user's birth date.
 * PATCH v1/users/me/profile/birth-date
 */
@Serializable
data class UpdateBirthDateRequest(
    @SerialName("birthDate") val birthDate: String // "yyyy-MM-dd"
)

/**
 * Request for partial profile update.
 * PATCH v1/users/me
 */
@Serializable
data class UpdateProfilePartialRequest(
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("preferredSessionDuration") val preferredSessionDuration: Int? = null,
    @SerialName("bufferBetweenSessions") val bufferBetweenSessions: Int? = null,
    @SerialName("wakeupTime") val wakeupTime: String? = null,
    @SerialName("sleepTime") val sleepTime: String? = null,
    @SerialName("schedulingType") val schedulingType: String? = null
)

/**
 * Request to update user's timezone.
 * PATCH v1/users/me/preferences/timezone
 */
@Serializable
data class UpdateTimezoneRequest(
    @SerialName("timezone") val timezone: String
)

/**
 * Request to update session settings.
 * PATCH v1/users/me/preferences/session
 */
@Serializable
data class UpdateSessionSettingsRequest(
    @SerialName("preferredSessionDuration") val preferredSessionDuration: Int,
    @SerialName("bufferBetweenSessions") val bufferBetweenSessions: Int
)

/**
 * Request to update sleep schedule.
 * PATCH v1/users/me/preferences/sleep-schedule
 */
@Serializable
data class UpdateSleepScheduleRequest(
    @SerialName("wakeupTime") val wakeupTime: String, // "HH:mm:ss"
    @SerialName("sleepTime") val sleepTime: String    // "HH:mm:ss"
)

/**
 * Request to update scheduling type.
 * PATCH v1/users/me/preferences/scheduling-type
 */
@Serializable
data class UpdateSchedulingTypeRequest(
    @SerialName("schedulingType") val schedulingType: String // e.g. "BALANCED"
)

/**
 * Request to award points.
 * PATCH v1/users/me/points/award
 */
@Serializable
data class AwardPointsRequest(
    @SerialName("points") val points: Int
)

/**
 * Request to deduct points.
 * PATCH v1/users/me/points/deduct
 */
@Serializable
data class DeductPointsRequest(
    @SerialName("points") val points: Int
)
