package com.awan.app.core.network.dto.gamification

import com.awan.app.core.network.dto.store.StoreItemDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WheelConfigDto(
    @SerialName("segments") val segments: List<WheelSegmentDto> = emptyList(),
    @SerialName("claimedToday") val claimedToday: Boolean = false,
    @SerialName("lastClaim") val lastClaim: WheelClaimDto? = null,
)

@Serializable
data class WheelSegmentDto(
    @SerialName("segmentId") val segmentId: String,
    @SerialName("coins") val coins: Int = 0,
    @SerialName("payoutType") val payoutType: String = PAYOUT_COINS,
)

@Serializable
data class WheelClaimDto(
    @SerialName("segmentId") val segmentId: String? = null,
    @SerialName("coinsAwarded") val coinsAwarded: Int = 0,
    @SerialName("itemId") val itemId: String? = null,
    @SerialName("itemName") val itemName: String? = null,
    @SerialName("claimDate") val claimDate: String? = null,
    @SerialName("claimedAt") val claimedAt: String? = null,
)

/**
 * A spin result. Branch on this [payoutType], never on the configured wedge's: when the user already
 * owns every item the server resolves the item wedge to a coin payout, and only the response is right.
 */
@Serializable
data class WheelSpinDto(
    @SerialName("segmentId") val segmentId: String,
    @SerialName("payoutType") val payoutType: String = PAYOUT_COINS,
    @SerialName("coinsAwarded") val coinsAwarded: Int = 0,
    @SerialName("newBalance") val newBalance: Int = 0,
    @SerialName("item") val item: StoreItemDto? = null,
)

const val PAYOUT_COINS = "COINS"
const val PAYOUT_ITEM = "ITEM"
