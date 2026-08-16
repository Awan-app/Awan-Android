package com.awan.app.core.data.gamification.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.gamification.GamificationEventBus
import com.awan.app.core.data.gamification.mapper.toActivityDates
import com.awan.app.core.data.gamification.mapper.toDomain
import com.awan.app.core.data.gamification.remote.GamificationRemoteDataSource
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.model.WheelConfig
import com.awan.app.core.domain.gamification.model.WheelSpinResult
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRepositoryImpl @Inject constructor(
    private val remoteDataSource: GamificationRemoteDataSource,
    private val eventBus: GamificationEventBus,
    private val userDao: UserDao,
) : GamificationRepository {

    /**
     * Seeds from the cached user on first collection so the badges show the last known numbers
     * immediately instead of flashing zero while `/progress` is in flight.
     */
    override fun observeProgress(): Flow<GamificationProgress> = eventBus.progress.onStart {
        userDao.getFirstUser()?.let { cached ->
            eventBus.seedProgressIfEmpty(
                GamificationProgress(
                    points = cached.points,
                    streak = cached.streak,
                    maxStreak = cached.maxStreak,
                )
            )
        }
    }

    override fun observeRewards(): Flow<RewardEvent> = eventBus.rewards

    override suspend fun refreshProgress(): Result<GamificationProgress> {
        val result = remoteDataSource.getProgress().map { it.toDomain() }
        if (result is Result.Success) {
            eventBus.setProgress(result.data)
        }
        return result
    }

    override suspend fun getActivityDates(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Result<Set<LocalDate>> =
        remoteDataSource.getActivityDates(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
        ).map { it.toActivityDates() }

    override suspend fun getWheelConfig(): Result<WheelConfig> =
        remoteDataSource.getWheelConfig().map { it.toDomain() }

    /**
     * Resolves the spin without crediting anything visible. The wheel is a full-screen overlay, so
     * banking the balance here would flash the new total behind it and leave the celebration with
     * nothing left to count up to — [publishWheelReward] does that once the wheel is dismissed.
     */
    override suspend fun spinWheel(): Result<WheelSpinResult> =
        remoteDataSource.spinWheel().map { it.toDomain() }

    override suspend fun publishWheelReward(result: WheelSpinResult) {
        eventBus.publishWheelSpin(result)
    }

    override fun publishReward(event: RewardEvent) {
        eventBus.publishReward(event)
    }
}

