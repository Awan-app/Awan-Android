package com.awan.app.gamification

import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.model.RewardSource

data class TimestampedRewardEvent(
    val event: RewardEvent,
    val timestampMs: Long,
)

data class BatchResult(
    val eventToPlay: RewardEvent?,
    val consumedCount: Int,
)

object RewardBatcher {

    const val DEFAULT_WINDOW_MS = 150L
    const val MAX_WINDOW_MS = 300L

    fun batchNext(
        queue: List<TimestampedRewardEvent>,
        windowMs: Long = DEFAULT_WINDOW_MS,
        maxWindowMs: Long = MAX_WINDOW_MS,
    ): BatchResult {
        if (queue.isEmpty()) return BatchResult(null, 0)

        val firstEntry = queue.first()
        val firstEvent = firstEntry.event

        if (firstEvent !is RewardEvent.Points || firstEvent.source != RewardSource.SESSION_COMPLETION) {
            return BatchResult(firstEvent, 1)
        }

        val startTime = firstEntry.timestampMs
        var lastTime = startTime
        val matchedPoints = mutableListOf<RewardEvent.Points>()
        var count = 0

        for (item in queue) {
            val ev = item.event
            val time = item.timestampMs

            if (ev is RewardEvent.Points && ev.source == RewardSource.SESSION_COMPLETION) {
                if (time - startTime <= maxWindowMs && time - lastTime <= windowMs) {
                    matchedPoints.add(ev)
                    count++
                    lastTime = time
                } else {
                    break
                }
            } else {
                break
            }
        }

        if (matchedPoints.isEmpty()) {
            return BatchResult(firstEvent, 1)
        }

        val totalAmount = matchedPoints.sumOf { it.amount }
        val totalCombo = matchedPoints.sumOf { it.comboCount }
        val finalNewTotal = matchedPoints.last().newTotal

        val comboEvent = RewardEvent.Points(
            amount = totalAmount,
            newTotal = finalNewTotal,
            comboCount = totalCombo,
            source = RewardSource.SESSION_COMPLETION,
        )

        return BatchResult(comboEvent, count)
    }
}
