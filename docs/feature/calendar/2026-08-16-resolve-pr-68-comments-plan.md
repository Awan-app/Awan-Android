# PR #68 Review Comments Resolution Plan

**Date:** 2026-08-16  
**Jira Issue:** AWAN-140  
**Pull Request:** [#68](https://github.com/Awan-app/Awan-Android/pull/68) (refactor/calendar-screen)

## Overview

Address code review feedback on PR #68 regarding deadline progress calculation, date mapping verification, and database setup.

## Review Feedback & Resolution

### 1. Database Schema & Migration Strategy (Comments #1 & #2)
- **Reviewer Comment:** Recommended keeping Room schema export enabled and avoiding destructive downgrade fallback in this PR.
- **Resolution:** Preserved the database configuration as intended for the development phase per explicit project instructions until v1.0 release.

### 2. Goal Deadline Runway Edge Cases (Comment #3)
- **Reviewer Comment:** Handle cases where createdAt >= targetDate explicitly in calculateDeadlineProgress rather than coercing duration to 1 day.
- **Resolution:** Added an explicit guard if (!start.isBefore(goal.targetDate)) return 0f to CalendarDateMapper.calculateDeadlineProgress. When a goal's creation date is on or after its target date, there is zero duration runway remaining, returning 0.0f (due immediately) cleanly without division/coercion anomalies.
- **Tests Added:** Unit test calculatesDeadlineProgressWhenCreatedAtIsOnOrEqualToOrAfterTargetDate() in CalendarDateMapperTest covering createdAt == targetDate and createdAt > targetDate.

### 3. Deadline Determination Verification (Comment #4)
- **Reviewer Comment:** Verify that hasDeadline is based on an actual deadline/target-date condition.
- **Resolution:** Verified and documented in CalendarDateMapper.buildMonthDays that the input goals are already filtered to active non-inbox goals with valid target dates by filterAndSortUpcomingGoals, ensuring dayGoals.isNotEmpty() && !date.isBefore(today) accurately identifies active upcoming deadlines.

## Implementation notes (what actually differed)

- **Verification:** Ran ./gradlew :feature:calendar:impl:testDebugUnitTest :core:design-system:testDebugUnitTest. All tests pass (0 failures).
- **Files Modified:**
  - feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarDateMapper.kt
  - feature/calendar/impl/src/test/java/com/awan/feature/calendar/impl/presentation/CalendarDateMapperTest.kt
