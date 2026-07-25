package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import java.util.TimeZone

@Composable
fun TimezoneSelectionDialog(
    currentSelection: String,
    onTimezoneSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val timezones = remember { 
        TimeZone.getAvailableIDs()
            .filter { it.contains("/") }
            .sortedBy { it }
    }
    var searchQuery by remember { mutableStateOf("") }
    val filteredTimezones = remember(searchQuery) {
        if (searchQuery.isBlank()) timezones
        else timezones.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanText(text = "Select Timezone", style = AwanTheme.styles.titleText)
                AwanTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search region or city...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            Box(modifier = Modifier.height(300.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredTimezones.size) { index ->
                        val tz = filteredTimezones[index]
                        val isSelected = tz == currentSelection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AwanTheme.colors.sky.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { 
                                    onTimezoneSelected(tz)
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AwanText(
                                text = tz.replace("_", " "),
                                style = if (isSelected) AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.skyPressed) else AwanTheme.styles.bodyText
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                AwanText(text = "Close", style = AwanTheme.styles.buttonCompactText)
            }
        },
        containerColor = AwanTheme.colors.surface,
        shape = AwanTheme.shapes.card
    )
}
