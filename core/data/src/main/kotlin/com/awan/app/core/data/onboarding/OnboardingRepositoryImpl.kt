package com.awan.app.core.data.onboarding

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.onboarding.remote.OnboardingRemoteDataSource
import com.awan.app.core.data.util.formatMinutesToTime
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.network.dto.CompleteOnboardingRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val remoteDataSource: OnboardingRemoteDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : OnboardingRepository {

    override suspend fun completeOnboarding(data: OnboardingData): Result<Unit> = withContext(ioDispatcher) {
        val firstName = data.profile?.firstName?.takeIf { it.isNotBlank() } ?: "User"
        val lastName = data.profile?.lastName?.takeIf { it.isNotBlank() } ?: "Awan"

        val request = CompleteOnboardingRequest(
            firstName = firstName,
            lastName = lastName,
            birthDate = "2000-01-01",
            timezone = TimeZone.getDefault().id.ifBlank { "Africa/Cairo" },
            preferredSessionDuration = data.preferredTaskLengthMinutes,
            bufferBetweenSessions = 10,
            wakeupTime = formatMinutesToTime(data.bounds.wakeMinutes),
            sleepTime = formatMinutesToTime(data.bounds.sleepMinutes),
            schedulingType = "BALANCED",
        )

        when (val result = remoteDataSource.completeOnboarding(request)) {
            is Result.Success -> {
                userPreferencesDataSource.setOnboardingCompleted(true)
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }
}
