package com.awan.feature.calendar.impl.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awan.app.core.designsystem.AwanTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
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
     * The day number must be present via the cell tag; no decoration tags
     * (today_primary, streak_fire, streak_number, deadline_shader) are rendered.
     */
    @Test
    fun dayCell_plainMissed_showsNumberAndNoDecoration() {
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

        // The plain day cell is tappable and visible.
        val plainDay = today.minusDays(5)
        composeRule.onNodeWithTag("calendar_day_$plainDay").assertIsDisplayed()
        // No streak, deadline, or badge decorations on the grid (today itself still shows
        // today_primary but plain/missed days contribute none of the streak or deadline tags).
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_fire").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_number").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_deadline_shader").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_badge").fetchSemanticsNodes().size)
    }

    /**
     * Today (no streak, no deadline) — primary sky circle is shown, number is
     * displayed inside it, and no streak/shader tags are present.
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
        composeRule.onNodeWithTag("calendar_day_today_primary").assertIsDisplayed()
        // No streak or deadline decorations expected
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_fire").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_number").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_deadline_shader").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_badge").fetchSemanticsNodes().size)
    }

    /**
     * Streak day (not today, no deadline) — static fire host and the
     * fire-surface number circle are both shown; no today-primary or shader.
     */
    @Test
    fun dayCell_streak_showsFireHostAndNumberCircle() {
        val today = LocalDate.of(2026, 8, 13)
        val yesterdayStreak = today.minusDays(1) // past streak day, not today
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
        composeRule.onNodeWithTag("calendar_day_streak_fire").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_day_streak_number").assertIsDisplayed()
        // The full grid also renders today (with today_primary) — we only verify that
        // no deadline shader is rendered on the grid, and no badge appears.
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_deadline_shader").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_badge").fetchSemanticsNodes().size)
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
        composeRule.onNodeWithTag("calendar_day_deadline_shader").assertIsDisplayed()
        // The full grid also renders today (with today_primary) — we only verify that
        // no streak decorations appear anywhere.
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_fire").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_number").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_badge").fetchSemanticsNodes().size)
    }

    /**
     * Today + deadline (no streak) — primary circle, circular shader host, and
     * number are all present; no fire host or streak badge.
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
        composeRule.onNodeWithTag("calendar_day_today_primary").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_day_deadline_shader").assertIsDisplayed()
        // No streak decorations
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_fire").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_number").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_badge").fetchSemanticsNodes().size)
    }

    /**
     * Today + streak (no deadline) — primary-tinted fire host and anchored
     * number circle are present; no deadline shader or fire badge.
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
        // Fire host (primary-tinted)
        composeRule.onNodeWithTag("calendar_day_streak_fire").assertIsDisplayed()
        // Anchored number circle inside the fire host
        composeRule.onNodeWithTag("calendar_day_streak_number").assertIsDisplayed()
        // No plain today_primary or shader in this branch
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_today_primary").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_deadline_shader").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_badge").fetchSemanticsNodes().size)
    }

    /**
     * Today + streak + deadline — primary circle, circular shader host, number,
     * AND top-right fire badge are all present.
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
        composeRule.onNodeWithTag("calendar_day_today_primary").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_day_deadline_shader").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_day_streak_badge").assertIsDisplayed()
        // Fire host/number circle are NOT in this branch (badge replaces them at top-right)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_fire").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("calendar_day_streak_number").fetchSemanticsNodes().size)
    }

    // =========================================================================
    // Past-deadline guard
    // =========================================================================

    /**
     * A goal date that has already passed must NOT render a deadline shader,
     * while a future goal date on the same screen still does.
     * `buildMonthDays` guards hasDeadline with `!date.isBefore(today)`.
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

        // Future deadline cell must have the shader host — confirms guard passes it through
        composeRule.onNodeWithTag("calendar_day_$futureDeadline").assertIsDisplayed()
        // Past deadline cell must be visible (rendered as plain/missed day)
        composeRule.onNodeWithTag("calendar_day_$pastDeadline").assertIsDisplayed()
        // Exactly one shader node in the tree: the future deadline.
        // If the past deadline leaked through, this count would be ≥ 2.
        val shaderNodes = composeRule.onAllNodesWithTag("calendar_day_deadline_shader").fetchSemanticsNodes()
        assertEquals(
            "Expected exactly one deadline shader (future only); past deadline guard may have failed",
            1,
            shaderNodes.size,
        )
    }

    // =========================================================================
    // Selection: no outline rendered, tap dispatches SelectDate
    // =========================================================================

    /**
     * The calendar must NOT render a selected-date outline for any day — the
     * selection ring was removed in the Task 1 refactor. Tapping a day must
     * still dispatch CalendarAction.SelectDate(date).
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
     * No selected-date outline tag is rendered anywhere in the calendar grid.
     * The implementation uses no outline/ring node — verifying its absence
     * confirms the design spec was met.
     */
    @Test
    fun noSelectedDateOutlineIsRendered() {
        val today = LocalDate.of(2026, 8, 13)
        val otherDay = today.plusDays(3)
        composeRule.setContent {
            AwanTheme {
                CalendarScreen(
                    // Select a non-today day so we can confirm no ring appears for it
                    state = stateWithDays(today = today, selectedDate = otherDay),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        // There is no "selected_outline" or equivalent tag anywhere in the tree
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("calendar_day_selected_outline").fetchSemanticsNodes().size,
        )
        // Both the currently-selected day and today cells are visible without outlines
        composeRule.onNodeWithTag("calendar_day_$otherDay").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_day_$today").assertIsDisplayed()
    }
}
