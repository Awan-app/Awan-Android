package com.awan.feature.auth.impl.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.auth.impl.ui.email.EmailScreen
import com.awan.feature.auth.impl.ui.email.EmailUiState

// ── Email Screen Previews ─────────────────────────────────────────────────────

@Preview(name = "Email · Empty / Button Disabled", showBackground = true)
@Composable
private fun EmailEmptyPreview() {
    AwanTheme {
        EmailScreen(
            state = EmailUiState(),
            onEmailChanged = {},
            onContinue = {},
            onSignInWithGoogle = {},
        )
    }
}

@Preview(name = "Email · Valid / Button Enabled", showBackground = true)
@Composable
private fun EmailValidPreview() {
    AwanTheme {
        EmailScreen(
            state = EmailUiState(email = "sam@cloud.com", isEmailValid = true),
            onEmailChanged = {},
            onContinue = {},
            onSignInWithGoogle = {},
        )
    }
}

@Preview(name = "Email · Invalid / Inline Error", showBackground = true)
@Composable
private fun EmailErrorPreview() {
    AwanTheme {
        EmailScreen(
            state = EmailUiState(
                email = "sam@cloud",
                isEmailValid = false,
                errorMessage = "Enter a valid email address",
            ),
            onEmailChanged = {},
            onContinue = {},
            onSignInWithGoogle = {},
        )
    }
}

@Preview(name = "Email · Loading / Sending", showBackground = true)
@Composable
private fun EmailLoadingPreview() {
    AwanTheme {
        EmailScreen(
            state = EmailUiState(email = "sam@cloud.com", isEmailValid = true, isLoading = true),
            onEmailChanged = {},
            onContinue = {},
            onSignInWithGoogle = {},
        )
    }
}

@Preview(name = "Email · Rate Limited", showBackground = true)
@Composable
private fun EmailRateLimitedPreview() {
    AwanTheme {
        EmailScreen(
            state = EmailUiState(
                email = "sam@cloud.com",
                isEmailValid = true,
                isRateLimited = true,
                rateLimitSecondsRemaining = 38,
            ),
            onEmailChanged = {},
            onContinue = {},
            onSignInWithGoogle = {},
        )
    }
}

@Preview(name = "Email · Offline", showBackground = true)
@Composable
private fun EmailOfflinePreview() {
    AwanTheme {
        EmailScreen(
            state = EmailUiState(email = "sam@cloud.com", isEmailValid = true, isOffline = true),
            onEmailChanged = {},
            onContinue = {},
            onSignInWithGoogle = {},
        )
    }
}

@Preview(
    name = "Email · Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun EmailDarkPreview() {
    AwanTheme(darkTheme = true) {
        EmailScreen(
            state = EmailUiState(email = "sam@cloud.com", isEmailValid = true),
            onEmailChanged = {},
            onContinue = {},
            onSignInWithGoogle = {},
        )
    }
}

@Preview(name = "Email · Large Font", showBackground = true, fontScale = 1.5f)
@Composable
private fun EmailLargeFontPreview() {
    AwanTheme {
        EmailScreen(
            state = EmailUiState(email = "sam@cloud.com", isEmailValid = true),
            onEmailChanged = {},
            onContinue = {},
            onSignInWithGoogle = {},
        )
    }
}
