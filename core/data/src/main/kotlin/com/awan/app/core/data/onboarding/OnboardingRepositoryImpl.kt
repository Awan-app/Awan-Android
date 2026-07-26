package com.awan.app.core.data.onboarding

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.onboarding.remote.OnboardingRemoteDataSource
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.model.DayBounds
import com.awan.app.core.network.dto.CompleteOnboardingRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.UserEntity

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val remoteDataSource: OnboardingRemoteDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val userDao: UserDao,
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
                val response = result.data
                userDao.upsertUser(
                    UserEntity(
                        id = response.id,
                        email = response.email ?: "",
                        firstName = response.firstName ?: firstName,
                        lastName = response.lastName ?: lastName,
                        birthDate = response.birthDate ?: "2000-01-01",
                        points = response.points ?: 0,
                        streak = response.streak ?: 0,
                        maxStreak = response.maxStreak ?: 0,
                    ),
                )
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    private fun formatMinutesToTime(minutes: Int): String {
        val totalMinutes = minutes.mod(DayBounds.MINUTES_PER_DAY)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return String.format(Locale.US, "%02d:%02d:00", hours, mins)
    }
}
