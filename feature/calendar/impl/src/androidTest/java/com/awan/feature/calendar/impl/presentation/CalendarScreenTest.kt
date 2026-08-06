package com.awan.feature.calendar.impl.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
    fun calendarTitleMatchesHomeGreetingScaleAndStreakIconIsVisible() {
        val today = LocalDate.of(2026, 8, 1)
        composeRule.setContent {
            AwanTheme(dark = true) {
                CalendarScreen(
                    state = CalendarUiState(
                        isLoading = false,
                        streak = 5,
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
        composeRule.onNodeWithTag("streak_flame_icon").assertIsDisplayed()
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
