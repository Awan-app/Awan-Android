package com.awan.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.awan.app.core.data.gamification.RewardBatcher
import com.awan.app.core.data.gamification.TimestampedRewardEvent
import com.awan.app.core.designsystem.ItemFlightOverlay
import com.awan.app.core.designsystem.LocalRewardAnchors
import com.awan.app.core.designsystem.PointsFlightOverlay
import com.awan.app.core.designsystem.StreakLootOverlay
import com.awan.app.core.domain.gamification.model.RewardEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun RewardOverlayHost(
    rewardEvents: Flow<RewardEvent>,
    modifier: Modifier = Modifier,
) {
    val queue = remember { mutableStateListOf<TimestampedRewardEvent>() }
    var showing by remember { mutableStateOf<RewardEvent?>(null) }
    val anchors = LocalRewardAnchors.current

    LaunchedEffect(rewardEvents) {
        rewardEvents.collect { event ->
            queue.add(TimestampedRewardEvent(event, System.currentTimeMillis()))
        }
    }

    LaunchedEffect(showing, queue.size) {
        if (showing == null && queue.isNotEmpty()) {
            delay(RewardBatcher.DEFAULT_WINDOW_MS)
            val result = RewardBatcher.batchNext(
                queue = queue,
                windowMs = RewardBatcher.DEFAULT_WINDOW_MS,
                maxWindowMs = RewardBatcher.MAX_WINDOW_MS,
            )
            if (result.eventToPlay != null) {
                repeat(result.consumedCount) {
                    if (queue.isNotEmpty()) queue.removeAt(0)
                }
                showing = result.eventToPlay
            }
        }
    }

    val finish = { showing = null }

    when (val event = showing) {
        null -> Unit

        is RewardEvent.Points -> PointsFlightOverlay(
            amount = event.amount,
            newTotal = event.newTotal,
            comboCount = event.comboCount,
            originBounds = anchors.lastTapOrigin,
            targetBounds = anchors.pointsBadge,
            onFinished = finish,
            modifier = modifier,
        )

        is RewardEvent.Item -> ItemFlightOverlay(
            name = event.name,
            imageUrl = event.imageUrl,
            targetBounds = anchors.profileTab,
            onFinished = finish,
            modifier = modifier,
        )

        is RewardEvent.Streak -> StreakLootOverlay(
            oldValue = event.oldValue,
            newValue = event.newValue,
            maxStreakBroken = event.maxStreakBroken,
            maxStreakNew = event.maxStreakNew,
            onDismiss = finish,
            modifier = modifier,
        )
    }
}
