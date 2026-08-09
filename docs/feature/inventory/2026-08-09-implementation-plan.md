# AWAN-137 User Inventory Implementation Plan

## Goal

Show every customization the authenticated user owns, grouped by type and ordered by rarity, and let the user equip an owned customization. The currently equipped avatar frame must render wherever the app renders the user avatar.

## Architecture

Inventory is a dedicated Navigation 3 feature. Room is its read model: the repository refreshes the server inventory and equipped slots into one owned-customizations table, while the feature and Profile observe local flows. Equip is network-only and updates the local active slot only after the server succeeds.

## Product decisions

- Inventory opens as a Profile sub-screen.
- Type is a single-select filter; rarity is a multi-select filter; sorting supports rarity (default), newest acquired, and name.
- Rarity is parsed from `item.info`: Common, Uncommon, Rare, Epic, Legendary; missing or unrecognised values are Unknown and sort last.
- v1 has no unequip action, Store screen, wheel UI, or acquisition-source label.
- All visible strings ship in English and Arabic.

## Verification

- Unit-test rarity, sorting/filtering, repository refresh/equip behavior, and ViewModel state changes.
- Test the Room migration and shared avatar frame overlay.
- Run focused Gradle tests, lint, and `assembleDebug`.
