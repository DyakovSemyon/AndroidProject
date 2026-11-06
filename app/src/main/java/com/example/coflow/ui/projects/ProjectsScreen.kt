package com.example.coflow.ui.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.R
import com.example.coflow.ui.components.CoFlowBottomNavigationBar
import com.example.coflow.ui.components.CoFlowFabButton
import com.example.coflow.ui.components.CoFlowGlassCard
import com.example.coflow.ui.components.CoFlowGlassTextField
import com.example.coflow.ui.components.CoFlowIcon
import com.example.coflow.ui.components.CoFlowScreenHeader
import com.example.coflow.ui.components.NavItem
import com.example.coflow.ui.components.NotificationBell
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowGlassStyles
import com.example.coflow.ui.theme.CoFlowSpacing
import com.example.coflow.ui.theme.CoFlowTheme
import com.example.coflow.ui.theme.coFlowTasksBackgroundBrush

// Подключение ViewModel и передача данных в UI
@Composable
fun ProjectsRoute(
    onNavigateToCreateProject: () -> Unit,
    onNavigateToTasks: () -> Unit,
    viewModel: ProjectsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProjectsScreen(
        state = state,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterMenuToggle = viewModel::onFilterMenuToggle,
        onFilterSelect = viewModel::onFilterSelect,
        onNavigateToCreateProject = onNavigateToCreateProject,
        onNavigateToTasks = onNavigateToTasks,
    )
}

// UI-экран
@Composable
fun ProjectsScreen(
    state: ProjectsUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterMenuToggle: () -> Unit,
    onFilterSelect: (ProjectFilter) -> Unit,
    onNavigateToCreateProject: () -> Unit,
    onNavigateToTasks: () -> Unit,
) {
    // Стейт для скролла и скрытия нижней панели
    val listState = rememberLazyListState()
    var isBottomPanelVisible by remember { mutableStateOf(true) }
    var prevIndex by remember { mutableIntStateOf(0) }
    var prevOffset by remember { mutableIntStateOf(0) }

    // Логика: скрываем панель при скролле вниз, показываем при скролле вверх
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index > prevIndex) {
                    isBottomPanelVisible = false
                    prevIndex = index
                    prevOffset = offset
                } else if (index < prevIndex) {
                    isBottomPanelVisible = true
                    prevIndex = index
                    prevOffset = offset
                } else {
                    // Буфер 10 пикселей, чтобы панель не дергалась от мелких движений
                    val delta = offset - prevOffset
                    if (delta > 10) {
                        if (isBottomPanelVisible) isBottomPanelVisible = false
                        prevOffset = offset
                    } else if (delta < -10) {
                        if (!isBottomPanelVisible) isBottomPanelVisible = true
                        prevOffset = offset
                    }
                }
            }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(coFlowTasksBackgroundBrush())
                .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
    ) {
        // Заголовок с подзаголовком и колокольчиком
        CoFlowScreenHeader(
            title = stringResource(R.string.projects_header),
            subtitle = "${state.activeCount} ${stringResource(R.string.my_tasks_active)}",
            trailingContent = {
                NotificationBell()
            },
        )

        // Поле поиска
        CoFlowGlassTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(R.string.project_search_placeholder),
            leadingIcon = "search",
        )

        Spacer(Modifier.height(4.dp))

        // Список проектов (занимает всё свободное место)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            if (state.projects.isEmpty()) {
                // Показываем текст, если ничего не найдено
                item {
                    Text(
                        text = stringResource(R.string.project_empty_state),
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        color = CoFlowColors.TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(state.projects, key = { it.id }) { project ->
                    ProjectCard(project = project)
                }
            }
        }

        // Нижняя панель
        AnimatedVisibility(visible = isBottomPanelVisible) {
            Column(
                modifier =
                    Modifier.fillMaxWidth().padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 2.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.widthIn(max = 330.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Фильтр
                    ProjectFilterPanel(
                        modifier = Modifier.weight(1f),
                        selectedFilter = state.selectedFilter,
                        isExpanded = state.isFilterMenuExpanded,
                        onToggle = onFilterMenuToggle,
                        onFilterSelect = onFilterSelect,
                    )

                    Spacer(Modifier.width(16.dp))

                    // Кнопка "+" (FAB)
                    CoFlowFabButton(onClick = onNavigateToCreateProject, iconName = "folder_plus")
                }

                Spacer(Modifier.height(7.dp))

                // Нижнее меню (Навигация)
                CoFlowBottomNavigationBar(
                    items =
                        listOf(
                            NavItem(
                                icon = "tasks",
                                title = stringResource(R.string.navigation_tasks),
                                selected = false,
                                onClick = onNavigateToTasks,
                            ),
                            NavItem(icon = "folder", title = stringResource(R.string.navigation_projects), selected = true, onClick = {}),
                            NavItem(
                                icon = "person",
                                title = stringResource(R.string.navigation_profile),
                                selected = false,
                                onClick = { /* TODO */ },
                            ),
                        ),
                )
            }
        }
    }
}

// Карточка проекта
@Composable
private fun ProjectCard(project: ProjectUiItem) {
    CoFlowGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Цветная полоска слева
            Box(
                modifier =
                    Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF86D3B5)),
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(text = project.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CoFlowColors.TextPrimary)
                Spacer(Modifier.height(6.dp))
                ProjectStatusChip()
            }
        }
    }
}

// Маленькая плашка статуса
@Composable
private fun ProjectStatusChip() {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE5F6EE))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(R.string.project_status_in_progress),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1B8A5A),
        )
    }
}

// Панель фильтра
@Composable
private fun ProjectFilterPanel(
    modifier: Modifier = Modifier,
    selectedFilter: ProjectFilter,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onFilterSelect: (ProjectFilter) -> Unit,
) {
    Box(modifier = modifier) {
        // Сама кнопка (используем GlassControlSurface)
        GlassControlSurface(modifier = Modifier.fillMaxWidth().clickable { onToggle() }) {
            Column {
                Text(text = stringResource(R.string.project_filter_title), fontSize = 9.sp, color = CoFlowColors.TextSecondary)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(selectedFilter.titleResId),
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

        // Выпадающее меню
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onToggle,
            shape = RoundedCornerShape(16.dp),
            containerColor = Color(0xFFF4F8FF).copy(alpha = 0.62f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
        ) {
            ProjectFilter.entries.forEach { option ->
                GlassFilterOption(
                    title = stringResource(option.titleResId),
                    selected = option == selectedFilter,
                ) { onFilterSelect(option) }
            }
        }
    }
}

// Стеклянная подложка для фильтра
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

// Элемент выпадающего списка
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

// Превью для Android Studio
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProjectsScreenPreview() {
    CoFlowTheme {
        ProjectsScreen(
            state =
                ProjectsUiState(
                    projects =
                        listOf(
                            ProjectUiItem("1", "Проект", ProjectRole.OWNER),
                        ),
                ),
            onSearchQueryChange = {},
            onFilterMenuToggle = {},
            onFilterSelect = {},
            onNavigateToCreateProject = {},
            onNavigateToTasks = {},
        )
    }
}
