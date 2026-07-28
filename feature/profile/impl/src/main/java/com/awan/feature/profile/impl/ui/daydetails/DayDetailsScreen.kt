package com.awan.feature.profile.impl.ui.daydetails

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import com.awan.feature.profile.impl.presentation.DayDetailsAction
import com.awan.feature.profile.impl.presentation.DayDetailsState
import com.awan.feature.profile.impl.ui.components.ZoneEditSheet
import com.awan.feature.profile.impl.ui.components.ZoneTimelineItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailsScreen(
    uiState: DayDetailsState,
    onAction: (DayDetailsAction) -> Unit,
    onBackClick: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editableZones by remember { mutableStateOf(uiState.effectiveZones) }
    var showZoneSheet by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<DailyZone?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.effectiveZones) {
        if (!isEditing) {
            editableZones = uiState.effectiveZones
        }
    }

    if (showResetDialog && uiState.overrideId != null) {
        AwanDialog(
            title = "Reset this day?",
            body = "Your custom schedule will be removed and this day will use its usual weekly routine again.",
            primaryLabel = "Reset",
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                showResetDialog = false
                onAction(DayDetailsAction.ResetToWeeklyRoutine(uiState.overrideId))
            },
            secondaryLabel = "Cancel",
            onSecondary = { showResetDialog = false },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showZoneSheet) {
        ZoneEditSheet(
            zone = editingZone ?: DailyZone(id = null, name = "", startTime = "09:00", endTime = "10:00", color = "#2EAAFF"),
            isNew = editingZone == null,
            onDismiss = {
                showZoneSheet = false
                editingZone = null
            },
            onConfirm = { zone ->
                editableZones = if (editingZone == null) {
                    (editableZones + zone).sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }
                } else {
                    editableZones.map { if (it == editingZone) zone else it }
                        .sortedBy { DailyZonesHelper.parseTimeToMinutes(it.startTime) }
                }
                showZoneSheet = false
                editingZone = null
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val date = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(uiState.date) } catch (e: Exception) { null }
                    val dateText = remember(date) {
                        date?.let { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(it) } ?: ""
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AwanText(text = dateText, style = AwanTheme.styles.titleText)
                        AwanText(
                            text = if (uiState.isOverride) "Custom Schedule" else "Weekly Routine",
                            style = AwanTheme.styles.metaText.copy(color = if (uiState.isOverride) AwanTheme.colors.sky else AwanTheme.colors.meta)
                        )
                    }
                },
                navigationIcon = {
                    AwanIconButton(onClick = onBackClick, contentDescription = "Back") {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AwanTheme.colors.textPrimary)
                    }
                },
                actions = {
                    if (uiState.isOverride && !isEditing) {
                        AwanIconButton(onClick = { showResetDialog = true }, contentDescription = "Reset") {
                            Icon(Icons.Default.Refresh, null, tint = AwanTheme.colors.sky)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AwanTheme.colors.background),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = AwanTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AwanTheme.colors.background)
                    .padding(20.dp)
            ) {
                if (isEditing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AwanButton(
                            onClick = { isEditing = false; editableZones = uiState.effectiveZones },
                            modifier = Modifier.weight(1f),
                            variant = AwanButtonVariant.Secondary
                        ) {
                            AwanText(text = "Cancel")
                        }
                        AwanButton(
                            onClick = {
                                if (uiState.isOverride && uiState.overrideId != null) {
                                    onAction(DayDetailsAction.UpdateOverride(uiState.overrideId, editableZones))
                                } else {
                                    onAction(DayDetailsAction.CustomizeDay(editableZones))
                                }
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f),
                            isLoading = uiState.isSaving,
                            icon = Icons.Default.Check
                        ) {
                            AwanText(text = "Save")
                        }
                    }
                } else {
                    AwanButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth(),
                        icon = if (uiState.isOverride) Icons.Default.Edit else Icons.Default.Add
                    ) {
                        AwanText(text = if (uiState.isOverride) "Edit custom schedule" else "Customize this day")
                    }
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AnimatedVisibility(
                visible = isEditing,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AwanTheme.colors.sky.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(18.dp))
                        AwanText(
                            text = "Changes made here will only apply to this specific day.",
                            style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.sky)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = "Timeline",
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                    )
                )
                
                if (isEditing) {
                    TextButton(onClick = {
                        editingZone = null
                        showZoneSheet = true
                    }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        AwanText(text = "Add Zone", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky))
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                if (editableZones.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        AwanText(text = "No zones scheduled for this day", style = AwanTheme.styles.bodySecondaryText)
                    }
                } else {
                    editableZones.forEachIndexed { index, zone ->
                        ZoneTimelineItem(
                            zone = zone,
                            isLast = index == editableZones.size - 1,
                            onEdit = if (isEditing) {
                                {
                                    editingZone = zone
                                    showZoneSheet = true
                                }
                            } else null,
                            onDelete = if (isEditing) {
                                {
                                    editableZones = editableZones - zone
                                }
                            } else null
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// Fixed ZoneTimelineItem call in DayDetailsScreen to handle optional callbacks correctly if needed
// Actually, ZoneTimelineItem requires non-null callbacks, so I should provide dummy ones or adjust ZoneTimelineItem.
// I'll adjust ZoneTimelineItem to accept nullable callbacks for more flexibility.
