package com.awan.app.core.data.gamification

import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.model.RewardSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardBatcherTest {

    @Test
    fun `batches multiple related session rewards arriving close together`() {
        val events = listOf(
            TimestampedRewardEvent(RewardEvent.Points(50, 150, source = RewardSource.SESSION_COMPLETION), 1000L),
            TimestampedRewardEvent(RewardEvent.Points(50, 200, source = RewardSource.SESSION_COMPLETION), 1050L),
            TimestampedRewardEvent(RewardEvent.Points(50, 250, source = RewardSource.SESSION_COMPLETION), 1100L),
        )

        val result = RewardBatcher.batchNext(events)

        assertNotNull(result.eventToPlay)
        assertEquals(3, result.consumedCount)

        val played = result.eventToPlay as RewardEvent.Points
        assertEquals(150, played.amount)
        assertEquals(250, played.newTotal)
        assertEquals(3, played.comboCount)
        assertEquals(RewardSource.SESSION_COMPLETION, played.source)
    }

    @Test
    fun `does not merge unrelated rewards into a combo`() {
        val events = listOf(
            TimestampedRewardEvent(RewardEvent.Points(50, 150, source = RewardSource.SESSION_COMPLETION), 1000L),
            TimestampedRewardEvent(RewardEvent.Points(100, 250, source = RewardSource.DAILY_WHEEL), 1020L),
        )

        val result = RewardBatcher.batchNext(events)

        assertNotNull(result.eventToPlay)
        assertEquals(1, result.consumedCount)

        val played = result.eventToPlay as RewardEvent.Points
        assertEquals(50, played.amount)
        assertEquals(1, played.comboCount)
        assertEquals(RewardSource.SESSION_COMPLETION, played.source)
    }

    @Test
    fun `does not merge non points events into a combo`() {
        val events = listOf(
            TimestampedRewardEvent(RewardEvent.Streak(1, 2, false, 2), 1000L),
            TimestampedRewardEvent(RewardEvent.Points(50, 200, source = RewardSource.SESSION_COMPLETION), 1020L),
        )

        val result = RewardBatcher.batchNext(events)

        assertNotNull(result.eventToPlay)
        assertEquals(1, result.consumedCount)
        assertTrue(result.eventToPlay is RewardEvent.Streak)
    }

    @Test
    fun `limits continuous event stream by maxWindowMs`() {
        val events = listOf(
            TimestampedRewardEvent(RewardEvent.Points(10, 110, source = RewardSource.SESSION_COMPLETION), 1000L),
            TimestampedRewardEvent(RewardEvent.Points(10, 120, source = RewardSource.SESSION_COMPLETION), 1080L),
            TimestampedRewardEvent(RewardEvent.Points(10, 130, source = RewardSource.SESSION_COMPLETION), 1160L),
            TimestampedRewardEvent(RewardEvent.Points(10, 140, source = RewardSource.SESSION_COMPLETION), 1240L),
            // This 5th event is at 1350L (350ms after first event at 1000L), exceeding maxWindowMs (300ms)
            TimestampedRewardEvent(RewardEvent.Points(10, 150, source = RewardSource.SESSION_COMPLETION), 1350L),
        )

        val result = RewardBatcher.batchNext(events, windowMs = 150L, maxWindowMs = 300L)

        assertNotNull(result.eventToPlay)
        assertEquals(4, result.consumedCount)

        val played = result.eventToPlay as RewardEvent.Points
        assertEquals(40, played.amount)
        assertEquals(4, played.comboCount)
    }

    @Test
    fun `creates separate combos for events arriving after debounce window`() {
        val events = listOf(
            TimestampedRewardEvent(RewardEvent.Points(50, 150, source = RewardSource.SESSION_COMPLETION), 1000L),
            // Arrives 250ms later (> windowMs 150ms)
            TimestampedRewardEvent(RewardEvent.Points(50, 200, source = RewardSource.SESSION_COMPLETION), 1250L),
        )

        val result = RewardBatcher.batchNext(events, windowMs = 150L, maxWindowMs = 300L)

        assertEquals(1, result.consumedCount)
        val played = result.eventToPlay as RewardEvent.Points
        assertEquals(50, played.amount)
        assertEquals(1, played.comboCount)
    }
}
