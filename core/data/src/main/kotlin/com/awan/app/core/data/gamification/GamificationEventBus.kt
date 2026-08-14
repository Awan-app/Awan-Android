package com.awan.app.core.data.gamification

import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.gamification.model.WheelSpinResult
import com.awan.app.core.database.dao.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide holder for the user's progress and for rewards worth celebrating.
 *
 * It sits beside `GamificationRepositoryImpl` rather than inside it because rewards are earned in
 * more than one place: the wheel spins through the gamification repository, but a completed session
 * goes through `HomeRepositoryImpl`. Both publish here, so the celebration is triggered by whoever
 * earned it rather than by whichever ViewModel happened to be on screen.
 *
 * Reward emissions are dropped when nothing is collecting — a celebration that has already been
 * missed should not fire late, when the user is somewhere else entirely.
 */
@Singleton
class GamificationEventBus @Inject constructor(
    private val userDao: UserDao,
) {

    private val _progress = MutableStateFlow(GamificationProgress())
    val progress = _progress.asStateFlow()

    private val _rewards = MutableSharedFlow<RewardEvent>(extraBufferCapacity = REWARD_BUFFER)
    val rewards: Flow<RewardEvent> = _rewards.asSharedFlow()

    suspend fun setProgress(progress: GamificationProgress) {
        _progress.value = progress
        cacheProgress(progress)
    }

    /** Seeds from cache without clobbering fresher numbers already published by an award. */
    fun seedProgressIfEmpty(progress: GamificationProgress) {
        _progress.update { current ->
            if (current == GamificationProgress()) progress else current
        }
    }

    suspend fun publishSessionReward(reward: SessionReward) {
        reward.points?.let { award ->
            _progress.update { it.copy(points = award.newValue) }
            _rewards.tryEmit(RewardEvent.Points(amount = award.amount, newTotal = award.newValue))
        }
        reward.streak?.let { change ->
            _progress.update {
                it.copy(streak = change.newValue, maxStreak = change.maxStreakNew)
            }
            _rewards.tryEmit(
                RewardEvent.Streak(
                    oldValue = change.oldValue,
                    newValue = change.newValue,
                    maxStreakBroken = change.maxStreakBroken,
                    maxStreakNew = change.maxStreakNew,
                )
            )
        }
        cacheProgress(_progress.value)
    }

    /**
     * A spin always pays out, but only one of the two ways. `newBalance` is authoritative either
     * way — on an item win it is the unchanged balance, so it is safe to apply unconditionally.
     */
    suspend fun publishWheelSpin(result: WheelSpinResult) {
        _progress.update { it.copy(points = result.newBalance) }
        cacheProgress(_progress.value)
        val item = result.item
        if (item != null) {
            _rewards.tryEmit(RewardEvent.Item(name = item.name, imageUrl = item.imageUrl))
        } else {
            _rewards.tryEmit(
                RewardEvent.Points(amount = result.coins, newTotal = result.newBalance)
            )
        }
    }

    /** Publishes an arbitrary reward event to the global celebration queue. */
    fun publishReward(event: RewardEvent) {
        _rewards.tryEmit(event)
    }

    /** Room is the progress cache — `UserEntity` already owns these three columns. */
    private suspend fun cacheProgress(progress: GamificationProgress) {
        val cached = userDao.getFirstUser() ?: return
        userDao.upsertUser(
            cached.copy(
                points = progress.points,
                streak = progress.streak,
                maxStreak = progress.maxStreak,
            )
        )
    }

    private companion object {
        const val REWARD_BUFFER = 8
    }
}

