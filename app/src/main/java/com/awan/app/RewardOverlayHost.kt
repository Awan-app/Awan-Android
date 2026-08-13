package com.awan.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.awan.app.core.designsystem.ItemFlightOverlay
import com.awan.app.core.designsystem.LocalRewardAnchors
import com.awan.app.core.designsystem.PointsFlightOverlay
import com.awan.app.core.designsystem.StreakLootOverlay
import com.awan.app.core.domain.gamification.model.RewardEvent
import kotlinx.coroutines.flow.Flow

/**
 * Plays reward celebrations over whatever screen the user is on.
 *
 * Rewards can arrive together — completing the day's first session pays points *and* moves the
 * streak — so they queue and play one at a time rather than stacking on top of each other.
 *
 * Play order is whatever order they were earned in, which the data layer already publishes
 * correctly: points before streak for a session, and a single payout for a spin. Re-sorting here
 * would only risk shuffling one action's rewards in front of an earlier action's.
 */
import kotlinx.coroutines.delay

@Composable
fun RewardOverlayHost(
    rewardEvents: Flow<RewardEvent>,
    modifier: Modifier = Modifier,
) {
    val queue = remember { mutableStateListOf<RewardEvent>() }
    var showing by remember { mutableStateOf<RewardEvent?>(null) }
    val anchors = LocalRewardAnchors.current

    LaunchedEffect(rewardEvents) {
        rewardEvents.collect(queue::add)
    }

    LaunchedEffect(showing, queue.size) {
        if (showing == null && queue.isNotEmpty()) {
            delay(150L)
            if (queue.firstOrNull() is RewardEvent.Points) {
                val pointsList = mutableListOf<RewardEvent.Points>()
                while (queue.isNotEmpty() && queue.first() is RewardEvent.Points) {
                    pointsList.add(queue.removeAt(0) as RewardEvent.Points)
                }
                val totalAmount = pointsList.sumOf { it.amount }
                val totalCombo = pointsList.sumOf { it.comboCount }
                val lastNewTotal = pointsList.last().newTotal
                showing = RewardEvent.Points(
                    amount = totalAmount,
                    newTotal = lastNewTotal,
                    comboCount = totalCombo,
                )
            } else if (queue.isNotEmpty()) {
                showing = queue.removeAt(0)
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
