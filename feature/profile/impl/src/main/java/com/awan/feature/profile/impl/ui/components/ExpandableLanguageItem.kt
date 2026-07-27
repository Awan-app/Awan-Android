package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R as ProfileR

@Composable
fun ExpandableLanguageItem(
    currentLanguage: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    showDivider: Boolean = false
) {
    val languages = listOf(
        "en" to stringResource(ProfileR.string.profile_language_english),
        "ar" to stringResource(ProfileR.string.profile_language_arabic)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        PreferenceRow(
            icon = Icons.Default.Language,
            title = stringResource(ProfileR.string.profile_language),
            value = languages.find { it.first == currentLanguage }?.second ?: stringResource(ProfileR.string.profile_language_english),
            onClick = onExpandClick,
            showDivider = showDivider && !isExpanded,
            isExpanded = isExpanded,
            iconColor = AwanTheme.colors.zoneSun
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { (code, name) ->
                    val isSelected = code == currentLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AwanTheme.colors.sky.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { 
                                onLanguageSelected(code)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AwanText(
                            text = name,
                            style = if (isSelected) AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.skyPressed) else AwanTheme.styles.bodyText
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        
        if (showDivider && isExpanded) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = AwanTheme.colors.line.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        }
    }
}
