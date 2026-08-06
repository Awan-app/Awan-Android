# Goal preview theme and image-entry plan

## Goal

Make goal preview surfaces follow the active Awan theme and add a local Camera/Photo-library image draft to initial goal creation.

## Implementation

- Use direct Awan theme surfaces for preview assistant and nested-task cards; keep light and dark Compose previews.
- Add a local image URI to `AddTaskState`, selected/removed through `GoalImageChanged`; initial goal submission is enabled by text or image.
- Preserve the approved temporary behavior: an image-only goal sends an empty message to the existing decomposition endpoint, never uploads the image, retains it after an error, and clears it after a successful first response.
- Use system Photo Picker and Camera `TakePicture` with a feature-local cache `FileProvider`; render a bounded thumbnail and remove control.
- Add English and Arabic UI/accessibility strings. No backend, domain, persistence, or dependency changes.

## Verification

- Focused ViewModel tests cover image enablement, image-only empty-message submission, error retention, and successful-response cleanup.
- Run `:feature:add-task:testDebugUnitTest` and compile the affected module; manually check both themes and both image sources on a device.

## Implementation notes (what actually differed)

- `:feature:add-task:testDebugUnitTest` and `:app:assembleDebug` passed.
- The Camera and Photo Picker flows still need a device or emulator pass.
- Arabic strings use XML numeric character references because the Windows fallback write pipeline did not preserve literal Arabic; Android resource linking resolves them normally.
