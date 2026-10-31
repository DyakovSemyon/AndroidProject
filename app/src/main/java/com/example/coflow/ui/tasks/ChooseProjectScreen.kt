package com.example.coflow.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.R
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*

@Composable
fun ChooseProjectScreen(
    state: CreateTaskUiState,
    onBackClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectProject: (String) -> Unit,
    onConfirmSelection: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val filteredProjects = state.filteredProjects
    val selectedProject = state.pendingProject

    // Системная кнопка "Назад"
    BackHandler(onBack = { onBackClick() })

    Box(modifier = Modifier.fillMaxSize().background(coFlowTasksBackgroundBrush())) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
        ) {
            // Заголовок экрана
            CoFlowScreenHeader(
                title = stringResource(R.string.choose_project_header),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )

            // Поиск по прооектам
            CoFlowGlassTextField(
                value = state.projectSearchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = stringResource(R.string.create_task_choose_project_name),
                leadingIcon = "search",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )

            Spacer(Modifier.height(20.dp))

            // Заголовок списка: название раздела + количество отфильтрованных проектов
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.create_task_projects),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoFlowColors.TextPrimary,
                )
                Text(
                    text = "${filteredProjects.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoFlowColors.TextSecondary,
                )
            }

            // Список проектов
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = filteredProjects,
                    key = { it },
                ) { project ->
                    val isSelected = project == selectedProject

                    CoFlowGlassCard(
                        isSelected = isSelected,
                        modifier =
                            Modifier
                                .height(52.dp)
                                .clickable { onSelectProject(project) },
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = project,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) CoFlowColors.Blue else CoFlowColors.TextPrimary,
                            )
                        }
                    }
                }
            }

            // Кнопка подтверждения
            CoFlowPrimaryButton(
                text = stringResource(R.string.done_button),
                onClick = {
                    focusManager.clearFocus()
                    onConfirmSelection(selectedProject)
                },
                enabled = state.canConfirmProject,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
fun ChooseProjectRoute(
    onBackClick: () -> Unit,
    onConfirmSelection: (String) -> Unit,
    viewModel: CreateTaskViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChooseProjectScreen(
        state = state,
        onBackClick = onBackClick,
        onSearchQueryChange = viewModel::onProjectSearchQueryChange,
        onSelectProject = viewModel::onPendingProjectChange,
        onConfirmSelection = { project ->
            viewModel.confirmProjectSelection(project)
            onConfirmSelection(project)
        },
    )
}

@Preview(
    name = "Выбор проекта",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun ChooseProjectScreenPreview() {
    CoFlowTheme {
        ChooseProjectScreen(
            state =
                CreateTaskUiState(
                    allProjects = listOf("Проект 1", "Проект 2"),
                    pendingProject = "Проект 1",
                ),
            onBackClick = {},
            onSearchQueryChange = {},
            onSelectProject = {},
            onConfirmSelection = {},
        )
    }
}
