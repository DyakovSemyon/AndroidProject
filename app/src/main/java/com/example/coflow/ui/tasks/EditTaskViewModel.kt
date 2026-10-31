package com.example.coflow.ui.tasks

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val assignees: List<String>,
    val projectName: String,
    val dueDate: String,
    val status: TaskStatusUi = TaskStatusUi.TODO,
)

data class EditTaskUiState(
    val taskId: Int = 0,
    val title: String = "",
    val description: String = "",
    val assignees: String = "",
    val project: String = "",
    val dueDate: String = "",
    val canSave: Boolean = false,
    val showDeleteDialog: Boolean = false, // <-- Добавлено для управления диалогом из ViewModel
)

class EditTaskViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(EditTaskUiState())
    val state: StateFlow<EditTaskUiState> = _state.asStateFlow()

    init {
        // Читаем taskId напрямую из аргументов навигации "edit_task/{taskId}"
        val taskId = savedStateHandle.get<String>("taskId")?.toIntOrNull() ?: 1
        loadTask(taskId)
    }

    private fun loadTask(taskId: Int) {
        val mockTask = getMockTask(taskId)
        _state.value =
            EditTaskUiState(
                taskId = mockTask.id,
                title = mockTask.title,
                description = mockTask.description,
                assignees = mockTask.assignees.joinToString(", "),
                project = mockTask.projectName,
                dueDate = mockTask.dueDate,
                canSave = true,
                showDeleteDialog = false,
            )
    }

    fun onTitleChange(newTitle: String) {
        _state.update { it.copy(title = newTitle, canSave = newTitle.isNotBlank() && it.dueDate.isNotBlank()) }
    }

    fun onDescriptionChange(newDescription: String) {
        _state.update { it.copy(description = newDescription) }
    }

    fun onAssigneesChange(newAssignees: String) {
        _state.update { it.copy(assignees = newAssignees) }
    }

    fun onProjectChange(newProject: String) {
        _state.update { it.copy(project = newProject) }
    }

    fun onDueDateChange(newDate: String) {
        _state.update { it.copy(dueDate = newDate, canSave = it.title.isNotBlank() && newDate.isNotBlank()) }
    }

    // Аналог onCreateTask из CreateTaskViewModel
    fun onSaveTask(onTaskSaved: (Task) -> Unit) {
        val currentState = _state.value
        val updatedTask =
            Task(
                id = currentState.taskId,
                title = currentState.title,
                description = currentState.description,
                assignees =
                    currentState.assignees
                        .split(", ")
                        .map { it.trim() }
                        .filter { it.isNotBlank() },
                projectName = currentState.project,
                dueDate = currentState.dueDate,
                status = TaskStatusUi.TODO, // Или сохраняем старый статус
            )
        onTaskSaved(updatedTask)
    }

    fun showDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    fun confirmDelete(onTaskDeleted: (Int) -> Unit) {
        onTaskDeleted(_state.value.taskId)
        hideDeleteDialog()
    }
}

// Заглушка для получения задачи по ID (можно вынести в отдельный файл TestData)
private fun getMockTask(taskId: Int): Task =
    when (taskId) {
        1 -> {
            Task(1, "Обновить иконки в навигации", "", emptyList(), "Дизайн-система", "30.10.26")
        }

        3 -> {
            Task(
                3,
                "Разработать новый онбординг",
                "Нужно сделать 3 экрана.",
                listOf("Мария Иванова", "Ты"),
                "Мобильное приложение",
                "24.10.26",
            )
        }

        else -> {
            Task(taskId, "Провести ревью кода", "Все замечания исправлены.", listOf("Ты"), "Бэкенд", "20.10.26")
        }
    }
