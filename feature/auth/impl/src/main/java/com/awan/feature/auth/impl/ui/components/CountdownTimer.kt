package com.awan.feature.auth.impl.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.feature.auth.impl.R
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
fun rememberCountdownTimerState(
    initialSeconds: Int = 120,
    key: Any? = null,
    onExpired: () -> Unit = {},
): CountdownTimerState {
    val state = rememberSaveable(key, saver = CountdownTimerState.Saver) {
        CountdownTimerState(initialSeconds)
    }

    val currentOnExpired by rememberUpdatedState(onExpired)

    LaunchedEffect(key, initialSeconds) {
        if (initialSeconds > 0) {
            state.secondsRemaining = initialSeconds
            while (state.secondsRemaining > 0) {
                delay(1_000)
                state.secondsRemaining -= 1
            }
            currentOnExpired()
        }
    }

    return state
}

@Composable
fun CountdownTimer(
    state: CountdownTimerState,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
    isResendEnabled: Boolean = false,
) {
    val canResend = isResendEnabled || state.isExpired
    val label = if (canResend) {
        stringResource(R.string.auth_resend_code)
    } else {
        stringResource(R.string.auth_resend_code_timer, state.formattedTime)
    }
    val semanticsLabel = if (canResend) {
        stringResource(R.string.auth_resend_code_button_desc)
    } else {
        stringResource(R.string.auth_resend_code_timer_desc, state.formattedTime)
    }

    AwanButton(
        onClick = { if (canResend) onResend() },
        modifier = modifier.semantics { contentDescription = semanticsLabel },
        variant = AwanButtonVariant.Quiet,
        enabled = canResend,
    ) {
        AwanText(text = label)
    }
}
