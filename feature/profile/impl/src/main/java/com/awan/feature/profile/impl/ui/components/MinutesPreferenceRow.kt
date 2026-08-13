package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanSegmentedControl
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme

private const val DISABLED_ALPHA = 0.45f

/**
 * A settings row whose choices sit on their own line.
 *
 * [PreferenceRow] puts its trailing slot beside the title, which works for two short options but
 * crushes the label once there are four — and Arabic's "دقيقة" is wider than "min", so it wraps
 * there first.
 */
@Composable
fun MinutesPreferenceRow(
    icon: ImageVector,
    title: String,
    selected: Int,
    choices: List<Int>,
    enabled: Boolean,
    label: @Composable (Int) -> String,
    onSelect: (Int) -> Unit,
    showDivider: Boolean = false,
    iconColor: Color = AwanTheme.colors.sky,
) {
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                // Only the header: AwanSegmentedControl draws its own disabled state, and dimming
                // it again on top of that reads as muddy rather than as off.
                modifier = Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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

            // One of a set, exactly what a segmented control is for: the chosen minute stays held
            // down on its rim rather than being marked by a colour change.
            AwanSegmentedControl(
                options = choices,
                selected = selected,
                onSelect = onSelect,
                label = label,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = AwanTheme.colors.line.copy(alpha = 0.5f),
                thickness = 1.dp,
            )
        }
    }
}
