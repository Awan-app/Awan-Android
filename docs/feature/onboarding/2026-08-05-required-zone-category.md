# Per-user categories & required zone `categoryId`

## Context

The backend made `categoryId` **required** on every request body that contains a zone (7 endpoints), and
stopped auto-creating a category named after the zone. Categories are now per-user, seeded with 8 rows at
signup (General, Work, Personal, Health, Learning, Finance, Home, Social), and `zones[]` is validated
element-by-element — a partial zone object that used to slip through now returns **422 VALIDATION_ERROR**.

Verified against the Postman collection `Awan` (workspace *Dukkan*, updated 2026-08-05): `Create Template`,
`Bulk Update Template Zones`, `Add Zone to Template`, `Update Zone` and the override equivalents all carry
`"categoryId": "{{categoryId}}"`, and the `Categories` folder has Create / List / Get / Update with no Delete.
`CATEGORY_NAME_TAKEN` (409) is documented per-request but is **absent from the collection's error-code table**.

Today the app cannot satisfy this. `DailyZone` and `Zone` have no category field at all; `ZonesMapper.toDto()`
silently drops the nested `category` on every round-trip; `ZonesRepositoryImpl` never passes the `categoryId`
that `CreateZoneRequest` already declares; and `UpdateZoneRequest` has no such field. Every zone write in the
app currently 422s.

**Outcome:** zones carry a category end-to-end, onboarding suggests four zones whose names map 1:1 onto seeded
categories and lets the user change each one, and the category vertical gains the full CRUD surface.

## Out of scope (explicit)

- `feature/profile/**` and `docs/feature/profile/**` — the Daily Zones / Edit Routine templates-management
  feature. **Consequence to flag in the PR:** the mapper fix below makes profile's *edit an existing zone* path
  keep working for free (the id round-trips), but its *add a new zone* path builds a fresh `DailyZone` with no
  category and will 422. Needs its own ticket.
- Localizing `Zone.defaults` names (pre-existing violation, flagged by the AWAN-90 plan, left as-is).
- Category **management screen** (§5.3) — it would live in profile. The CRUD use cases land now; the screen does not.
- Caching categories in Room (§5.1 explicitly defers this to offline-support work). Note it in the PR.

---

## 1. `:core:network`

**`dto/zone/ZoneDto.kt`** — add `@SerialName("categoryId") val categoryId: String? = null`.
Keep the existing read-side `category: CategoryDto?`. This is safe in both directions because
`NetworkModule.providesNetworkJson` sets `explicitNulls = false`: a null `categoryId` is omitted on write, and
the server never sends the key so reads are untouched. One field beats a parallel `ZoneRequestDto` + mappers.

**`dto/zone/UpdateZoneRequest.kt`** — add `categoryId: String? = null`.
**`dto/zone/CreateZoneRequest.kt`** — already has it; no change.

**`api/CategoryApiService.kt`** — add the three missing endpoints, reusing the currently-orphaned
`dto/category/CategoryRequestDto.kt`:

```kotlin
@POST("v1/categories")           suspend fun createCategory(@Body request: CategoryRequestDto): CategoryDto
@GET("v1/categories/{id}")       suspend fun getCategory(@Path("id") id: String): CategoryDto
@PUT("v1/categories/{id}")       suspend fun updateCategory(@Path("id") id: String, @Body request: CategoryRequestDto): CategoryDto
```

No DI change — `CategoryApiService` is already provided in `NetworkModule`.

## 2. `:core:domain`

**`zones/model/DailyZone.kt`** — `DailyZone` gains `val categoryId: String? = null`.
Nullable with a default is deliberate: every existing construction site (profile, tests) keeps compiling.

**`zones/model/Zone.kt`** — gains `val categoryId: String? = null`, and `defaults` is replaced so every zone
name matches a seeded category exactly, which makes the auto-assign a plain name lookup with no mapping table:

| id | name | colour | weight |
|---|---|---|---|
| `work` | Work | `0xFF2EAAFF` (existing Work) | 6 |
| `learning` | Learning | `0xFF7A64FF` (existing Study) | 2 |
| `personal` | Personal | `0xFFFF9838` (existing Personal) | 2 |
| `general` | General | `0xFFFF6F91` (freed by dropping Play) | 5 |

`STUDY` / `PLAY` constants are removed; `LEARNING` / `GENERAL` replace them.

**`category/repository/CategoryRepository.kt`** — add `createCategory(name)`, `getCategory(id)`,
`updateCategory(id, name)`, all returning `Result<Category>`.

**`category/usecase/`** — three new use cases beside the existing `GetCategoriesUseCase`, same shape
(one `operator fun invoke`): `CreateCategoryUseCase`, `GetCategoryUseCase`, `UpdateCategoryUseCase`.

**`onboarding/usecase/SuggestZoneScheduleUseCase.kt`** — the split becomes weighted instead of equal.
Keep the existing cumulative-boundary + `snapToFive` structure so the overnight wrap and the tiny-window
degradation keep behaving identically; only the fraction changes:

```kotlin
private val WEIGHTS = mapOf(Zone.WORK to 6, Zone.LEARNING to 2, Zone.PERSONAL to 2, Zone.GENERAL to 5)
// boundaries[i] = firstBoundary + snapToFive(usable * cumulativeWeight[i] / totalWeight)
```

Weights are ratios of whatever pool the user has, which is the requirement ("relative to the hour pool").
At the default 07:00–23:00 (900 usable minutes) this yields exactly:

```
07:30 ─ 13:30  Work      6h
13:30 ─ 15:30  Learning  2h
15:30 ─ 17:30  Personal  2h
17:30 ─ 22:30  General   5h
```

The weight map lives in the use case, not on `Zone` — layout is the suggester's concern, not the model's.

## 3. `:core:common`

**`error/AppErrorExt.kt`** — `toUiText()`'s `else` branch currently renders the raw server `body` as a
`DynamicString`. With 422s now routine that leaks backend prose into the UI. Add three branches above it:

- `errorCode == "CATEGORY_NAME_TAKEN"` → new `error_category_name_taken`
- `errorCode == "CATEGORY_NOT_FOUND"` → new `error_category_not_found`
- `errorCode == "VALIDATION_ERROR"` → existing `R.string.error_validation`

New keys in `core/common` `values/strings.xml` **and** `values-ar/strings.xml`.

## 4. `:core:data`

**`zones/mapper/ZonesMapper.kt`** — the highest-value edit in the whole change:

```kotlin
fun ZoneDto.toDomain(): DailyZone = DailyZone(..., categoryId = category?.id)
fun DailyZone.toDto(): ZoneDto    = ZoneDto(..., categoryId = categoryId)
```

Because every bulk path (`updateTemplateZones`, `updateOverrideZones`, `createTemplate`, `createOverride`)
maps server zones → domain → back, this one fix makes a load-edit-save round-trip preserve the category with
no call-site changes anywhere — including in profile, which we are not touching.

**`zones/repository/ZonesRepositoryImpl.kt`** — pass `categoryId = zone.categoryId` at the three request sites
that currently omit it: `addZoneToTemplate` (~L82), `addZoneToOverride` (~L145), `updateZone` (~L186).

**`category/`** — extend `remote/CategoryRemoteDataSource(+Impl)` and `CategoryRepositoryImpl` with the three
new operations, following the existing `getCategories` shape exactly (`safeApiCall` + `.map { it.toModel() }`).
No new Hilt bindings — `DataModule` already binds both.

**`onboarding/OnboardingRepositoryImpl.kt`** — carry the category into the zone it builds (~L55):

```kotlin
DailyZone(..., categoryId = zone.categoryId)
```

and guard the template write: if every enabled zone has a null `categoryId`, skip the create/update entirely
rather than firing a request that is certain to 422. This is doc §3's "no category → no zone can be created",
and it is what keeps a legacy zero-category account from being trapped in onboarding by a hard failure.

## 5. `:feature:onboarding:impl`

No `build.gradle.kts` change — `:core:model` and `:core:domain` are already dependencies.

- **`presentation/OnboardingState.kt`** — add `val availableCategories: List<Category> = emptyList()`.
- **`presentation/OnboardingAction.kt`** — add `data class ZoneCategoryPicked(val zoneId: String, val categoryId: String)`.
- **`presentation/OnboardingViewModel.kt`** — inject `GetCategoriesUseCase` (5th constructor arg). Load
  categories in `init`; on success store them and stamp each zone's default `categoryId` by exact
  case-insensitive name match → `General` → first category. Re-stamp inside `applySuggestedZones()` so
  *Reset to my suggestion* doesn't wipe the assignment. Handle `ZoneCategoryPicked` through the existing
  `updateZones { }` helper. A failed category load is non-fatal — the zones step still works, the sheet shows
  the empty-menu row, and the repository guard skips the template write.
- **`ui/components/ZoneSheet.kt`** — add a category row under the two `TimeField`s, opening an
  `AwanDropdownMenu` of `AwanDropdownMenuItem`s. Copy the pattern from
  [TaskAttributeChips.kt:164](feature/add-task/src/main/java/com/awan/feature/addtask/ui/components/TaskAttributeChips.kt:164)
  verbatim, including its disabled "no categories" row for the empty state. `ZoneSheet` gains
  `categories: List<Category>` and `onPickCategory: (String) -> Unit` params.
- **`ui/steps/ZonesStep.kt`** — pass `state.availableCategories` down and dispatch `ZoneCategoryPicked`.
- **`res/values/strings.xml` + `res/values-ar/strings.xml`** — two new keys in both:
  `onboarding_zone_category` ("Category" / "الفئة"), `onboarding_zone_category_empty`
  ("No categories yet" / "لا توجد فئات بعد").

## 6. Tests

- **`core/domain/.../SuggestZoneScheduleUseCaseTest.kt`** — rewrite all four cases for the new zone set and
  weighted boundaries. Keep the overnight-wrap and tiny-window cases; they are the ones that catch a bad
  refactor of the boundary math.
- **`core/data/.../zones/mapper/`** — new small test: a `ZoneDto` with a nested `category` survives
  `toDomain()` → `toDto()` with its `categoryId` intact. This is the check for the one non-trivial fix.
- **`feature/onboarding/impl/src/test/.../OnboardingViewModelTest.kt`** — add a fake `CategoryRepository`
  (mirror `AddTaskViewModelTest`'s), assert default assignment by name and that `ZoneCategoryPicked` sticks.
- **`core/data/.../template/TemplateRepositoryImplTest.kt`** — uses `Zone.defaults` generically; check whether
  it asserts zone names and update if so.

`ZonesRepository`'s interface is unchanged, so `AddTaskViewModelTest.FakeZoneRepository` needs no edit.

## 7. Feature plan doc

Per `CLAUDE.md`, save this plan to `docs/feature/onboarding/2026-08-05-required-zone-category.md` before
starting, and append the `## Implementation notes (what actually differed)` section when the work lands.

---

## Verification

```bash
./gradlew assembleDebug testDebugUnitTest lint
```

(`detekt` does not exist in this project — see the `detekt-task-does-not-exist` memory.)

Then against a **fresh** account, since existing accounts have zero categories:

1. Run onboarding end to end. Confirm the zones step shows Work / Learning / Personal / General at
   6h / 2h / 2h / 5h, and that changing wake/sleep rescales them proportionally.
2. Open a zone sheet, confirm the category row is pre-filled with the matching seeded category, change it.
3. Finish onboarding and confirm `POST /v1/templates` carries `categoryId` on every element of `zones[]`
   (visible in the `HttpLoggingInterceptor` BODY log on debug builds), and returns 200 not 422.
4. Re-run onboarding on the same account to hit the update path — confirm
   `PUT /v1/templates/{id}/zones` also carries `categoryId` per element.
5. Arabic locale pass on the zones step for the two new strings.

**Blocked on you:** `CLAUDE.md` requires an `AWAN-…` issue ID in the branch name or commit message, and the
Atlassian MCP is not authorized in this session so I cannot search Jira for a match. Tell me the issue ID
before I commit, or I'll ask again at that point.

---

## Implementation notes (what actually differed)

### Verification

`./gradlew assembleDebug testDebugUnitTest lint` — all green. Test counts after a `--rerun-tasks` pass, to
confirm they really executed rather than resolving from cache: core:domain 110, core:data 42,
feature:onboarding:impl 25, feature:add-task 45, feature:ai-tasks:impl 26 — 0 failures. `values/` ↔ `values-ar/`
key parity verified by diff for both touched modules (core:common 15/15, onboarding 79/79); lint reports no
`MissingTranslation`. Not yet exercised against the real backend.

### Deviations from the plan

- **`ZoneDto.toDomain()` reads `categoryId ?: category?.id`, not just `category?.id`.** The server only ever
  sends the nested object, but the app now round-trips its own request DTOs through the same mapper, and
  preferring the explicit id keeps that idempotent.
- **Added a serialization test that the plan did not call for.** The decision to put both `category` (read)
  and `categoryId` (write) on one `ZoneDto` rests entirely on `explicitNulls = false` dropping the unset half.
  That is a silent-failure mode — get it wrong and every zone write posts `category: null` and 422s with
  nothing in the diff to point at. `ZonesMapperTest` now asserts the encoded body directly. Verified: the
  written body contains `"categoryId":"cat-1"` and no `category` key.
- **`OnboardingRepositoryImpl` filters category-less zones instead of skipping the whole template.** The plan
  said "skip the write when every zone lacks a category"; per-zone filtering is the same guard but also covers
  a partially-resolved list, and it collapses to the same skip when the list empties.
- **`SuggestZoneScheduleUseCaseTest`'s short-day case asserts relationships, not fixed durations.** Pinning
  170/55/55/140 would re-break on any future weight tweak while testing nothing about the property that
  matters — that the shares hold and nothing starves.
- **Two out-of-scope test fakes had to change.** `CategoryRepository` grew three methods, so
  `AddTaskViewModelTest` and `AiTasksViewModelTest`'s `FakeCategoryRepository` needed `error("not used")`
  overrides. Unavoidable with a fat repository interface.

### Traps for whoever touches this next

- **`DailyZone.categoryId` is nullable and that is load-bearing, not laziness.** Making it required would
  break every construction site in profile, which this change deliberately does not touch. The nullability is
  what lets the two features migrate independently — do not "tighten" it without migrating profile first.
- **Profile's *add a new zone* path still 422s.** `ZoneEditSheet(isNew = true)` builds a `DailyZone` with no
  category. Editing an *existing* zone works, because the id round-trips through the mapper. Own ticket.
- **`SuggestZoneScheduleUseCase.WEIGHTS` is keyed by `Zone` id.** `getValue` throws on a miss, so adding a
  default zone without adding its weight is a crash at first suggestion, not a silent bad layout. Intentional.
- **The overnight boundary math is easy to get wrong by hand.** Boundaries are snapped to 5 *before* the
  modulo, so the naive `wake + share` arithmetic is off by a few minutes — the first draft of the overnight
  test failed for exactly this reason. Trust the test, not mental arithmetic.
- **`Zone.defaults` names are still hardcoded English and are what the name-match keys off.** Localizing the
  display name now means splitting display name from server name. Deferred by decision, not oversight.

### PR #33 review follow-ups

- **The name-match rule moved out of the ViewModel into `AssignDefaultCategoriesUseCase`**
  (`core/domain/onboarding/usecase/`), beside `SuggestZoneScheduleUseCase`. §5 had it as a private
  ViewModel helper, which is a Clean Architecture violation the review caught. Behaviour is unchanged;
  the existing ViewModel tests cover it through the use case.
- **A failed template write now fails `completeOnboarding` instead of being discarded.** The plan only
  guarded the *no category* case; an actual API failure was silently dropped, costing the user their
  whole zone setup with no path back into onboarding. Two knock-on changes were required for the fix to
  hold: the already-onboarded (409) branch now writes the template too — otherwise the very first retry
  swallows the 409 as success and skips the zones forever — and a failed `getTemplates()` is returned
  rather than treated as "no default exists", which would have created a second `Default` template.

### Deferred, and noted for the PR

- Category **management screen** (§5.3) — the CRUD use cases (`CreateCategoryUseCase`, `GetCategoryUseCase`,
  `UpdateCategoryUseCase`) landed and are wired through data/network, but nothing consumes them yet.
- Caching categories in a store (§5.1) — explicitly deferred to the Room/offline-support work. The zones step
  refetches on each ViewModel creation.
