package com.awan.feature.splash.impl.ui

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.model.AuthSession
import com.awan.app.core.domain.auth.model.User
import com.awan.app.core.domain.auth.repository.AuthRepository
import com.awan.app.core.domain.auth.usecase.ObserveAuthStateUseCase
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.domain.goal.usecase.GetPendingScheduleDraftGoalIdUseCase
import com.awan.app.core.domain.onboarding.model.OnboardingData
import com.awan.app.core.domain.onboarding.repository.OnboardingRepository
import com.awan.app.core.domain.onboarding.usecase.HasCompletedOnboardingUseCase
import com.awan.app.core.model.ConfirmedGoalSession
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.model.GoalDecompositionTranscript
import com.awan.app.core.model.GoalScheduleDraft
import com.awan.app.core.model.GoalScheduleProposal
import com.awan.app.core.model.ProposedGoalSession
import com.awan.app.core.model.ProposedTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var onboardingRepository: FakeOnboardingRepository
    private lateinit var goalRepository: FakeGoalRepository

    private lateinit var observeAuthStateUseCase: ObserveAuthStateUseCase
    private lateinit var hasCompletedOnboardingUseCase: HasCompletedOnboardingUseCase
    private lateinit var getPendingScheduleDraftGoalIdUseCase: GetPendingScheduleDraftGoalIdUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        onboardingRepository = FakeOnboardingRepository()
        goalRepository = FakeGoalRepository()

        observeAuthStateUseCase = ObserveAuthStateUseCase(authRepository)
        hasCompletedOnboardingUseCase = HasCompletedOnboardingUseCase(onboardingRepository)
        getPendingScheduleDraftGoalIdUseCase = GetPendingScheduleDraftGoalIdUseCase(goalRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SplashViewModel = SplashViewModel(
        observeAuthStateUseCase = observeAuthStateUseCase,
        hasCompletedOnboardingUseCase = hasCompletedOnboardingUseCase,
        getPendingScheduleDraftGoalIdUseCase = getPendingScheduleDraftGoalIdUseCase,
    )

    @Test
    fun `when user is logged out, destination is Auth`() = runTest(testDispatcher) {
        authRepository.isLoggedInFlow.value = false
        onboardingRepository.isCompleted = true
        goalRepository.pendingGoalIdResult = Result.Success("goal-123")

        val viewModel = createViewModel()
        val destinations = mutableListOf<SplashDestination>()
        val job = launch { viewModel.destination.collect { destinations.add(it) } }

        assertEquals(SplashDestination.Auth, destinations.last())
        job.cancel()
    }

    @Test
    fun `when user is logged in but onboarding is incomplete, destination is Onboarding`() = runTest(testDispatcher) {
        authRepository.isLoggedInFlow.value = true
        onboardingRepository.isCompleted = false
        goalRepository.pendingGoalIdResult = Result.Success("goal-123")

        val viewModel = createViewModel()
        val destinations = mutableListOf<SplashDestination>()
        val job = launch { viewModel.destination.collect { destinations.add(it) } }

        assertEquals(SplashDestination.Onboarding, destinations.last())
        job.cancel()
    }

    @Test
    fun `when user is logged in, onboarding complete, and pending schedule draft exists, destination is ResumeGoalScheduling`() = runTest(testDispatcher) {
        authRepository.isLoggedInFlow.value = true
        onboardingRepository.isCompleted = true
        goalRepository.pendingGoalIdResult = Result.Success("goal-123")

        val viewModel = createViewModel()
        val destinations = mutableListOf<SplashDestination>()
        val job = launch { viewModel.destination.collect { destinations.add(it) } }

        assertEquals(SplashDestination.ResumeGoalScheduling("goal-123"), destinations.last())
        job.cancel()
    }

    @Test
    fun `when user is logged in, onboarding complete, and pending draft goal is null, destination is Home`() = runTest(testDispatcher) {
        authRepository.isLoggedInFlow.value = true
        onboardingRepository.isCompleted = true
        goalRepository.pendingGoalIdResult = Result.Success(null)

        val viewModel = createViewModel()
        val destinations = mutableListOf<SplashDestination>()
        val job = launch { viewModel.destination.collect { destinations.add(it) } }

        assertEquals(SplashDestination.Home, destinations.last())
        job.cancel()
    }

    @Test
    fun `when user is logged in, onboarding complete, and pending draft goal is blank, destination is Home`() = runTest(testDispatcher) {
        authRepository.isLoggedInFlow.value = true
        onboardingRepository.isCompleted = true
        goalRepository.pendingGoalIdResult = Result.Success("   ")

        val viewModel = createViewModel()
        val destinations = mutableListOf<SplashDestination>()
        val job = launch { viewModel.destination.collect { destinations.add(it) } }

        assertEquals(SplashDestination.Home, destinations.last())
        job.cancel()
    }

    @Test
    fun `when user is logged in, onboarding complete, and pending draft goal lookup errors, destination is Home`() = runTest(testDispatcher) {
        authRepository.isLoggedInFlow.value = true
        onboardingRepository.isCompleted = true
        goalRepository.pendingGoalIdResult = Result.Error(AppError.Network)

        val viewModel = createViewModel()
        val destinations = mutableListOf<SplashDestination>()
        val job = launch { viewModel.destination.collect { destinations.add(it) } }

        assertEquals(SplashDestination.Home, destinations.last())
        job.cancel()
    }

    @Test
    fun `dynamic auth state transitions update destination appropriately`() = runTest(testDispatcher) {
        authRepository.isLoggedInFlow.value = false
        onboardingRepository.isCompleted = true
        goalRepository.pendingGoalIdResult = Result.Success("goal-456")

        val viewModel = createViewModel()
        val destinations = mutableListOf<SplashDestination>()
        val job = launch { viewModel.destination.collect { destinations.add(it) } }

        assertEquals(SplashDestination.Auth, destinations.last())

        // User logs in
        authRepository.isLoggedInFlow.value = true
        assertEquals(SplashDestination.ResumeGoalScheduling("goal-456"), destinations.last())

        // User logs out
        authRepository.isLoggedInFlow.value = false
        assertEquals(SplashDestination.Auth, destinations.last())

        job.cancel()
    }

    private class FakeAuthRepository : AuthRepository {
        val isLoggedInFlow = MutableStateFlow(false)
        val sessionExpiredFlow = MutableSharedFlow<Unit>()

        override suspend fun requestOtp(email: String): Result<Unit> = Result.Success(Unit)
        override suspend fun verifyOtp(email: String, code: String): Result<AuthSession> = error("Not implemented")
        override suspend fun signInWithFirebase(idToken: String): Result<AuthSession> = error("Not implemented")
        override suspend fun logout(): Result<Unit> {
            isLoggedInFlow.value = false
            return Result.Success(Unit)
        }
        override fun observeIsLoggedIn(): Flow<Boolean> = isLoggedInFlow
        override fun observeSessionExpired(): Flow<Unit> = sessionExpiredFlow
        override suspend fun getUser(): User? = null
        override suspend fun getLastUsedEmail(): String? = null
        override suspend fun refreshUserData(): Result<User> = error("Not implemented")
    }

    private class FakeOnboardingRepository : OnboardingRepository {
        var isCompleted: Boolean = false

        override suspend fun completeOnboarding(data: OnboardingData): Result<Unit> {
            isCompleted = true
            return Result.Success(Unit)
        }

        override suspend fun hasCompletedOnboarding(): Boolean = isCompleted
    }

    private class FakeGoalRepository : GoalRepository {
        var pendingGoalIdResult: Result<String?> = Result.Success(null)

        override suspend fun getGoals(): Result<List<Goal>> = error("Not implemented")
        override suspend fun createGoal(title: String, description: String?, targetDate: String?, tasks: List<ProposedTask>): Result<Goal> = error("Not implemented")
        override suspend fun getInboxGoal(): Result<Goal> = error("Not implemented")
        override suspend fun getGoal(goalId: String): Result<Goal> = error("Not implemented")
        override suspend fun deleteGoal(goalId: String): Result<Unit> = error("Not implemented")
        override suspend fun continueDecomposition(sessionId: String?, message: String): Result<GoalDecompositionReply> = error("Not implemented")
        override suspend fun confirmDecomposition(sessionId: String): Result<Goal> = error("Not implemented")
        override suspend fun getDecompositionTranscript(sessionId: String): Result<GoalDecompositionTranscript> = error("Not implemented")
        override suspend fun cancelDecomposition(sessionId: String): Result<Unit> = error("Not implemented")
        override suspend fun scheduleGoal(goalId: String): Result<Unit> = error("Not implemented")
        override suspend fun proposeGoalSchedule(goalId: String): Result<GoalScheduleProposal> = error("Not implemented")
        override suspend fun confirmGoalSchedule(goalId: String, sessions: List<ProposedGoalSession>): Result<List<ConfirmedGoalSession>> = error("Not implemented")
        override suspend fun clearScheduleDraft(goalId: String) = Unit
        override suspend fun getPendingScheduleDraftGoalId(): Result<String?> = pendingGoalIdResult
    }
}
