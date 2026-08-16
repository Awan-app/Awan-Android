# Design System: Awan

**Source:** `:core:design-system` (`core/design-system/src/main/java/com/awan/app/core/designsystem/`) — synthesized from `Tokens.kt`, `AwanTypography.kt`, `AwanStyles.kt`, `AwanTheme.kt`.
**Stitch Project ID:** _not linked — this document was generated from the in-repo design system, and doubles as the visual brief to hand Stitch when generating new Awan screens._

## 1. Visual Theme & Atmosphere

Awan is **soft, airy, and toy-like** — a scheduling app that reads as a clear morning sky rather than a productivity dashboard. The name means "clouds," and the whole system commits to that: screens are washed in a vertical sky gradient, cards float as opaque white cutouts against it, and drifting clouds and a mascot inhabit the empty space instead of grey placeholder blocks.

The density is **generous but not sparse**. Content sits in chunky rounded containers with visible outlines, sized so a card is unmistakably a discrete object you could pick up. Nothing is flat or hairline — every surface has a 2dp stroke, every button has a physical bottom edge.

The defining trait is **tactility over gloss**. Controls are drawn as solid objects with a darker "rim" beneath them; pressing one sinks the face into the rim in 40ms, so the button visibly bottoms out under the thumb. There is no ripple, no glow, no ambient elevation — depth is communicated by an exposed edge, not by a shadow. Motion is spring-driven and slightly overshooting, so panels arrive with a small bounce rather than easing to a stop.

Typography is uniformly **heavy and rounded**, near-ExtraBold at every size including body copy. Combined with the pastel palette, this pushes the mood toward friendly and encouraging — closer to a well-made kids' game than to an enterprise calendar — which is deliberate, because the app's job is to make an AI-rearranged schedule feel gentle rather than authoritarian.

## 2. Color Palette & Roles

### Sky ground (light theme)

| Color | Hex | Role |
|---|---|---|
| **Clear Morning Blue** | `#CFEEFF` | Top of the screen's vertical gradient — the sky the app opens onto |
| **Faded Horizon White** | `#F4FAFF` | Bottom of that gradient; the app's base background |
| **Pure Cloud White** | `#FFFFFF` | Every card, sheet, field, and floating surface |
| **Pale Contour Blue** | `#DCEAF5` | The 2dp outline on every surface, and the resting rim under quiet controls |

### Ink and text

| Color | Hex | Role |
|---|---|---|
| **Deep Harbour Navy** | `#16455E` | Primary text, headings, and the app's ink color |
| **Overcast Slate Blue** | `#5B7A8E` | Secondary text, captions, supporting copy |
| **Distant Haze Blue** | `#8FB0C4` | Metadata, timestamps, field placeholders — present but receding |

### Actions

| Color | Hex | Role |
|---|---|---|
| **Bright Signal Sky** | `#2EAAFF` | The single primary action color — filled buttons, focus rings, selected nav item |
| **Sunken Signal Blue** | `#1D84CC` | The rim beneath a primary button and its pressed state; also the label color on quiet/secondary buttons |
| **Soft Warning Rose** | `#FF6F91` | Destructive actions, error strokes, error text |
| **Deepened Rose** | `#D94F72` | Rim and pressed state beneath destructive actions |
| **Fresh Meadow Green** | `#22C55E` | Success confirmation only |
| **Muted Frost** | `#E8F1F8` | Disabled surfaces |
| **Ghosted Blue-Grey** | `#A9C4D6` | Disabled labels and icons |

### Time-of-day sky

Used to tint the schedule's background by hour, so the timeline reads as a day passing:

| Color | Hex | Role |
|---|---|---|
| **Warm Sunrise Apricot** | `#FFD9A8` | Dawn |
| **Pale Waking Blue** | `#CFE8FF` | Morning |
| **Saturated Noon Blue** | `#A6D2FA` | Midday |
| **Cooling Twilight Indigo** | `#6E7FB8` | Dusk |

### Zone pastels

Ten muted pastels identify a task's zone. They are deliberately desaturated so a full day of colored blocks stays calm rather than carnival-loud, and they are **identical in light and dark** — only the fill opacity changes (35% light / 45% dark) so the same zone is recognizably the same color in either theme.

| Color | Hex |
|---|---|
| **Soft Melon Green** | `#97DBAE` |
| **Powder Sky Blue** | `#8EC5E8` |
| **Dusty Lilac** | `#B7A1E8` |
| **Faded Blossom Pink** | `#F2A7B5` |
| **Sea Glass Green** | `#8FD3B0` |
| **Pale Butter Yellow** | `#F1D98A` |
| **Muted Apricot Orange** | `#F4A261` |
| **Clay Rose Red** | `#E98B8B` |
| **Washed Aqua** | `#86D5D5` |
| **Quiet Stone Grey** | `#B8C0CC` |

### Gamification

| Color | Hex | Role |
|---|---|---|
| **Warm Honey Cream** | `#FFE7B3` | Streak badge background |
| **Burnt Amber** | `#B45309` | Streak flame icon |
| **Pale Parchment Gold** | `#FDF3D0` | Points badge background |
| **Aged Gold** | `#CA8A04` | Points coin icon |
| **Rich Gold / Bright Gold Core** | `#FFD700` / `#FFF7C2` | The reward wheel's outer and inner gold ring |

The wheel's own segments break from the pastel discipline on purpose — **Midnight Navy** (`#1E2D4A`), **Sun Yellow** (`#F7C92B`), **Vivid Orange** (`#FF8E00`), **Alarm Red** (`#E83D3D`), **Electric Violet** (`#8B5CF6`), **Pure Blue** (`#0096FF`), **Meadow Green** (`#22C55E`) — because the daily wheel is the one moment the app is allowed to be loud.

### Dark theme — night sky, not inverted paper

Dark mode re-grounds the same system at dusk rather than flipping values. The gradient collapses to a single **Deep Night Navy** (`#0F2536`); surfaces lift to **Submerged Slate Blue** (`#17364C`) with a **Moonlit Outline** (`#29506D`) stroke. Text rises to **Moonlight White** (`#EAF6FF`) and **Cool Cloud Grey** (`#A8C6DA`). The action blue brightens to **Luminous Sky** (`#4DBAFF`) and — importantly — its label color flips to the dark navy, so a filled button is a bright chip of daylight on a night ground.

## 3. Typography Rules

**One family carries everything.** Latin text is set in **Baloo 2**, a rounded, high-x-height display face; Arabic swaps to **Cairo** at the theme level (detected by locale, not by string). No secondary or serif face exists in the system.

The unusual rule: **weight is essentially constant at ExtraBold** — display, title, heading, body, and button labels all sit at ExtraBold, with only captions stepping back to Bold. Hierarchy is therefore carried almost entirely by **size**, not by weight contrast. This is what gives the app its sturdy, storybook character, and it means a screen that needs to feel lighter should use more whitespace or a softer color, never a lighter weight.

The scale is tight at the working end and dramatic at the top:

| Token | Size / Line height | Use |
|---|---|---|
| **Display** | 44sp / 31sp | Greeting moments, hero numbers. Deliberately tight-leading so a large word sits compactly |
| **Title** | 22sp / 26sp | Screen and sheet titles |
| **Heading** | 17sp / 22sp | Card titles, section labels, and clock readouts |
| **Body** | 14.5sp / 21sp | All reading copy |
| **Button** | 15sp / 20sp, +0.5sp tracking | Button labels |
| **Button compact** | 13sp / 18sp, +0.5sp tracking | Chips, skip links, inline actions |
| **Caption** | 11.5sp / 17sp, Bold | Metadata, zone band labels |

Letter-spacing is neutral everywhere except button labels, which get a slight **0.5sp opening** so short all-caps-feeling labels don't crowd inside a chunky pill.

## 4. Component Stylings

### Buttons — the rim system

Every button in Awan is two stacked shapes: a **face** and a darker **rim** peeking out beneath it. The rim shows **4dp along the bottom and 2dp along the trailing side**, giving the control a real, visible edge — like a physical key cap. Pressing translates the face down and inward over **40ms**, so it visually seats into the rim; releasing springs it back. This replaces ripple entirely.

- **Shape:** generously rounded corners at 18dp — soft, but still clearly a rectangle, not a lozenge
- **Height:** 48dp minimum face, before the rim
- **Primary:** bright signal-blue face on a sunken-blue rim, white label
- **Secondary:** white face with a pale blue 2dp outline over a pale blue rim; label in sunken blue
- **Destructive:** rose face on a deepened-rose rim, white label
- **Quiet:** fully transparent face and rim — the flat member of the family, used for tertiary text actions; it keeps the same 48dp target
- **Chip:** the compact pill member — **fully pill-shaped**, 40dp face, tinted per attribute by the caller while inheriting the same rim and sink
- **Focus:** a 3dp signal-blue ring on a slightly larger 23dp-radius shape, appearing only on keyboard focus
- **Disabled:** face fades to a muted frost, label to ghosted blue-grey, rim to the pale contour, animated over 220ms

### Cards & containers

The base surface is **pure white, subtly rounded at 16dp, outlined in a 2dp pale contour stroke**, with tight internal padding (13dp horizontal, 11dp vertical). Definition comes from that stroke, not from elevation — a resting card casts **no shadow at all**.

Shadows are reserved for genuine state changes and floating chrome, and stay small and specific: a **selected** card lifts to 10dp; the bottom navigation bar floats at 8dp and its center action at 6dp; header pills sit at 2dp, rising to 4dp for today; the progress summary card at 3dp; the live time pointer carries a 4dp glow tinted with its own color. Everything else is flat.

### Inputs & forms

Text fields are **white, 18dp-rounded, 56dp tall**, with a **1.5dp pale contour stroke** that thickens to **2dp in signal blue on focus** — the stroke is the entire focus affordance; no fill change, no floating label motion. Errors restate the same stroke in soft rose. Placeholders use the distant haze blue so they never compete with entered text.

OTP entry breaks the shape rule intentionally: **44 × 56dp cells at a tighter 12dp radius**, spaced 8dp apart, set in Title-size type. Active cells take the blue 2dp stroke; error cells take the rose stroke plus an 8%-opacity rose wash and rose digits, and shake for 400ms.

### Navigation

The bottom bar is a **white surface floating at 8dp** above the sky, separated by a 2dp divider. Items are **pill-shaped, 48dp, captioned**, resting in the distant haze color and turning signal blue when selected. Pressing an item nudges it **2dp downward over 120ms** — a small, deliberate echo of the button sink, so the whole system presses the same way.

### Illustration & mascot

The system includes first-class illustrative components rather than treating decoration as an afterthought: a **200dp mascot** with expression states, drifting cloud fields, an AI "aura," sparkle bursts, shimmer skeletons, and reward-flight overlays that arc points from a completed task to the header counter. These carry the app's personality during empty, loading, and celebratory states — moments a conventional scheduler would leave blank.

## 5. Layout Principles

Spacing runs on a **4dp base with a seven-step scale**: 4, 8, 12, 16, 20, 24, 32. Screen padding sits at 20dp; content inside a group is spaced at 12dp; related lines within a block at 4dp.

Screens are **single-column and vertically stacked**, painted edge-to-edge with the sky gradient so the background is continuous behind system bars — content is inset from the edges, but the sky never is. Cards span the full content width rather than sitting in a grid; hierarchy comes from stacking order and card grouping, not from columns.

**Touch targets never drop below 48dp**, including quiet text buttons and nav items, and a chip's rim is part of its target rather than dead space beneath it.

The layout must survive **RTL** — Arabic is a shipping language, so the button rim mirrors to the leading side, and any directional offset is expressed against layout direction rather than hardcoded.

Motion is **spring-first**: a settling spring (damping 0.85) for ordinary arrivals, a bouncy one (0.55) for panels, and a playful one (0.42) for celebratory elements. Durations, where springs don't apply, are 40ms for a press, 220ms standard, 320ms emphasized, with **55ms stagger** between items in a cascading list. Reduced-motion is respected system-wide.
