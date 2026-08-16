package com.awan.app.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.MicOff
import kotlin.math.PI
import kotlin.math.sin

private const val IdlePulseMillis = 800
private val MicIconSize = 20.dp

/** How far the halo grows past the button at full volume, and how fast it chases the level. */
private const val HaloMaxGrowth = 0.85f
private const val HaloRiseRate = 0.35f
private const val HaloFallRate = 0.12f
private const val HaloAlpha = 0.30f

/**
 * Dictation toggle for a text field's `trailingContent`, paired with [rememberSpeechRecognizer].
 *
 * While listening, a halo behind the button tracks [amplitude] — the actual microphone level, not a
 * timer. That distinction is the point: a halo that never moves while listening means the device is
 * handing us silence, which is a diagnosis rather than a mystery. Falls back to a plain breathing
 * pulse when no level is supplied, or when the user has animations turned down.
 */
@Composable
fun AwanMicButton(
    isListening: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    amplitude: (() -> Float)? = null,
) {
    val reduced = reducedMotion()
    val live = isListening && enabled && !reduced
    val haloColor = AwanTheme.colors.sky

    /**
     * Snapshot state, read only inside `drawBehind`/`graphicsLayer`: writing it invalidates the
     * draw phase and nothing else, so a level arriving every audio frame never recomposes the
     * field it is sitting in.
     */
    val level = remember { mutableFloatStateOf(0f) }
    val latestAmplitude by rememberUpdatedState(amplitude)

    // Smoothed here rather than in the recognizer: raw onRmsChanged is jumpy enough to strobe, and
    // a slower fall than rise is what makes the halo read as a voice rather than a flicker.
    LaunchedEffect(live) {
        if (!live) {
            level.floatValue = 0f
            return@LaunchedEffect
        }
        var elapsedNanos = 0L
        var lastFrame = 0L
        while (true) {
            withFrameNanos { frame ->
                if (lastFrame != 0L) elapsedNanos += frame - lastFrame
                lastFrame = frame
                // No amplitude source: breathe, so the button still reads as live.
                val target = latestAmplitude?.invoke()
                    ?: (sin(elapsedNanos / 1_000_000f / IdlePulseMillis * PI.toFloat()) * 0.5f + 0.5f)
                val rate = if (target > level.floatValue) HaloRiseRate else HaloFallRate
                level.floatValue += (target - level.floatValue) * rate
            }
        }
    }

    Box(
        modifier = modifier.drawBehind {
            val loudness = level.floatValue
            if (loudness <= 0.01f) return@drawBehind
            drawCircle(
                color = haloColor,
                radius = size.minDimension / 2f * (1f + HaloMaxGrowth * loudness),
                alpha = HaloAlpha * loudness,
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        AwanIconButton(
            onClick = onToggle,
            contentDescription = stringResource(
                if (isListening) R.string.ds_mic_listening else R.string.ds_mic_idle,
            ),
            enabled = enabled,
            modifier = Modifier.graphicsLayer {
                val scale = 1f + 0.06f * level.floatValue
                scaleX = scale
                scaleY = scale
            },
        ) {
            Icon(
                imageVector = if (isListening) Lucide.MicOff else Lucide.Mic,
                contentDescription = null,
                tint = if (isListening) AwanTheme.colors.sky else AwanTheme.colors.textSecondary,
                modifier = Modifier.size(MicIconSize),
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "AwanMicButton · Light", showBackground = true)
@Composable
private fun LightMicButtonPreview() {
    MicButtonPreview(dark = false)
}

@Preview(name = "AwanMicButton · Dark", showBackground = true)
@Composable
private fun DarkMicButtonPreview() {
    MicButtonPreview(dark = true)
}

@Composable
private fun MicButtonPreview(dark: Boolean) {
    AwanTheme(dark = dark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .styleable(null, AwanTheme.styles.screen)
                .padding(20.dp),
        ) {
            AwanTextField(
                value = "",
                onValueChange = {},
                placeholder = "Idle",
                trailingContent = { AwanMicButton(isListening = false, onToggle = {}) },
                modifier = Modifier.fillMaxWidth(),
            )
            AwanTextField(
                value = "Go for a run",
                onValueChange = {},
                placeholder = "",
                trailingContent = {
                    AwanMicButton(isListening = true, onToggle = {}, amplitude = { 0.7f })
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }
}
