package com.awan.feature.auth.impl.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.awan.app.core.common.text.UiText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.auth.impl.ui.otp.OtpScreen
import com.awan.feature.auth.impl.ui.otp.OtpStatus
import com.awan.feature.auth.impl.ui.otp.OtpUiState

// ── OTP Screen Previews ───────────────────────────────────────────────────────

private val partialDigits = listOf("4", "2", "1", "", "", "")
private val fullDigits = listOf("4", "2", "8", "1", "9", "3")
private val emptyDigits = List(6) { "" }

@Preview(name = "OTP · Default + Countdown", showBackground = true)
@Composable
private fun OtpDefaultPreview() {
    AwanTheme {
        OtpScreen(
            state = OtpUiState(
                email = "sam@cloud.com",
                digits = partialDigits,
                status = OtpStatus.Idle,
                resendSecondsRemaining = 24,
            ),
            onDigitsChanged = {},
            onBack = {},
            onResend = {},
            onResendTimerExpired = {},
            onUseDifferentEmail = {},
        )
    }
}

@Preview(name = "OTP · Verifying", showBackground = true)
@Composable
private fun OtpVerifyingPreview() {
    AwanTheme {
        OtpScreen(
            state = OtpUiState(
                email = "sam@cloud.com",
                digits = fullDigits,
                status = OtpStatus.Verifying,
            ),
            onDigitsChanged = {},
            onBack = {},
            onResend = {},
            onResendTimerExpired = {},
            onUseDifferentEmail = {},
        )
    }
}

@Preview(name = "OTP · Wrong / Error", showBackground = true)
@Composable
private fun OtpWrongPreview() {
    AwanTheme {
        OtpScreen(
            state = OtpUiState(
                email = "sam@cloud.com",
                digits = fullDigits,
                status = OtpStatus.Wrong,
                errorMessage = UiText.DynamicString("That code isn't right. Check and try again."),
            ),
            onDigitsChanged = {},
            onBack = {},
            onResend = {},
            onResendTimerExpired = {},
            onUseDifferentEmail = {},
        )
    }
}

@Preview(name = "OTP · Expired", showBackground = true)
@Composable
private fun OtpExpiredPreview() {
    AwanTheme {
        OtpScreen(
            state = OtpUiState(
                email = "sam@cloud.com",
                digits = emptyDigits,
                status = OtpStatus.Expired,
                errorMessage = UiText.DynamicString("This code has expired. Request a new one."),
            ),
            onDigitsChanged = {},
            onBack = {},
            onResend = {},
            onResendTimerExpired = {},
            onUseDifferentEmail = {},
        )
    }
}

@Preview(name = "OTP · Locked", showBackground = true)
@Composable
private fun OtpLockedPreview() {
    AwanTheme {
        OtpScreen(
            state = OtpUiState(
                email = "sam@cloud.com",
                digits = emptyDigits,
                status = OtpStatus.Locked,
                errorMessage = UiText.DynamicString("Too many attempts. Request a new code."),
            ),
            onDigitsChanged = {},
            onBack = {},
            onResend = {},
            onResendTimerExpired = {},
            onUseDifferentEmail = {},
        )
    }
}

@Preview(
    name = "OTP · Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun OtpDarkPreview() {
    AwanTheme(darkTheme = true) {
        OtpScreen(
            state = OtpUiState(
                email = "sam@cloud.com",
                digits = partialDigits,
                status = OtpStatus.Idle,
                resendSecondsRemaining = 24,
            ),
            onDigitsChanged = {},
            onBack = {},
            onResend = {},
            onResendTimerExpired = {},
            onUseDifferentEmail = {},
        )
    }
}

@Preview(name = "OTP · Large Font", showBackground = true, fontScale = 1.5f)
@Composable
private fun OtpLargeFontPreview() {
    AwanTheme {
        OtpScreen(
            state = OtpUiState(
                email = "sam@cloud.com",
                digits = partialDigits,
                status = OtpStatus.Idle,
                resendSecondsRemaining = 24,
            ),
            onDigitsChanged = {},
            onBack = {},
            onResend = {},
            onResendTimerExpired = {},
            onUseDifferentEmail = {},
        )
    }
}
