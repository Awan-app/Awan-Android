package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.Category
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.presentation.CategoryManagementAction
import com.awan.feature.profile.impl.presentation.CategoryManagementState
import com.awan.feature.profile.impl.ui.components.CategoryAddEditSheet
import com.awan.feature.profile.impl.ui.components.CategoryChip
import com.awan.feature.profile.impl.ui.components.EmptyCategoriesState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryManagementScreen(
    uiState: CategoryManagementState,
    onAction: (CategoryManagementAction) -> Unit,
    onBackClick: () -> Unit
) {
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    if (isCreating || editingCategory != null) {
        CategoryAddEditSheet(
            category = editingCategory,
            onDismiss = {
                isCreating = false
                editingCategory = null
            },
            onConfirm = { name ->
                if (isCreating) {
                    onAction(CategoryManagementAction.CreateCategory(name))
                } else {
                    onAction(CategoryManagementAction.UpdateCategory(editingCategory!!.id, name))
                }
                isCreating = false
                editingCategory = null
            },
            isSaving = uiState.isSaving
        )
    }

    if (categoryToDelete != null) {
        AwanDialog(
            title = stringResource(R.string.profile_category_delete_confirm_title),
            body = stringResource(R.string.profile_category_delete_confirm_message),
            primaryLabel = stringResource(R.string.profile_routine_delete),
            primaryVariant = AwanButtonVariant.Destructive,
            onPrimary = {
                onAction(CategoryManagementAction.DeleteCategory(categoryToDelete!!.id))
                categoryToDelete = null
            },
            secondaryLabel = stringResource(R.string.profile_cancel),
            onSecondary = { categoryToDelete = null },
            onDismiss = { categoryToDelete = null }
        )
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onAction(CategoryManagementAction.ClearError) // Using this as a signal or just clear state
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AwanText(text = stringResource(R.string.profile_categories_title), style = AwanTheme.styles.titleText)
                        AwanText(text = stringResource(R.string.profile_routine_summary_subtitle), style = AwanTheme.styles.metaText)
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        AwanIconButton(
                            onClick = onBackClick,
                            contentDescription = stringResource(R.string.profile_back)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AwanTheme.colors.textPrimary)
                        }
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        AwanIconButton(
                            onClick = { isCreating = true },
                            contentDescription = stringResource(R.string.profile_category_add)
                        ) {
                            Icon(Icons.Default.Add, null, tint = AwanTheme.colors.sky)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AwanTheme.colors.background)
            )
        },
        containerColor = AwanTheme.colors.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading && uiState.categories.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AwanTheme.colors.sky)
                }
                uiState.categories.isEmpty() -> {
                    EmptyCategoriesState(onAddClick = { isCreating = true })
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.categories.forEach { category ->
                                CategoryChip(
                                    category = category,
                                    onEditClick = { editingCategory = category },
                                    onDeleteClick = { categoryToDelete = category }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.error != null) {
                AwanErrorSnackbar(
                    message = uiState.error.asString(),
                    onDismiss = { onAction(CategoryManagementAction.ClearError) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
                )
            }
        }
    }
}
