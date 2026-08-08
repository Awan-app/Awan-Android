package com.awan.feature.profile.impl.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.Category
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.presentation.CategoryManagementAction
import com.awan.feature.profile.impl.presentation.CategoryManagementState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    uiState: CategoryManagementState,
    onAction: (CategoryManagementAction) -> Unit,
    onBackClick: () -> Unit
) {
    var editingCategory by remember { mutableStateOf<Category?>(null) }
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.categories, key = { it.id }) { category ->
                            CategoryItem(
                                category = category,
                                onEditClick = { editingCategory = category }
                            )
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

@Composable
private fun EmptyCategoriesState(onAddClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "categories_empty")
    val animY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = com.awan.app.core.designsystem.R.drawable.awan_mascot_idle),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { translationY = animY },
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.height(24.dp))
        AwanText(
            text = stringResource(R.string.profile_zone_category_empty),
            style = AwanTheme.styles.titleText.copy(
                textStyle = AwanTheme.styles.titleText.textStyle.copy(textAlign = TextAlign.Center)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        AwanText(
            text = stringResource(R.string.profile_daily_zones_no_zones_hint),
            style = AwanTheme.styles.bodyText.copy(
                color = AwanTheme.colors.textSecondary,
                textStyle = AwanTheme.styles.bodyText.textStyle.copy(textAlign = TextAlign.Center)
            )
        )
        Spacer(modifier = Modifier.height(32.dp))
        AwanButton(onClick = onAddClick) {
            AwanText(text = stringResource(R.string.profile_category_add))
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onEditClick: () -> Unit
) {
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = onEditClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = AwanTheme.colors.zoneLavender.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    tint = AwanTheme.colors.zoneLavender,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = category.name,
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.profile_category_edit),
                tint = AwanTheme.colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryAddEditSheet(
    category: Category?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isSaving: Boolean
) {
    var name by remember { mutableStateOf(category?.name ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AwanTheme.colors.line) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = if (category == null) stringResource(R.string.profile_category_add) else stringResource(R.string.profile_category_edit),
                    style = AwanTheme.styles.titleText
                )
                AwanIconButton(
                    onClick = onDismiss,
                    contentDescription = stringResource(R.string.profile_close)
                ) {
                    Icon(Icons.Default.Close, null, tint = AwanTheme.colors.textSecondary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AwanText(
                    text = stringResource(R.string.profile_routine_name),
                    style = AwanTheme.styles.captionText
                )
                AwanTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.profile_category_name_placeholder),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AwanButton(
                onClick = { onConfirm(name) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && !isSaving,
                isLoading = isSaving
            ) {
                AwanText(text = stringResource(R.string.profile_save))
            }
        }
    }
}
