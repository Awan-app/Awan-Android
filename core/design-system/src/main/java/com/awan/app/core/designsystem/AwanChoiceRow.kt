package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val DisabledAlpha = 0.45f
private const val SelectedTint = 0.14f
private val RowPadding = 16.dp
private val TitleGap = 10.dp
private val ChipGap = 8.dp
private val ChipMinWidth = 56.dp

/**
 * A settings row that names a choice and puts the options underneath it.
 *
 * The options go on their own line rather than in a trailing slot: a slot beside the title fits two
 * short values and crushes the label at four, and it crushes it sooner in Arabic, where the same
 * words are wider.
 *
 * The options are flat outlined keys that carry selection in colour alone — no rim, no press sink,
 * and no track behind them. With four or more of them a raised-and-sunk treatment turns a simple
 * pick into a row of competing depths, and the chosen one reads as misaligned rather than chosen.
 *
 * ```
 * AwanChoiceRow(
 *     icon = Icons.Default.Alarm,
 *     title = stringResource(R.string.remind_me),
 *     options = listOf(5, 10, 15, 30),
 *     selected = leadMinutes,
 *     onSelect = { onAction(SetReminderLead(it)) },
 *     label = { stringResource(R.string.minutes, it) },
 * )
 * ```
 */
@Composable
fun <T> AwanChoiceRow(
    icon: ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDivider: Boolean = false,
    iconColor: Color = AwanTheme.colors.sky,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else DisabledAlpha)
                .padding(horizontal = RowPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(TitleGap),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RowPadding),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                AwanText(
                    text = title,
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChipGap),
            ) {
                options.forEach { option ->
                    ChoiceKey(
                        label = label(option),
                        isSelected = option == selected,
                        enabled = enabled,
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = RowPadding),
                color = AwanTheme.colors.line.copy(alpha = 0.5f),
                thickness = 1.dp,
            )
        }
    }
}

@Composable
private fun ChoiceKey(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val spec = AwanTheme.motion.settle.spec<Color>()

    // Blended rather than translucent, so the fill sits on the card without picking up whatever is
    // behind it.
    val fill by animateColorAsState(
        targetValue = if (isSelected) lerp(colors.surface, colors.sky, SelectedTint) else colors.surface,
        animationSpec = spec,
        label = "choiceFill",
    )
    val edge by animateColorAsState(
        targetValue = if (isSelected) colors.sky else colors.line,
        animationSpec = spec,
        label = "choiceEdge",
    )
    val ink by animateColorAsState(
        targetValue = if (isSelected) colors.sky else colors.textSecondary,
        animationSpec = spec,
        label = "choiceInk",
    )

    val shape = AwanTheme.shapes.chip
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .widthIn(min = ChipMinWidth)
            .clip(shape)
            .background(fill)
            .border(2.dp, edge, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
            // One of a set, which a plain clickable does not say on its own.
            .semantics {
                role = Role.RadioButton
                selected = isSelected
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        AwanText(
            text = label,
            style = AwanTheme.styles.buttonCompactText.copy(
                color = ink,
                textStyle = AwanTheme.typography.buttonCompact.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                ),
            ),
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "AwanChoiceRow · Light", showBackground = true)
@Composable
private fun LightChoiceRowPreview() {
    ChoiceRowPreview(dark = false)
}

@Preview(name = "AwanChoiceRow · Dark", showBackground = true)
@Composable
private fun DarkChoiceRowPreview() {
    ChoiceRowPreview(dark = true)
}

@Composable
private fun ChoiceRowPreview(dark: Boolean) {
    AwanTheme(dark = dark) {
        AwanCard(contentPadding = PaddingValues(0.dp)) {
            Column {
                AwanChoiceRow(
                    icon = Icons.Default.Alarm,
                    title = "Remind me (min)",
                    options = listOf(5, 10, 15, 30),
                    selected = 10,
                    onSelect = {},
                    label = { "$it" },
                    showDivider = true,
                )
                AwanChoiceRow(
                    icon = Icons.Default.Snooze,
                    title = "Snooze by (min)",
                    options = listOf(5, 10, 15),
                    selected = 5,
                    onSelect = {},
                    label = { "$it" },
                    enabled = false,
                )
            }
        }
    }
}
