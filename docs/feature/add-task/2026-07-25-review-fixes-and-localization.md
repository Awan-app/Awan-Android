# PR #14 review comments — triage and fixes

## Context

PR #14 (`feature/AWAN-82-quick-add-task`) received 5 inline review comments from @ZeiadT, all
submitted against commit `7009e29`. Two commits have landed since (`bc5f2af`, `bb9e824` — the
AWAN-89 AI-task work), which silently resolved three of them. This plan verifies each comment
against the code as it stands today and fixes what's genuinely still broken.

Note on vocabulary: the reviewer's "zone" is what the code now calls **category** (`@work`). The
`bb9e824` commit renamed the concept and introduced a separate `DayZone`. Comments written against
the old naming still read as "zone".

## Triage

| # | Comment | Verdict |
|---|---------|---------|
| 1 | P1 — `@zone` silently lost for Inbox tasks (`CreateTaskUseCase.kt:19`) | **Already fixed** in `bb9e824` |
| 2 | P1 — zone field missing from mapper (`TaskMappers.kt:22`) | **Already fixed** in `bb9e824` |
| 3 | P1 — explicit clock ignored when a day-part phrase is present (`TaskInputParser.kt:96`) | **Valid — fix** |
| 4 | P2 — zone resolution races submission / hammers network (`AddTaskState.kt:35`) | **Already fixed** in `bb9e824` |
| 5 | P2 — Arabic UI broken by picker edits (`TaskInputWriter.kt:43`) | **Valid — fix** |

### Why 1, 2 and 4 are already resolved

- `TaskDraft` now carries `categoryId` (`core/model/.../TaskDraft.kt:21`), `AddTaskState.toDraft()`
  populates it from `resolvedCategory` (`AddTaskState.kt:157`), and `TaskDraft.toRequest()` maps it
  to `CreateTaskRequest.categoryId` (`TaskMappers.kt:33`). The Inbox path
  (`CreateTaskUseCase.kt:25` → `taskRepository.createTask(draft)`) goes through the same mapper, so
  the category persists with or without a schedule.
- `CreateTaskRequest` has a `categoryId` field (`core/network/.../CreateTaskRequest.kt`).
- Category resolution is no longer per-keystroke: `AddTaskViewModel.loadCategories()` fetches once in
  `init` (`AddTaskViewModel.kt:50,164`) and `onInputChanged` matches the typed token against the
  cached list locally (`AddTaskViewModel.kt:132`, `matching()` at `:185`). No network call on typing,
  so no race with submit.

These need a reply on the PR pointing at the commits, not code changes.

---

## Fix 1 — explicit clock beats a day-part phrase (P1)

**Bug.** `TaskInputParser.parse` at `TaskInputParser.kt:104`:

```kotlin
val time = range?.start ?: anchored?.toLocalTime() ?: explicitTime
```

`anchored = relative ?: dayPart`. For `"Call this morning at 10am"`, `matchDayPart` returns
today@09:00 and wins over `explicitTime` (10:00), so the task lands at 09:00.

**Change.** `core/domain/src/main/kotlin/com/awan/app/core/domain/task/parser/TaskInputParser.kt`:

```kotlin
// An anchor phrase names an hour only as a default; a stated clock time always beats it.
val time = range?.start ?: explicitTime ?: anchored?.toLocalTime()
```

`date` (line 105) is untouched, so the day anchor from the phrase still stands. This also improves
`"Meeting in 3 days at 2pm"`, which currently keeps `now`'s time-of-day.

No behaviour change to `hasExplicitTime` (still `time != null`) or to `"Run this morning"` /
`"Call back in 2 hours"`, where `explicitTime` is null.

**Test** in `TaskInputParserTest.kt` (alongside `parts of this day resolve to their usual hour`,
line 301) — the exact input the reviewer named:

```kotlin
@Test
fun `an explicit clock time beats the hour a day part would default to`() {
    val result = parse("Call this morning at 10am")

    assertEquals("Call", result.title)
    assertEquals(LocalDateTime.of(2026, 7, 22, 10, 0), result.startAt)
}
```

---

## Fix 2 — localize the parser and the writer (P2)

**Problem.** Picker edits inject English parser syntax (`today at 3pm`, `for 45 min`) into the
user-visible task field regardless of app language. The writer alone can't be fixed: the parser
reads English only, so an Arabic phrase written back would fail to re-parse and the chips would go
blank. Both halves move together, per the user's decision.

`:core:domain` is Android-free, so `values-ar/strings.xml` is not reachable. The locale-varying
literals become domain data (`java.util.Locale` is JDK, not Android).

### New file — `core/domain/src/main/kotlin/com/awan/app/core/domain/task/parser/TaskLexicon.kt`

A plain data holder for every locale-varying literal, with two instances and a selector:

```kotlin
data class TaskLexicon(
    val weekdays: Map<String, DayOfWeek>,
    val hourUnits: List<String>, val minuteUnits: List<String>,
    val dayUnits: List<String>, val weekUnits: List<String>,
    val am: List<String>, val pm: List<String>,
    val rangeFrom: List<String>, val rangeTo: List<String>,
    val relativeIn: List<String>, val durationFor: List<String>, val timeAt: List<String>,
    val noon: List<String>, val midnight: List<String>,
    val morning: List<String>, val afternoon: List<String>, val evening: List<String>,
    val today: List<String>, val tonight: List<String>, val tomorrow: List<String>,
    val nextWeek: List<String>,
    val weekdayPrefixes: List<String>, val weekdaySuffixes: List<String>,
    // What the writer emits — always a form this lexicon's parser reads back to the same value.
    val writeToday: String, val writeTomorrow: String,
    val writeAt: String, val writeFor: String,
    val writeAm: String, val writePm: String,
    val writeHour: String, val writeHours: String, val writeMinutes: String,
    /** Joins `1h30`; null means write the total in minutes instead. */
    val hourMinuteSeparator: String?,
    val writeWeekday: Map<DayOfWeek, String>,
) {
    companion object {
        val English = TaskLexicon(/* today's literals, moved verbatim out of TaskInputParser */)
        val Arabic = TaskLexicon(/* اليوم، غدًا، الساعة، لمدة، دقيقة، ساعة، ص/م، من…إلى، بعد، … */)
        fun of(locale: Locale): TaskLexicon = if (locale.language == "ar") Arabic else English
    }
}
```

Arabic coverage, matching the English surface one-for-one:
- weekdays with and without the `ال` prefix (الاثنين/الإثنين، الثلاثاء، الأربعاء/الاربعاء، الخميس، الجمعة، السبت، الأحد/الاحد)
- `weekdaySuffixes` = القادم/القادمة/المقبل/المقبلة — Arabic puts the qualifier **after** the day
  (`الجمعة القادمة`), where English puts it before (`next fri`). English's list is empty. Both are
  consumed out of the title; neither changes the resolved date.
- units: ساعة/ساعات/ساعتين/س، دقيقة/دقائق/د، يوم/أيام/يومين/ي، أسبوع/أسابيع/اسبوعين
- meridiem: صباحًا/صباحا/صباح/ص and مساءً/مساء/م — **listed longest-first**, since regex alternation
  is leftmost-first and a bare `م` would otherwise shadow `مساء`
- connectors: من…إلى/الى/حتى، بعد (relative), لمدة (duration), الساعة/في الساعة/عند (at)
- literals: الظهر/ظهرًا، منتصف الليل، هذا الصباح/صباح اليوم، بعد الظهر، هذا المساء، اليوم، الليلة، غدًا/غدا/بكرة، الأسبوع القادم/المقبل

### `TaskInputParser.kt`

Two structural changes; every matcher body stays as it is.

1. **Regexes become per-lexicon.** The `private val` patterns move into a
   `private class Patterns(lexicon: TaskLexicon)` that builds them from the lexicon's word lists
   (`joinToString("|")`, each list sorted longest-first). Exactly two instances are built eagerly —
   `Patterns(English)` and `Patterns(Arabic)` — so there is no cache and no per-call compilation.

2. **`(?U)` on every pattern.** Java's `\b` and `\w` are ASCII-only by default, so `\bالجمعة\b`
   never matches. Prefixing each pattern with the inline `(?U)` flag turns on
   `UNICODE_CHARACTER_CLASS` (Kotlin's `RegexOption` has no equivalent). Applied uniformly to both
   lexicons — for ASCII input it changes nothing.

Public entry point gains a defaulted parameter, so every existing call site and test still compiles:

```kotlin
fun parse(input: String, now: LocalDateTime, locale: Locale = Locale.getDefault()): ParsedTaskInput
```

3. **Arabic-Indic digits.** `٠١٢٣٤٥٦٧٨٩` (and `۰-۹`) map 1:1 to `0-9`, so a normalizing pass
   preserves string length and therefore every token `IntRange`. Matchers run on the normalized
   copy; `titleFrom(input, claimed)` and the emitted token ranges stay on the **original** input, so
   highlights in `TokenHighlight.kt` keep lining up and the user's own digits are never rewritten.

### `TaskInputWriter.kt`

Each public function takes `locale: Locale = Locale.getDefault()` and resolves a `TaskLexicon`;
`timePhrase`, `datePhrase`, `clockPhrase` and `durationPhrase` read their words from it instead of
hardcoding. `categoryPhrase` is unchanged (`@name` is not language-bearing). The class KDoc's claim
that "the words are English even when the UI is not" is replaced by the real contract: *the writer
emits in the reading lexicon's language, and every phrase it emits parses back to the same value.*

`datePhrase`'s weekday branch uses `lexicon.writeWeekday[date.dayOfWeek]` rather than
`dayOfWeek.name.lowercase()`, so the emitted word is one the lexicon's `weekdays` map contains.
`durationPhrase`'s `1h30` branch falls back to total minutes when `hourMinuteSeparator` is null
(Arabic), because `لمدة 90 دقيقة` reads naturally and round-trips exactly.

### Call sites

- `ParseTaskInputUseCase` / `ApplyTaskAttributeUseCase` (`core/domain/.../task/usecase/`) pass
  `Locale.getDefault()` at call time — read per invocation, not injected, so a language change is
  picked up on the next keystroke without any DI plumbing.
- `AddTaskViewModel.intoReview` (`AddTaskViewModel.kt:241-251`) currently calls
  `TaskInputWriter.withDuration` / `withCategory` **directly from presentation**. Route both through
  the existing `applyTaskAttribute(sentence, parsed, TaskAttribute.Lasting(...) / .In(...))` — same
  semantics, drops the `TaskInputWriter` import from the feature module, and fixes a standing
  Clean-Architecture violation (CLAUDE.md: ViewModels consume use cases only).

### Not in scope

Bidi rendering of a mixed Arabic sentence containing `@category` and ASCII digits. The writer emits
ASCII digits in both languages; that is what Arabic Android keyboards produce by default and what
the parser reads without normalization.

---

## Tests

- `TaskInputParserTest.kt` — the day-part regression above. Existing tests are unchanged and must
  still pass (they exercise the English lexicon through the defaulted parameter).
- **New** `core/domain/src/test/kotlin/.../parser/TaskInputParserArabicTest.kt` — the English suite's
  cases in Arabic: bare title, غدًا + clock, weekday + suffix (`اجتماع الجمعة القادمة`), `من 3م إلى 5م`
  range, `لمدة 45 دقيقة`, `بعد 20 دقيقة`, `هذا الصباح الساعة 10`, `@work`, Arabic-Indic digits
  (`غدًا الساعة ٣م`), and a title that must survive untouched.
- `TaskInputWriterTest.kt` — mirror the existing phrase assertions for Arabic, plus the contract
  test that matters: **round-trip** — write a time / date / duration under `Locale("ar")`, re-parse
  the result with the same locale, assert the value comes back identical. Also assert token ranges
  are non-overlapping after an Arabic write (the existing `token ranges never overlap` test shape).
- Guard determinism: `TaskInputParserTest` / `TaskInputWriterTest` / `AddTaskViewModelTest` set
  `Locale.setDefault(Locale.ENGLISH)` in `@Before` (restore in `@After`) so a non-English CI machine
  can't flip the English suites onto the Arabic lexicon.

## Verification

1. `./gradlew :core:domain:testDebugUnitTest` — parser + writer suites, English and Arabic.
2. `./gradlew :feature:add-task:testDebugUnitTest` — `AddTaskViewModelTest` (52 tests) still green
   after routing `intoReview` through `ApplyTaskAttributeUseCase`.
3. `./gradlew testDebugUnitTest` — full suite; the last full run predates `bb9e824`, so this also
   clears the PR's own "not re-run since the last feature commit" caveat.
4. `./gradlew detekt` — the lexicon is a wide data class; expect to need `@Suppress("LongParameterList")`
   or a detekt config note on `TaskLexicon`.
5. Manually, in Arabic (system language → العربية, or per-app language): open the add-task sheet,
   tap the date chip → pick a day → pick a time, and confirm the field reads `اليوم الساعة 3م` with
   the chips still populated (chips populated is the proof the writer's output re-parsed). Then tap
   the duration chip and confirm `لمدة 45 دقيقة` appears and the length chip updates. Repeat in
   English and confirm the sentence is byte-identical to today's behaviour.
6. Type `Call this morning at 10am` in English and confirm the when-chip reads 10:00, not 9:00.

## Follow-up on the PR

Reply to the three obsolete threads pointing at `bb9e824` (category threaded end-to-end through
`TaskDraft.categoryId` → `CreateTaskRequest`; categories loaded once in `init`, matched locally).
Reply to the parser and Arabic threads with the commits that fix them.

Per CLAUDE.md, save this plan to `docs/feature/add-task/2026-07-25-review-fixes-and-localization.md`
before work starts, and append an `## Implementation notes (what actually differed)` section when
done.

---

## Implementation notes (what actually differed)

**Status.** `./gradlew testDebugUnitTest` — 177 tests, 0 failures (was 145 before this work; +20
`TaskInputParserArabicTest`, +11 `TaskInputWriterArabicTest`, +2 parser regressions). `./gradlew
assembleDebug --rerun-tasks` clean; the only warnings are the pre-existing unresolved
Compose opt-in markers.

### Deviations

- **`./gradlew detekt` does not exist.** CLAUDE.md documents it, but no detekt plugin, task or
  config file is present anywhere in the repo. The `@Suppress("LongParameterList")` on `TaskLexicon`
  is left in place as documentation of intent, not because anything checks it. CLAUDE.md's "Static
  analysis" line is stale.
- **`ParseTaskInputUseCase` / `ApplyTaskAttributeUseCase` were not touched.** The plan had them pass
  `Locale.getDefault()` explicitly; the defaulted parameter on `parse` / `withTime` already evaluates
  it per call, so an explicit argument would have been the same code written twice.
- **`TaskLexicon` is a plain `class`, not a `data class`.** Nothing compares or copies it, and
  `Patterns` selects between the two instances by identity (`===`).
- **The English suites pin the locale with `Locale.setDefault` in `@Before`/`@After`** rather than
  threading `Locale.ENGLISH` through every helper. `TaskInputWriterTest` calls `durationPhrase` /
  `timePhrase` directly in eight places; a setup hook was one edit instead of eight.

### Traps

- **`durationPhrase(60)` briefly regressed to `"for hour"`.** The lexicon made the singular hour a
  word (`writeHour = "hour"`), which reads correctly in Arabic (`لمدة ساعة`) — but the parser's
  `DURATION` regex only matches a unit that *follows a digit*, so a bare unit does not round-trip.
  Both languages must keep the numeral: `for 1 hour`, `لمدة 1 ساعة`. Caught by
  `re-picking replaces rather than appends`. Easy to reintroduce by "fixing" the stilted Arabic.
- **`alt()` must emit its own `(?:…)` group.** Callers concatenate `\s+` onto it, and
  `(?:الساعة|في الساعة|عند)\s+` is not `الساعة|في الساعة|عند\s+` — the bare form binds `\s+` to the
  last alternative only, so `الساعة 3م` silently stopped matching. The wrapping now lives inside
  `alt()` so no caller can forget it.
- **`(?U)` is mandatory and invisible when missing.** Java's `\b`/`\w` are ASCII-only by default, so
  `\bالجمعة\b` matches nothing at all — every Arabic pattern fails silently rather than erroring.
  Kotlin's `RegexOption` has no `UNICODE_CHARACTER_CLASS`, hence the inline flag in `compile()`.
- **Alternations are sorted longest-first.** Arabic `م` (pm) would otherwise shadow `مساء`, and the
  match would end mid-word. English survived without this only because `\b` forced backtracking.
- **Digit folding must stay 1:1.** `toAsciiDigits()` maps `٠-٩`/`۰-۹` one code point to one code
  point precisely so every `TaskToken.range` still indexes the string the user typed —
  `titleFrom` and the UI highlights read the original, only the matchers read the folded copy.
  Anything that changes length here (stripping, normalising) breaks highlighting.
- **The category token reads the raw input, not the folded copy.** It is a name matched against the
  user's own categories, so folding its digits would stop it matching.

### Review threads

Three of the five PR comments were already fixed by `bb9e824` before this work started
(category threaded end-to-end, categories loaded once in `init`). Only the day-part precedence and
the Arabic writer needed code. See the triage table above.
