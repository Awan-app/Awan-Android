<div align="center">

<img src="docs/assets/mascot.svg" alt="Awan mascot" width="240">

# Awan

**AI-assisted adaptive scheduling for Android**

</div>

Awan turns a vague goal — *"pass the CFA in six months"* — into a real, conflict-free calendar. A cloud **AI Architect** decomposes the goal into a strict JSON contract of tasks; the app owns 100% of the placement math. The AI never writes start times, and the scheduler never guesses intent.

Built as a multi-module Gradle project following Now in Android architecture with strict Clean Architecture layering — offline-first, fully localized in English and Arabic, and 100% Jetpack Compose on Navigation 3.

---

## Features

### Planning a goal

Describe a goal in plain language and the app runs a short clarifying dialogue — multiple-choice follow-ups plus free-text answers — before proposing anything. The result is a full plan preview: every task with its duration, points, target date, and dependencies, laid out before a single thing is committed. Not right? Send a revision in natural language and the plan is regenerated. Accept, and the whole goal lands on the calendar at once.

### Creating a task

- **Type it** — title, description, duration, category, mandatory/optional, and a day-and-time chip, all from one bottom sheet.
- **Speak it** — an in-app speech recognizer feeds the same sheet.
- **Photograph it** — snap or pick a syllabus, whiteboard, or handwritten list and the AI reads tasks out of the image.

AI-created tasks arrive as **proposals**, never as commitments: each one is editable, individually removable (with undo), and shows the reasoning behind its suggested session times. Accept all, accept some, or reset the plan.

### Zones — the core scheduling primitive

The day is divided into named **zones** (Study, Work, Play, Personal by default) with editable windows, colors, and categories. Every task carries a zone, and the scheduler only ever places it inside that zone's window.

- **Routines** group zones into named day-templates applied to any set of weekdays.
- Any single day can be **customized** away from its routine without disturbing the rest of the week.
- Overlapping zones are rejected at edit time, not discovered later.
- Onboarding builds the first set from the user's real wake and sleep times, with a live preview of the resulting day.

### The day view

A draggable timeline with a live time pointer, overlap-aware track layout, and drag-to-reschedule. Sessions can be marked done, reopened, locked against being moved, edited (title, description, duration, points, start/end, zone, date), or deleted — with the delete flow distinguishing *this session* from *the whole task*.

When a change would collide, an **Intelligent Nudge** surfaces the conflict with real options — Skip, Double Up, Reschedule, Approve. No schedule change is ever committed without user approval.

### Goals, calendar, and inbox

- **Goals** — active and completed, each with live progress derived from its sessions.
- **Calendar** — month view with deadline markers, upcoming deadlines, and a continuous streak line.
- **Inbox** — searchable session backlog filtered by *active now* and *missed*.

### Gamification

Points, streaks, and rewards are **server-owned** — never computed on the client. Completing a session pays out through a flight-and-burst animation into the balance; a daily wheel and a streak loot overlay handle the rest. Points spend in a **marketplace** of frames, skins, themes, and icons, sorted by rarity, with an **inventory** for equipping what's owned — equip state and all.

### Assistant access via MCP

Users mint, name, regenerate, and revoke **Model Context Protocol** tokens from inside the app, then copy a ready-made Claude or Cursor config to point an external AI client at their own schedule. Tokens are shown once, stored encrypted, and obscured thereafter.

### Notifications

Session reminders, live-activity updates, session-end prompts, follow-up nudges, streak alerts, a daily brief, and reward notices — each with **its own switch**, plus configurable reminder lead time, snooze length, and follow-up delay. A notification the user can't turn off individually is treated as a bug, not a preference gap.

### Everywhere else

- Passwordless auth: email OTP or Google sign-in, with rate-limit and offline banners.
- Profile with personal info, photo (camera or gallery), timezone, session length, scheduling mode, custom categories, theme, and language.
- Deep links straight from a notification into the running session.
- Full RTL support — Arabic is a first-class locale, not a bolt-on.

---

## Architecture

Dependency direction is always `presentation → domain ← data`. The rule is enforced structurally, not by convention: presentation modules don't have the data layer on their classpath.

| Layer / Pattern | Description |
|---|---|
| **Clean Architecture** | `:core:domain` holds repository *contracts* and use cases and depends on nothing but `:core:model`. `:core:data` holds only implementations, data sources, DTOs, and mappers. A repository interface in the data layer is a bug. |
| **MVI + UDF** | ViewModels expose a single `StateFlow` of sealed UI state; user intent flows down as Actions, one-shot effects flow up as Events. A ViewModel consumes use cases only — never a repository, DAO, or HTTP service. |
| **Feature `api` / `impl` split** | Every feature is two modules: `api` exports only its navigation route, `impl` holds screens, ViewModels, and the entry provider. Features depend on each other's `api` only, so no feature can reach into another's internals. |
| **Offline-first, Room as SSOT** | The UI observes Room `Flow`s exclusively. The network never feeds the UI directly — it only refills the database. Freshness is per-row via TTL-stamped expiry, refreshed in the background by WorkManager. Writes gate on connectivity and return a typed error instead of failing deep in the stack. |
| **Convention plugins** | Custom Gradle plugins in `build-logic/` own all shared config — Compose, Hilt, Room, WorkManager, Navigation. Module build files stay declarative; no repeated Android blocks. |
| **Server-owned economy** | Points and streaks are never computed client-side. Session status changes only through dedicated endpoints, and balances are read back from the response. |

---

## Tech Stack

### Language & Build

| Technology | Description |
|---|---|
| **Kotlin** | Whole codebase — coroutines, `Flow`, sealed hierarchies, value classes. |
| **Gradle + AGP** | Configuration cache enabled; modules build in parallel. |
| **Version catalog** | A single `libs.versions.toml` is the source of truth for every dependency and plugin. |
| **build-logic convention plugins** | `awan.android.application`, `.library`, `.compose`, `.hilt`, `.feature`, `.room`, `.navigation`, `.workmanager`, `awan.jvm.library` — composed per module instead of copy-pasted. |

### UI

| Technology | Description |
|---|---|
| **Jetpack Compose** | 100% declarative UI. No XML layouts anywhere in the app. |
| **Navigation 3** | Type-safe `NavKey` routes with `EntryProvider`s and a central `Navigator` — not navigation-compose. |
| **Compose Styles API** | The design system is themed through component themes and `Modifier.styleable` rather than hardcoded parameters. |
| **Material 3 Adaptive** | Window-size-aware navigation and layout. |
| **Custom timeline engine** | A layout engine plus Canvas track renderer positions overlapping sessions on the draggable day timeline. |
| **Lottie · Coil · Lucide** | Mascot and reward animation, network image loading, and the icon set. |
| **Motion & haptics** | Cascade reveals, sparkle bursts, points-flight overlays, cloud drift — all gated behind a reduced-motion setting. |

### Data & Infrastructure

| Technology | Description |
|---|---|
| **Room** | Single source of truth. DAOs expose `Flow`, so every write propagates to the UI automatically. |
| **Retrofit + OkHttp** | REST transport with an auth interceptor and a token authenticator that transparently refreshes expired sessions. |
| **kotlinx.serialization** | JSON for the network layer and for type-safe navigation keys. |
| **Proto DataStore** | Typed user preferences backed by Protocol Buffers. Notification toggles are stored *negated*, so a newly added switch defaults to on. |
| **androidx.security-crypto** | Encrypted storage for auth and MCP tokens. |
| **WorkManager + Hilt Work** | Constraint-aware background sync and notification reschedule/action workers, with a foreground catch-up path so nothing depends on a background job having run. |
| **Hilt (KSP)** | DI across every module; `@Binds` modules wire repository impls to their domain contracts. |
| **Firebase Auth + FCM** | Google sign-in and push delivery for registered device tokens. |

### Quality

| Technology | Description |
|---|---|
| **Unit tests** | JUnit + `kotlinx-coroutines-test` across use cases, mappers, ViewModels, and the notification planner. |
| **Compose UI tests** | `ComposeTestRule` and Espresso for instrumented coverage. |
| **Lint as a gate** | Compose lint checks with `abortOnError`, and `HardcodedText` / `MissingTranslation` promoted to **errors** — an untranslated or hardcoded string fails the build. |

---

## Module Structure

```plaintext
Awan/
├── build-logic/convention/        # Gradle convention plugins — all shared module config
├── app/                           # Navigation 3 shell: AwanApp, AwanAppState, MainActivity,
│                                  #   TopLevelDestination, reward overlay host, session deep links
├── core/
│   ├── model/                     # Pure domain models (Task, Goal, TaskSession, DayZone, StoreItem…)
│   ├── domain/                    # Repository CONTRACTS + use cases, grouped by feature
│   ├── data/                      # Repository impls, remote/local data sources, DTOs, mappers,
│   │                              #   offline sync coordinator and TTL policy
│   ├── database/                  # Room database, entities, DAOs, exported schemas
│   ├── network/                   # Retrofit/OkHttp, auth interceptor, token authenticator
│   ├── datastore/                 # Proto DataStore prefs + encrypted token storage
│   ├── datastore-proto/           # .proto definitions (user_preferences.proto)
│   ├── notifications/             # Channels, alarm scheduling, session notification planner
│   ├── design-system/             # Awan* components, Styles API themes, motion, tokens
│   ├── navigation/                # Navigator, NavigationState, Route contracts
│   └── common/                    # Dispatchers, Result, AppError, connectivity monitor
└── feature/                       # api = route only · impl = screens + ViewModels
    ├── splash/  onboarding/  auth/            # Cold start, zone setup, email OTP + Google sign-in
    ├── home/  calendar/  goals/               # Day timeline, month schedule, goals + inbox
    ├── add-task/                              # State-driven sheet (no route → no api/impl split)
    ├── ai-tasks/  chat/  assistant/           # AI decomposition, proposals, conversational entry
    ├── profile/  profile-setup/               # Zones, routines, MCP tokens, notification switches
    └── marketplace/  inventory/               # Points store and owned/equipped cosmetics
```

---

## Team

* [Mohannad El-Sayeh](https://github.com/mSaayeh)
* [Esraa Ehab](https://github.com/esraaehab333)
* [Abdallah Elsobky](https://github.com/Abdallah-Elsobky)
* [Zeiad Mohammed Abdelaziz](https://github.com/ZeiadT)
* [Sherif Ashraf](https://github.com/SherifAshraf2020)
