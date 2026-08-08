# Update Profile Image Feature Plan

- **Date**: 2026-08-07
- **Feature**: Profile — Update Profile Image

## Goal

Allow the user to select a new profile image from the device gallery and upload it to the backend (`PATCH /v1/users/me/profile/picture`).
After successful upload, the new profile image immediately appears on the profile screen.

## Specification Summary

- API Endpoint: `PATCH /v1/users/me/profile/picture`
- Format: `multipart/form-data`
- Part Name: `image` (must be exact)
- Supported Formats: JPEG, PNG, WebP
- Maximum Size: 5 MB (`5 * 1024 * 1024` bytes)
- Authentication: existing authenticated API / OkHttp Interceptor.
- Response: `{"profilePictureUrl": "https://..."}`

## Architectural Strategy

1. **`:core:network`**: Update `ProfileApiService.updateProfilePicture` signature to `@Part image: MultipartBody.Part`.
2. **`:core:common`**: Map `ValidationReason.IMAGE_TOO_LARGE` and `ValidationReason.IMAGE_TYPE_UNSUPPORTED` to localized strings if not present.
3. **`:core:data`**: Update `ProfileRemoteDataSourceImpl` to create multipart part with name `"image"`. On success, `ProfileRepositoryImpl` updates local Room DB (`userDao`), triggering reactive `observeProfile()` updates.
4. **`:core:domain`**: In `UpdateProfilePictureUseCase`, validate image format (JPEG, PNG, WebP) and size (<= 5 MB) prior to invoking repository layer.
5. **`:feature:profile:impl`**:
   - `ProfileViewModel`: Prevent duplicate upload calls while uploading (`isUpdatingField = true`), set loading state, invoke `UpdateProfilePictureUseCase`, handle success/failure appropriately.
   - `ProfileHeaderCard`: Display `CircularProgressIndicator` inside avatar box when `isUpdatingField` is true during image upload.
6. **Testing**: Add unit tests for `UpdateProfilePictureUseCase`, `ProfileRemoteDataSourceImpl`, and `ProfileViewModel`.

## Implementation notes (what actually differed)

- **Verification Results**:
  - `./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest :feature:profile:impl:testDebugUnitTest` — PASSED.
  - `./gradlew assembleDebug` — BUILD SUCCESSFUL in 1m 15s.
- **Deviations / Findings**:
  - `ProfileApiService` was updated to accept `@Part image: MultipartBody.Part` matching the exact field name requirement.
  - Added localized strings for `error_image_too_large` and `error_image_type_unsupported` in `:core:common` for English (`res/values`) and Arabic (`res/values-ar`).
  - Single source of truth in Room DB (`userDao`) automatically syncs new `profilePictureUrl` to `observeProfile()` state flows upon successful repository upload.
