package com.awan.app.core.domain.gamification.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.model.WheelConfig
import com.awan.app.core.domain.gamification.model.WheelSpinResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface GamificationRepository {

    fun observeProgress(): Flow<GamificationProgress>

    /** Rewards worth celebrating, from any source. Hot and conflation-free — nothing is replayed. */
    fun observeRewards(): Flow<RewardEvent>

    suspend fun refreshProgress(): Result<GamificationProgress>

    suspend fun getActivityDates(startDate: LocalDate, endDate: LocalDate): Result<Set<LocalDate>>

    suspend fun getWheelConfig(): Result<WheelConfig>

    suspend fun spinWheel(): Result<WheelSpinResult>

    /**
     * Credits a spin's payout and releases it for celebration. Split from [spinWheel] so the caller
     * can hold it back until the wheel overlay is off screen.
     */
    suspend fun publishWheelReward(result: WheelSpinResult)

    /** Publishes an arbitrary reward event for app-wide celebration. */
    fun publishReward(event: RewardEvent)
}
