package com.awan.app.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MenuMaxHeight = 200.dp
private val ThumbWidth = 3.dp
private val ThumbInset = 6.dp
private val MinThumbLength = 24.dp

/**
 * A menu that hangs off whatever composable opened it, for choices too small to be worth a dialog.
 *
 * It caps its own height and fades the edge it can still scroll towards, so a list longer than the
 * menu says so instead of ending in a clean line that reads as "that's all there is".
 */
@Composable
fun AwanDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = MenuMaxHeight,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    // Booleans, not the raw offset: the menu body would otherwise recompose on every scrolled pixel.
    val hasAbove by remember { derivedStateOf { scroll.canScrollBackward } }
    val hasBelow by remember { derivedStateOf { scroll.canScrollForward } }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = AwanTheme.colors.surface,
        shape = AwanTheme.shapes.card,
        modifier = modifier,
    ) {
        Box {
            Column(
                modifier = Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(scroll),
                content = content,
            )

            ScrollThumb(scroll = scroll, visible = hasAbove || hasBelow)
        }
    }
}

/**
 * A thumb rather than a faded edge: the menu's height can land exactly on a row boundary, and a list
 * that ends flush reads as the whole list no matter how it's tinted.
 *
 * Drawn, not laid out — the scroll offset is read inside the draw scope, so scrolling repaints this
 * one node instead of recomposing the rows.
 */
@Composable
private fun BoxScope.ScrollThumb(scroll: ScrollState, visible: Boolean) {
    val color = AwanTheme.colors.meta
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "menuScrollThumb",
    )
    if (alpha == 0f) return

    Canvas(Modifier.matchParentSize().padding(vertical = ThumbInset)) {
        val track = size.height
        val content = track + scroll.maxValue
        val thumb = (track * track / content).coerceAtLeast(MinThumbLength.toPx())
        val travel = (track - thumb) * (scroll.value.toFloat() / scroll.maxValue)
        val width = ThumbWidth.toPx()
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(size.width - width, travel),
            size = Size(width, thumb),
            cornerRadius = CornerRadius(width / 2),
        )
    }
}

/**
 * A row of [AwanDropdownMenu]. [selected] is the difference between the current value and the rest;
 * a non-[enabled] row reads as a static note in muted ink — used for empty-state messages.
 */
@Composable
fun AwanDropdownMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null,
) {
    val hapticClick = rememberHapticClick(onClick, HapticFeedbackType.SegmentTick)
    val colors = AwanTheme.colors
    val ink = when {
        !enabled -> colors.meta
        selected -> colors.textPrimary
        else -> colors.textSecondary
    }
    DropdownMenuItem(
        onClick = hapticClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leading,
        text = { AwanText(text = label, style = AwanTheme.styles.buttonCompactText.copy(color = ink)) },
    )
}
