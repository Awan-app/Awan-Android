package com.awan.feature.profile.impl.presentation

import com.awan.app.core.domain.notifications.usecase.GetNotificationPreferencesUseCase
import com.awan.app.core.domain.notifications.usecase.SetNotificationPreferencesUseCase
import com.awan.app.core.domain.profile.model.UserData
import com.awan.app.core.domain.profile.repository.UserDataRepository
import com.awan.app.core.model.DarkThemeConfig
import com.awan.app.core.model.NotificationPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: FakeUserDataRepository
    private lateinit var viewModel: NotificationSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeUserDataRepository()
        viewModel = NotificationSettingsViewModel(
            getNotificationPreferences = GetNotificationPreferencesUseCase(repository),
            setNotificationPreferences = SetNotificationPreferencesUseCase(repository),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `stored preferences load into state`() = runTest(testDispatcher) {
        repository.emit(NotificationPreferences(dailyBriefEnabled = false, reminderLeadMinutes = 30))

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.preferences.dailyBriefEnabled)
        assertEquals(30, state.preferences.reminderLeadMinutes)
    }

    @Test
    fun `every switch writes the preference it names, and only that one`() =
        runTest(testDispatcher) {
            // One case per switch rather than one per action: a copy-paste in the when block that
            // writes the wrong field is the failure this catches, and it is invisible on screen
            // until a notification the user muted arrives anyway.
            val cases = listOf<Pair<NotificationSettingsAction, (NotificationPreferences) -> Boolean>>(
                NotificationSettingsAction.SetSessionReminders(false) to { it.sessionRemindersEnabled },
                NotificationSettingsAction.SetLiveActivity(false) to { it.sessionLiveActivityEnabled },
                NotificationSettingsAction.SetSessionEnd(false) to { it.sessionEndEnabled },
                NotificationSettingsAction.SetSessionFollowUp(false) to { it.sessionFollowUpEnabled },
                NotificationSettingsAction.SetStreakReminder(false) to { it.streakReminderEnabled },
                NotificationSettingsAction.SetDailyBrief(false) to { it.dailyBriefEnabled },
                NotificationSettingsAction.SetRewards(false) to { it.rewardsEnabled },
            )

            cases.forEach { (action, field) ->
                repository.emit(NotificationPreferences())
                viewModel.onAction(action)

                val saved = repository.saved!!
                assertFalse("$action left its own switch on", field(saved))
                assertEquals(
                    "$action changed a switch it does not own",
                    1,
                    switchesOff(saved),
                )
            }
        }

    @Test
    fun `the timing choices write through`() = runTest(testDispatcher) {
        viewModel.onAction(NotificationSettingsAction.SetReminderLead(15))
        assertEquals(15, repository.saved?.reminderLeadMinutes)

        viewModel.onAction(NotificationSettingsAction.SetFollowUpDelay(120))
        assertEquals(120, repository.saved?.followUpDelayMinutes)
    }

    @Test
    fun `ask-every-time is a snooze value like any other`() = runTest(testDispatcher) {
        // It travels as a sentinel through the same Int as a real length, so the one thing worth
        // asserting is that nothing on the way to storage sanitises it into a number.
        viewModel.onAction(NotificationSettingsAction.SetSnooze(NotificationPreferences.SNOOZE_ASK))

        assertEquals(NotificationPreferences.SNOOZE_ASK, repository.saved?.snoozeMinutes)
        assertEquals(
            NotificationPreferences.SNOOZE_ASK,
            viewModel.uiState.value.preferences.snoozeMinutes,
        )
    }

    @Test
    fun `the system permission is state only and is never stored`() = runTest(testDispatcher) {
        // It belongs to the OS, not to the user's preferences: writing it would let a revoked
        // permission masquerade as a switch the user turned off, and survive re-granting it.
        viewModel.onAction(NotificationSettingsAction.SystemPermissionChanged(false))

        assertFalse(viewModel.uiState.value.systemNotificationsEnabled)
        assertEquals(null, repository.saved)
    }

    @Test
    fun `state updates before the write, so a switch does not lag the tap`() =
        runTest(testDispatcher) {
            viewModel.onAction(NotificationSettingsAction.SetStreakReminder(false))

            assertFalse(viewModel.uiState.value.preferences.streakReminderEnabled)
            assertTrue(repository.writes == 1)
        }

    private fun switchesOff(preferences: NotificationPreferences): Int = listOf(
        preferences.sessionRemindersEnabled,
        preferences.sessionLiveActivityEnabled,
        preferences.sessionEndEnabled,
        preferences.sessionFollowUpEnabled,
        preferences.streakReminderEnabled,
        preferences.dailyBriefEnabled,
        preferences.rewardsEnabled,
    ).count { !it }

    private class FakeUserDataRepository : UserDataRepository {

        private val state = MutableStateFlow(
            UserData(darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM, locale = "en")
        )

        var saved: NotificationPreferences? = null
            private set

        var writes = 0
            private set

        override val userData: Flow<UserData> = state

        fun emit(preferences: NotificationPreferences) {
            saved = null
            writes = 0
            state.value = state.value.copy(notificationPreferences = preferences)
        }

        // Writes round-trip back out of the flow, as DataStore's do. Without that the ViewModel's
        // own copy and the "stored" one drift apart and every assertion after the first is against
        // a state no real device can be in.
        override suspend fun setNotificationPreferences(preferences: NotificationPreferences) {
            saved = preferences
            writes++
            state.value = state.value.copy(notificationPreferences = preferences)
        }

        override suspend fun setDarkThemeConfig(config: DarkThemeConfig) = Unit

        override suspend fun setLocale(locale: String) = Unit

        override suspend fun setMicPermissionRequested(requested: Boolean) = Unit
    }
}
