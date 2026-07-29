package com.awan.feature.profile.impl.ui.routinedetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.presentation.RoutineDetailsAction
import com.awan.feature.profile.impl.presentation.RoutineDetailsState
import com.awan.feature.profile.impl.ui.components.ZoneDetailItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailsScreen(
    uiState: RoutineDetailsState,
    onAction: (RoutineDetailsAction) -> Unit,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AwanDialog(
            title = stringResource(R.string.profile_routine_delete_this_confirm_title),
            body = stringResource(R.string.profile_routine_delete_this_confirm_message),
            primaryLabel = stringResource(R.string.profile_routine_delete),
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                showDeleteDialog = false
                uiState.template?.id?.let { onAction(RoutineDetailsAction.DeleteRoutine(it)) }
            },
            secondaryLabel = stringResource(R.string.profile_cancel),
            onSecondary = { showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { AwanText(text = uiState.template?.name ?: stringResource(R.string.profile_routine_select), style = AwanTheme.styles.titleText) },
                navigationIcon = {
                    AwanIconButton(onClick = onBackClick, contentDescription = stringResource(R.string.profile_back)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AwanTheme.colors.textPrimary)
                    }
                },
                actions = {
                    if (uiState.template?.name?.equals("Default", ignoreCase = true) == false) {
                        AwanIconButton(onClick = { showDeleteDialog = true }, contentDescription = stringResource(R.string.profile_routine_delete)) {
                            Icon(Icons.Default.Delete, null, tint = AwanTheme.colors.destructive)
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
            Box(modifier = Modifier.padding(20.dp)) {
                AwanButton(
                    onClick = onEditClick,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Edit
                ) {
                    AwanText(text = stringResource(R.string.profile_routine_edit))
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AwanTheme.colors.sky)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AwanText(
                        text = stringResource(R.string.profile_routine_detail_applies_to),
                        style = AwanTheme.styles.bodyText.copy(
                            textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                        )
                    )
                    AwanText(
                        text = uiState.template?.daysOfWeek?.joinToString(", ") { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } } ?: "",
                        style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary)
                    )
                    AwanText(
                        text = stringResource(R.string.profile_routine_detail_change_hint),
                        style = AwanTheme.styles.metaText
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AwanText(
                        text = stringResource(R.string.profile_routine_zones),
                        style = AwanTheme.styles.bodyText.copy(
                            textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                        )
                    )

                    if (uiState.template?.zones?.isEmpty() == true) {
                        AwanText(
                            text = stringResource(R.string.profile_routine_no_zones),
                            style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary)
                        )
                    } else {
                        uiState.template?.zones?.forEach { zone ->
                            ZoneDetailItem(zone)
                        }
                    }
                }
            }
        }
    }
}
