package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R as ProfileR
import java.util.TimeZone

@Composable
fun ExpandableTimezoneItem(
    currentSelection: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onTimezoneSelected: (String) -> Unit,
    isLoading: Boolean = false,
    showDivider: Boolean = false
) {
    val timezones = remember {
        TimeZone.getAvailableIDs()
            .filter { it.contains("/") }
            .sortedBy { it }
    }
    var searchQuery by remember(isExpanded) { mutableStateOf("") }
    val filteredTimezones = remember(searchQuery) {
        if (searchQuery.isBlank()) timezones
        else timezones.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PreferenceRow(
            icon = Icons.Default.Public,
            title = stringResource(ProfileR.string.profile_time_zone),
            value = currentSelection.replace("_", " "),
            onClick = onExpandClick,
            showDivider = showDivider && !isExpanded,
            isExpanded = isExpanded,
            iconColor = AwanTheme.colors.sky
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AwanTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = stringResource(ProfileR.string.profile_search_timezone),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.height(240.dp)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = filteredTimezones,
                            key = { it }
                        ) { tz ->
                            val isSelected = tz == currentSelection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AwanTheme.colors.sky.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { onTimezoneSelected(tz) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AwanText(
                                    text = tz.replace("_", " "),
                                    style = if (isSelected) AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.skyPressed) else AwanTheme.styles.bodyText
                                )
                                if (isSelected) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Check, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
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
