package com.awan.app.core.data.onboarding

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.onboarding.remote.OnboardingRemoteDataSource
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.Zone
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingRequest
import com.awan.app.core.domain.onboarding.model.OnboardingData
import com.awan.app.core.domain.onboarding.repository.OnboardingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.domain.network.NetworkConnectivityMonitor

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val remoteDataSource: OnboardingRemoteDataSource,
    private val zonesRepository: ZonesRepository,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val userDao: UserDao,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : OnboardingRepository {

    override suspend fun completeOnboarding(data: OnboardingData): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
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
                saveDefaultTemplate(data.zones)?.let { return@withContext Result.Error(it) }

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
                        profilePictureUrl = response.profilePictureUrl,
                        isNew = false,
                    ),
                )
                Result.Success(Unit)
            }
            // An already-onboarded user is not a failure: swallow it as a success so they reach Home
            // instead of being stranded in onboarding with an error they cannot act on. The template
            // is still written here — this is the path every retry takes once the account exists, so
            // skipping it would drop the zones of anyone whose first attempt failed on the template.
            is Result.Error -> if (result.error.isAlreadyOnboarded()) {
                saveDefaultTemplate(data.zones)?.let { return@withContext Result.Error(it) }

                userPreferencesDataSource.setOnboardingCompleted(true)
                Result.Success(Unit)
            } else {
                Result.Error(result.error)
            }
            Result.Loading -> Result.Loading
        }
    }

    /**
     * Writes the onboarding zones as the user's default weekly template, returning the failure that
     * stopped it or null when there was nothing to fail. A zone without a category is rejected by the
     * backend, so those are dropped rather than sending a request that is certain to fail — an
     * account with no categories simply gets no template, which is not an error.
     *
     * A real failure is reported rather than swallowed: nothing re-enters onboarding once it is left,
     * so a dropped template would cost the user their whole zone setup with no way to redo it.
     */
    private suspend fun saveDefaultTemplate(zones: List<Zone>): AppError? {
        val dailyZones = zones
            .filter { it.isEnabled && it.categoryId != null }
            .map { zone ->
                DailyZone(
                    id = null,
                    name = zone.name,
                    startTime = formatMinutesToTimeShort(zone.startMinutes),
                    endTime = formatMinutesToTimeShort(zone.endMinutes),
                    color = String.format("#%06X", 0xFFFFFF and zone.colorArgb),
                    categoryId = zone.categoryId,
                )
            }
        if (dailyZones.isEmpty()) return null

        // Creating on an unread template list would leave the account with two "Default" templates.
        val templates = zonesRepository.getTemplates()
        if (templates is Result.Error) return templates.error
        val existingDefault = (templates as? Result.Success)
            ?.data
            ?.find { it.name.equals(DEFAULT_TEMPLATE_NAME, ignoreCase = true) }

        val write = if (existingDefault != null) {
            zonesRepository.updateTemplateZones(existingDefault.id, dailyZones)
        } else {
            zonesRepository.createTemplate(
                name = DEFAULT_TEMPLATE_NAME,
                daysOfWeek = DayOfWeek.entries,
                zones = dailyZones,
            )
        }
        return (write as? Result.Error)?.error
    }

    private fun formatMinutesToTime(minutes: Int): String {
        if (minutes >= DayBounds.MINUTES_PER_DAY) return "23:59:59"
        val totalMinutes = minutes.mod(DayBounds.MINUTES_PER_DAY)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return String.format(Locale.US, "%02d:%02d:00", hours, mins)
    }

    private fun formatMinutesToTimeShort(minutes: Int): String {
        if (minutes >= DayBounds.MINUTES_PER_DAY) return "23:59:00"
        val totalMinutes = minutes.mod(DayBounds.MINUTES_PER_DAY)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return String.format(Locale.US, "%02d:%02d:00", hours, mins)
    }

    // The API doc pins this to 400 ONBOARDING_ALREADY_COMPLETED while the deployed backend answers 409,
    // so match either rather than betting on one.
    private fun AppError.isAlreadyOnboarded(): Boolean =
        this is AppError.Api && (errorCode == ONBOARDING_ALREADY_COMPLETED || code == HTTP_CONFLICT)

    override suspend fun hasCompletedOnboarding(): Boolean = withContext(ioDispatcher) {
        if (userPreferencesDataSource.userPreferences.first().onboardingCompleted) return@withContext true

        val completedRemotely = remoteDataSource.isNewUser().let { it is Result.Success && !it.data }
        if (completedRemotely) userPreferencesDataSource.setOnboardingCompleted(true)
        completedRemotely
    }

    private companion object {
        const val DEFAULT_TEMPLATE_NAME = "Default"
        const val HTTP_CONFLICT = 409
        const val ONBOARDING_ALREADY_COMPLETED = "ONBOARDING_ALREADY_COMPLETED"
    }
}
