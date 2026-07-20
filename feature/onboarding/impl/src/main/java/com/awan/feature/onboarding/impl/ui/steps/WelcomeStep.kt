package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.CascadeItem
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.ui.components.StepBody

@Composable
fun WelcomeStepBody() {
    StepBody(scrollable = false, arrangement = Arrangement.spacedBy(AwanTheme.spacing.lg)) {
        CascadeItem(0, Modifier.align(Alignment.CenterHorizontally)) {
            BasicText(
                text = stringResource(R.string.onboarding_welcome_greeting),
                style = AwanTheme.typography.display.copy(
                    color = AwanTheme.colors.textPrimary,
                    fontSize = 32.sp,
                    lineHeight = 35.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        CascadeItem(1, Modifier.align(Alignment.CenterHorizontally)) {
            Box(Modifier.widthIn(max = 300.dp)) {
                BasicText(
                    text = stringResource(R.string.onboarding_welcome_subtitle),
                    style = AwanTheme.typography.body.copy(
                        color = AwanTheme.colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}
