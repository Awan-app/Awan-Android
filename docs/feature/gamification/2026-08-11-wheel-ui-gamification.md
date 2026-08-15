# Gamification Wheel UI & Motion Redesign Plan

## Overview
Redesign the Daily Gift Wheel (`AwanWheelOverlay` and `AwanWheelBadge`) into a high-end, 2D game-inspired arcade spin wheel that matches Awan's tactile sky theme, complete with glowing bezel lights, mechanical ticker recoil physics, tactile spin cap, spring deceleration, and celebratory prize reveal animations.

## Proposed Upgrades

### 1. 2D Arcade Wheel Visuals (`AwanWheelOverlay.kt`)
- **Game Dialog Frame**: Modal card container with Awan's 3D rim edge, sky blue background glow, gold ribbon header ("DAILY GIFT"), and cloud accents.
- **Arcade Bezel Ring**: Dual-layer metallic outer border with 12 perimeter light bulbs that chase around the rim while spinning and pulse gently when idle.
- **Wedge Styling & Separator Pegs**: Vibrant 2D Awan color palette (`zoneSun`, `zoneTangerine`, `zoneCoral`, `zoneViolet`, `zoneLavender`, `sky`), radial inner shading, crisp separator lines, and gold rivet pegs on segment boundaries.
- **Tactile Center Spin Cap**: 2D pressable hub with 3D rim offset, "SPIN!" text/icon, and active click state.
- **Dynamic Pointer Ticker Recoil**: Mechanical recoil animation where the golden top arrow flicks back when hit by segment pegs and springs forward with fluid motion.

### 2. Smooth Spin Physics & Victory Celebration
- **Free-Spin to Landing Deceleration**: Seamless transition from continuous speed free-spin to landing target calculation with an ease-out overshoot spring bounce into place.
- **Prize Reveal Card**: Animated loot pop-up with `SparkleBurst` particle effects and tactile "COLLECT" button.

### 3. Tactile Wheel Badge (`AwanWheelBadge.kt`)
- Glowing pulse aura around floating gift chest.
- Gentle floating sine wobble animation + 3D tactile press feedback.

## Verification Strategy
- Run existing gamification unit tests `./gradlew testDebugUnitTest --tests "com.awan.app.core.data.gamification.*"`.
- Run build `./gradlew assembleDebug` to ensure compilation.
