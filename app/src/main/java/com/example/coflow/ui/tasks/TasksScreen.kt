package com.example.coflow.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*

@Composable
fun TasksScreen(
    state: TasksUiState,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onGroupingChange: (TaskGrouping) -> Unit,
    onFilterToggle: (String) -> Unit,
    onToggleGroup: (String) -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onClearError: () -> Unit,
) {
    val scrollState = rememberScrollState()

    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onClearError,
            title = { Text(stringResource(R.string.error_title), color = CoFlowColors.Error) },
            text = { Text(state.errorMessage) },
            confirmButton = {
                TextButton(onClick = onClearError) {
                    Text(stringResource(R.string.ok_button), color = CoFlowColors.Blue)
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(coFlowTasksBackgroundBrush())) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = CoFlowSpacing.ScreenHorizontal)) {
            CoFlowScreenHeader(
                title = stringResource(R.string.my_tasks_header),
                subtitle = "${state.activeCount} ${stringResource(
                    R.string.my_tasks_active,
                )} · ${state.completedCount} ${stringResource(R.string.my_tasks_completed)}",
                trailingContent = {
                    NotificationBell()
                },
            )

            Spacer(Modifier.height(16.dp))

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
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            AnimatedVisibility(visible = state.isBottomBarVisible) {
                Column(
                    modifier =
                        Modifier.fillMaxWidth().padding(
                            bottom =
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 2.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.widthIn(max = 330.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SortFilterPanel(
                            modifier = Modifier.weight(1f),
                            grouping = state.currentGrouping,
                            onGroupingChange = onGroupingChange,
                            filterButtonText = state.filterButtonText,
                            onFilterToggle = onFilterToggle,
                            selectedFilters = state.selectedFilters,
                            availableFilters = state.availableFilters,
                        )
                        Spacer(Modifier.width(16.dp))

                        CoFlowFabButton(onClick = onNavigateToCreateTask, iconName = "task_plus")
                    }
                    Spacer(Modifier.height(7.dp))

                    CoFlowBottomNavigationBar(
                        items =
                            listOf(
                                NavItem(icon = "tasks", title = stringResource(R.string.navigation_tasks), selected = true, onClick = {}),
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
    }
}

@Composable
private fun TaskGroupCard(
    group: TaskGroupUi,
    onToggle: () -> Unit,
    onTaskClick: (TaskRowUi) -> Unit,
) {
    CoFlowGlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = group.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CoFlowColors.TextPrimary)
            Spacer(Modifier.width(7.dp))
            Box(
                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(5.dp),
                        ).background(CoFlowColors.BlueLight.copy(alpha = 0.38f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(text = group.tasks.size.toString(), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = CoFlowColors.Blue)
            }
            Spacer(Modifier.weight(1f))
            Text(text = if (group.isExpanded) "⌄" else "›", color = CoFlowColors.Blue, fontSize = 17.sp)
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
                    TaskRow(task = task, onClick = { onTaskClick(task) })
                    if (index < group.tasks.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CoFlowGlassStyles.BlueBorder.copy(alpha = 0.45f)))
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
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = task.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (task.isDone) CoFlowColors.TextSecondary else CoFlowColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (task.isOverdue) {
                Spacer(Modifier.width(4.dp))
                Text(text = stringResource(R.string.task_outdated), fontSize = 8.sp, color = CoFlowColors.Error)
            }
        }
        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = task.projectName, fontSize = 10.sp, color = CoFlowColors.TextSecondary, maxLines = 1)
            Text(text = task.dueDateFormatted, fontSize = 10.sp, color = CoFlowColors.TextSecondary)
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
    onFilterToggle: (String) -> Unit,
    availableFilters: List<String>,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            GlassControlSurface(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
                Column {
                    Text(text = stringResource(R.string.grouping_header), fontSize = 9.sp, color = CoFlowColors.TextSecondary)
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
                        Spacer(Modifier.width(3.dp))
                        Text("⌄", fontSize = 11.sp, color = CoFlowColors.Blue)
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
                        alpha = 0.62f,
                    ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
            ) {
                GlassGroupingOption(title = stringResource(R.string.grouping_status), selected = grouping == TaskGrouping.STATUS) {
                    onGroupingChange(TaskGrouping.STATUS)
                    expanded =
                        false
                }
                GlassGroupingOption(title = stringResource(R.string.grouping_deadline), selected = grouping == TaskGrouping.DEADLINE) {
                    onGroupingChange(TaskGrouping.DEADLINE)
                    expanded =
                        false
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            GlassControlSurface(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
                Column {
                    Text(text = stringResource(R.string.filter_header), fontSize = 9.sp, color = CoFlowColors.TextSecondary)
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
                        Text("⌄", fontSize = 11.sp, color = CoFlowColors.Blue)
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
                        alpha = 0.62f,
                    ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
            ) {
                availableFilters.forEach { option ->
                    GlassFilterOption(title = option, selected = selectedFilters.contains(option)) { onFilterToggle(option) }
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
                    brush = Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.30f), Color(0xFFDDEAFF).copy(alpha = 0.16f))),
                ).border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.92f), CoFlowColors.Blue.copy(alpha = 0.14f))),
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
                drawCircle(color = CoFlowGlassStyles.BlueBorder, style = Stroke(width = 1.2.dp.toPx()))
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
            if (selected) Text("✓", fontSize = 10.sp, color = Color.White)
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
                                title = "К исполнению",
                                count = 2,
                                isExpanded = true,
                                tasks =
                                    listOf(
                                        TaskRowUi(
                                            "1",
                                            "Обновить иконки навигации",
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
                                title = "В процессе",
                                count = 3,
                                isExpanded = false,
                                tasks =
                                    listOf(
                                        TaskRowUi(
                                            "3",
                                            "Разработать онбординг",
                                            "Мобильное приложение",
                                            "user_1",
                                            "user_3",
                                            "24.10.26",
                                            TaskStatus.IN_PROGRESS,
                                            isDone = false,
                                            isOverdue = true,
                                        ),
                                        TaskRowUi(
                                            "4",
                                            "Настроить CI/CD",
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
                                title = "Сделано",
                                count = 12,
                                isExpanded = false,
                                tasks = emptyList(),
                            ),
                        ),
                    currentGrouping = TaskGrouping.STATUS,
                    filterButtonText = "Без фильтра",
                    availableFilters = listOf("Без фильтра", "Мои задачи", "Проект: Дизайн-система", "Проект: Мобильное приложение"),
                    isBottomBarVisible = true,
                    selectedFilters = setOf("Мои задачи"),
                ),
            onNavigateToCreateTask = {},
            onNavigateToTaskDetail = {},
            onGroupingChange = {},
            onFilterToggle = {},
            onToggleGroup = {},
            onNavigateToProjects = {},
            onNavigateToProfile = {},
            onClearError = {},
        )
    }
}
