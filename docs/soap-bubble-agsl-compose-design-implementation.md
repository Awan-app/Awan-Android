# Flat 2D Soap Bubble Shader — Jetpack Compose + AGSL

## Purpose

This document defines the visual system and implementation for a **flat, transparent, animated soap-bubble overlay** designed for Jetpack Compose UI.

The target look is the approved visual concept:

- a normal UI item remains completely readable underneath;
- the bubble has an almost invisible center;
- soft pastel soap-film colors appear mainly around the perimeter;
- thin white crescents create the recognizable soap-bubble highlight;
- the rainbow film drifts organically instead of rotating;
- the effect remains **flat and 2D**, not glassy, volumetric, glossy, or photorealistic.

A representative use case is a **white circular token with a dark number centered inside it**, with the soap-film shader rendered as a transparent animated layer above the token.

---

# 1. Visual Direction

## Desired feeling

The effect should feel:

- light;
- playful;
- magical;
- clean;
- polished;
- soft;
- premium;
- slightly whimsical;
- visually alive without becoming distracting.

The bubble should **not** feel like:

- a glass sphere;
- a shield;
- a neon ring;
- a holographic disc;
- a rotating rainbow;
- a blurred frosted-glass card;
- a 3D object;
- a lens that visibly bends the entire item beneath it.

The effect should be something the user notices after seeing the item itself.

A useful visual balance is:

> **80–90% underlying item + 10–20% soap-film effect**

The content remains the hero.

---

# 2. Core Design Principle

The shader is an **overlay**, not a replacement surface.

The application should render in this order:

```text
Underlying UI item
        ↓
White circle / icon / avatar / item artwork
        ↓
Text or number
        ↓
Transparent soap-film shader
```

The shader does not own the underlying content.

This gives the effect several important properties:

1. the number or artwork stays crisp;
2. the same shader can be reused over many UI components;
3. the effect can be enabled or disabled independently;
4. opacity can be adjusted without changing the original item;
5. older Android versions can simply omit the shader.

---

# 3. Visual Anatomy

The bubble is composed conceptually from four visual layers.

## 3.1 Transparent body

The center is intentionally almost invisible.

There should be no white wash across the middle and no large translucent glass fill.

The user should mostly see the original UI underneath.

---

## 3.2 Iridescent interference film

The main visual identity comes from pastel soap-film colors:

- cyan;
- lavender;
- pink;
- pale yellow.

These colors are intentionally desaturated toward white.

The rainbow should not appear as one clean circular gradient.

Instead, the shader creates several overlapping mathematical waves that produce:

- broken bands;
- curved regions;
- changing film thickness;
- asymmetric patches;
- slow color migration.

---

## 3.3 Thin edge rim

A faint edge helps establish the bubble boundary.

It must remain thin and low-opacity.

The rim should never become a heavy stroke.

---

## 3.4 White highlight crescents

Two small directional highlights make the object immediately recognizable as a bubble.

Primary highlight:

- upper-left;
- longer;
- slightly stronger.

Secondary highlight:

- lower-right;
- shorter;
- weaker.

They remain soft enough that the result still reads as flat UI artwork.

---

# 4. Motion Design

Motion is essential, but it should be restrained.

## Motion rule

> **The film moves more than the bubble itself.**

The implementation does not animate the circle size or position.

Instead, multiple wave fields move through the shader.

This creates the impression of liquid soap film flowing across a stable circular surface.

---

## 4.1 Avoid simple rotation

Do not implement the effect as:

```text
rainbow texture
→ rotate clockwise
→ repeat
```

That creates an artificial holographic-spinner appearance.

Instead, the shader combines waves moving in different directions and at different rates.

The visual result should feel closer to:

```text
cyan patch grows
        ↓
lavender band curls around it
        ↓
pink region slowly separates
        ↓
yellow highlight fades into the edge
        ↓
pattern evolves into a new configuration
```

---

## 4.2 Animation speed

Recommended default:

```kotlin
cycleDurationMillis = 12_000
```

The full state repeats every 12 seconds.

Because every time-dependent component is periodic, the end of the cycle mathematically matches the beginning.

This makes the restart visually seamless.

---

## 4.3 Stable geometry

The shader only adds very small mathematical radius distortion.

The circle itself stays stable.

The desired perception is approximately:

```text
90% stable shape
10% animated optical detail
```

---

# 5. Color System

The base soap palette is:

| Role | Approximate RGB | Character |
|---|---:|---|
| Cyan | `(0.48, 0.90, 1.00)` | fresh / watery |
| Lavender | `(0.76, 0.66, 1.00)` | soft magical transition |
| Pink | `(1.00, 0.67, 0.87)` | warm playful accent |
| Pale Yellow | `(1.00, 0.91, 0.58)` | warm interference highlight |

The shader then mixes these colors toward white:

```agsl
rainbow = mix(
    rainbow,
    float3(1.0),
    0.23
);
```

This is important.

Without the white mix, the shader quickly becomes too saturated and starts resembling a neon or holographic game effect.

---

# 6. Opacity System

The shader should remain delicate.

The final alpha is capped:

```agsl
alpha = clamp(
    alpha,
    0.0,
    0.30
);
```

Even at the strongest region, the overlay cannot become more than 30% opaque before normal compositing.

The majority of the surface is significantly more transparent.

This preserves:

- number readability;
- icon readability;
- item artwork;
- white surfaces;
- dark surfaces;
- compatibility with different content types.

---

# 7. Gravity Bias

The simulated film is slightly stronger toward the bottom of the bubble.

This is intentionally subtle.

The design purpose is not physical simulation accuracy; it is to prevent the shader from appearing perfectly symmetrical and synthetic.

The bottom receives a small visibility boost while the top remains cleaner.

---

# 8. Technical Architecture

The implementation uses:

- **Jetpack Compose**
- `Canvas`
- `RuntimeShader`
- Android Graphics Shading Language (**AGSL**)
- `ShaderBrush`
- `rememberInfiniteTransition`

`RuntimeShader` is available starting on Android 13 / API 33.

For older devices, the preferred fallback for this effect is simply:

> render the original UI without the animated bubble overlay.

The effect is decorative, so the absence of the shader should never affect usability.

---

# 9. Complete AGSL Shader

```kotlin
private const val SOAP_BUBBLE_SHADER = """
    uniform float2 resolution;
    uniform float time;
    uniform float strength;

    const float PI = 3.14159265359;
    const float TAU = 6.28318530718;

    // Pastel soap-film palette:
    // cyan -> lavender -> pink -> pale yellow -> cyan
    float3 soapPalette(float t) {
        t = fract(t) * 4.0;

        float3 cyan     = float3(0.48, 0.90, 1.00);
        float3 lavender = float3(0.76, 0.66, 1.00);
        float3 pink     = float3(1.00, 0.67, 0.87);
        float3 yellow   = float3(1.00, 0.91, 0.58);

        if (t < 1.0) {
            return mix(cyan, lavender, t);
        }

        if (t < 2.0) {
            return mix(lavender, pink, t - 1.0);
        }

        if (t < 3.0) {
            return mix(pink, yellow, t - 2.0);
        }

        return mix(yellow, cyan, t - 3.0);
    }

    half4 main(float2 fragCoord) {
        float minDim = min(resolution.x, resolution.y);

        // Normalize around the center.
        // Radius 1.0 represents the outer circle edge.
        float2 p =
            (fragCoord - resolution * 0.5)
            / (minDim * 0.5);

        float r = length(p);

        if (r > 1.0) {
            return half4(0.0);
        }

        float2 dir = p / max(r, 0.0001);
        float angle = atan(p.y, p.x);

        // Kotlin provides normalized time in the range 0..1.
        // TAU turns it into one full periodic mathematical cycle.
        float t = time * TAU;

        // ------------------------------------------------------------
        // ORGANIC FILM MOVEMENT
        //
        // Counter-moving waves avoid the appearance of a rainbow
        // texture simply rotating around the circle.
        // ------------------------------------------------------------

        float organicWarp =
              0.010 * sin(angle * 3.0 + t)
            + 0.006 * sin(angle * 5.0 - t * 2.0)
            + 0.004 * sin(angle * 7.0 + t * 3.0);

        float warpedRadius = r + organicWarp;

        // Film thickness simulation.
        //
        // Radial variation creates interference bands.
        // Angular variation breaks those bands into flowing patches.
        float thickness =
              warpedRadius * 23.0
            + 1.65 * sin(angle * 2.0 + sin(t))
            + 1.10 * sin(angle * 5.0 - cos(t * 2.0))
            + 0.80 * sin(
                angle * 3.0
                + warpedRadius * 7.0
                + sin(t * 3.0)
              );

        // Small gravitational bias.
        // Android canvas coordinates increase downward, so positive y
        // means the lower portion of the bubble.
        thickness += p.y * 1.20;

        float waveA =
            0.5 + 0.5 * sin(thickness);

        float waveB =
            0.5 + 0.5 * sin(
                thickness * 0.72
                + angle * 2.0
                - sin(t * 2.0)
            );

        float interference =
            waveA * 0.68 +
            waveB * 0.32;

        // Convert continuous waves into more isolated visible regions.
        float patches =
            smoothstep(0.32, 0.95, interference);

        // ------------------------------------------------------------
        // EDGE MASK
        //
        // The center stays almost entirely transparent.
        // ------------------------------------------------------------

        float filmRegion =
            smoothstep(
                0.66,
                0.94,
                warpedRadius
            );

        // Very thin physical bubble boundary.
        float thinRim =
            smoothstep(0.91, 0.975, r)
            *
            (
                1.0 -
                smoothstep(0.975, 1.0, r)
            );

        // ------------------------------------------------------------
        // GRAVITY VISIBILITY BIAS
        // ------------------------------------------------------------

        float gravity =
            smoothstep(
                -0.45,
                0.90,
                dir.y
            );

        float gravityBoost =
            mix(
                0.82,
                1.18,
                gravity
            );

        // ------------------------------------------------------------
        // IRIDESCENT COLOR
        // ------------------------------------------------------------

        float palettePhase =
            thickness / TAU
            + 0.08 * sin(angle * 4.0 + t)
            + 0.04 * sin(t * 2.0);

        float3 rainbow =
            soapPalette(palettePhase);

        // Move the palette toward white to maintain the pastel,
        // delicate appearance.
        rainbow =
            mix(
                rainbow,
                float3(1.0),
                0.23
            );

        // ------------------------------------------------------------
        // WHITE SPECULAR CRESCENTS
        // ------------------------------------------------------------

        // Upper-left primary highlight.
        float2 highlightDir1 =
            normalize(float2(-0.68, -0.74));

        float highlight1 =
            pow(
                max(
                    dot(dir, highlightDir1),
                    0.0
                ),
                22.0
            );

        // Lower-right secondary highlight.
        float2 highlightDir2 =
            normalize(float2(0.70, 0.72));

        float highlight2 =
            pow(
                max(
                    dot(dir, highlightDir2),
                    0.0
                ),
                32.0
            );

        float highlightRadial =
            smoothstep(0.78, 0.93, r);

        float highlights =
            (
                highlight1 * 0.85 +
                highlight2 * 0.55
            )
            * highlightRadial;

        // ------------------------------------------------------------
        // FINAL ALPHA
        // ------------------------------------------------------------

        float rainbowAlpha =
            filmRegion
            *
            (
                0.012 +
                patches * 0.105
            )
            *
            gravityBoost;

        float rimAlpha =
            thinRim * 0.13;

        float highlightAlpha =
            highlights * 0.22;

        float alpha =
            (
                rainbowAlpha +
                rimAlpha +
                highlightAlpha
            )
            * strength;

        alpha =
            clamp(
                alpha,
                0.0,
                0.30
            );

        // Blend the interference color toward white at highlights.
        float specMix =
            clamp(
                highlights * 0.85,
                0.0,
                1.0
            );

        float3 color =
            mix(
                rainbow,
                float3(1.0),
                specMix
            );

        // AGSL output participates in Android's compositing pipeline.
        // Return premultiplied RGB for the transparent overlay.
        half a = half(alpha);
        half3 rgb = half3(color) * a;

        return half4(rgb, a);
    }
"""
```

---

# 10. Reusable Compose Overlay

```kotlin
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush

@Composable
fun SoapBubbleOverlay(
    modifier: Modifier = Modifier,
    strength: Float = 1f,
    cycleDurationMillis: Int = 12_000,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        content()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SoapBubbleShaderLayer(
                modifier = Modifier.matchParentSize(),
                strength = strength,
                cycleDurationMillis = cycleDurationMillis,
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun SoapBubbleShaderLayer(
    modifier: Modifier,
    strength: Float,
    cycleDurationMillis: Int,
) {
    val shader = remember {
        RuntimeShader(SOAP_BUBBLE_SHADER)
    }

    val brush = remember(shader) {
        ShaderBrush(shader)
    }

    val transition =
        rememberInfiniteTransition(
            label = "SoapBubbleTransition"
        )

    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = cycleDurationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "SoapBubbleTime",
    )

    Canvas(modifier = modifier) {
        shader.setFloatUniform(
            "resolution",
            size.width,
            size.height,
        )

        shader.setFloatUniform(
            "time",
            time,
        )

        shader.setFloatUniform(
            "strength",
            strength,
        )

        drawCircle(
            brush = brush,
            radius = size.minDimension / 2f,
        )
    }
}
```

---

# 11. Example: White Number Circle

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BubbleNumber(
    number: Int,
    modifier: Modifier = Modifier,
) {
    SoapBubbleOverlay(
        modifier = modifier.size(88.dp),
        strength = 1f,
        cycleDurationMillis = 12_000,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.White,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                color = Color(0xFF27282D),
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
```

Usage:

```kotlin
BubbleNumber(
    number = 7,
)
```

---

# 12. Design Tokens

These values should be treated as the initial design-system defaults.

## Default bubble

```kotlin
strength = 1.0f
cycleDurationMillis = 12_000
```

Use this for:

- standard bubble badges;
- number tokens;
- medium-size UI items;
- the base design-system appearance.

---

## Small inventory item

```kotlin
strength = 0.85f
cycleDurationMillis = 14_000
```

Why:

Small objects have fewer pixels available for the interference bands.

A slightly weaker and slower effect prevents the edge from becoming visually noisy.

---

## Selected / rewarded item

```kotlin
strength = 1.15f
cycleDurationMillis = 9_000
```

Why:

The bubble becomes a little easier to notice without changing its fundamental visual identity.

This can be useful when the effect communicates:

- selection;
- rarity;
- reward;
- activation;
- a temporary positive state.

Avoid pushing the default strength much beyond approximately:

```kotlin
1.25f
```

At higher values the effect begins moving away from delicate soap film and toward a holographic or shield-like appearance.

---

# 13. Recommended Component API

For a larger design system, the public component can remain intentionally small.

Recommended public inputs:

```kotlin
@Composable
fun SoapBubbleOverlay(
    modifier: Modifier = Modifier,
    strength: Float = 1f,
    cycleDurationMillis: Int = 12_000,
    content: @Composable BoxScope.() -> Unit,
)
```

Do not expose every internal shader number immediately.

The AGSL constants should initially be treated as visual-design internals.

This protects consistency across the app.

Only expose additional properties if the product eventually requires meaningful variants.

---

# 14. Expected Final Output

On a white circle containing a dark number, the result should visually behave like this:

```text
        faint cyan / lavender
              ↓
       ╭─────────────╮
     ╱                 ╲
 pink      mostly        pale yellow
film      transparent       film

           7

 cyan                       lavender
     ╲                 ╱
       ╰─────────────╯
           pink / cyan
```

The diagram exaggerates the colors for clarity.

In the actual UI:

- the color is softer;
- the bands are broken;
- much of the circumference can temporarily appear almost empty;
- the center is nearly untouched;
- the dark number remains the highest-contrast element.

---

# 15. Expected Behaviour Over Time

At one point in the animation:

```text
upper edge       cyan
left edge        pink
right edge       faint yellow
bottom edge      lavender
```

Several seconds later:

```text
upper edge       lavender
left edge        mostly transparent
right edge       cyan
bottom edge      pink + pale yellow
```

Later:

```text
upper edge       pink
left edge        cyan
right edge       lavender
bottom edge      cyan
```

The transition between these configurations is continuous.

There should be no obvious moment where the user can identify:

> "the animation restarted."

---

# 16. Interaction With Different Backgrounds

## White surfaces

The rainbow interference provides most of the visual information.

The white highlight crescents may become less visible, which is acceptable.

The pastel rainbow should remain subtle enough that the white circle still feels white.

---

## Dark surfaces

The white crescents become more visible.

The pastel colors should appear brighter because of contrast.

Do not automatically increase shader opacity simply because the background is dark.

The same design should work across both themes before adding theme-specific tuning.

---

## Rich artwork

The shader should behave as an accent.

Avoid large alpha values because the underlying illustration or item should not become washed out.

---

# 17. Why This Is Not a True Refraction Shader

A physically accurate bubble could distort or refract the pixels underneath it.

That is intentionally excluded from the base implementation.

Reasons:

1. the UI content needs to remain crisp;
2. text and numbers must remain immediately readable;
3. refraction increases complexity;
4. refraction moves the visual language toward glass / lens rendering;
5. this application's desired aesthetic is flat 2D.

If refraction is ever introduced, it should be:

- a separate optional effect;
- concentrated near the outer edge;
- approximately 1–2 px;
- weak enough that users do not perceive the item as warped.

It is **not required** for the intended design.

---

# 18. Accessibility and Motion

The bubble is decorative.

Important information must never depend on:

- rainbow position;
- animation state;
- highlight visibility;
- color alone.

If the application implements a reduced-motion preference, a good fallback is to freeze the shader at one static time value instead of removing its entire visual identity.

For example:

```text
Normal motion:
time continuously animates 0 → 1.

Reduced motion:
time = 0.18
```

The static frame still communicates the bubble style without animation.

---

# 19. Performance Guidance

Keep one `RuntimeShader` instance per composable instance and reuse its `ShaderBrush`.

Do not recreate the shader inside every draw pass.

The implementation above uses:

```kotlin
remember {
    RuntimeShader(SOAP_BUBBLE_SHADER)
}
```

and:

```kotlin
remember(shader) {
    ShaderBrush(shader)
}
```

Only uniforms are updated during drawing.

The shader itself contains procedural math and does not require:

- PNG textures;
- bitmap assets;
- sprite sheets;
- Lottie files;
- network assets.

---

# 20. Android Version Behaviour

## Android 13+ / API 33+

Render the complete animated AGSL bubble.

## Android 12 and below

Render the content normally without the shader.

Example:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    SoapBubbleShaderLayer(...)
}
```

This is intentionally a graceful degradation rather than attempting to emulate the full shader using a different rendering system.

---

# 21. Visual Acceptance Checklist

The implementation is correct when all of the following are true:

- [ ] The center looks almost completely transparent.
- [ ] The underlying number or icon stays crisp.
- [ ] Rainbow colors are pastel rather than neon.
- [ ] Color exists mainly near the outer part of the circle.
- [ ] The rainbow does not form a perfectly uniform ring.
- [ ] The pattern changes organically over time.
- [ ] The animation does not look like simple rotation.
- [ ] The animation loop does not visibly snap.
- [ ] The bubble does not scale or pulse.
- [ ] The white circle underneath still looks white.
- [ ] The shader feels flat and 2D.
- [ ] The result does not resemble a shield or glass orb.
- [ ] Highlights remain thin and restrained.
- [ ] The effect works as decoration over arbitrary UI content.

---

# 22. Things to Avoid During Future Iteration

Do not add these unless the visual direction intentionally changes:

```text
heavy blur
strong chromatic aberration
large refraction
neon saturation
thick rainbow borders
3D lighting
drop shadows belonging to the bubble
glossy glass fill
constant clockwise rotation
rapid animation
scale pulsing
large center opacity
noise that makes the shader grainy
```

All of these push the design away from the intended clean soap-film aesthetic.

---

# 23. Final Design Summary

The final shader should be perceived in this order:

1. **the UI item**
2. **the number / icon / artwork**
3. **a delicate soap-film edge**
4. **slow organic color movement**
5. **small white highlights**

The user should never feel that the content has been placed *inside* a heavy visual effect.

Instead, the impression should be:

> a nearly weightless, transparent soap bubble is resting over the item.

The most important principles are:

```text
transparent center
+
pastel interference
+
thin rim
+
soft highlights
+
slow counter-moving waves
+
stable 2D geometry
```

That combination produces the intended flat, playful, premium soap-bubble effect while preserving the clarity of the application's underlying design system.

---

# 24. Android Reference Notes

The implementation is based on Android's official `RuntimeShader` / AGSL and Jetpack Compose shader-brush APIs.

- Android `RuntimeShader` was added in API level 33.
- Compose can render a `RuntimeShader` through `ShaderBrush` in drawing APIs such as `Canvas`.
- Android recommends providing a fallback for devices below Android 13 when using `RuntimeShader`.

Official documentation:

- Android Developers — **Brush: gradients and shaders**
- Android Developers — **Using AGSL in your Android app**
- Android Developers — **RuntimeShader API reference**
- Android Developers — **Android Graphics Shading Language (AGSL)**
