package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R

@Composable
fun ZoneColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = AwanTheme.colors
    val colorOptions = remember(themeColors) {
        listOf(
            themeColors.zoneMelon to R.string.profile_color_melon,
            themeColors.zoneBlue to R.string.profile_color_blue,
            themeColors.zonePurple to R.string.profile_color_purple,
            themeColors.zonePink to R.string.profile_color_pink,
            themeColors.zoneGreen to R.string.profile_color_green,
            themeColors.zoneYellow to R.string.profile_color_yellow,
            themeColors.zoneOrange to R.string.profile_color_orange,
            themeColors.zoneRed to R.string.profile_color_red,
            themeColors.zoneCyan to R.string.profile_color_cyan,
            themeColors.zoneGray to R.string.profile_color_gray
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AwanText(
            text = stringResource(R.string.profile_zone_label_color),
            style = AwanTheme.styles.bodyText.copy(
                color = AwanTheme.colors.textSecondary,
                textStyle = AwanTheme.styles.bodyText.textStyle.copy(
                    fontSize = 13.sp
                )
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colorOptions.forEach { (color, nameRes) ->
                val hex = color.toHexString()
                val isSelected = selectedColor.equals(hex, ignoreCase = true)
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val colorName = stringResource(nameRes)

                val rimDepth = 3.dp
                val surface = AwanTheme.colors.surface

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .selectable(
                            selected = isSelected,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.RadioButton,
                            onClick = { onColorSelected(hex) }
                        )
                        .semantics {
                            contentDescription = colorName
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(36.dp, 40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = if (isPressed) rimDepth else 0.dp)
                                .background(AwanTheme.colors.line, CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = if (isPressed) 0.dp else rimDepth)
                                .background(
                                    if (isSelected) {
                                        color.copy(alpha = 0.2f).compositeOver(surface)
                                    } else {
                                        surface
                                    },
                                    CircleShape
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) {
                                        color
                                    } else {
                                        AwanTheme.colors.line
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}
