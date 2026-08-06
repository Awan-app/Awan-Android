package com.awan.app.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A tactile button with an attached surface that unfolds behind the trigger. The caller owns
 * [expanded]; this component owns only the disclosure geometry and motion.
 */
@Composable
fun AwanDisclosure(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    label: String,
    stateDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduced = reducedMotion()
    val enter = if (reduced) {
        EnterTransition.None
    } else {
        fadeIn(tween(AwanTheme.motion.standardMillis)) + expandVertically(
            animationSpec = tween(AwanTheme.motion.standardMillis),
            expandFrom = Alignment.Top,
        )
    }
    val exit = if (reduced) {
        ExitTransition.None
    } else {
        fadeOut(tween(AwanTheme.motion.standardMillis)) + shrinkVertically(
            animationSpec = tween(AwanTheme.motion.standardMillis),
            shrinkTowards = Alignment.Top,
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        AwanButton(
            onClick = { onExpandedChange(!expanded) },
            variant = AwanButtonVariant.Secondary,
            enabled = enabled,
            latchedPressed = expanded,
            icon = icon?.let { imageVector ->
                {
                    Icon(imageVector = imageVector, contentDescription = null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .semantics { this.stateDescription = stateDescription },
        ) {
            AwanText(label)
        }

        AnimatedVisibility(
            visible = expanded,
            enter = enter,
            exit = exit,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -AwanButtonRimDepth)
                .zIndex(0f),
        ) {
            AwanCard(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}
