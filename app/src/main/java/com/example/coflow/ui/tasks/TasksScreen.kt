package com.example.coflow.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.R
import com.example.coflow.models.TaskStatus
import com.example.coflow.ui.components.CoFlowBottomNavigationBar
import com.example.coflow.ui.components.CoFlowFabButton
import com.example.coflow.ui.components.CoFlowGlassCard
import com.example.coflow.ui.components.CoFlowIcon
import com.example.coflow.ui.components.CoFlowScreenHeader
import com.example.coflow.ui.components.NavItem
import com.example.coflow.ui.components.NotificationBell
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowGlassStyles
import com.example.coflow.ui.theme.CoFlowSpacing
import com.example.coflow.ui.theme.CoFlowTheme
import com.example.coflow.ui.theme.coFlowTasksBackgroundBrush
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
    state: TasksUiState,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onGroupingChange: (TaskGrouping) -> Unit,
    onFilterToggle: (TaskFilterItem) -> Unit,
    onToggleGroup: (String) -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onClearError: () -> Unit,
    onTaskSwipe: (TaskRowUi) -> Unit,
) {
    var pendingTask by remember { mutableStateOf<TaskRowUi?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var snackbarColor by remember {
        mutableStateOf(Color(0xFF2860D9))
    }

    val scrollState = rememberScrollState()

    // Собираем текст кнопки фильтра из ресурсов
    val filterButtonText =
        if (state.selectedFilters.isEmpty()) {
            stringResource(R.string.filter_none)
        } else {
            state.selectedFilters
                .mapNotNull { id ->
                    val item = state.availableFilters.find { it.id == id }
                    when (item?.type) {
                        FilterType.MINE -> stringResource(R.string.filter_mine)
                        FilterType.PROJECT -> item.title
                        else -> null
                    }
                }.joinToString(", ")
        }

    // Показ ошибки (строковая или ресурсная)
    val errorText =
        when {
            state.errorMessageResId != null && state.errorMessage != null -> {
                stringResource(state.errorMessageResId, state.errorMessage)
            }

            state.errorMessageResId != null -> {
                stringResource(state.errorMessageResId)
            }

            else -> {
                state.errorMessage
            }
        }

    if (errorText != null) {
        AlertDialog(
            onDismissRequest = onClearError,
            title = { Text(stringResource(R.string.error_title), color = CoFlowColors.Error) },
            text = { Text(errorText) },
            confirmButton = {
                TextButton(onClick = onClearError) {
                    Text(stringResource(R.string.ok_button), color = CoFlowColors.Blue)
                }
            },
        )
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(coFlowTasksBackgroundBrush()),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
        ) {
            CoFlowScreenHeader(
                title = stringResource(R.string.my_tasks_header),
                subtitle = "${state.activeCount} ${
                    stringResource(
                        R.string.my_tasks_active,
                    )
                } · ${state.completedCount} ${stringResource(R.string.my_tasks_completed)}",
                trailingContent = {
                    NotificationBell()
                },
            )

            Spacer(Modifier.height(16.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = CoFlowColors.Blue)
                }
            } else {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(bottom = 8.dp)
                            .clipToBounds()
                            .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.groups.forEach { group ->
                        TaskGroupCard(
                            group = group,
                            onToggle = { onToggleGroup(group.id) },
                            onTaskClick = { task -> onNavigateToTaskDetail(task.id) },
                            onTaskSwipe = { task ->
                                pendingTask = task
                            },
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            AnimatedVisibility(visible = state.isBottomBarVisible) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom =
                                    WindowInsets.navigationBars
                                        .asPaddingValues()
                                        .calculateBottomPadding() + 2.dp,
                            ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .widthIn(max = 330.dp)
                                .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SortFilterPanel(
                            modifier = Modifier.weight(1f),
                            grouping = state.currentGrouping,
                            onGroupingChange = onGroupingChange,
                            filterButtonText = filterButtonText,
                            onFilterToggle = onFilterToggle,
                            selectedFilters = state.selectedFilters,
                            availableFilters = state.availableFilters,
                        )
                        Spacer(Modifier.width(16.dp))

                        CoFlowFabButton(
                            onClick = onNavigateToCreateTask,
                            iconName = "task_plus",
                        )
                    }
                    Spacer(Modifier.height(7.dp))

                    CoFlowBottomNavigationBar(
                        items =
                            listOf(
                                NavItem(
                                    icon = "tasks",
                                    title = stringResource(R.string.navigation_tasks),
                                    selected = true,
                                    onClick = {},
                                ),
                                NavItem(
                                    icon = "folder",
                                    title = stringResource(R.string.navigation_projects),
                                    selected = false,
                                    onClick = onNavigateToProjects,
                                ),
                                NavItem(
                                    icon = "person",
                                    title = stringResource(R.string.navigation_profile),
                                    selected = false,
                                    onClick = {},
                                ),
                            ),
                    )
                }
            }
        }
        pendingTask?.let { task ->
            val isTakingTask = task.status == TaskStatus.TODO

            AlertDialog(
                onDismissRequest = { pendingTask = null },
                title = {
                    Text(
                        if (isTakingTask) {
                            "Взять задачу?"
                        } else {
                            "Завершить задачу?"
                        },
                    )
                },
                text = {
                    Text(
                        if (isTakingTask) {
                            "Вы действительно хотите взять задачу «${task.title}» в работу?"
                        } else {
                            "Вы действительно хотите отметить задачу «${task.title}» выполненной?"
                        },
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingTask = null
                            onTaskSwipe(task)

                            snackbarColor =
                                if (isTakingTask) {
                                    Color(0xFF2860D9)
                                } else {
                                    Color(0xFF20A65A)
                                }

                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isTakingTask) {
                                        "Вы успешно взяли задачу"
                                    } else {
                                        "Задача выполнена успешно"
                                    },
                                )
                            }
                        },
                    ) {
                        Text(if (isTakingTask) "Взять" else "Завершить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingTask = null }) {
                        Text("Отмена")
                    }
                },
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp),
            snackbar = { data ->
                Row(
                    modifier =
                        Modifier
                            .widthIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(snackbarColor)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = if (snackbarColor == Color(0xFF20A65A)) "✓" else "●",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = data.visuals.message,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            },
        )
    }
}

@Composable
private fun TaskGroupCard(
    group: TaskGroupUi,
    onToggle: () -> Unit,
    onTaskClick: (TaskRowUi) -> Unit,
    onTaskSwipe: (TaskRowUi) -> Unit,
) {
    CoFlowGlassCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(group.titleResId),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CoFlowColors.TextPrimary,
            )
            Spacer(Modifier.width(7.dp))
            Box(
                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(5.dp),
                        ).background(CoFlowColors.BlueLight.copy(alpha = 0.38f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    text = group.tasks.size.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = CoFlowColors.Blue,
                )
            }
            Spacer(Modifier.weight(1f))
            CoFlowIcon(
                name = if (group.isExpanded) "chevron_up" else "chevron",
                modifier = Modifier.size(18.dp),
                color = CoFlowColors.Blue,
            )
        }

        if (group.isExpanded) {
            if (group.tasks.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_tasks_placeholder),
                    fontSize = 11.sp,
                    color = CoFlowColors.TextSecondary,
                )
            } else {
                Spacer(Modifier.height(3.dp))
                group.tasks.forEachIndexed { index, task ->
                    TaskRow(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onSwipe = { onTaskSwipe(task) },
                    )
                    if (index < group.tasks.lastIndex) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(CoFlowGlassStyles.BlueBorder.copy(alpha = 0.45f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskRowUi,
    onClick: () -> Unit,
    onSwipe: () -> Unit,
) {
    var offsetX by remember(task.id) { mutableFloatStateOf(0f) }

    val swipeEnabled = task.isMine && !task.isDone
    val actionColor =
        when (task.status) {
            TaskStatus.TODO -> CoFlowColors.Blue
            TaskStatus.IN_PROGRESS -> CoFlowColors.Green
            else -> Color.Transparent
        }

    val density = LocalDensity.current
    val maxSwipe = with(density) { 64.dp.toPx() }
    val swipeThreshold = with(density) { 42.dp.toPx() }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .then(
                    if (swipeEnabled) {
                        Modifier.pointerInput(task.id) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()

                                    // Разрешаем только свайп справа налево
                                    if (dragAmount < 0f || offsetX < 0f) {
                                        offsetX =
                                            (offsetX + dragAmount)
                                                .coerceIn(-maxSwipe, 0f)
                                    }
                                },
                                onDragEnd = {
                                    val shouldSwipe = offsetX <= -swipeThreshold
                                    offsetX = 0f

                                    if (shouldSwipe) {
                                        onSwipe()
                                    }
                                },
                                onDragCancel = {
                                    offsetX = 0f
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
    ) {
        // Иконка справа. Подложка карточки не меняет цвет.
        if (swipeEnabled) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier =
                        Modifier
                            .padding(end = 8.dp)
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(actionColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (task.status == TaskStatus.TODO) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Take task",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Complete task",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        }

        // Сама карточка сдвигается, не меняя цвет и оформление.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = offsetX
                    }.clip(RoundedCornerShape(10.dp))
                    .background(CoFlowColors.White)
                    .clickable { onClick() }
                    .padding(vertical = 6.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color =
                        if (task.isDone) {
                            CoFlowColors.TextSecondary
                        } else {
                            CoFlowColors.TextPrimary
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (task.isOverdue) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.task_outdated),
                        fontSize = 8.sp,
                        color = CoFlowColors.Error,
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.projectName,
                    fontSize = 10.sp,
                    color = CoFlowColors.TextSecondary,
                    maxLines = 1,
                )

                Text(
                    text = task.dueDateFormatted,
                    fontSize = 10.sp,
                    color = CoFlowColors.TextSecondary,
                )
            }
        }
    }
}

// --- Внутренние компоненты для панели фильтров ---
@Composable
private fun SortFilterPanel(
    modifier: Modifier = Modifier,
    grouping: TaskGrouping,
    onGroupingChange: (TaskGrouping) -> Unit,
    selectedFilters: Set<String>,
    filterButtonText: String,
    onFilterToggle: (TaskFilterItem) -> Unit,
    availableFilters: List<TaskFilterItem>,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            GlassControlSurface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.grouping_header),
                        fontSize = 9.sp,
                        color = CoFlowColors.TextSecondary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text =
                                if (grouping ==
                                    TaskGrouping.STATUS
                                ) {
                                    stringResource(R.string.grouping_status)
                                } else {
                                    stringResource(R.string.grouping_deadline)
                                },
                            fontSize = 11.sp,
                            color = CoFlowColors.TextPrimary,
                            maxLines = 1,
                        )
                        CoFlowIcon(
                            name = "chevron_down",
                            modifier = Modifier.size(14.dp),
                            color = CoFlowColors.Blue,
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                shape =
                    RoundedCornerShape(
                        16.dp,
                    ),
                containerColor = Color(0xFFF4F8FF),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
            ) {
                GlassGroupingOption(
                    title = stringResource(R.string.grouping_status),
                    selected = grouping == TaskGrouping.STATUS,
                ) {
                    onGroupingChange(TaskGrouping.STATUS)
                    expanded =
                        false
                }
                GlassGroupingOption(
                    title = stringResource(R.string.grouping_deadline),
                    selected = grouping == TaskGrouping.DEADLINE,
                ) {
                    onGroupingChange(TaskGrouping.DEADLINE)
                    expanded =
                        false
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            GlassControlSurface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.filter_header),
                        fontSize = 9.sp,
                        color = CoFlowColors.TextSecondary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = filterButtonText,
                            fontSize = 11.sp,
                            color = CoFlowColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(3.dp))
                        CoFlowIcon(
                            name = "chevron_down",
                            modifier = Modifier.size(14.dp),
                            color = CoFlowColors.Blue,
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                shape =
                    RoundedCornerShape(
                        16.dp,
                    ),
                containerColor =
                    Color(
                        0xFFF4F8FF,
                    ).copy(
                        alpha = 1f,
                    ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
            ) {
                availableFilters.forEach { item ->
                    val displayName =
                        when (item.type) {
                            FilterType.NONE -> stringResource(R.string.filter_none)
                            FilterType.MINE -> stringResource(R.string.filter_mine)
                            FilterType.PROJECT -> item.title
                        }
                    val isSelected =
                        if (item.type == FilterType.NONE) {
                            selectedFilters.isEmpty()
                        } else {
                            selectedFilters.contains(item.id)
                        }

                    GlassFilterOption(
                        title = displayName,
                        selected = isSelected,
                    ) { onFilterToggle(item) }
                }
            }
        }
    }
}

@Composable
private fun GlassControlSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(13.dp))
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.30f),
                                    Color(0xFFDDEAFF).copy(alpha = 0.16f),
                                ),
                        ),
                ).border(
                    width = 1.dp,
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.92f),
                                    CoFlowColors.Blue.copy(alpha = 0.14f),
                                ),
                        ),
                    shape = RoundedCornerShape(13.dp),
                ).padding(horizontal = 9.dp, vertical = 7.dp),
    ) { content() }
}

@Composable
private fun GlassGroupingOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .padding(
                    horizontal = 6.dp,
                    vertical = 2.dp,
                ).fillMaxWidth()
                .clip(
                    RoundedCornerShape(12.dp),
                ).background(
                    if (selected) CoFlowColors.BlueLight.copy(alpha = 0.14f) else Color.Transparent,
                ).clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            if (selected) {
                drawCircle(
                    brush =
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6EA5FF), CoFlowColors.Blue),
                            start = Offset.Zero,
                            end =
                                androidx.compose.ui.geometry
                                    .Offset(size.width, size.height),
                        ),
                )
            } else {
                drawCircle(
                    color = CoFlowGlassStyles.BlueBorder,
                    style = Stroke(width = 1.2.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.width(9.dp))
        Text(text = title, fontSize = 13.sp, color = CoFlowColors.TextPrimary, maxLines = 1)
    }
}

@Composable
private fun GlassFilterOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .padding(
                    horizontal = 6.dp,
                    vertical = 2.dp,
                ).fillMaxWidth()
                .clip(
                    RoundedCornerShape(12.dp),
                ).background(
                    if (selected) CoFlowColors.BlueLight.copy(alpha = 0.14f) else Color.Transparent,
                ).clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(
                        16.dp,
                    ).clip(
                        RoundedCornerShape(5.dp),
                    ).background(
                        if (selected) CoFlowColors.Blue else Color.Transparent,
                    ).border(
                        width = 1.dp,
                        color = if (selected) CoFlowColors.Blue else CoFlowGlassStyles.BlueBorder,
                        shape = RoundedCornerShape(5.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                CoFlowIcon(
                    name = "check",
                    modifier = Modifier.size(10.dp),
                    color = Color.White,
                )
            }
        }
        Spacer(Modifier.width(9.dp))
        Text(text = title, fontSize = 13.sp, color = CoFlowColors.TextPrimary, maxLines = 1)
    }
}

@Composable
fun TasksRoute(
    viewModel: TasksScreenViewModel = viewModel(),
    onNavigateToCreateTask: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onTaskSwipe: (TaskRowUi) -> Unit,
) {
    // Подписываемся на изменения состояния с учетом жизненного цикла
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TasksScreen(
        state = state,
        onNavigateToCreateTask = onNavigateToCreateTask,
        onNavigateToTaskDetail = onNavigateToTaskDetail,
        onGroupingChange = viewModel::onGroupingChange,
        onFilterToggle = viewModel::onFilterToggle,
        onToggleGroup = viewModel::onToggleGroup,
        onNavigateToProjects = onNavigateToProjects,
        onNavigateToProfile = onNavigateToProfile,
        onClearError = viewModel::clearError,
        onTaskSwipe = viewModel::onTaskSwipe,
    )
}

@Preview(name = "По статусу (Обычное)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun TasksScreen_Default_Preview() {
    CoFlowTheme {
        TasksScreen(
            state =
                TasksUiState(
                    activeCount = 5,
                    completedCount = 12,
                    groups =
                        listOf(
                            TaskGroupUi(
                                id = "TODO",
                                titleResId = R.string.tasks_group_planned,
                                count = 2,
                                isExpanded = true,
                                tasks =
                                    listOf(
                                        TaskRowUi(
                                            "1",
                                            "Обновить иконки навигации",
                                            "project_1",
                                            "Дизайн-система",
                                            "user_1",
                                            "user_1",
                                            "30.10.26",
                                            TaskStatus.TODO,
                                            isDone = false,
                                            isOverdue = false,
                                        ),
                                        TaskRowUi(
                                            "2",
                                            "Согласовать ТЗ с заказчиком",
                                            "project_1",
                                            "Сайт",
                                            "user_2",
                                            "user_1",
                                            "01.11.26",
                                            TaskStatus.TODO,
                                            isDone = false,
                                            isOverdue = false,
                                        ),
                                    ),
                            ),
                            TaskGroupUi(
                                id = "IN_PROGRESS",
                                titleResId = R.string.tasks_group_active,
                                count = 3,
                                isExpanded = false,
                                tasks =
                                    listOf(
                                        TaskRowUi(
                                            "3",
                                            "Разработать онбординг",
                                            "project_1",
                                            "Мобильное приложение",
                                            "user_1",
                                            "user_3",
                                            "24.10.26",
                                            TaskStatus.IN_PROGRESS,
                                            isDone = false,
                                            isOverdue = true,
                                        ), // Просрочено!
                                        TaskRowUi(
                                            "4",
                                            "Настроить CI/CD",
                                            "project_1",
                                            "Бэкенд",
                                            "user_3",
                                            "user_2",
                                            "28.10.26",
                                            TaskStatus.IN_PROGRESS,
                                            isDone = false,
                                            isOverdue = false,
                                        ),
                                    ),
                            ),
                            TaskGroupUi(
                                id = "DONE",
                                titleResId = R.string.tasks_group_completed,
                                count = 12,
                                isExpanded = false,
                                tasks = emptyList(),
                            ),
                        ),
                    currentGrouping = TaskGrouping.STATUS,
                    availableFilters =
                        listOf(
                            TaskFilterItem("NONE", "", FilterType.NONE),
                            TaskFilterItem("MINE", "", FilterType.MINE),
                            TaskFilterItem("project_1", "Дизайн-система", FilterType.PROJECT),
                            TaskFilterItem("project_2", "Мобильное приложение", FilterType.PROJECT),
                        ),
                    isBottomBarVisible = true,
                    selectedFilters = setOf("MINE"),
                ),
            onNavigateToCreateTask = {},
            onNavigateToTaskDetail = {},
            onGroupingChange = {},
            onFilterToggle = {},
            onToggleGroup = {},
            onNavigateToProjects = {},
            onNavigateToProfile = {},
            onClearError = {},
            onTaskSwipe = {},
        )
    }
}
