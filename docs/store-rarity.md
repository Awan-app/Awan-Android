# Store item rarity — a guide for the mobile apps

Store items now have a **rarity**, and rarity is what decides the price. This is everything you
need to render it on iOS and Android.

Before, all 53 frames cost 100 points and the store was a flat wall of pictures. Now there is a
ladder — something a user can afford in their first week, and something they save weeks for —
so it's worth showing rarity clearly rather than treating it as one more field.

## What you get in the payload

Every item object now has a `rarity` alongside the fields you already handle:

```json
{
  "id": "8b1f…",
  "name": "Twilight Light 3",
  "description": "A beautiful Twilight Light 3 frame.",
  "image": "/images/store/twilight_light_03.png",
  "info": "twilight_light_03",
  "price": 820,
  "version": "2.0",
  "type": "FRAME",
  "rarity": "RARE"
}
```

`rarity` is one of five strings: `COMMON`, `UNCOMMON`, `RARE`, `EPIC`, `LEGENDARY`.

**Please decode it defensively.** The column is nullable on the backend, so treat a missing or
unrecognised value as common and carry on rather than failing the parse — a null rarity should
never be able to break the store screen. On Android that means a fallback branch in your enum
adapter; on iOS, decoding into an optional and defaulting, rather than a non-optional
`Rarity` that throws.

Nothing else about the payload changed, and `price` still means the same thing: points.

## Where rarity shows up

You get it everywhere an item appears, because they all embed the same item object:

- `GET /api/v1/store/items` — the store listing (`?type=FRAME` still filters by type)
- `GET /api/v1/store/inventory` — what the user owns
- `GET /api/v1/store/equipped` — what they're wearing
- the user profile response, inside `equippedItems`
- the daily wheel result, when the spin awards an item

So if you have one shared item cell or component, adding the rarity badge in that one place
covers every screen at once.

## The price ladder

Five tiers, each with its own price band. Within a band the price varies by visual family, so
two frames of the same tier aren't necessarily the same price:

- **Common** — 50 to 150 points
- **Uncommon** — 200 to 400
- **Rare** — 500 to 900
- **Epic** — 1200 to 2000
- **Legendary** — 3000 to 5000

53 frames, 30 distinct prices, from 50 up to 5000. Counts per tier: 11 common, 11 uncommon,
11 rare, 10 epic, 10 legendary — so if you group the store by tier, expect roughly even
sections rather than a narrow top.

Cheapest in each tier is Cloud Stratus and priciest is Celestial Orbit, with Pastel Rainbow,
Golden Sunrise, Floating Isles and Twilight in between, in that order.

## How tiers map onto the artwork

Each family ships as a numbered series — `cloud_stratus_dark_01` through `_05` — and the number
*is* the tier: `_01` common, `_05` legendary. Light and dark versions of the same design share
a tier and a price.

The nice consequence for the UI: **every family runs the full ladder**. A user who loves
Twilight is never locked out of it — there's a Twilight at 130 points and a Twilight at 4600.
If you build a "browse by family" view, each family will show a complete common → legendary
progression, which makes a good upsell story.

Two irregularities to guard against, because they come from the artwork: Celestial Orbit has no
light/dark split (5 frames, not 10), and Twilight Dark only has three variants, so it stops at
rare. Don't assume every family has ten items or all five tiers.

## Rendering suggestions

Nothing here is enforced by the API — it's yours to design — but a few things that tend to work:

- Give each tier a consistent colour and use it everywhere: badge, card border, the glow behind
  an equipped frame. Users learn the colour faster than the word.
- Sort the store by price ascending by default. It reads as a natural progression and puts what
  the user can actually afford at the top.
- Show affordability up front — dim or mark items above the current balance instead of letting
  the user tap through and hit an error.
- Legendary items deserve more room than a grid cell. They're the reason someone keeps earning
  points.

## Things the API does *not* do yet

- **No sorting or filtering by rarity.** `GET /items` only takes `type`. Sort and group on the
  client. If a `rarity` query parameter would genuinely help, ask and we'll add it.
- **No pagination.** All 53 items come back in one response, so fetch once and keep them.
- **The daily wheel ignores rarity.** Its free item is drawn evenly from everything the user
  doesn't own, so a legendary is exactly as likely as a common. If you're writing celebration
  copy for a wheel win, don't promise that legendaries are rare — right now they aren't.
