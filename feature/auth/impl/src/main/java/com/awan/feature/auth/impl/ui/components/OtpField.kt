package com.awan.feature.auth.impl.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.OTP_SHAKE_DURATION_MILLIS
import com.awan.feature.auth.impl.R
import com.awan.feature.auth.impl.ui.otp.OtpStatus
import kotlin.math.roundToInt

@Composable
fun OtpField(
    digits: List<String>,
    onDigitsChange: (List<String>) -> Unit,
    status: OtpStatus,
    modifier: Modifier = Modifier,
) {
    require(digits.size == 6) { "OtpField requires exactly 6 digits, got ${digits.size}" }

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val isError = status == OtpStatus.Wrong
    val isDisabled = status == OtpStatus.Verifying || status == OtpStatus.Locked

    val codeString = digits.joinToString("")

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(status) {
        if (status == OtpStatus.Wrong) {
            val shakeDistance = 14f
            val stepDuration = OTP_SHAKE_DURATION_MILLIS / 4
            shakeOffset.animateTo(-shakeDistance, tween(stepDuration))
            shakeOffset.animateTo(shakeDistance, tween(stepDuration))
            shakeOffset.animateTo(-shakeDistance / 2, tween(stepDuration))
            shakeOffset.animateTo(0f, tween(stepDuration))
        }
    }

    LaunchedEffect(Unit) {
        if (!isDisabled) {
            focusRequester.requestFocus()
        }
    }

    val digitsDesc = stringResource(R.string.auth_otp_digits_description)

    Box(
        modifier = modifier
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .semantics(mergeDescendants = true) {
                contentDescription = digitsDesc
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = codeString,
            onValueChange = { newValue ->
                if (isDisabled) return@BasicTextField
                val filtered = newValue.filter { it.isDigit() }.take(6)
                val newDigits = List(6) { index ->
                    if (index < filtered.length) filtered[index].toString() else ""
                }
                onDigitsChange(newDigits)
            },
            modifier = Modifier
                .matchParentSize()
                .alpha(0.01f)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            enabled = !isDisabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { }
            ),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val activeIndex = codeString.length.coerceAtMost(5)
            digits.forEachIndexed { index, digit ->
                val isCellFocused = isFocused && !isDisabled && (index == activeIndex)
                OtpDigitCell(
                    digit = digit,
                    index = index,
                    isFocused = isCellFocused,
                    isError = isError,
                    isDisabled = isDisabled,
                    onClick = {
                        if (!isDisabled) {
                            focusRequester.requestFocus()
                        }
                    },
                )
            }
        }
    }
}
