package com.awan.app.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.MicOff

private const val PulseMillis = 800
private val MicIconSize = 20.dp

/**
 * Dictation toggle for a text field's `trailingContent`, paired with [rememberSpeechRecognizer].
 * Breathes while listening so the field shows it is live without a second status line.
 */
@Composable
fun AwanMicButton(
    isListening: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shouldPulse = isListening && !reducedMotion() && enabled
    val pulseScale by if (shouldPulse) {
        rememberInfiniteTransition(label = "micPulse").animateFloat(
            initialValue = 1.0f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(PulseMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "micPulseScale",
        )
    } else {
        rememberUpdatedState(1.0f)
    }

    AwanIconButton(
        onClick = onToggle,
        contentDescription = stringResource(
            if (isListening) R.string.ds_mic_listening else R.string.ds_mic_idle,
        ),
        enabled = enabled,
        modifier = modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
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
                trailingContent = { AwanMicButton(isListening = true, onToggle = {}) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }
}
