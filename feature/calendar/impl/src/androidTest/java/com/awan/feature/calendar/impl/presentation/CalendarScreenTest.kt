package com.awan.feature.calendar.impl.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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

    // ---------------------------------------------------------------------------
    // Helper — builds a CalendarUiState showing a single-month grid centred on
    // `today` with the given streak and goal-date sets, and no loading/error.
    // ---------------------------------------------------------------------------
    private fun stateWithDays(
        today: LocalDate,
        streakDates: Set<LocalDate> = emptySet(),
        goalDates: Set<LocalDate> = emptySet(),
        selectedDate: LocalDate = today,
    ): CalendarUiState = CalendarUiState(
        isLoading = false,
        streak = streakDates.size,
        maxStreak = streakDates.size,
        isTodayActive = today in streakDates,
        streakHeaderState = CalendarStreakHeaderState.from(
            streak = streakDates.size,
            maxStreak = streakDates.size,
            isTodayActive = today in streakDates,
        ),
        timezone = ZoneId.of("UTC"),
        today = today,
        selectedDate = selectedDate,
        currentYearMonth = YearMonth.from(today),
        monthDays = CalendarDateMapper.buildMonthDays(
            YearMonth.from(today),
            today,
            selectedDate,
            streakDates,
            goalDates,
        ),
    )

    // =========================================================================
    // Existing streak-header tests (preserved)
    // =========================================================================

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

    // =========================================================================
    // Day-cell visual state tests
    // =========================================================================

    /**
     * Plain / missed day — a past non-streak day with no deadline.
     * The day number is displayed bound to plainDay cell; no decoration tags are rendered.
     */
    @Test
    fun dayCell_plainMissed_showsNumberAndNoDecoration() {
        val today = LocalDate.of(2026, 8, 13)
        val plainDay = today.minusDays(5)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = stateWithDays(today = today),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        // Cell container is displayed
        composeRule.onNodeWithTag("calendar_day_$plainDay").assertIsDisplayed()

        // Day number is displayed bound to plainDay cell
        composeRule.onNode(
            hasTestTag("calendar_day_${plainDay}_number") and
            hasText(plainDay.dayOfMonth.toString()) and
            hasAnyAncestor(hasTestTag("calendar_day_$plainDay")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // No decorations bound to plainDay cell
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${plainDay}_today_primary") and
                hasAnyAncestor(hasTestTag("calendar_day_$plainDay")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${plainDay}_streak_fire") and
                hasAnyAncestor(hasTestTag("calendar_day_$plainDay")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${plainDay}_streak_number") and
                hasAnyAncestor(hasTestTag("calendar_day_$plainDay")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${plainDay}_deadline_shader") and
                hasAnyAncestor(hasTestTag("calendar_day_$plainDay")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${plainDay}_streak_badge") and
                hasAnyAncestor(hasTestTag("calendar_day_$plainDay")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * Today (no streak, no deadline) — primary sky circle is shown, number is
     * displayed inside it, and no streak/shader tags are present on today.
     */
    @Test
    fun dayCell_today_showsPrimaryCircleAndNumber() {
        val today = LocalDate.of(2026, 8, 13)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = stateWithDays(today = today),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("calendar_day_$today").assertIsDisplayed()

        // Day number is displayed bound to today cell
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_number") and
            hasText(today.dayOfMonth.toString()) and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Primary sky circle decoration is displayed bound to today cell
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_today_primary") and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Unexpected decorations absent on today cell
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_streak_fire") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_streak_number") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_deadline_shader") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_streak_badge") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * Streak day (not today, no deadline) — static fire host and the
     * fire-surface number circle are both shown bound to the streak day; no today-primary or shader.
     */
    @Test
    fun dayCell_streak_showsFireHostAndNumberCircle() {
        val today = LocalDate.of(2026, 8, 13)
        val yesterdayStreak = today.minusDays(1)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = stateWithDays(
                        today = today,
                        streakDates = setOf(yesterdayStreak),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("calendar_day_$yesterdayStreak").assertIsDisplayed()

        // Day number bound to streak cell
        composeRule.onNode(
            hasTestTag("calendar_day_${yesterdayStreak}_number") and
            hasText(yesterdayStreak.dayOfMonth.toString()) and
            hasAnyAncestor(hasTestTag("calendar_day_$yesterdayStreak")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Fire host and number circle bound to streak cell
        composeRule.onNode(
            hasTestTag("calendar_day_${yesterdayStreak}_streak_fire") and
            hasAnyAncestor(hasTestTag("calendar_day_$yesterdayStreak")),
        useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("calendar_day_${yesterdayStreak}_streak_number") and
            hasAnyAncestor(hasTestTag("calendar_day_$yesterdayStreak")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Unexpected decorations absent on streak cell
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${yesterdayStreak}_today_primary") and
                hasAnyAncestor(hasTestTag("calendar_day_$yesterdayStreak")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${yesterdayStreak}_deadline_shader") and
                hasAnyAncestor(hasTestTag("calendar_day_$yesterdayStreak")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${yesterdayStreak}_streak_badge") and
                hasAnyAncestor(hasTestTag("calendar_day_$yesterdayStreak")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * Future deadline (non-streak, non-today) — the circular shader host is
     * reserved and the number is inside the cell; no primary circle or fire.
     */
    @Test
    fun dayCell_futureDeadline_showsShaderHostAndNumber() {
        val today = LocalDate.of(2026, 8, 13)
        val deadlineDay = today.plusDays(3)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = stateWithDays(
                        today = today,
                        goalDates = setOf(deadlineDay),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("calendar_day_$deadlineDay").assertIsDisplayed()

        // Day number bound to deadline cell
        composeRule.onNode(
            hasTestTag("calendar_day_${deadlineDay}_number") and
            hasText(deadlineDay.dayOfMonth.toString()) and
            hasAnyAncestor(hasTestTag("calendar_day_$deadlineDay")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Circular shader host bound to deadline cell
        composeRule.onNode(
            hasTestTag("calendar_day_${deadlineDay}_deadline_shader") and
            hasAnyAncestor(hasTestTag("calendar_day_$deadlineDay")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Unexpected decorations absent on deadline cell
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${deadlineDay}_today_primary") and
                hasAnyAncestor(hasTestTag("calendar_day_$deadlineDay")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${deadlineDay}_streak_fire") and
                hasAnyAncestor(hasTestTag("calendar_day_$deadlineDay")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${deadlineDay}_streak_number") and
                hasAnyAncestor(hasTestTag("calendar_day_$deadlineDay")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${deadlineDay}_streak_badge") and
                hasAnyAncestor(hasTestTag("calendar_day_$deadlineDay")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * Today + deadline (no streak) — primary circle, circular shader host, and
     * number are all present bound to today; no fire host or streak badge.
     */
    @Test
    fun dayCell_todayWithDeadline_showsPrimaryCircleShaderAndNumber() {
        val today = LocalDate.of(2026, 8, 13)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = stateWithDays(
                        today = today,
                        goalDates = setOf(today),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("calendar_day_$today").assertIsDisplayed()

        // Day number bound to today cell
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_number") and
            hasText(today.dayOfMonth.toString()) and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Primary circle and shader host bound to today cell
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_today_primary") and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_deadline_shader") and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Streak decorations absent on today cell
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_streak_fire") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_streak_number") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_streak_badge") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * Today + streak (no deadline) — primary-tinted fire host and anchored
     * number circle are present bound to today; no deadline shader or fire badge.
     */
    @Test
    fun dayCell_todayWithStreak_showsTintedFireHostAndAnchoredNumberCircle() {
        val today = LocalDate.of(2026, 8, 13)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = stateWithDays(
                        today = today,
                        streakDates = setOf(today),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("calendar_day_$today").assertIsDisplayed()

        // Day number bound to today cell
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_number") and
            hasText(today.dayOfMonth.toString()) and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Fire host and number circle bound to today cell
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_streak_fire") and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_streak_number") and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Unexpected decorations absent on today cell
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_today_primary") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_deadline_shader") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_streak_badge") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
    }

    /**
     * Today + streak + deadline — primary circle, circular shader host, number,
     * AND top-right fire badge are all present bound to today.
     */
    @Test
    fun dayCell_todayWithStreakAndDeadline_showsAllFourElements() {
        val today = LocalDate.of(2026, 8, 13)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = stateWithDays(
                        today = today,
                        streakDates = setOf(today),
                        goalDates = setOf(today),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("calendar_day_$today").assertIsDisplayed()

        // Day number bound to today cell
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_number") and
            hasText(today.dayOfMonth.toString()) and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Primary circle, deadline shader, and top-right fire badge bound to today cell
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_today_primary") and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_deadline_shader") and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("calendar_day_${today}_streak_badge") and
            hasAnyAncestor(hasTestTag("calendar_day_$today")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Full fire host and number circle absent (badge replaces them)
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_streak_fire") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${today}_streak_number") and
                hasAnyAncestor(hasTestTag("calendar_day_$today")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
    }

    // =========================================================================
    // Past-deadline guard
    // =========================================================================

    /**
     * A goal date that has already passed must NOT render a deadline shader,
     * while a future goal date on the same screen still does.
     */
    @Test
    fun dayCell_pastDeadlineIsAbsentWhileFutureDeadlineIsPresent() {
        val today = LocalDate.of(2026, 8, 13)
        val pastDeadline = today.minusDays(2)
        val futureDeadline = today.plusDays(2)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = stateWithDays(
                        today = today,
                        goalDates = setOf(pastDeadline, futureDeadline),
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        // Future deadline cell displays its date-scoped shader host
        composeRule.onNodeWithTag("calendar_day_$futureDeadline").assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("calendar_day_${futureDeadline}_deadline_shader") and
            hasAnyAncestor(hasTestTag("calendar_day_$futureDeadline")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

        // Past deadline cell displays its number but no deadline shader host
        composeRule.onNodeWithTag("calendar_day_$pastDeadline").assertIsDisplayed()
        composeRule.onNode(
            hasTestTag("calendar_day_${pastDeadline}_number") and
            hasText(pastDeadline.dayOfMonth.toString()) and
            hasAnyAncestor(hasTestTag("calendar_day_$pastDeadline")),
        useUnmergedTree = true,
        ).assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodes(
                hasTestTag("calendar_day_${pastDeadline}_deadline_shader") and
                hasAnyAncestor(hasTestTag("calendar_day_$pastDeadline")),
            useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        )
    }

    // =========================================================================
    // Selection: no outline rendered, tap dispatches SelectDate
    // =========================================================================

    /**
     * Tapping a day dispatches CalendarAction.SelectDate(date).
     */
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

    /**
     * Selected day retains its date-scoped number semantics.
     */
    @Test
    fun selectedDayKeepsDateScopedNumber() {
        val today = LocalDate.of(2026, 8, 13)
        val selectedDay = today.plusDays(3)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    state = stateWithDays(today = today, selectedDate = selectedDay),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        // Selected day cell container is displayed
        composeRule.onNodeWithTag("calendar_day_$selectedDay").assertIsDisplayed()

        // Day number for selected date is present as a date-scoped child
        composeRule.onNode(
            hasTestTag("calendar_day_${selectedDay}_number") and
            hasText(selectedDay.dayOfMonth.toString()) and
            hasAnyAncestor(hasTestTag("calendar_day_$selectedDay")),
        useUnmergedTree = true,
        ).assertIsDisplayed()

    }
}
