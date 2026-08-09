package com.awan.app.core.data.gamification

import com.awan.app.core.data.gamification.mapper.toActivityDates
import com.awan.app.core.data.gamification.mapper.toDomain
import com.awan.app.core.network.dto.gamification.PAYOUT_COINS
import com.awan.app.core.network.dto.gamification.PAYOUT_ITEM
import com.awan.app.core.network.dto.gamification.PointsRewardDto
import com.awan.app.core.network.dto.gamification.RewardDto
import com.awan.app.core.network.dto.gamification.StreakRewardDto
import com.awan.app.core.network.dto.gamification.WheelSpinDto
import com.awan.app.core.network.dto.store.StoreItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GamificationMapperTest {

    @Test
    fun `an unawarded points block maps to no award even though it carries values`() {
        val reward = RewardDto(
            points = PointsRewardDto(
                awarded = false,
                amount = 25,
                oldValue = 150,
                newValue = 175,
            ),
        ).toDomain()

        assertNull(reward.points)
    }

    @Test
    fun `an unchanged streak block maps to no change even though it carries values`() {
        val reward = RewardDto(
            streak = StreakRewardDto(
                updated = false,
                oldValue = 5,
                newValue = 6,
                maxStreakBroken = true,
                maxStreakNew = 7,
            ),
        ).toDomain()

        assertNull(reward.streak)
    }

    @Test
    fun `a re-completion maps to an empty reward`() {
        val reward = RewardDto(
            points = PointsRewardDto(awarded = false),
            streak = StreakRewardDto(updated = false),
        ).toDomain()

        assertTrue(reward.isEmpty)
    }

    @Test
    fun `an awarded reward carries the amounts and the personal best flag through`() {
        val reward = RewardDto(
            points = PointsRewardDto(awarded = true, amount = 25, oldValue = 150, newValue = 175),
            streak = StreakRewardDto(
                updated = true,
                oldValue = 5,
                newValue = 6,
                maxStreakBroken = true,
                maxStreakNew = 7,
            ),
        ).toDomain()

        assertEquals(25, reward.points?.amount)
        assertEquals(175, reward.points?.newValue)
        assertEquals(6, reward.streak?.newValue)
        assertEquals(true, reward.streak?.maxStreakBroken)
        assertEquals(7, reward.streak?.maxStreakNew)
    }

    @Test
    fun `a missing reward block maps to an empty reward rather than failing`() {
        assertTrue((null as RewardDto?).toDomain().isEmpty)
    }

    @Test
    fun `a spin is read as coins when the response says coins, whatever the wedge is called`() {
        // The server resolves the item wedge to coins when the user owns everything already, so
        // the response's payout type is the only trustworthy signal.
        val result = WheelSpinDto(
            segmentId = "SEG_ITEM",
            payoutType = PAYOUT_COINS,
            coinsAwarded = 20,
            newBalance = 195,
            item = null,
        ).toDomain()

        assertNull(result.item)
        assertEquals(20, result.coins)
        assertEquals(195, result.newBalance)
    }

    @Test
    fun `an item payout without an item degrades to a coin win`() {
        val result = WheelSpinDto(
            segmentId = "SEG_ITEM",
            payoutType = PAYOUT_ITEM,
            coinsAwarded = 0,
            newBalance = 175,
            item = null,
        ).toDomain()

        assertNull(result.item)
    }

    @Test
    fun `an item payout keeps the name and image the win animation needs`() {
        val result = WheelSpinDto(
            segmentId = "SEG_ITEM",
            payoutType = PAYOUT_ITEM,
            coinsAwarded = 0,
            newBalance = 175,
            item = StoreItemDto(
                id = "item-1",
                name = "Aurora Frame",
                image = "https://cdn.example.com/frames/aurora.png",
                price = 200,
            ),
        ).toDomain()

        assertEquals("Aurora Frame", result.item?.name)
        assertEquals("https://cdn.example.com/frames/aurora.png", result.item?.imageUrl)
    }

    @Test
    fun `unparseable activity dates are dropped rather than failing the whole month`() {
        val dates = listOf("2026-07-01", "not-a-date", "2026-07-03").toActivityDates()

        assertEquals(
            setOf(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3)),
            dates,
        )
    }
}
