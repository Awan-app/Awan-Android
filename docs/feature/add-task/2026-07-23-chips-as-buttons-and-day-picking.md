# Add task — chips as buttons, day-then-time picking, bare "at 3"

Date: 2026-07-23
Covers: `:core:domain` task parser/writer, `:core:design-system` button + date picker,
`:feature:add-task` chip row.

Follows `2026-07-23-quick-add-task-plan.md`. That plan stands; this one changes four things on top
of it.

## Why

Screenshot review of the shipped sheet turned up four problems:

1. `Go to gym at 3` parses to nothing. The parser understands `3pm`, `15:00` and `from 3 to 5pm`,
   but a bare hour after `at` — the single most common way people write a time — falls through and
   stays in the title.
2. The day cannot be changed. The when-chip opens a *time* picker, so a task can only ever be moved
   within the day the sentence already names.
3. The chips read as Material filter chips: flat, 2dp outline, no rim, no press sink. Every other
   pressable surface in the app is a chunky rim button. They are the one thing on the sheet that
   looks borrowed from another app.
4. Mandatory is indistinguishable from the chips either side of it, so nothing says it toggles.

## 1 — Bare `at N` times

New matcher between the meridiem and 24-hour ones:

```
TIME_AT = \bat\s+(\d{1,2})(?::(\d{2}))?\b
```

The `at` keyword is the whole guard. Without it `Read chapter 3` and `Review 5 PRs` would become
times, and the existing rule — the parser can only ever be *less* helpful, never wrong — forbids
that. With it, the user has already said the number is a clock.

Ambiguity rule: a bare hour of 1–7 reads as afternoon (`at 3` → 15:00), 8–23 as written
(`at 8` → 08:00, `at 20` → 20:00). This is how people speak, and `resolveStart` already rolls a
past time to tomorrow, so `at 8` typed at 09:00 lands tomorrow morning rather than in the past.

Runs *after* `TIME_MERIDIEM`, so `at 3pm` is still claimed by the meridiem rule and never shifted
twice. Runs *before* `TIME_24H`, so `at 3:30` gets the same daytime treatment as `at 3` — the two
must not disagree. A colon time with no `at` (`15:00`) is unambiguous as written and is left alone.

## 2 — Day first, then time

The when-chip now opens the date picker, and confirming the date opens the time picker. Two steps,
in the order the user stated: pick the day, then pick the time inside it.

- `AddTaskPicker.DATE` added; the chip raises `PickerOpened(DATE)`.
- `DatePicked(date)` parks the date in `AddTaskState.pendingDate` and advances `openPicker` to
  `TIME`. Nothing is written to the sentence yet.
- `TimePicked(minutes)` combines `pendingDate` (falling back to the day already in the sentence,
  then today) with the clock and writes the whole moment.
- Backing out of the *time* step with a date pending writes the date alone rather than discarding
  the edit. The confirm label on the date step is therefore "Next", not "Set" — it names what
  happens.
- The date picker refuses days before today. A new task placed in the past is never what was meant,
  and the conflict engine would have to reject it anyway.

Writing a date alone needs `TaskInputWriter.withDate`. It keeps a clock time the sentence already
stated (`Gym tomorrow at 6pm` + Friday → `Gym friday at 6pm`) instead of flattening it to
`Gym friday`, because the DATE_TIME token set covers both the day and the clock and replacing all of
them is what stops phrases piling up.

## 3 — Pickers as one swappable seam

`AwanTimePickerDialog` already hides Material's time picker behind a domain-shaped API (minutes in,
minutes out). `AwanDatePickerDialog` joins it with the same shape (`LocalDate` in, `LocalDate` out,
labels passed in so each feature keeps its own localised strings).

That pair *is* the abstraction: no interface, no factory, no injection. Both are the only files that
know Material's pickers exist, so replacing them with hand-drawn Awan composables later is a change
to two file bodies and nothing else. A picker interface with one implementation would buy nothing a
`@Composable` function signature does not already buy.

## 4 — Chips become buttons

New `AwanButtonVariant.Chip` in the design system: pill shape, `buttonCompact` type, 40dp face on
the standard 4dp rim, compact padding. It inherits the family's press physics — the face sinks 4dp
down and 2dp across into its own rim — so a chip presses exactly like the Add task button under it.

`AwanButton` gains one optional `rimStyle: Style` slot alongside the existing face `style` slot.
Chips are tone-coloured per attribute (sky for when, violet for length, lavender for zone, tangerine
for mandatory) and the rim is what carries the tone into the third dimension; without the slot every
chip would sit on the same grey shelf regardless of tone.

Touch target: the chip's outer minimum drops from 48dp to 44dp, which is exactly face + rim, so the
pill is the target with nothing dead around it.

The chip row moves from `horizontalScroll` to `FlowRow`. The mandatory control was the last chip in
a scrolling row — i.e. the one most likely to be off-screen — which is part of why it went unnoticed.

## 5 — Mandatory as a switch

Signature element. Every chip carries a leading dot. The mandatory chip's dot sits **on a rail**, and
toggling slides it across on the bouncy spring while the pill's tone and label swap:

```
( ●───  Can skip )        ( ───●  Must do )
  line rail, grey           tangerine rail, tangerine pill
```

The affordance is built out of the chip family's own parts — it is the same pill, the same dot, the
same rim — so it reads as a member of the row that happens to move. Nothing else is added: no icon,
no rotation, no second colour. The label carries the state in words (`Must do` / `Can skip`), which
is also what TalkBack announces, so the visual and the spoken state cannot drift apart.

Copy: "Mandatory"/"Optional" are the database's words. "Must do"/"Can skip" are the user's, and
"Can skip" matches the Skip option in the Intelligent Nudge, so one vocabulary covers both screens.

## Files

- `core/domain/.../parser/TaskInputParser.kt` — `TIME_AT`, `matchBareClockTime`
- `core/domain/.../parser/TaskInputWriter.kt` — `withDate`
- `core/domain/.../usecase/ApplyTaskAttributeUseCase.kt` — `TaskAttribute.On`
- `core/design-system/.../AwanButton.kt` — `Chip` variant, `rimStyle` slot
- `core/design-system/.../AwanStyles.kt` — `chipButtonRim`, `chipButtonFace`
- `core/design-system/.../AwanDatePickerDialog.kt` — new
- `feature/add-task/.../presentation/*` — `DATE` picker, `pendingDate`, `DatePicked`
- `feature/add-task/.../ui/components/AttributeChip.kt` — rebuilt on `AwanButton`
- `feature/add-task/.../ui/components/MandatoryToggle.kt` — new
- `feature/add-task/.../ui/components/TaskAttributeChips.kt` — `FlowRow`, wiring
- `feature/add-task/.../ui/AddTaskSheet.kt` — date picker branch
- `res/values/strings.xml` + `res/values-ar/strings.xml`

## Tests

- Parser: `at 3` → 15:00, `at 8` → 08:00 tomorrow, `at 3:30` → 15:30, `at 15:00` unchanged,
  `at 3pm` not double-shifted, `Read chapter 3` still a plain title.
- Writer: `withDate` round-trips through the parser and preserves an existing clock time.
- ViewModel: date → time writes both; date → cancel writes the day alone; date → cancel on a
  sentence with a time keeps the time.

## Implementation notes (what actually differed)

Verified with `./gradlew clean testDebugUnitTest` (all green), `assembleDebug`,
`:feature:add-task:lintDebug`, `:core:design-system:lintDebug`, and by driving the real sheet on
emulator-5554: typed `Go to gym at 3` → title `Go to gym` + chip `Tomorrow at 3:00 PM`; tapped the
chip → date step on Jul 24 with everything before today greyed out → picked Jul 28 → Next → time
step at 3:00 PM → Set → sentence rewritten to `Go to gym tuesday at 3pm`, chip
`Jul 28, 2026 at 3:00 PM`; toggled mandatory both ways.

`./gradlew detekt` does **not** exist in this project despite CLAUDE.md listing it. Detekt is not
applied by any convention plugin. Not fixed here — flagged so the next person doesn't assume the
gate ran.

### Two things only the running app showed

**Translucent chip fill over a tone-coloured rim is invisible.** The chip started as a direct port
of the old flat one: face tinted `tone.copy(alpha = 0.16f)`, label inked in `tone`. That worked when
the chip sat on the sheet background. It does not work on the new rim, because the rim behind the
face is now that same solid tone — 16% orange composited over solid orange is solid orange, so the
active mandatory chip rendered as a solid orange slab with its label the same colour as its
background, i.e. no label at all. The fix is `lerp(surface, tone, 0.18f)`: opaque, so what is behind
it cannot bleed through. The label also moved from `tone` to `textPrimary` — on an 18% tint, tone-on-
tint was legible but weak, and the dot, border and rim already carry the tone three times over.

Easy to reintroduce: any future chip/badge state that reaches for `.copy(alpha = …)` as a "tint" is
one rim away from the same bug. On a rim component, tint by blending, never by alpha.

**The Material colour scheme was missing whole role families.** `AwanTheme` mapped `surface` but not
`surfaceContainer*`, and not `primaryContainer` / `secondaryContainer` / `tertiary*`. Nothing in the
app had surfaced it because nothing used a Material container component — the first date picker came
up in baseline M3 lavender with a pink AM/PM switch. Fixed in `AwanTheme` rather than by passing
`colors = …` per dialog: one mapping covers the date picker, the time picker, and every menu, sheet
and Material control added later. This was not in the plan and is the change with the widest blast
radius in this batch — worth a look on any screen that uses a Material component.

### Deviations

- No `add_task_toggle_state_*` strings. The plan considered a `stateDescription`; the label already
  changes between `Must do` and `Can skip`, so a state description would have duplicated it.
  `Role.Switch` is set; the label is the state, spoken and seen.
- `AwanDatePickerDialog` ended up with no `colors` argument at all — see above, the scheme supplies
  it. Deleting it is why the file is shorter than the plan implies.
- `AttributeChip` grew a `leading` slot (defaulting to the dot) so `MandatoryToggle` could swap in
  the rail without a second chip implementation. The plan did not say how the two would share a body.
- A chip with `onClick = null` (the zone readout) renders through the same `AwanButton` and clears
  its semantics, rather than having a separate non-interactive branch. One render path, one press
  behaviour, no divergence to keep in sync.
