package com.awan.feature.auth.impl.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import kotlinx.coroutines.delay

@Stable
class CountdownTimerState(initialSeconds: Int) {
    var secondsRemaining by mutableIntStateOf(initialSeconds)
        internal set

    val isExpired: Boolean get() = secondsRemaining <= 0

    val formattedTime: String
        get() = "%d:%02d".format(secondsRemaining / 60, secondsRemaining % 60)

    companion object {
        val Saver: Saver<CountdownTimerState, *> = Saver(
            save = { it.secondsRemaining },
            restore = { CountdownTimerState(it) }
        )
    }
}

@Composable
fun CountdownTimer(
    state: CountdownTimerState,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
    isResendEnabled: Boolean = false,
) {
    // Tick every second while the timer has not expired.
    LaunchedEffect(state) {
        while (!state.isExpired) {
            delay(1_000)
            state.secondsRemaining = (state.secondsRemaining - 1).coerceAtLeast(0)
        }
    }

    val canResend = isResendEnabled || state.isExpired
    val label = if (canResend) {
        "RESEND CODE"
    } else {
        "RESEND CODE · ${state.formattedTime}"
    }
    val semanticsLabel = if (canResend) "Resend code button" else "Resend code in ${state.formattedTime}"

    AwanButton(
        onClick = { if (canResend) onResend() },
        modifier = modifier.semantics { contentDescription = semanticsLabel },
        variant = AwanButtonVariant.Quiet,
        enabled = canResend,
    ) {
        AwanText(text = label)
    }
}

@Composable
fun rememberCountdownTimerState(initialSeconds: Int = 120): CountdownTimerState {
    return rememberSaveable(initialSeconds, saver = CountdownTimerState.Saver) {
        CountdownTimerState(initialSeconds)
    }
}
