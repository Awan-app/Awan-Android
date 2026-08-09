package com.awan.app.core.data.gamification

import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.model.PointsAward
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.gamification.model.StreakChange
import com.awan.app.core.domain.gamification.model.WheelSpinResult
import com.awan.app.core.domain.gamification.model.WonItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GamificationEventBusTest {

    private val bus = GamificationEventBus()

    /** Collects on an unconfined dispatcher so emissions land before the assertions run. */
    private fun runCollecting(block: suspend (List<RewardEvent>) -> Unit) = runTest {
        val collected = mutableListOf<RewardEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.rewards.collect(collected::add)
        }
        block(collected)
        job.cancel()
    }

    @Test
    fun `a points award emits one points event and banks the new balance`() = runCollecting { events ->
        bus.publishSessionReward(
            SessionReward(points = PointsAward(amount = 25, oldValue = 150, newValue = 175))
        )

        assertEquals(listOf(RewardEvent.Points(amount = 25, newTotal = 175)), events)
        assertEquals(175, bus.progress.value.points)
    }

    @Test
    fun `an empty reward emits nothing - a re-completed session earns nothing`() = runCollecting { events ->
        bus.publishSessionReward(SessionReward())

        assertTrue(events.isEmpty())
        assertEquals(GamificationProgress(), bus.progress.value)
    }

    @Test
    fun `points are emitted before the streak so the big moment lands last`() = runCollecting { events ->
        bus.publishSessionReward(
            SessionReward(
                points = PointsAward(amount = 25, oldValue = 150, newValue = 175),
                streak = StreakChange(
                    oldValue = 5,
                    newValue = 6,
                    maxStreakBroken = true,
                    maxStreakNew = 7,
                ),
            )
        )

        assertEquals(2, events.size)
        assertTrue(events[0] is RewardEvent.Points)
        assertTrue(events[1] is RewardEvent.Streak)
        assertEquals(true, (events[1] as RewardEvent.Streak).maxStreakBroken)
        assertEquals(7, bus.progress.value.maxStreak)
    }

    @Test
    fun `a coin spin emits points and takes the balance from the response`() = runCollecting { events ->
        bus.publishWheelSpin(
            WheelSpinResult(segmentId = "SEG_2", coins = 5, newBalance = 180, item = null)
        )

        assertEquals(listOf(RewardEvent.Points(amount = 5, newTotal = 180)), events)
        assertEquals(180, bus.progress.value.points)
    }

    @Test
    fun `an item spin emits an item and leaves the balance alone`() = runCollecting { events ->
        bus.publishWheelSpin(
            WheelSpinResult(
                segmentId = "SEG_ITEM",
                coins = 0,
                newBalance = 175,
                item = WonItem(id = "i1", name = "Aurora Frame", imageUrl = "https://x/y.png"),
            )
        )

        assertEquals(
            listOf(RewardEvent.Item(name = "Aurora Frame", imageUrl = "https://x/y.png")),
            events,
        )
        assertEquals(175, bus.progress.value.points)
    }

    @Test
    fun `seeding from cache does not undo a fresher award`() = runTest {
        bus.publishSessionReward(
            SessionReward(points = PointsAward(amount = 25, oldValue = 150, newValue = 175))
        )

        bus.seedProgressIfEmpty(GamificationProgress(points = 150, streak = 5, maxStreak = 6))

        assertEquals(175, bus.progress.value.points)
    }
}
