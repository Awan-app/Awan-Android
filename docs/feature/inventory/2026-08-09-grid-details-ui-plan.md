# AWAN-137 Inventory Grid & Item Details UI Plan

## Summary

Enhance the existing inventory on `feature/AWAN-137-user-inventory` into a calm, two-column collectible cabinet. Preserve its current grouping, filtering, sorting, refresh, offline behavior, and data contracts.

## Implementation Changes

- Replace the grouped list with a fixed two-column grid, retaining full-width customization-type headers and existing ordering.
- Reuse `AwanCard` for the binary border state: neutral for unequipped, existing sky-selected rim/glow for equipped. Tapping an unequipped card calls the existing equip action; equipped cards do nothing.
- Keep card content limited to item art, name, rarity color, and an overlaid accessible top-right info button. Use a restrained artwork backdrop gradient from existing theme colors: neutral for Unknown, blue for Common, green for Uncommon, sky for Rare, violet for Epic, and sun/tangerine for Legendary.
- Add an item-details modal bottom sheet driven by a locally held selected inventory ID, always deriving the displayed item from the latest screen state. Show larger rarity-glowing art, name, rarity, type, description, and a bottom Equip action; show a disabled localized Equipped action for the current item.
- Replace the expanded filter/sort controls with one app-bar collection-controls action and one combined bottom sheet. Type, rarity, and sort choices update the existing state immediately; refresh remains available.
- Use existing design-system components, theme tokens, motion values, and English/Arabic string resources. No domain, network, Room, navigation, or avatar-widget changes.

## Test Plan

- Update inventory Compose coverage for two-column cards, equipped semantics, direct equip taps, info-sheet content, disabled Equipped action, and combined collection controls.
- Update previews to show mixed rarities, an equipped card, and the details sheet state.
- Run focused inventory tests, `:feature:inventory:impl:lintDebug`, and `:app:assembleDebug`.

## Assumptions

- The enhancement applies only to owned customizations; no marketplace, unequip action, or acquisition-source display is added.
- Unknown rarity remains visually neutral and retains the current parsing behavior.
- Equip remains disabled offline, including from the details sheet, with existing offline/error behavior preserved.
- Commit and push the UI update with the `AWAN-137:` prefix.

## Implementation notes (what actually differed)

- Replaced the `LazyColumn` inventory rows with a fixed two-column `LazyVerticalGrid`, retaining grouped headers and the existing state-derived filters and sort order.
- Added localized controls/details strings, card semantics, Compose test coverage, and previews for the grid and details sheet. No domain, data, navigation, or avatar changes were needed.
- `:feature:inventory:impl:testDebugUnitTest`, `:feature:inventory:impl:lintDebug`, focused production/Android-test compilation, and `:app:assembleDebug` passed. Connected Compose execution could not run because no Android device was connected.
