package com.awan.app.core.data.onboarding

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.onboarding.remote.OnboardingRemoteDataSource
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingRequest
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
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
    private val zonesRepository: ZonesRepository,
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
                // Create or update the default template with onboarding zones
                val dailyZones = data.zones.filter { it.isEnabled }.map { zone ->
                    DailyZone(
                        id = null,
                        name = zone.name,
                        startTime = formatMinutesToTimeShort(zone.startMinutes),
                        endTime = formatMinutesToTimeShort(zone.endMinutes),
                        color = String.format("#%06X", 0xFFFFFF and zone.colorArgb)
                    )
                }

                val templatesResult = zonesRepository.getTemplates()
                val existingDefault = if (templatesResult is Result.Success) {
                    templatesResult.data.find { it.name.equals("Default", ignoreCase = true) }
                } else null

                if (existingDefault != null) {
                    zonesRepository.updateTemplateZones(existingDefault.id, dailyZones)
                } else {
                    zonesRepository.createTemplate(
                        name = "Default",
                        daysOfWeek = DayOfWeek.entries,
                        zones = dailyZones
                    )
                }

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
        if (minutes >= DayBounds.MINUTES_PER_DAY) return "23:59:59"
        val totalMinutes = minutes.mod(DayBounds.MINUTES_PER_DAY)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return String.format(Locale.US, "%02d:%02d:00", hours, mins)
    }

    private fun formatMinutesToTimeShort(minutes: Int): String {
        if (minutes >= DayBounds.MINUTES_PER_DAY) return "23:59"
        val totalMinutes = minutes.mod(DayBounds.MINUTES_PER_DAY)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return String.format(Locale.US, "%02d:%02d", hours, mins)
    }
}
