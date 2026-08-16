# Clean Store UI and Inventory Image URL Resolution Plan

- Date: 2026-08-16
- Jira: AWAN-137
- Branch: refactor/AWAN-137-environmental-inventory-screen

## Objectives
1. Remove all store (marketplace) screen UI edits from branch `refactor/AWAN-137-environmental-inventory-screen`.
2. Ensure the inventory screen and data mappers correctly resolve image URLs from backend responses (e.g. `"/images/store/cloud_stratus_dark_01.png"`) against base URL without the `/api` prefix (producing `baseurl/images/store/cloud_stratus_dark_01.png`).

## Changes Made
1. Reverted UI changes in `feature/marketplace/impl/`:
   - `MarketplaceUiState.kt` (reverted sorting)
   - `MarketplaceScreen.kt` (reverted canAfford argument)
   - `ItemDetailsBottomSheet.kt` (reverted rarity color calculation and rarity chip)
   - `StoreItemCard.kt` (reverted rarity color, canAfford, and rarity badge)
2. Verified image URL resolution in `:core:network`:
   - `resolveBackendImageUrl` trims `/api` from `AWAN_BASE_URL` and trims leading slashes from paths like `"/images/store/cloud_stratus_dark_01.png"`
   - Added unit test cases for `cloud_stratus_dark_01.png` in `UrlUtilsTest.kt`.
3. Verified `:core:data` mappers:
   - `StoreItemDto.asExternalModel()` and `StoreItemEntity.asExternalModel()` resolve image URLs via `resolveBackendImageUrl`.
   - Added unit test case in `StoreMappersTest.kt`.
4. Verified `:feature:inventory:impl` renders resolved item images via `AwanRemoteImage` across grid cards and details sheet.

## Implementation notes (what actually differed)
- Build/test status: Unit tests passing across `:core:network`, `:core:data`, `:feature:inventory:impl`, `:feature:marketplace:impl`.
- Marketplace UI: Fully reverted to match `origin/develop` baseline.
- Image URLs: Base URL without `/api` suffix correctly prefixed to backend paths.
