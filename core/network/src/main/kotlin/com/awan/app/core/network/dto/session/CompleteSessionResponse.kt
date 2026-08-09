package com.awan.app.core.network.dto.session

import com.awan.app.core.network.dto.gamification.RewardDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The response to completing a session.
 *
 * [reward] is present on every completion, including repeats — its `awarded`/`updated` flags are what
 * say whether anything was actually earned. The mapper turns an unawarded block into a null domain
 * reward, so nothing downstream has to remember to check.
 */
@Serializable
data class CompleteSessionResponse(
    @SerialName("session") val session: SessionDto,
    @SerialName("reward") val reward: RewardDto? = null,
)
