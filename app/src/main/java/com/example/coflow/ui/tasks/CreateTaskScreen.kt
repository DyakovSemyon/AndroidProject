package com.example.coflow.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.R
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*

@Composable
fun CreateTaskScreen(
    state: CreateTaskUiState,
    onBackClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNavigateToAssignees: () -> Unit,
    onNavigateToProject: () -> Unit,
    onNavigateToDate: () -> Unit,
    onCreateTask: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    BackHandler(onBack = onBackClick)

    Box(modifier = Modifier.fillMaxSize().background(coFlowTasksBackgroundBrush())) {
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = CoFlowSpacing.ScreenHorizontal)) {
            // Заголовок экрана создания задачи
            CoFlowScreenHeader(
                title = stringResource(R.string.create_task_header),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )

            // Скроллящаяся форма
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Название задачи
                CoFlowGlassTextField(
                    label = stringResource(R.string.creation_task_name_label),
                    value = state.title,
                    onValueChange = onTitleChange,
                    placeholder = stringResource(R.string.creation_task_name_placeholder),
                )

                // Описание
                CoFlowGlassTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = stringResource(R.string.creation_task_description_label),
                    placeholder = stringResource(R.string.creation_task_description_placeholder),
                    singleLine = false,
                    maxLength = 250,
                )

                // Исполнители
                CoFlowGlassTextField(
                    value = state.assignees.joinToString(", ") { it.name },
                    onValueChange = { },
                    label = stringResource(R.string.creation_task_assignee_label),
                    placeholder = stringResource(R.string.creation_task_assignee_placeholder),
                    leadingIcon = "person",
                    trailingIcon = "chevron",
                    onClick = { onNavigateToAssignees() },
                )

                // Проект
                CoFlowGlassTextField(
                    value = state.project,
                    onValueChange = { },
                    label = stringResource(R.string.creation_task_project_label),
                    placeholder = stringResource(R.string.creation_task_project_placeholder),
                    leadingIcon = "folder",
                    trailingIcon = "chevron",
                    onClick = { onNavigateToProject() },
                )

                // Дата окончания
                CoFlowGlassTextField(
                    value = state.dueDate,
                    onValueChange = { },
                    label = stringResource(R.string.creation_task_deadline_label),
                    placeholder = stringResource(R.string.creation_task_deadline_placeholder),
                    leadingIcon = "calendar",
                    trailingIcon = "chevron",
                    onClick = { onNavigateToDate() },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }

            // Кнопка создания
            CoFlowPrimaryButton(
                text = stringResource(R.string.create_task_button),
                onClick = {
                    focusManager.clearFocus()
                    onCreateTask()
                },
                enabled = state.canCreate,
            )
        }
    }
}

@Composable
fun CreateTaskRoute(
    onBackClick: () -> Unit,
    onTaskCreated: (CreateTaskFormData) -> Unit,
    onNavigateToAssignees: () -> Unit,
    onNavigateToProject: () -> Unit,
    onNavigateToDate: () -> Unit,
    viewModel: CreateTaskViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CreateTaskScreen(
        state = state,
        onBackClick = onBackClick,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onNavigateToAssignees = onNavigateToAssignees,
        onNavigateToProject = onNavigateToProject,
        onNavigateToDate = onNavigateToDate,
        onCreateTask = { viewModel.onCreateTask(onTaskCreated) },
    )
}

@Preview(
    name = "Создание задачи",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun CreateTaskScreenPreview() {
    CoFlowTheme {
        CreateTaskScreen(
            state = CreateTaskUiState(),
            onBackClick = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onNavigateToAssignees = {},
            onNavigateToProject = {},
            onNavigateToDate = {},
            onCreateTask = {},
        )
    }
}
