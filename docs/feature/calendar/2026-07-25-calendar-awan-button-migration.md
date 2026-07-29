# Calendar Screen AwanButton Migration Plan

**Date**: 2026-07-25  
**Feature**: Calendar (`:feature:calendar:impl`)  
**Scope**: Replace `AwanIconButton` with `AwanButton` (using composable icons) for month navigation and back button in `CalendarScreen`.

## Motivation
Standardize UI components across screens by migrating icon buttons in `CalendarScreen` from `AwanIconButton` to `AwanButton` with the composable `icon` parameter, configured with zero content padding, `minHeight(0.dp)`, and `size(38.dp)` to preserve the original 38dp compact button dimensions safely without Compose layout constraint exceptions.

## Root Cause of Previous Exception
- `secondaryButtonFace` in `AwanStyles.kt` defines `minHeight(48.dp)`.
- Using `requiredSize(38.dp)` forced `maxHeight = 38.dp`. Because `minHeight (48.dp)` was greater than `maxHeight (38.dp)`, Compose's `Constraints` threw `java.lang.IllegalArgumentException: maxHeight must be >= than minHeight`.
- Fix: Passing `minHeight(0.dp)` in `Style { ... }` overrides the style token's 48dp minimum height, allowing `size(38.dp)` to measure cleanly.

## Changes Made
- Updated [CalendarScreen.kt](file:///Z:/Business/Awan/Awan-Android/feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/CalendarScreen.kt):
  - Removed import `com.awan.app.core.designsystem.AwanIconButton`.
  - Replaced back button with `AwanButton(..., icon = { Icon(Lucide.ArrowLeft, ...) })`.
  - Replaced month navigation buttons with `AwanButton(..., icon = { Icon(Lucide.ChevronLeft/Right, ...) })`.
  - Applied `style = Style { minHeight(0.dp); contentPadding(0.dp) }` and `Modifier.size(38.dp)`.
- Added localized string resources in [strings.xml](file:///Z:/Business/Awan/Awan-Android/feature/calendar/impl/src/main/res/values/strings.xml) and [values-ar/strings.xml](file:///Z:/Business/Awan/Awan-Android/feature/calendar/impl/src/main/res/values-ar/strings.xml):
  - `calendar_back`: "Back" / "رجوع"
  - `calendar_prev_month`: "Previous month" / "الشهر السابق"
  - `calendar_next_month`: "Next month" / "الشهر التالي"

## Implementation notes
- `./gradlew :feature:calendar:impl:testDebugUnitTest`: BUILD SUCCESSFUL.
