package com.awan.feature.auth.impl.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.OtpCellSize
import com.awan.app.core.designsystem.OtpCellWidth
import com.awan.feature.auth.impl.R

@Composable
fun OtpDigitCell(
    digit: String,
    index: Int,
    isFocused: Boolean,
    isError: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> AwanTheme.colors.destructive
            isFocused -> AwanTheme.colors.sky
            digit.isNotEmpty() -> AwanTheme.colors.textPrimary
            else -> AwanTheme.colors.line
        },
        animationSpec = tween(durationMillis = 200),
        label = "cellBorderColor"
    )

    val cellScale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "cellScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "cursorTransition")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val shape = RoundedCornerShape(12.dp)
    val digitEmpty = stringResource(R.string.auth_otp_digit_cell_empty)
    val digitFilledPattern = stringResource(R.string.auth_otp_digit_cell_filled)
    val digitCellPattern = stringResource(R.string.auth_otp_digit_cell_desc)

    Box(
        modifier = modifier
            .width(OtpCellWidth)
            .height(OtpCellSize)
            .scale(cellScale)
            .background(
                color = if (isDisabled) AwanTheme.colors.disabledSurface else AwanTheme.colors.surface,
                shape = shape
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isDisabled,
                onClick = onClick
            )
            .semantics {
                val detail = if (digit.isNotEmpty()) {
                    digitFilledPattern.format(digit)
                } else {
                    digitEmpty
                }
                contentDescription = digitCellPattern.format(index + 1, detail)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (digit.isNotEmpty()) {
            AnimatedContent(
                targetState = digit,
                transitionSpec = {
                    (fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.7f)) togetherWith
                            (fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.7f))
                },
                label = "digitAnimation"
            ) { char ->
                AwanText(
                    text = char,
                    style = AwanTheme.styles.titleText,
                )
            }
        } else if (isFocused && !isDisabled) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .graphicsLayer { alpha = cursorAlpha }
                    .background(AwanTheme.colors.sky, shape = RoundedCornerShape(1.dp))
            )
        } else {
            AwanText(
                text = "•",
                style = AwanTheme.styles.bodyText,
                modifier = Modifier.graphicsLayer { alpha = 0.3f },
            )
        }
    }
}
