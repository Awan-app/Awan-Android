package com.awan.app.core.data.gamification.mapper

import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.model.PointsAward
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.gamification.model.StreakChange
import com.awan.app.core.domain.gamification.model.WheelConfig
import com.awan.app.core.domain.gamification.model.WheelSegment
import com.awan.app.core.domain.gamification.model.WheelSpinResult
import com.awan.app.core.domain.gamification.model.WonItem
import com.awan.app.core.network.dto.gamification.GamificationProgressDto
import com.awan.app.core.network.dto.gamification.PAYOUT_ITEM
import com.awan.app.core.network.dto.gamification.PointsRewardDto
import com.awan.app.core.network.dto.gamification.RewardDto
import com.awan.app.core.network.dto.gamification.StreakRewardDto
import com.awan.app.core.network.dto.gamification.WheelConfigDto
import com.awan.app.core.network.dto.gamification.WheelSegmentDto
import com.awan.app.core.network.dto.gamification.WheelSpinDto
import com.awan.app.core.network.dto.store.StoreItemDto
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun GamificationProgressDto.toDomain(): GamificationProgress = GamificationProgress(
    points = points,
    streak = streak,
    maxStreak = maxStreak,
)

fun RewardDto?.toDomain(): SessionReward = SessionReward(
    points = this?.points.toDomain(),
    streak = this?.streak.toDomain(),
)

/** `awarded: false` means nothing was earned, whatever the amounts say — so it becomes `null`. */
private fun PointsRewardDto?.toDomain(): PointsAward? =
    if (this == null || !awarded) null
    else PointsAward(amount = amount, oldValue = oldValue, newValue = newValue)

/** Likewise `updated: false` — the values come back unchanged on a re-completion. */
private fun StreakRewardDto?.toDomain(): StreakChange? =
    if (this == null || !updated) null
    else StreakChange(
        oldValue = oldValue,
        newValue = newValue,
        maxStreakBroken = maxStreakBroken,
        maxStreakNew = maxStreakNew,
    )

fun WheelConfigDto.toDomain(): WheelConfig = WheelConfig(
    segments = segments.map { it.toDomain() },
    claimedToday = claimedToday,
)

private fun WheelSegmentDto.toDomain(): WheelSegment = WheelSegment(
    id = segmentId,
    coins = coins,
    isItem = payoutType == PAYOUT_ITEM,
)

/**
 * Branches on the response's own `payoutType`, never on the configured wedge's: when the user
 * already owns every item the server silently resolves the item wedge to a coin payout, and only
 * the response reflects that. The item is dropped if the payout says ITEM but no item came back,
 * so a malformed response degrades to a coin win rather than an empty celebration.
 */
fun WheelSpinDto.toDomain(): WheelSpinResult = WheelSpinResult(
    segmentId = segmentId,
    coins = coinsAwarded,
    newBalance = newBalance,
    item = if (payoutType == PAYOUT_ITEM) item?.toWonItem() else null,
)

private fun StoreItemDto.toWonItem(): WonItem? {
    val itemName = name?.takeIf { it.isNotBlank() } ?: return null
    return WonItem(id = id, name = itemName, imageUrl = image?.takeIf { it.isNotBlank() })
}

/** Unparseable dates are dropped rather than failing the whole month's calendar. */
fun List<String>.toActivityDates(): Set<LocalDate> = mapNotNullTo(mutableSetOf()) { raw ->
    try {
        LocalDate.parse(raw.trim())
    } catch (_: DateTimeParseException) {
        null
    }
}
