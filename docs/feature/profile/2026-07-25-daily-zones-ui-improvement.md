# Daily Zones UI Improvement — 2026-07-25

## Goal

Improve the existing Daily Zones screen without rebuilding it. Add missing interactions (Customize a day picker, live zone card preview, 3-letter day abbreviations, override indicator dots, improved error handling) and fix a critical flow bug where "Customize a day" was calling the override API directly instead of navigating to the day-detail screen.

## Scope — Additive Changes Only

No new ViewModels, no new screens, no architecture change.

### Files Changed

| File | Change |
|---|---|
| `DailyZonesScreen.kt` | DaySelector 3-letter abbrevs + override dots; RoutineSummaryCard remove broken dropdown + add edit route; AddEditZoneSheet live preview + hint; CustomizeDayPickerSheet composable; updated signature |
| `ProfileEntryProvider.kt` | Wire `onNavigateToDayDetails` and `onNavigateToRoutineDetails` into DailyZonesScreen; remove old `onCustomizeDay = viewModel::customizeDay` |
| `DailyZonesViewModel.kt` | ZONE_OVERLAP and network error improvements in `saveZones()` and `addZone()` |
| `values/strings.xml` | New keys: add/customize/reset/error strings |
| `values-ar/strings.xml` | Arabic translations for new keys |

## Key Design Decisions

### "Customize a day" flow
- Old (broken): button → `viewModel.customizeDay()` → `POST /v1/template-overrides` for currently selected day
- New (correct): button → `CustomizeDayPickerSheet` → pick any day → navigate to `DayDetailsRoute(date)` → `DayDetailsViewModel` handles override creation via existing `POST /v1/template-overrides`

### "Edit Default Routine" path
- Single path: `RoutineSummaryCard` "Edit Default Routine" text button → `RoutineDetailsRoute(templateId)` → existing `RoutineDetailsScreen` → "Edit routine" button → `EditRoutineRoute`
- No duplicate flow introduced. The `EditRoutineRoute` is already wired.

### Effective zones logic
- `DailyZonesViewModel.updateSelectedDayData()` computes effective zones from cached `templates + overrides` list. This is correct: override zones take priority, otherwise Default template zones show.
- `DayDetailsViewModel` uses `GET /v1/zones/date/{date}` for the per-day detailed view. Both approaches are appropriate for their context.

### Pause / Resume
- **Not implemented.** `DailyZone` model and `ZoneDto` have no `isEnabled`/`isPaused` field. Backend does not support this. No fake persistence introduced.

## API Endpoints Used

- `GET /v1/templates` — load Default template + zones
- `GET /v1/template-overrides` — load override days
- `PUT /v1/templates/{templateId}/zones` — add/edit/delete/reorder zones in Default template
- `POST /v1/template-overrides` — create custom day schedule (via DayDetailsViewModel)
- `PUT /v1/template-overrides/{overrideId}/zones` — edit custom day zones (via DayDetailsViewModel)
- `DELETE /v1/template-overrides/{overrideId}` — reset day to Default
- `GET /v1/zones/date/{date}` — effective zones for a date (via DayDetailsViewModel)

## Backend Limitations

- No `enabled`/`paused` field on zones → Pause/Resume not implementable
- `ZONE_OVERLAP` error code handled client-side (local validation in ViewModel) and gracefully if returned by backend
- `DAY_ALREADY_ASSIGNED` error code: handled in customizeDay error path
