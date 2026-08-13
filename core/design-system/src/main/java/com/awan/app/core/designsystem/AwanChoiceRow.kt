package com.awan.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val DisabledAlpha = 0.45f
private val RowPadding = 16.dp
private val TitleGap = 6.dp

/**
 * A settings row that names a choice and puts the options underneath it.
 *
 * The options go on their own line rather than in a trailing slot: a slot beside the title fits two
 * short values and crushes the label at four, and it crushes it sooner in Arabic, where the same
 * words are wider.
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
                .padding(horizontal = RowPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(TitleGap),
        ) {
            Row(
                // Only the header dims: AwanSegmentedControl draws its own disabled state, and
                // fading it again on top of that reads as muddy rather than as off.
                modifier = Modifier.alpha(if (enabled) 1f else DisabledAlpha),
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

            AwanSegmentedControl(
                options = options,
                selected = selected,
                onSelect = onSelect,
                label = label,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
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
