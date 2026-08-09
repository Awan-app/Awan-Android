# AWAN-137 Details Sheet Inner-Shadow Gradient

## Summary

Replace the centered radial gradient in the details bottom sheet with a rarity-tinted inner shadow anchored to the sheet width and clipped to the artwork frame.

## Implementation Changes

- Keep the details artwork in its existing full-width, fixed-aspect frame.
- Render the left, right, and top fades as a separate overlay whose width follows the sheet content bounds and whose height exactly matches the artwork frame.
- Stop the side gradients at the artwork frame’s bottom edge; they must not continue into the type, rarity, description, or Equip sections.
- Base the gradient geometry on the sheet-width artwork container, not the remote image’s intrinsic dimensions, so changing image aspect/content does not move or resize the effect.
- Use the selected rarity accent at low opacity, fading toward transparent at the center; leave the bottom edge untreated.
- Disable the image-level radial backdrop in the details sheet while preserving the current radial treatment on grid cards.
- Clip the overlay to the existing artwork/sheet shape and preserve all existing details, equip behavior, localization, previews, and offline handling.

## Test Plan

- Update the details-sheet preview to verify the side gradients end at the artwork boundary.
- Preserve Compose semantics coverage for details content and Equip state.
- Run inventory Android-test compilation, unit tests, lint, and `:app:assembleDebug`.

## Assumptions

- The artwork frame’s current fixed aspect ratio defines the gradient height; intrinsic image dimensions do not.
- The gradient remains rarity-colored.
- No new public domain, ViewModel, navigation, or design-system API is required.
- Commit with the `AWAN-137:` prefix.

## Implementation notes (what actually differed)

- The details sheet now uses the full-width fixed-aspect artwork frame as the gradient host. Rarity-colored left, right, and top fades are drawn after the neutral artwork and clipped to that frame, so they stop before the detail fields.
- The grid cards retain their existing radial rarity backdrop. Added a details artwork semantics tag and renamed the details preview to expose the edge-shadow treatment.
- Inventory unit tests, lint, Android-test compilation, and `:app:assembleDebug` passed. Connected Compose execution remains unavailable because no Android device is connected.
