# Calendar Screen Shader Android 13+ Compatibility Fix

**Date:** 2026-08-16  
**Branch:** `hotfix/calendar-shader-compatibility`  
**Target:** Calendar Screen Deadline Info Bottom Sheet (`DeadlineAirflowBanner`)

## Overview

Fix an issue where the animated "threads" shader inside `DeadlineAirflowBanner` (displayed in `DeadlineInfoBottomSheet` when tapping the upcoming deadlines info icon) fails on Android 13+ (API 33+) while functioning on Android 12 and below.

## Root Cause Analysis

1. **Android 12 and below (< API 33):** The banner executes `AirflowCanvasFallback`, drawing continuous sinusoids directly onto the Compose Canvas using standard paths and gradient brushes. This path has no dependency on AGSL / `RuntimeShader`.
2. **Android 13 and above (>= API 33):** The banner executes `AirflowShaderTiramisu`, utilizing AGSL (`RuntimeShader`). However, `AirflowShaderTiramisu` attempted to apply the shader via:
   ```kotlin
   renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
   ```
   - `RenderEffect.createRuntimeShaderEffect(shader, uniformShaderName)` requires the AGSL shader to declare an input child shader named `uniform shader <uniformShaderName>;`.
   - `AIRFLOW_AGSL_SHADER` is a standalone procedural color generator that does not take or declare any `uniform shader content;`.
   - When Skia attempts to construct/bind the `RenderEffect`, the missing child uniform causes a runtime failure or invalid effect.
   - Additionally, reconstructing `RenderEffect` on each animation frame inside `graphicsLayer` introduced unnecessary allocations and JNI overhead.

## Solution

1. **ShaderBrush Rendering:** Transitioned `AirflowShaderTiramisu` to use `ShaderBrush(shader)` drawn directly on Compose `Canvas` via `drawRect(brush = brush)`, identical to the proven AGSL pattern in `:core:design-system:AwanSoapBubble.kt`.
2. **State & Memory Optimization:** 
   - Cached `ShaderBrush` with `remember(shader)`.
   - Used `mutableFloatStateOf` for `isReducedMotion` fallback to prevent primitive boxing.
   - Removed unused `RenderEffect`, `asComposeRenderEffect`, `Offset`, and `graphicsLayer` imports.
   - Added `@Language("AGSL")` annotation to `AIRFLOW_AGSL_SHADER` for IDE syntax support and static verification.

## Implementation notes (what actually differed)

- **Verification:**
  - `./gradlew :feature:calendar:impl:testDebugUnitTest :core:design-system:testDebugUnitTest` (All tests PASSED).
  - `./gradlew assembleDebug` (Build SUCCESSFUL across all modules).
- **Files Modified:**
  - `feature/calendar/impl/src/main/java/com/awan/feature/calendar/impl/presentation/DeadlineAirflowBanner.kt`
