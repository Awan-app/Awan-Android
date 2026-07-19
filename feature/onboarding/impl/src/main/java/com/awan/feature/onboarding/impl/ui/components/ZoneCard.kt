package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.onboarding.impl.R
import com.awan.feature.onboarding.impl.ui.formatClock

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZoneCard(
    name: String,
    colorArgb: Int,
    startMinutes: Int,
    endMinutes: Int,
    enabled: Boolean,
    overlapping: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSetStart: (Int) -> Unit,
    onSetEnd: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoneColor = Color(colorArgb)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AwanTheme.shapes.card)
            .background(zoneColor.copy(alpha = if (enabled) 0.14f else 0.06f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
            DragHandle(zoneColor)
            AwanText(
                name,
                style = if (enabled) AwanTheme.styles.headingText else AwanTheme.styles.metaText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = zoneColor, checkedThumbColor = Color.White),
            )
        }
        if (enabled) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
            ) {
                ReorderButton("↑", canMoveUp, onMoveUp)
                ReorderButton("↓", canMoveDown, onMoveDown)
                EditTimeChip(stringResource(R.string.onboarding_zone_starts), startMinutes, onSetStart)
                EditTimeChip(stringResource(R.string.onboarding_zone_ends), endMinutes, onSetEnd)
            }
            if (overlapping) {
                AwanText(stringResource(R.string.onboarding_zone_overlap), style = AwanTheme.styles.captionText)
            }
        }
    }
}

@Composable
private fun ReorderButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    AwanButton(onClick = onClick, variant = AwanButtonVariant.Secondary, enabled = enabled) {
        AwanText(glyph, style = AwanTheme.styles.buttonCompactText)
    }
}

@Composable
private fun EditTimeChip(label: String, minutes: Int, onSet: (Int) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    AwanCard(onClick = { picking = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
            AwanText(label, style = AwanTheme.styles.metaText)
            AwanText(formatClock(minutes), style = AwanTheme.styles.buttonCompactText)
        }
    }
    if (picking) {
        TimePickerDialog(
            initialMinutes = minutes,
            onDismiss = { picking = false },
            onConfirm = {
                picking = false
                onSet(it)
            },
        )
    }
}

@Composable
private fun DragHandle(color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Box(Modifier.size(3.dp).clip(CircleShape).background(color))
                }
            }
        }
    }
}
