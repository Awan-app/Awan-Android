# Add task — length as an anchored menu, chip promoted to the design system

Date: 2026-07-23
Covers: `:core:design-system` chip + dropdown menu, `:feature:add-task` chip row,
`:feature:onboarding:impl` badge call site.

Follows `2026-07-23-chips-as-buttons-and-day-picking.md`. That plan stands; this one replaces how
the length attribute is chosen and moves two components down into the design system.

## Why

The length picker was an `AlertDialog` of preset chips. For a choice between eight fixed values it
reads as far too much ceremony: the sheet dims, the sentence disappears behind a modal, and a
"Cancel" button is needed to get back to where you already were. Day and time keep their dialogs —
a calendar and a clock face genuinely need the room — but length does not.

## 1 — Length hangs off its own chip

`DurationPickerDialog` is deleted. The presets now live in an `AwanDropdownMenu` anchored to the
length chip, so the sentence stays visible and the choice lands next to the thing it changes.

`AddTaskPicker.DURATION` still drives it, so nothing in the ViewModel or its tests changed — only
where the state is rendered. `AttributePickers` in the sheet handles `DATE`/`TIME` and lets
`DURATION` fall through to the chip row, which owns the anchor.

The dialog's title string (`add_task_duration_title`) is dropped from both locales; a menu hanging
off the chip that says "Any length" does not need to ask "How long?".

## 2 — The scroll indicator is a thumb, not a faded edge

Eight presets in a 200dp menu means four are hidden. The first attempt used the usual trick — a
surface-to-transparent gradient over the edge that can still scroll. It does not work here, and the
emulator showed exactly why: the menu's height landed on a row boundary (8dp padding + 4 × 48dp
rows = 200dp), so the last visible row ended flush and the gradient had nothing to fade. A list that
ends flush reads as the whole list no matter how it is tinted.

`AwanDropdownMenu` draws a scroll thumb instead: correct wherever the rows land, and it also shows
*how much* is left rather than just that something is. It is drawn in a `Canvas`, reading the scroll
offset inside the draw scope, so scrolling repaints one node instead of recomposing every row. The
visible/hidden booleans come from `derivedStateOf` over `canScrollForward/Backward` for the same
reason.

## 3 — `AwanChip` and `AwanBadge`

The attribute chip moves from `:feature:add-task` into the design system as `AwanChip`, with
`AwanChipDot` and `AwanChipDotSize` public so callers can swap the leading slot (`MandatoryToggle`
puts a rail there). It is the app's pressable pill and nothing about it was add-task-specific.

The name was already taken. The old `AwanChip` — a small alpha-tinted caption pill with an
`AwanChipTone` enum, used once, for the "New" badge in the onboarding first-task card — is a
different component: a readout, no rim, no press. It is renamed `AwanBadge` / `AwanBadgeTone`.

Two names because they are two things. Merging them would mean a variant flag threading two sets of
visuals through one body, which is more code than the rename and leaves both call sites less clear.

## Files

- `core/design-system/.../AwanDropdownMenu.kt` — new: `AwanDropdownMenu`, `AwanDropdownMenuItem`
- `core/design-system/.../AwanChip.kt` — the pressable chip, moved from `:feature:add-task`
- `core/design-system/.../AwanBadge.kt` — the old `AwanChip`, renamed
- `feature/add-task/.../ui/components/TaskAttributeChips.kt` — anchors the menu
- `feature/add-task/.../ui/AddTaskSheet.kt` — `DURATION` no longer a dialog branch
- `feature/add-task/.../ui/components/DurationPickerDialog.kt` — deleted
- `feature/onboarding/impl/.../steps/FirstTaskStep.kt` — `AwanBadge` call site
- `res/values/strings.xml` + `res/values-ar/strings.xml` — `add_task_duration_title` removed

## Implementation notes (what actually differed)

Verified with `./gradlew testDebugUnitTest` (all green), `:app:installDebug`, and by driving the
sheet on emulator-5554: tapped the "Any length" chip → menu opens anchored to it showing four of
eight rows with the thumb parked at the top → swiped inside the menu → rows scrolled, thumb
travelled to mid-track, top row cut mid-height.

**The name collision was found by the compiler, not by looking.** `AwanChip` already existed and the
moved file overwrote it; `:feature:onboarding:impl` was the only thing that noticed. Before moving a
component into `:core:design-system`, grep the target name there first — the module is large enough
that "there's no chip yet" is a guess, not a fact.

**A faded edge is not a scroll indicator on a fixed-height menu.** Covered above; the trap is that
it looks correct in code and disappears entirely at one specific height. Any future "there's more
below" affordance should assume the cut can land exactly on a content boundary.

### Deviations

- The first pass wrote the menu inline in `TaskAttributeChips`, and only the second request moved it
  into the design system. It ended up more general than the feature needed (`maxHeight` parameter,
  slot content) because the thumb logic is the part worth having once rather than per feature.
- The thumb is tinted `colors.meta`, not `colors.line`. `line` is the chip border tone and was too
  faint to register against a white menu surface on the device.
