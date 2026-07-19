package com.awan.feature.onboarding.impl.ui.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.presentation.OnboardingAction
import com.awan.feature.onboarding.impl.ui.components.StepScaffold

@Composable
fun WelcomeStep(onAction: (OnboardingAction) -> Unit) {
    StepScaffold(
        onBack = { onAction(OnboardingAction.Back) },
        showBack = false,
        scrollableContent = false,
        contentArrangement = Arrangement.Center,
        footer = {
            AwanButton(
                onClick = { onAction(OnboardingAction.Next) },
                modifier = Modifier.fillMaxWidth(),
            ) { AwanText(stringResource(R.string.onboarding_welcome_lets_go)) }
            AwanButton(onClick = { onAction(OnboardingAction.Skip) }, variant = AwanButtonVariant.Quiet) {
                AwanText(stringResource(R.string.onboarding_welcome_skip_setup), style = AwanTheme.styles.skipLink)
            }
        },
    ) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        AnimatedVisibility(visible = shown, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 6 }) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.lg),
            ) {
                AwanMascot(MascotExpression.Greet, width = 190.dp)
                BasicText(
                    text = stringResource(R.string.onboarding_welcome_greeting),
                    style = AwanTheme.typography.display.copy(
                        color = AwanTheme.colors.textPrimary,
                        fontSize = 32.sp,
                        lineHeight = 35.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
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
}
