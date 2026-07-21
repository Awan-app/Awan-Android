package com.awan.app.core.data.onboarding

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.DayBounds
import com.awan.app.core.model.FirstTask
import com.awan.app.core.model.UserProfile
import com.awan.app.core.model.Zone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

// ponytail: in-memory mock, swap for an OfflineFirst impl when the backend lands.
// Public (not internal) so the onboarding ViewModel test can drive the real repo, per plan.
@Singleton
class InMemoryOnboardingRepository @Inject constructor() : OnboardingRepository {

    private val state = MutableStateFlow(OnboardingData())

    override val draft: Flow<OnboardingData> = state.asStateFlow()

    override suspend fun saveProfile(profile: UserProfile) = state.update { it.copy(profile = profile) }

    override suspend fun saveDayBounds(bounds: DayBounds) = state.update { it.copy(bounds = bounds) }

    override suspend fun saveZones(zones: List<Zone>) = state.update { it.copy(zones = zones) }

    override suspend fun savePreferredTaskLength(minutes: Int) =
        state.update { it.copy(preferredTaskLengthMinutes = minutes) }

    override suspend fun saveFirstTask(task: FirstTask): Result<Unit> {
        state.update { it.copy(firstTask = task) }
        return Result.Success(Unit)
    }

    override suspend fun completeOnboarding() = state.update { it.copy(completed = true) }
}
