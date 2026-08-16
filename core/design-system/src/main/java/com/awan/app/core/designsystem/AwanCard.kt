package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * The Skyward "rim" card: a soft white surface sitting on a slightly darker bottom edge, giving the
 * whole kit its chunky, tactile feel. [selected] lifts it with a sky border + glow.
 */
@Composable
fun AwanCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectedColor: Color = AwanTheme.colors.sky,
    onClick: (() -> Unit)? = null,
    haptic: HapticFeedbackType? = HapticFeedbackType.ContextClick,
    background: Color = AwanTheme.colors.surface,
    customRimColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AwanTheme.colors
    val shape = AwanTheme.shapes.card
    val borderColor by animateColorAsState(if (selected) selectedColor else colors.line, label = "cardBorder")

    val defaultRimColor = if (selected) selectedColor.copy(alpha = 0.35f) else colors.line
    val targetRimColor = customRimColor ?: defaultRimColor
    val rimColor by animateColorAsState(targetRimColor, label = "cardRim")
    val sizeSpec = if (reducedMotion()) snap() else AwanTheme.motion.settle.spec<IntSize>()

    val hapticClick = rememberHapticClick(onClick = { onClick?.invoke() }, haptic = haptic)
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = hapticClick,
        )
    } else {
        Modifier
    }

    Layout(
        modifier = modifier
            .then(clickModifier)
            .shadow(
                elevation = if (selected) 10.dp else 0.dp,
                shape = shape,
                spotColor = selectedColor,
                ambientColor = selectedColor,
            )
            .animateContentSize(animationSpec = sizeSpec),
        content = {
            // Rim (index 0)
            Box(Modifier.clip(shape).background(rimColor))
            // Face (index 1)
            Column(
                modifier = Modifier
                    .padding(bottom = AwanCardRimDepth)
                    .clip(shape)
                    .background(background)
                    .border(2.dp, borderColor, shape)
                    .padding(contentPadding),
                content = content,
            )
        }
    ) { measurables, constraints ->
        // Face is measured first. Coerce constraints to be valid.
        val safeMaxWidth = constraints.maxWidth.coerceAtLeast(0)
        val safeMaxHeight = constraints.maxHeight.coerceAtLeast(0)
        val minW = constraints.minWidth.coerceIn(0, safeMaxWidth)
        val minH = constraints.minHeight.coerceIn(0, safeMaxHeight)

        val safeConstraints = Constraints(
            minWidth = minW,
            maxWidth = maxOf(safeMaxWidth, minW),
            minHeight = minH,
            maxHeight = maxOf(safeMaxHeight, minH)
        )

        val facePlaceable = measurables[1].measure(safeConstraints)
        val width = facePlaceable.width.coerceAtLeast(0)
        val height = facePlaceable.height.coerceAtLeast(0)

        // Rim needs room for padding(bottom = AwanCardRimDepth)
        val minRimHeight = AwanCardRimDepth.roundToPx().coerceAtLeast(0)
        val rimConstraints = Constraints.fixed(
            width,
            height.coerceAtLeast(minRimHeight)
        )
        val rimPlaceable = measurables[0].measure(rimConstraints)

        layout(width, height) {
            rimPlaceable.placeRelative(0, 0)
            facePlaceable.placeRelative(0, 0)
        }
    }
}

internal val AwanCardRimDepth = 4.dp

/** The rim must hug the surface on every edge, whatever the content width. */
@Preview(name = "Card rim · narrow content in a full-width card", showBackground = true)
@Composable
private fun CardRimPreview() {
    AwanTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AwanCard(modifier = Modifier.fillMaxWidth()) {
                AwanText("Short line.", style = AwanTheme.styles.metaText)
            }
            AwanCard(modifier = Modifier.fillMaxWidth()) {
                AwanText(
                    "A line long enough to wrap onto a second row inside the card surface.",
                    style = AwanTheme.styles.metaText,
                )
            }
            AwanCard {
                AwanText("Wrap-content card.", style = AwanTheme.styles.metaText)
            }
        }
    }
}
