package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.CascadeItem
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.presentation.OnboardingState
import com.awan.feature.onboarding.impl.ui.components.CenteredHeadline
import com.awan.feature.onboarding.impl.ui.components.StepBody

@Composable
fun NotificationsStepBody(state: OnboardingState, @Suppress("UNUSED_PARAMETER") onAction: (OnboardingAction) -> Unit) {
    StepBody {
        CascadeItem(0, Modifier.fillMaxWidth()) {
            CenteredHeadline(stringResource(R.string.onboarding_notifications_title))
        }
        CascadeItem(1, Modifier.fillMaxWidth()) {
            SampleNotifications()
        }
        CascadeItem(2, Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                NotificationBullet(stringResource(R.string.onboarding_notifications_bullet_sessions))
                NotificationBullet(stringResource(R.string.onboarding_notifications_bullet_checkin))
                NotificationBullet(stringResource(R.string.onboarding_notifications_bullet_control))
            }
        }
        CascadeItem(3, Modifier.fillMaxWidth()) {
            AwanText(
                stringResource(R.string.onboarding_notifications_permission_note),
                style = AwanTheme.styles.metaText,
            )
        }
        if (state.notificationsPermanentlyDenied) {
            AwanText(
                stringResource(R.string.onboarding_notifications_denied),
                style = AwanTheme.styles.captionText,
            )
        }
    }
}

/**
 * The three notifications the app actually sends, in the order a day produces them.
 *
 * Showing them beats describing them: the step is asking for a permission, and the honest answer to
 * "what will you send me" is the notifications themselves.
 */
@Composable
private fun SampleNotifications() {
    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
        SampleNotification(
            emoji = "🔔",
            accent = AwanTheme.colors.sky,
            title = stringResource(R.string.onboarding_notifications_preview_reminder_title),
            body = stringResource(R.string.onboarding_notifications_preview_reminder_body),
        )
        SampleNotification(
            emoji = "⏳",
            accent = AwanTheme.colors.zoneViolet,
            title = stringResource(R.string.onboarding_notifications_preview_live_title),
            body = stringResource(R.string.onboarding_notifications_preview_live_body),
            progress = 0.55f,
        )
        SampleNotification(
            emoji = "🌤",
            accent = AwanTheme.colors.zoneTangerine,
            title = stringResource(R.string.onboarding_notifications_preview_checkin_title),
            body = stringResource(R.string.onboarding_notifications_preview_checkin_body),
        )
    }
}

@Composable
private fun SampleNotification(
    emoji: String,
    accent: Color,
    title: String,
    body: String,
    progress: Float? = null,
) {
    AwanCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(emoji, style = AwanTheme.styles.headingText)
            }
            Column(modifier = Modifier.weight(1f)) {
                AwanText(title, style = AwanTheme.styles.headingText)
                AwanText(body, style = AwanTheme.styles.bodySecondaryText)
                if (progress != null) ProgressBar(fraction = progress, accent = accent)
            }
        }
    }
}

/** Stands in for the running session's progress bar. Static — it is a picture, not a live update. */
@Composable
private fun ProgressBar(fraction: Float, accent: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(CircleShape)
                .background(accent),
        )
    }
}

@Composable
private fun NotificationBullet(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(AwanTheme.colors.sky.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            AwanText("✓", style = AwanTheme.styles.skipLink)
        }
        AwanText(text, style = AwanTheme.styles.bodySecondaryText, modifier = Modifier.weight(1f))
    }
}
