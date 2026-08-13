package com.awan.feature.calendar.impl.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awan.app.core.designsystem.AwanTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calendarTitleMatchesHomeGreetingScaleAndStreakHeaderRendersStart() {
        val today = LocalDate.of(2026, 8, 1)
        composeRule.setContent {
            AwanTheme(dark = true) {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        streak = 0,
                        maxStreak = 0,
                        isTodayActive = false,
                        streakHeaderState = CalendarStreakHeaderState.Start,
                        timezone = ZoneId.of("UTC"),
                        today = today,
                        selectedDate = today,
                        currentYearMonth = YearMonth.from(today),
                        monthDays = CalendarDateMapper.buildMonthDays(
                            YearMonth.from(today),
                            today,
                            today,
                            emptySet(),
                            emptySet(),
                        ),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        val title = composeRule.onNodeWithTag("calendar_title")
        title.assertIsDisplayed()
        val titleBounds = title.getUnclippedBoundsInRoot()
        assertTrue(titleBounds.bottom - titleBounds.top <= 32.dp)
        composeRule.onNodeWithTag("streak_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_start").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_start_icon").assertIsDisplayed()
        composeRule.onNodeWithText("Start your streak").assertIsDisplayed()
    }

    @Test
    fun streakHeaderRendersRestartStateForEndedStreak() {
        val today = LocalDate.of(2026, 8, 1)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        streak = 0,
                        maxStreak = 5,
                        isTodayActive = false,
                        streakHeaderState = CalendarStreakHeaderState.Restart,
                        timezone = ZoneId.of("UTC"),
                        today = today,
                        selectedDate = today,
                        currentYearMonth = YearMonth.from(today),
                        monthDays = emptyList(),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("streak_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_restart").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_restart_icon").assertIsDisplayed()
        composeRule.onNodeWithText("Ready to start again?").assertIsDisplayed()
    }

    @Test
    fun streakHeaderRendersProtectStateForActiveStreakNotCompletedToday() {
        val today = LocalDate.of(2026, 8, 1)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        streak = 5,
                        maxStreak = 10,
                        isTodayActive = false,
                        streakHeaderState = CalendarStreakHeaderState.Protect,
                        timezone = ZoneId.of("UTC"),
                        today = today,
                        selectedDate = today,
                        currentYearMonth = YearMonth.from(today),
                        monthDays = emptyList(),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("streak_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_protect").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_protect_icon").assertIsDisplayed()
        composeRule.onNodeWithText("5-day streak at risk").assertIsDisplayed()
    }

    @Test
    fun streakHeaderRendersCelebrateStateForActiveStreakCompletedToday() {
        val today = LocalDate.of(2026, 8, 1)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        streak = 5,
                        maxStreak = 10,
                        isTodayActive = true,
                        streakHeaderState = CalendarStreakHeaderState.Celebrate,
                        timezone = ZoneId.of("UTC"),
                        today = today,
                        selectedDate = today,
                        currentYearMonth = YearMonth.from(today),
                        monthDays = emptyList(),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("streak_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_celebrate").assertIsDisplayed()
        composeRule.onNodeWithTag("streak_header_celebrate_icon").assertIsDisplayed()
        composeRule.onNodeWithText("5-day streak! You're on fire").assertIsDisplayed()
    }

    @Test
    fun selectingADayDispatchesItsDate() {
        val today = LocalDate.of(2026, 8, 1)
        val actions = mutableListOf<CalendarAction>()
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        today = today,
                        selectedDate = today,
                        currentYearMonth = YearMonth.from(today),
                        monthDays = CalendarDateMapper.buildMonthDays(
                            YearMonth.from(today),
                            today,
                            today,
                            emptySet(),
                            emptySet(),
                        ),
                    ),
                    onAction = actions::add,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("calendar_day_$today").performClick()

        assertTrue(actions == listOf(CalendarAction.SelectDate(today)))
    }
}
