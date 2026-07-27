package com.awan.app.core.data.onboarding

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.onboarding.remote.OnboardingRemoteDataSource
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.datastore.model.UserPreferencesData
import com.awan.app.core.model.DayBounds
import com.awan.app.core.model.UserProfile
import com.awan.app.core.network.dto.CompleteOnboardingRequest
import com.awan.app.core.network.dto.CompleteOnboardingResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRemoteDataSource: FakeOnboardingRemoteDataSource
    private lateinit var fakePreferencesDataSource: FakeUserPreferencesDataSource
    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var repository: OnboardingRepositoryImpl

    @Before
    fun setUp() {
        fakeRemoteDataSource = FakeOnboardingRemoteDataSource()
        fakePreferencesDataSource = FakeUserPreferencesDataSource()
        fakeUserDao = FakeUserDao()
        repository = OnboardingRepositoryImpl(
            remoteDataSource = fakeRemoteDataSource,
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
    }
}
