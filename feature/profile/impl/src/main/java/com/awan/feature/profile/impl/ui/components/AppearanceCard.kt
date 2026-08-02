package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.presentation.ProfileState
import com.awan.feature.profile.impl.R as ProfileR

@Composable
fun AppearanceCard(
    uiState: ProfileState,
    onThemeClick: (Boolean) -> Unit,
    onLanguageClick: (String) -> Unit,
) {
    var isLanguageExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(ProfileR.string.profile_section_appearance))
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            ExpandableLanguageItem(
                currentLanguage = uiState.language,
                isExpanded = isLanguageExpanded,
                onExpandClick = { isLanguageExpanded = !isLanguageExpanded },
                onLanguageSelected = onLanguageClick,
                showDivider = true
            )

            PreferenceRow(
                icon = Icons.Default.Contrast,
                title = stringResource(ProfileR.string.profile_theme),
                onClick = null,
                iconColor = AwanTheme.colors.sky
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    ThemeOption(
                        label = stringResource(ProfileR.string.profile_theme_light),
                        isSelected = !uiState.useDarkTheme,
                        onClick = { onThemeClick(false) }
                    )
                    ThemeOption(
                        label = stringResource(ProfileR.string.profile_theme_dark),
                        isSelected = uiState.useDarkTheme,
                        onClick = { onThemeClick(true) }
                    )
                }
            }
        }
    }
}
