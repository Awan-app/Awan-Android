package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.CenteredHeadline
import com.awan.feature.onboarding.impl.ui.components.StepScaffold

@Composable
fun NotificationsStep(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    StepScaffold(
        onBack = { onAction(OnboardingAction.Back) },
        progressCurrent = state.step.dotIndex,
        footer = {
            AwanButton(onClick = { onAction(OnboardingAction.EnableNotifications) }, modifier = Modifier.fillMaxWidth()) {
                AwanText(stringResource(R.string.onboarding_notifications_turn_on))
            }
            AwanButton(onClick = { onAction(OnboardingAction.Next) }, variant = AwanButtonVariant.Quiet) {
                AwanText(stringResource(R.string.onboarding_notifications_not_now), style = AwanTheme.styles.skipLink)
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AwanMascot(MascotExpression.Idle, width = 136.dp)
        }
        CenteredHeadline(stringResource(R.string.onboarding_notifications_title))
        SampleNotification()
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm), modifier = Modifier.fillMaxWidth()) {
            NotificationBullet(stringResource(R.string.onboarding_notifications_bullet_starts))
            NotificationBullet(stringResource(R.string.onboarding_notifications_bullet_fix))
            NotificationBullet(stringResource(R.string.onboarding_notifications_bullet_spam))
        }
        AwanText(
            stringResource(R.string.onboarding_notifications_permission_note),
            style = AwanTheme.styles.metaText,
        )
        if (state.notificationsPermanentlyDenied) {
            AwanText(
                stringResource(R.string.onboarding_notifications_denied),
                style = AwanTheme.styles.captionText,
            )
        }
    }
}

@Composable
private fun SampleNotification() {
    AwanCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(AwanTheme.colors.sky),
                contentAlignment = Alignment.Center,
            ) {
                AwanText("🔔", style = AwanTheme.styles.headingText)
            }
            Column(modifier = Modifier.weight(1f)) {
                AwanText(stringResource(R.string.onboarding_notifications_preview_sender), style = AwanTheme.styles.headingText)
                AwanText(stringResource(R.string.onboarding_notifications_preview_body), style = AwanTheme.styles.bodySecondaryText)
            }
        }
    }
}

@Composable
private fun NotificationBullet(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(AwanTheme.colors.sky.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            AwanText("✓", style = AwanTheme.styles.skipLink)
        }
        AwanText(text, style = AwanTheme.styles.bodySecondaryText, modifier = Modifier.weight(1f))
    }
}
