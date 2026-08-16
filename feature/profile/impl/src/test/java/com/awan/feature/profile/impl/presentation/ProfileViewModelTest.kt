package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.model.AuthSession
import com.awan.app.core.domain.auth.model.User
import com.awan.app.core.domain.auth.repository.AuthRepository
import com.awan.app.core.domain.auth.usecase.LogoutUseCase
import com.awan.app.core.domain.category.usecase.GetCategoriesUseCase
import com.awan.app.core.domain.image.repository.ImageRepository
import com.awan.app.core.domain.image.usecase.ReadImageUseCase
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.domain.marketplace.usecase.GetEquippedItemsUseCase
import com.awan.app.core.domain.marketplace.usecase.RefreshMarketplaceUseCase
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.model.UserData
import com.awan.app.core.domain.profile.model.UserPreferences
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.app.core.domain.profile.repository.UserDataRepository
import com.awan.app.core.domain.profile.usecase.*
import com.awan.app.core.model.DarkThemeConfig
import com.awan.app.core.model.EquippedItem
import com.awan.app.core.model.ImageBytes
import com.awan.app.core.model.NotificationPreferences
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var imageRepository: FakeImageRepository
    private lateinit var userDataRepository: FakeUserDataRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var storeRepository: FakeStoreRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepository()
        imageRepository = FakeImageRepository()
        userDataRepository = FakeUserDataRepository()
        authRepository = FakeAuthRepository()
        storeRepository = FakeStoreRepository()
        categoryRepository = FakeCategoryRepository()

        viewModel = ProfileViewModel(
            getProfileUseCase = GetProfileUseCase(profileRepository),
            observeProfileUseCase = ObserveProfileUseCase(profileRepository),
            updateSleepScheduleUseCase = UpdateSleepScheduleUseCase(profileRepository),
            updateSessionSettingsUseCase = UpdateSessionSettingsUseCase(profileRepository),
            updateTimezoneUseCase = UpdateTimezoneUseCase(profileRepository),
            updateProfilePartialUseCase = UpdateProfilePartialUseCase(profileRepository),
            updateProfilePictureUseCase = UpdateProfilePictureUseCase(profileRepository),
            deleteProfilePictureUseCase = DeleteProfilePictureUseCase(profileRepository),
            readImage = ReadImageUseCase(imageRepository),
            getUserDataUseCase = GetUserDataUseCase(userDataRepository),
            setDarkThemeUseCase = SetDarkThemeUseCase(userDataRepository),
            setLocaleUseCase = SetLocaleUseCase(userDataRepository),
            logoutUseCase = LogoutUseCase(authRepository),
            getEquippedItemsUseCase = GetEquippedItemsUseCase(storeRepository),
            refreshMarketplaceUseCase = RefreshMarketplaceUseCase(storeRepository),
            getCategoriesUseCase = GetCategoriesUseCase(categoryRepository),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state loads profile, categories, and preferences`() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertNotNull(state.profile)
        assertEquals("John", state.profile?.firstName)
        assertEquals("Doe", state.profile?.lastName)
        assertEquals(2, state.categories.size)
        assertEquals("en", state.language)
    }

    @Test
    fun `updatePersonalInfo passes raw unnormalized user input to use case without ViewModel-level trimming`() =
        runTest(testDispatcher) {
            viewModel.onAction(ProfileAction.UpdatePersonalInfo("  Jane  ", "  Smith  "))
            advanceUntilIdle()

            assertEquals("  Jane  ", profileRepository.lastUpdatePartialFirstName)
            assertEquals("  Smith  ", profileRepository.lastUpdatePartialLastName)
        }

    @Test
    fun `updatePersonalInfo with unchanged name and no pending picture emits UpdateSuccess without network calls`() =
        runTest(testDispatcher) {
            val events = mutableListOf<ProfileEvent>()
            val job = launch { viewModel.events.toList(events) }

            viewModel.onAction(ProfileAction.UpdatePersonalInfo("John", "Doe"))
            advanceUntilIdle()

            assertEquals(0, profileRepository.updateProfilePartialCallCount)
            assertEquals(1, events.size)
            assertEquals(ProfileEvent.UpdateSuccess, events.first())

            job.cancel()
        }

    @Test
    fun `updatePersonalInfo when only name changed and succeeds emits UpdateSuccess`() =
        runTest(testDispatcher) {
            val events = mutableListOf<ProfileEvent>()
            val job = launch { viewModel.events.toList(events) }

            viewModel.onAction(ProfileAction.UpdatePersonalInfo("Johnny", "Doe"))
            advanceUntilIdle()

            assertEquals(1, profileRepository.updateProfilePartialCallCount)
            assertEquals("Johnny", viewModel.uiState.value.profile?.firstName)
            assertNull(viewModel.uiState.value.fieldError)
            assertEquals(1, events.size)
            assertEquals(ProfileEvent.UpdateSuccess, events.first())

            job.cancel()
        }

    @Test
    fun `updatePersonalInfo when only name changed and fails sets fieldError`() =
        runTest(testDispatcher) {
            val events = mutableListOf<ProfileEvent>()
            val job = launch { viewModel.events.toList(events) }

            profileRepository.updatePartialResult = Result.Error(AppError.Network)

            viewModel.onAction(ProfileAction.UpdatePersonalInfo("Johnny", "Doe"))
            advanceUntilIdle()

            assertEquals(1, profileRepository.updateProfilePartialCallCount)
            assertNotNull(viewModel.uiState.value.fieldError)
            assertFalse(viewModel.uiState.value.isUpdatingField)
            assertTrue(events.isEmpty())

            job.cancel()
        }

    @Test
    fun `updatePersonalInfo when only picture changed Picked and succeeds clears pendingPicture and emits UpdateSuccess`() =
        runTest(testDispatcher) {
            val events = mutableListOf<ProfileEvent>()
            val job = launch { viewModel.events.toList(events) }

            viewModel.onAction(ProfileAction.UpdateProfilePicture("file://profile.jpg"))
            assertEquals(PendingPicture.Picked("file://profile.jpg"), viewModel.uiState.value.pendingPicture)

            viewModel.onAction(ProfileAction.UpdatePersonalInfo("John", "Doe"))
            advanceUntilIdle()

            assertEquals(0, profileRepository.updateProfilePartialCallCount)
            assertEquals(1, profileRepository.updateProfilePictureCallCount)
            assertNull(viewModel.uiState.value.pendingPicture)
            assertNull(viewModel.uiState.value.fieldError)
            assertEquals(1, events.size)
            assertEquals(ProfileEvent.UpdateSuccess, events.first())

            job.cancel()
        }

    @Test
    fun `updatePersonalInfo when only picture changed Clear and succeeds emits UpdateSuccess`() =
        runTest(testDispatcher) {
            val events = mutableListOf<ProfileEvent>()
            val job = launch { viewModel.events.toList(events) }

            viewModel.onAction(ProfileAction.DeleteProfilePicture)
            assertEquals(PendingPicture.Clear, viewModel.uiState.value.pendingPicture)

            viewModel.onAction(ProfileAction.UpdatePersonalInfo("John", "Doe"))
            advanceUntilIdle()

            assertEquals(0, profileRepository.updateProfilePartialCallCount)
            assertEquals(1, profileRepository.deleteProfilePictureCallCount)
            assertNull(viewModel.uiState.value.pendingPicture)
            assertNull(viewModel.uiState.value.fieldError)
            assertEquals(1, events.size)
            assertEquals(ProfileEvent.UpdateSuccess, events.first())

            job.cancel()
        }

    @Test
    fun `updatePersonalInfo when only picture changed and fails sets fieldError and keeps pendingPicture`() =
        runTest(testDispatcher) {
            val events = mutableListOf<ProfileEvent>()
            val job = launch { viewModel.events.toList(events) }

            profileRepository.updatePictureResult = Result.Error(AppError.Network)

            viewModel.onAction(ProfileAction.UpdateProfilePicture("file://profile.jpg"))
            viewModel.onAction(ProfileAction.UpdatePersonalInfo("John", "Doe"))
            advanceUntilIdle()

            assertEquals(1, profileRepository.updateProfilePictureCallCount)
            assertEquals(PendingPicture.Picked("file://profile.jpg"), viewModel.uiState.value.pendingPicture)
            assertNotNull(viewModel.uiState.value.fieldError)
            assertFalse(viewModel.uiState.value.isUploadingPicture)
            assertTrue(events.isEmpty())

            job.cancel()
        }

    @Test
    fun `updatePersonalInfo when both name and picture changed and both succeed emits UpdateSuccess`() =
        runTest(testDispatcher) {
            val events = mutableListOf<ProfileEvent>()
            val job = launch { viewModel.events.toList(events) }

            viewModel.onAction(ProfileAction.UpdateProfilePicture("file://profile.jpg"))
            viewModel.onAction(ProfileAction.UpdatePersonalInfo("Jane", "Smith"))
            advanceUntilIdle()

            assertEquals(1, profileRepository.updateProfilePartialCallCount)
            assertEquals(1, profileRepository.updateProfilePictureCallCount)
            assertEquals("Jane", viewModel.uiState.value.profile?.firstName)
            assertNull(viewModel.uiState.value.pendingPicture)
            assertNull(viewModel.uiState.value.fieldError)
            assertEquals(1, events.size)
            assertEquals(ProfileEvent.UpdateSuccess, events.first())

            job.cancel()
        }

    @Test
    fun `updatePersonalInfo when name succeeds but picture fails keeps persisted name, retains pendingPicture, and sets fieldError`() =
        runTest(testDispatcher) {
            val events = mutableListOf<ProfileEvent>()
            val job = launch { viewModel.events.toList(events) }

            profileRepository.updatePictureResult = Result.Error(AppError.Network)

            viewModel.onAction(ProfileAction.UpdateProfilePicture("file://profile.jpg"))
            viewModel.onAction(ProfileAction.UpdatePersonalInfo("Jane", "Smith"))
            advanceUntilIdle()

            assertEquals(1, profileRepository.updateProfilePartialCallCount)
            assertEquals(1, profileRepository.updateProfilePictureCallCount)
            assertEquals("Jane", viewModel.uiState.value.profile?.firstName)
            assertEquals(PendingPicture.Picked("file://profile.jpg"), viewModel.uiState.value.pendingPicture)
            assertNotNull(viewModel.uiState.value.fieldError)
            assertFalse(viewModel.uiState.value.isUpdatingField)
            assertFalse(viewModel.uiState.value.isUploadingPicture)
            assertTrue(events.isEmpty())

            job.cancel()
        }

    @Test
    fun `updatePersonalInfo when name fails but picture succeeds updates picture, clears pendingPicture, and sets fieldError`() =
        runTest(testDispatcher) {
            val events = mutableListOf<ProfileEvent>()
            val job = launch { viewModel.events.toList(events) }

            profileRepository.updatePartialResult = Result.Error(AppError.Network)

            viewModel.onAction(ProfileAction.UpdateProfilePicture("file://profile.jpg"))
            viewModel.onAction(ProfileAction.UpdatePersonalInfo("Jane", "Smith"))
            advanceUntilIdle()

            assertEquals(1, profileRepository.updateProfilePartialCallCount)
            assertEquals(1, profileRepository.updateProfilePictureCallCount)
            assertEquals("https://cdn.awan.app/profile.jpg", viewModel.uiState.value.profile?.profilePictureUrl)
            assertNull(viewModel.uiState.value.pendingPicture)
            assertNotNull(viewModel.uiState.value.fieldError)
            assertFalse(viewModel.uiState.value.isUpdatingField)
            assertFalse(viewModel.uiState.value.isUploadingPicture)
            assertTrue(events.isEmpty())

            job.cancel()
        }

    @Test
    fun `DismissEditSheet action clears pendingPicture and fieldError`() = runTest(testDispatcher) {
        viewModel.onAction(ProfileAction.UpdateProfilePicture("file://profile.jpg"))
        assertEquals(PendingPicture.Picked("file://profile.jpg"), viewModel.uiState.value.pendingPicture)

        viewModel.onAction(ProfileAction.DismissEditSheet)
        assertNull(viewModel.uiState.value.pendingPicture)
        assertNull(viewModel.uiState.value.fieldError)
    }

    private class FakeProfileRepository : ProfileRepository {
        var currentProfile = Profile(
            id = "user-1",
            email = "user@example.com",
            firstName = "John",
            lastName = "Doe",
            birthDate = "2000-01-01",
            points = 100,
            streak = 5,
            maxStreak = 10,
            profilePictureUrl = null,
            isNew = false,
            preferences = UserPreferences(
                timezone = "UTC",
                preferredSessionDuration = 60,
                bufferBetweenSessions = 5,
                wakeupTime = "07:00:00",
                sleepTime = "23:00:00",
                schedulingType = "BALANCED",
            )
        )
        val profileFlow = MutableStateFlow<Profile?>(currentProfile)

        var updateProfilePartialCallCount = 0
        var updateProfilePictureCallCount = 0
        var deleteProfilePictureCallCount = 0

        var lastUpdatePartialFirstName: String? = null
        var lastUpdatePartialLastName: String? = null

        var updatePartialResult: Result<Profile>? = null
        var updatePictureResult: Result<Profile>? = null
        var deletePictureResult: Result<Profile>? = null

        override fun observeProfile(): Flow<Profile?> = profileFlow.asStateFlow()

        override suspend fun getProfile(): Result<Profile> = Result.Success(currentProfile)

        override suspend fun updateName(firstName: String, lastName: String): Result<Profile> =
            Result.Success(currentProfile)

        override suspend fun updateBirthDate(birthDate: String): Result<Profile> =
            Result.Success(currentProfile)

        override suspend fun updateProfilePicture(imageBytes: ByteArray, mimeType: String): Result<Profile> {
            updateProfilePictureCallCount++
            updatePictureResult?.let { return it }
            currentProfile = currentProfile.copy(profilePictureUrl = "https://cdn.awan.app/profile.jpg")
            profileFlow.value = currentProfile
            return Result.Success(currentProfile)
        }

        override suspend fun deleteProfilePicture(): Result<Profile> {
            deleteProfilePictureCallCount++
            deletePictureResult?.let { return it }
            currentProfile = currentProfile.copy(profilePictureUrl = null)
            profileFlow.value = currentProfile
            return Result.Success(currentProfile)
        }

        override suspend fun updateProfilePartial(
            firstName: String?,
            lastName: String?,
            timezone: String?,
            preferredSessionDuration: Int?,
            bufferBetweenSessions: Int?,
            wakeupTime: String?,
            sleepTime: String?,
            schedulingType: String?
        ): Result<Profile> {
            updateProfilePartialCallCount++
            lastUpdatePartialFirstName = firstName
            lastUpdatePartialLastName = lastName

            updatePartialResult?.let { return it }

            currentProfile = currentProfile.copy(
                firstName = firstName ?: currentProfile.firstName,
                lastName = lastName ?: currentProfile.lastName,
            )
            profileFlow.value = currentProfile
            return Result.Success(currentProfile)
        }

        override suspend fun updateTimezone(timezone: String): Result<Profile> =
            Result.Success(currentProfile)

        override suspend fun updateSessionSettings(
            preferredSessionDuration: Int,
            bufferBetweenSessions: Int
        ): Result<Profile> = Result.Success(currentProfile)

        override suspend fun updateSleepSchedule(
            wakeupTime: String,
            sleepTime: String
        ): Result<Profile> = Result.Success(currentProfile)

        override suspend fun updateSchedulingType(schedulingType: String): Result<Profile> =
            Result.Success(currentProfile)
    }

    private class FakeImageRepository : ImageRepository {
        override suspend fun read(uri: String): Result<ImageBytes> =
            Result.Success(ImageBytes(byteArrayOf(1, 2, 3), "image/jpeg"))
    }

    private class FakeUserDataRepository : UserDataRepository {
        val userPreferencesFlow = MutableStateFlow(
            UserData(
                darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                locale = "en",
                micPermissionRequested = false,
                notificationPreferences = NotificationPreferences()
            )
        )

        override val userData: Flow<UserData> = userPreferencesFlow.asStateFlow()

        override suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
            userPreferencesFlow.value = userPreferencesFlow.value.copy(darkThemeConfig = config)
        }

        override suspend fun setLocale(locale: String) {
            userPreferencesFlow.value = userPreferencesFlow.value.copy(locale = locale)
        }

        override suspend fun setMicPermissionRequested(requested: Boolean) {
            userPreferencesFlow.value = userPreferencesFlow.value.copy(micPermissionRequested = requested)
        }

        override suspend fun setNotificationPreferences(preferences: NotificationPreferences) {
            userPreferencesFlow.value = userPreferencesFlow.value.copy(notificationPreferences = preferences)
        }
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun requestOtp(email: String): Result<Unit> = Result.Success(Unit)
        override suspend fun verifyOtp(email: String, code: String): Result<AuthSession> =
            Result.Success(AuthSession("token", "refresh", 3600L, User("1", "test@awan.app")))
        override suspend fun signInWithFirebase(idToken: String): Result<AuthSession> =
            Result.Success(AuthSession("token", "refresh", 3600L, User("1", "test@awan.app")))
        override suspend fun logout(): Result<Unit> = Result.Success(Unit)
        override fun observeIsLoggedIn(): Flow<Boolean> = flowOf(true)
        override fun observeSessionExpired(): Flow<Unit> = flowOf()
        override suspend fun getUser(): User = User("1", "test@awan.app")
        override suspend fun getLastUsedEmail(): String = "test@awan.app"
        override suspend fun refreshUserData(): Result<User> = Result.Success(User("1", "test@awan.app"))
    }

    private class FakeStoreRepository : StoreRepository {
        override fun getEquippedItems(): Flow<List<EquippedItem>> = flowOf(emptyList())
        override fun getInventory(): Flow<List<OwnedItem>> = flowOf(emptyList())
        override fun getStoreItems(type: StoreItemType?): Flow<List<StoreItem>> = flowOf(emptyList())
        override suspend fun refreshStoreItems(type: StoreItemType?) {}
        override suspend fun refreshInventory(): Result<Unit> = Result.Success(Unit)
        override suspend fun refreshEquippedItems(): Result<Unit> = Result.Success(Unit)
        override suspend fun buyItem(itemId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun equipItem(itemId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun unequipItem(itemType: StoreItemType): Result<Unit> = Result.Success(Unit)
        override suspend fun markInventorySeen() {}
    }
}
