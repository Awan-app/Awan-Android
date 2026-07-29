package com.awan.app.core.data.profile.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.common.result.suspendOnSuccess
import com.awan.app.core.data.profile.mapper.asEntity
import com.awan.app.core.data.profile.mapper.asExternalModel
import com.awan.app.core.data.profile.mapper.toDomain
import com.awan.app.core.data.profile.remote.ProfileRemoteDataSource
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserWithPreferences
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.app.core.network.dto.AwardPointsRequest
import com.awan.app.core.network.dto.DeductPointsRequest
import com.awan.app.core.network.dto.UpdateBirthDateRequest
import com.awan.app.core.network.dto.UpdateNameRequest
import com.awan.app.core.network.dto.UpdateProfilePartialRequest
import com.awan.app.core.network.dto.UpdateSchedulingTypeRequest
import com.awan.app.core.network.dto.UpdateSessionSettingsRequest
import com.awan.app.core.network.dto.UpdateSleepScheduleRequest
import com.awan.app.core.network.dto.UpdateTimezoneRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileRemoteDataSource: ProfileRemoteDataSource,
    private val userDao: UserDao,
    private val authTokenProvider: AuthTokenProvider,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ProfileRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeProfile(): Flow<Profile?> =
        authTokenProvider.observeIsLoggedIn()
            .flatMapLatest { isLoggedIn ->
                if (isLoggedIn) {
                    flow<Profile?> {
                        val userId = authTokenProvider.getUserId()
                        if (userId != null) {
                            val profileFlow: Flow<Profile?> = userDao.observeUserWithPreferences(userId)
                                .map { userWithPreferences: UserWithPreferences? ->
                                    userWithPreferences?.asExternalModel()
                                }
                            emitAll(profileFlow)
                        } else {
                            emit(null)
                        }
                    }
                } else {
                    flowOf(null)
                }
            }
            .flowOn(ioDispatcher)

    private suspend fun updateLocalCache(newProfile: Profile) {
        val userId = newProfile.id ?: authTokenProvider.getUserId() ?: return
        val existing = userDao.getUserWithPreferences(userId)?.asExternalModel()

        val newPrefs = newProfile.preferences
        val mergedProfile = if (existing == null) {
            newProfile
        } else {
            existing.copy(
                email = newProfile.email ?: existing.email,
                firstName = newProfile.firstName ?: existing.firstName,
                lastName = newProfile.lastName ?: existing.lastName,
                birthDate = newProfile.birthDate ?: existing.birthDate,
                points = newProfile.points ?: existing.points,
                streak = newProfile.streak ?: existing.streak,
                maxStreak = newProfile.maxStreak ?: existing.maxStreak,
                preferences = if (newPrefs != null) {
                    val existingPrefs = existing.preferences
                    if (existingPrefs == null) {
                        newPrefs
                    } else {
                        existingPrefs.copy(
                            timezone = newPrefs.timezone ?: existingPrefs.timezone,
                            preferredSessionDuration = newPrefs.preferredSessionDuration ?: existingPrefs.preferredSessionDuration,
                            bufferBetweenSessions = newPrefs.bufferBetweenSessions ?: existingPrefs.bufferBetweenSessions,
                            wakeupTime = newPrefs.wakeupTime ?: existingPrefs.wakeupTime,
                            sleepTime = newPrefs.sleepTime ?: existingPrefs.sleepTime,
                            schedulingType = newPrefs.schedulingType ?: existingPrefs.schedulingType,
                        )
                    }
                } else {
                    existing.preferences
                }
            )
        }

        val userEntity: UserEntity = mergedProfile.asEntity().copy(id = userId)
        val preferences = mergedProfile.preferences
        if (preferences != null) {
            userDao.upsertUserWithPreferences(userEntity, preferences.asEntity(userId))
        } else {
            userDao.upsertUser(userEntity)
        }
    }

    override suspend fun getProfile(): Result<Profile> =
        profileRemoteDataSource.getProfileInfo()
            .map { it.toDomain() }
            .suspendOnSuccess { updateLocalCache(it) }

    override suspend fun updateName(
        firstName: String,
        lastName: String,
    ): Result<Profile> =
        profileRemoteDataSource.updateProfileName(
            UpdateNameRequest(firstName = firstName, lastName = lastName),
        ).map { it.toDomain() }.suspendOnSuccess { updateLocalCache(it) }

    override suspend fun updateBirthDate(birthDate: String): Result<Profile> =
        profileRemoteDataSource.updateProfileBirthDate(
            UpdateBirthDateRequest(birthDate = birthDate),
        ).map { it.toDomain() }.suspendOnSuccess { updateLocalCache(it) }

    override suspend fun updateProfilePartial(
        firstName: String?,
        lastName: String?,
        timezone: String?,
        preferredSessionDuration: Int?,
        bufferBetweenSessions: Int?,
        wakeupTime: String?,
        sleepTime: String?,
        schedulingType: String?,
    ): Result<Profile> =
        profileRemoteDataSource.updateProfilePartial(
            UpdateProfilePartialRequest(
                firstName = firstName,
                lastName = lastName,
                timezone = timezone,
                preferredSessionDuration = preferredSessionDuration,
                bufferBetweenSessions = bufferBetweenSessions,
                wakeupTime = wakeupTime,
                sleepTime = sleepTime,
                schedulingType = schedulingType,
            ),
        ).map { it.toDomain() }.suspendOnSuccess { updateLocalCache(it) }

    override suspend fun updateTimezone(timezone: String): Result<Profile> =
        profileRemoteDataSource.updateTimezone(
            UpdateTimezoneRequest(timezone = timezone),
        ).map { it.toDomain() }.suspendOnSuccess { updateLocalCache(it) }

    override suspend fun updateSessionSettings(
        preferredSessionDuration: Int,
        bufferBetweenSessions: Int,
    ): Result<Profile> =
        profileRemoteDataSource.updateSessionSettings(
            UpdateSessionSettingsRequest(
                preferredSessionDuration = preferredSessionDuration,
                bufferBetweenSessions = bufferBetweenSessions,
            ),
        ).map { it.toDomain() }.suspendOnSuccess { updateLocalCache(it) }

    override suspend fun updateSleepSchedule(
        wakeupTime: String,
        sleepTime: String,
    ): Result<Profile> =
        profileRemoteDataSource.updateSleepSchedule(
            UpdateSleepScheduleRequest(
                wakeupTime = wakeupTime,
                sleepTime = sleepTime,
            ),
        ).map { it.toDomain() }.suspendOnSuccess { updateLocalCache(it) }

    override suspend fun updateSchedulingType(schedulingType: String): Result<Profile> =
        profileRemoteDataSource.updateSchedulingType(
            UpdateSchedulingTypeRequest(schedulingType = schedulingType),
        ).map { it.toDomain() }.suspendOnSuccess { updateLocalCache(it) }

    override suspend fun incrementStreak(): Result<Profile> =
        profileRemoteDataSource.incrementStreak()
            .map { it.toDomain() }
            .suspendOnSuccess { updateLocalCache(it) }

    override suspend fun resetStreak(): Result<Profile> =
        profileRemoteDataSource.resetStreak()
            .map { it.toDomain() }
            .suspendOnSuccess { updateLocalCache(it) }

    override suspend fun awardPoints(points: Int): Result<Profile> =
        profileRemoteDataSource.awardPoints(
            AwardPointsRequest(points = points),
        ).map { it.toDomain() }.suspendOnSuccess { updateLocalCache(it) }

    override suspend fun deductPoints(points: Int): Result<Profile> =
        profileRemoteDataSource.deductPoints(
            DeductPointsRequest(points = points),
        ).map { it.toDomain() }.suspendOnSuccess { updateLocalCache(it) }
}
