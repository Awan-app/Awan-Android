package com.awan.app.core.data.onboarding

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.onboarding.remote.OnboardingRemoteDataSource
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.datastore.model.UserPreferencesData
import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.profile.model.UserProfile
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingRequest
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.model.DayZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class OnboardingRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRemoteDataSource: FakeOnboardingRemoteDataSource
    private lateinit var fakePreferencesDataSource: FakeUserPreferencesDataSource
    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var fakeZonesRepository: FakeZonesRepository
    private lateinit var repository: OnboardingRepositoryImpl

    @Before
    fun setUp() {
        fakeRemoteDataSource = FakeOnboardingRemoteDataSource()
        fakePreferencesDataSource = FakeUserPreferencesDataSource()
        fakeUserDao = FakeUserDao()
        fakeZonesRepository = FakeZonesRepository()
        repository = OnboardingRepositoryImpl(
            remoteDataSource = fakeRemoteDataSource,
            zonesRepository = fakeZonesRepository,
            userPreferencesDataSource = fakePreferencesDataSource,
            userDao = fakeUserDao,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `completeOnboarding sends formatted payload and updates datastore completion`() = runTest(testDispatcher.scheduler) {
        val onboardingData = OnboardingData(
            profile = UserProfile(firstName = "Sarah", lastName = "Connor"),
            bounds = DayBounds(wakeMinutes = 7 * 60, sleepMinutes = 23 * 60),
            preferredTaskLengthMinutes = 45,
        )

        val result = repository.completeOnboarding(onboardingData)

        assertTrue(result is Result.Success)
        assertTrue(fakePreferencesDataSource.isOnboardingCompleted)

        val sentRequest = fakeRemoteDataSource.lastReceivedRequest
        assertEquals("Sarah", sentRequest?.firstName)
        assertEquals("Connor", sentRequest?.lastName)
        assertEquals(45, sentRequest?.preferredSessionDuration)
        assertEquals("07:00:00", sentRequest?.wakeupTime)
        assertEquals("23:00:00", sentRequest?.sleepTime)
    }

    private class FakeOnboardingRemoteDataSource : OnboardingRemoteDataSource {
        var lastReceivedRequest: CompleteOnboardingRequest? = null
        var shouldFail: Boolean = false

        override suspend fun completeOnboarding(request: CompleteOnboardingRequest): Result<CompleteOnboardingResponse> {
            lastReceivedRequest = request
            return if (shouldFail) {
                Result.Error(com.awan.app.core.common.error.AppError.Network)
            } else {
                Result.Success(CompleteOnboardingResponse(id = "user-id-123"))
            }
        }
    }

    private class FakeUserPreferencesDataSource : UserPreferencesDataSource {
        var isOnboardingCompleted: Boolean = false
        private val prefs = MutableStateFlow(UserPreferencesData())

        override val userPreferences: Flow<UserPreferencesData> = prefs

        override suspend fun setDarkThemeEnabled(enabled: Boolean) {}
        override suspend fun setDynamicColorEnabled(enabled: Boolean) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {
            isOnboardingCompleted = completed
        }
        override suspend fun setDefaultZone(zone: String) {}
        override suspend fun setLocale(locale: String) {}
        override suspend fun setDefaultRegion(region: String) {}
    }

    private class FakeUserDao : com.awan.app.core.database.dao.UserDao {
        override suspend fun upsertUser(user: com.awan.app.core.database.model.UserEntity) {}
        override fun observeUser(userId: String): Flow<com.awan.app.core.database.model.UserEntity?> = MutableStateFlow(null)
        override suspend fun getUser(userId: String): com.awan.app.core.database.model.UserEntity? = null
        override suspend fun getFirstUser(): com.awan.app.core.database.model.UserEntity? = null
        override suspend fun deleteUser(userId: String) {}
        override suspend fun upsertPreferences(preferences: com.awan.app.core.database.model.UserPreferencesEntity) {}
        override fun observePreferences(userId: String): Flow<com.awan.app.core.database.model.UserPreferencesEntity?> = MutableStateFlow(null)
        override suspend fun getPreferences(userId: String): com.awan.app.core.database.model.UserPreferencesEntity? = null

        override fun observeUserWithPreferences(userId: String): Flow<com.awan.app.core.database.model.UserWithPreferences?> =
            MutableStateFlow(null)

        override suspend fun getUserWithPreferences(userId: String): com.awan.app.core.database.model.UserWithPreferences? =
            null
    }

    private class FakeZonesRepository : ZonesRepository {
        override suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>> = Result.Success(emptyList())
        override suspend fun getTemplates(): Result<List<WeeklyTemplate>> = Result.Success(emptyList())
        override suspend fun createTemplate(name: String, daysOfWeek: List<DayOfWeek>, zones: List<DailyZone>): Result<WeeklyTemplate> =
            Result.Success(WeeklyTemplate(id = "default", name = name, daysOfWeek = daysOfWeek, zones = zones))
        override suspend fun getTemplate(templateId: String): Result<WeeklyTemplate> = Result.Error(com.awan.app.core.common.error.AppError.Unknown())
        override suspend fun updateTemplate(templateId: String, name: String, daysOfWeek: List<DayOfWeek>): Result<WeeklyTemplate> = Result.Error(com.awan.app.core.common.error.AppError.Unknown())
        override suspend fun deleteTemplate(templateId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun addZoneToTemplate(templateId: String, zone: DailyZone): Result<DailyZone> = Result.Success(zone)
        override suspend fun getTemplateZones(templateId: String): Result<List<DailyZone>> = Result.Success(emptyList())
        override suspend fun updateTemplateZones(templateId: String, zones: List<DailyZone>): Result<List<DailyZone>> = Result.Success(zones)
        override suspend fun createOverride(date: String, zones: List<DailyZone>): Result<TemplateOverride> = Result.Error(com.awan.app.core.common.error.AppError.Unknown())
        override suspend fun getOverrides(): Result<List<TemplateOverride>> = Result.Success(emptyList())
        override suspend fun getOverride(overrideId: String): Result<TemplateOverride> = Result.Error(com.awan.app.core.common.error.AppError.Unknown())
        override suspend fun updateOverride(overrideId: String, name: String?, date: String): Result<TemplateOverride> = Result.Error(com.awan.app.core.common.error.AppError.Unknown())
        override suspend fun deleteOverride(overrideId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun addZoneToOverride(overrideId: String, zone: DailyZone): Result<DailyZone> = Result.Success(zone)
        override suspend fun getOverrideZones(overrideId: String): Result<List<DailyZone>> = Result.Success(emptyList())
        override suspend fun updateOverrideZones(overrideId: String, zones: List<DailyZone>): Result<List<DailyZone>> = Result.Success(zones)
        override suspend fun getZone(zoneId: String): Result<DailyZone> = Result.Error(com.awan.app.core.common.error.AppError.Unknown())
        override suspend fun getZoneSessions(zoneId: String): Result<List<Session>> = Result.Success(emptyList())
        override suspend fun getEffectiveZones(date: String): Result<List<DailyZone>> = Result.Success(emptyList())
        override suspend fun updateZone(zoneId: String, zone: DailyZone): Result<DailyZone> = Result.Success(zone)
        override suspend fun deleteZone(zoneId: String): Result<Unit> = Result.Success(Unit)
    }
}
