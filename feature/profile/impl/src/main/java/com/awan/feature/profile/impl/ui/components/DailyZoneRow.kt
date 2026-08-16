package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.helpers.DailyZonesHelper

private const val PRESS_SCALE = 0.98f
private val TimeIndent = 25.dp

@Composable
fun DailyZoneRow(
    zone: DailyZone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val zoneColor = zone.color.toColor()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hapticClick = rememberHapticClick(onClick)
    val reduced = reducedMotion()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !reduced) PRESS_SCALE else 1f,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "zonePress",
    )

    val backgroundColor = zoneColor
        .copy(alpha = AwanTheme.colors.zoneCardAlpha)
        .compositeOver(AwanTheme.colors.surface)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(AwanTheme.shapes.card)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = hapticClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {
            dragHandle()
        Column(modifier = Modifier.weight(1f)) {
            AwanText(
                zone.name,
                style = AwanTheme.styles.headingText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            zone.category?.name?.let { categoryName ->
                AwanText(
                    text = categoryName,
                    style = AwanTheme.styles.metaText.copy(color = zoneColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = TimeIndent),
        ) {
            val startTime = DailyZonesHelper.formatTime12h(context, zone.startTime)
            val endTime = DailyZonesHelper.formatTime12h(context, zone.endTime)
            AwanText(
                text = "$startTime - $endTime",
                style = AwanTheme.styles.skipLink,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = zoneColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
