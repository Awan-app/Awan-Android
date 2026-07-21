package com.awan.app.core.data.onboarding

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.DayBounds
import com.awan.app.core.model.FirstTask
import com.awan.app.core.model.UserProfile
import com.awan.app.core.model.Zone
import kotlinx.coroutines.flow.Flow

/**
 * Persists the onboarding draft. Placement math (zone layout, task slotting) is domain logic and
 * happens above this interface — the repository only stores what it is handed.
 */
interface OnboardingRepository {
    val draft: Flow<OnboardingData>

    suspend fun saveProfile(profile: UserProfile)

    suspend fun saveDayBounds(bounds: DayBounds)

    suspend fun saveZones(zones: List<Zone>)

    suspend fun savePreferredTaskLength(minutes: Int)

    suspend fun saveFirstTask(task: FirstTask): Result<Unit>

    suspend fun completeOnboarding()
}
