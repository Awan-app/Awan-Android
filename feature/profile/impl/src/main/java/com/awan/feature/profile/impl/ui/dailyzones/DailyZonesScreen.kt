package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.presentation.dailyzones.DailyZonesUiState
import com.awan.app.core.model.DailyZone
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyZonesScreen(
    uiState: DailyZonesUiState,
    onDateSelected: (Date) -> Unit,
    onAddZoneClick: () -> Unit,
    onEditZoneClick: (DailyZone) -> Unit,
    onCopyToClick: () -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AwanText(text = "Daily zones", style = AwanTheme.styles.titleText)
                        AwanText(
                            text = "Shape ${SimpleDateFormat("EEEE", Locale.getDefault()).format(uiState.selectedDate)} your way.",
                            style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.meta)
                        )
                    }
                },
                navigationIcon = {
                    AwanIconButton(onClick = onBackClick, contentDescription = "Back") {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = AwanTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.awan.app.core.designsystem.R.drawable.awan_mascot_greet),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AwanTheme.colors.background
                )
            )
        },
        containerColor = AwanTheme.colors.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(AwanTheme.colors.background)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.awan.app.core.designsystem.R.drawable.awan_mascot_idle),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                    AwanText(
                        text = "${SimpleDateFormat("EEEE", Locale.getDefault()).format(uiState.selectedDate)} looks balanced",
                        style = AwanTheme.styles.captionText
                    )
                }
                AwanButton(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Check
                ) {
                    AwanText(text = "Save ${SimpleDateFormat("EEEE", Locale.getDefault()).format(uiState.selectedDate)}")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DaySelectorCompact(
                selectedDate = uiState.selectedDate,
                onDateSelected = onDateSelected
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Surface(
                    onClick = onCopyToClick,
                    shape = RoundedCornerShape(8.dp),
                    color = AwanTheme.colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AwanTheme.colors.line)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = AwanTheme.colors.sky)
                        AwanText(text = "Copy to...", style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.sky))
                    }
                }
            }

            DayInfoCard(
                date = uiState.selectedDate,
                zoneCount = uiState.effectiveZones.size
            )

            TimelineDetailed(
                zones = uiState.effectiveZones,
                onEditZoneClick = onEditZoneClick,
                onAddZoneClick = onAddZoneClick
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DaySelectorCompact(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit
) {
    val today = Calendar.getInstance()
    val startOfWeek = (today.clone() as Calendar).apply {
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    val days = (0..6).map {
        (startOfWeek.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, it) }.time
    }

    val dayInitialFormat = SimpleDateFormat("EEEEE", Locale.getDefault()) // Single letter

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { date ->
            val isSelected = isSameDay(date, selectedDate)
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AwanTheme.colors.sky else Color.Transparent)
                    .clickable { onDateSelected(date) },
                contentAlignment = Alignment.Center
            ) {
                AwanText(
                    text = dayInitialFormat.format(date).first().toString(),
                    style = AwanTheme.styles.bodyText.copy(
                        color = if (isSelected) AwanTheme.colors.onSky else AwanTheme.colors.textSecondary,
                        textStyle = AwanTheme.typography.body.copy(fontWeight = FontWeight.Medium)
                    )
                )
            }
        }
    }
}

@Composable
private fun DayInfoCard(date: Date, zoneCount: Int) {
    AwanCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.CalendarToday, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AwanText(
                    text = SimpleDateFormat("EEEE", Locale.getDefault()).format(date),
                    style = AwanTheme.styles.bodyText.copy(textStyle = AwanTheme.typography.body.copy(fontWeight = FontWeight.Bold))
                )
                AwanText(text = "•", style = AwanTheme.styles.captionText)
                AwanText(text = "$zoneCount zones", style = AwanTheme.styles.captionText)
            }
        }
    }
}

@Composable
private fun TimelineDetailed(
    zones: List<DailyZone>,
    onEditZoneClick: (DailyZone) -> Unit,
    onAddZoneClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        zones.forEachIndexed { index, zone ->
            TimelineZoneItem(
                zone = zone,
                showBottomLine = index < zones.size, // Always show line if there's a next item or "Add zone"
                onEditClick = { onEditZoneClick(zone) }
            )
        }
        
        // Add zone item at the end of timeline
        AddZoneTimelineItem(onAddZoneClick = onAddZoneClick)
    }
}

@Composable
private fun TimelineZoneItem(
    zone: DailyZone,
    showBottomLine: Boolean,
    onEditClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Timeline Column
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
            AwanText(
                text = zone.startTime.replace(":00", ""),
                style = AwanTheme.styles.captionText.copy(
                    textStyle = AwanTheme.typography.caption.copy(fontSize = 12.sp)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.sky)
            )
            if (showBottomLine) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp)
                        .background(AwanTheme.colors.line)
                )
            }
        }

        // Zone Card
        val zoneColor = remember(zone.color) {
            try { Color(android.graphics.Color.parseColor(zone.color)) } 
            catch (e: Exception) { Color(0xFF2EAAFF) }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(zoneColor.copy(alpha = 0.1f))
                .border(1.dp, zoneColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.DragIndicator, null, tint = zoneColor, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = zone.name,
                    style = AwanTheme.styles.bodyText.copy(textStyle = AwanTheme.typography.body.copy(fontWeight = FontWeight.Bold))
                )
                AwanText(
                    text = "${zone.startTime} - ${zone.endTime}",
                    style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
                )
            }
            AwanIconButton(onClick = onEditClick, contentDescription = "Edit") {
                Icon(Icons.Default.Edit, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AddZoneTimelineItem(onAddZoneClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Last time marker for image look (e.g. 11 PM)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
            AwanText(
                text = "11 PM",
                style = AwanTheme.styles.captionText.copy(
                    textStyle = AwanTheme.typography.caption.copy(fontSize = 12.sp)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.sky)
            )
        }

        // Dashed Add Button
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
                .height(56.dp)
                .drawDashedBorder(color = AwanTheme.colors.sky, shape = RoundedCornerShape(12.dp))
                .clickable { onAddZoneClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Add, null, tint = AwanTheme.colors.sky)
                AwanText(text = "Add zone", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky))
            }
        }
    }
}

// Helper to draw dashed border as seen in the image
private fun Modifier.drawDashedBorder(color: Color, shape: RoundedCornerShape): Modifier = this.then(
    Modifier.border(
        width = 1.dp,
        color = color,
        shape = shape
    )
    // Note: Standard Compose border doesn't support dash easily without Canvas.
    // For simplicity in this mock, we use a normal border. In production, use Canvas.
)

private fun isSameDay(d1: Date, d2: Date): Boolean {
    val c1 = Calendar.getInstance().apply { time = d1 }
    val c2 = Calendar.getInstance().apply { time = d2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
