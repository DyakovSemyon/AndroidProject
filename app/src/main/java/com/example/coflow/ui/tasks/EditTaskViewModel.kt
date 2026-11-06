package com.example.coflow.ui.tasks

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.R
import com.example.coflow.models.TaskStatus
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseTaskRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.TaskRepository
import com.example.coflow.repositories.UserRepository
import com.example.coflow.repositories.dto.TaskUpdateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val assignees: List<String>,
    val projectName: String,
    val dueDate: String,
    val status: TaskStatusUi = TaskStatusUi.TODO,
)

data class EditTaskUiState(
    val taskId: String = "",
    val title: String = "",
    val description: String = "",
    val assignees: String = "",
    val project: String = "",
    val dueDate: String = "",
    val canSave: Boolean = false,
    val showDeleteDialog: Boolean = false, // <-- Добавлено для управления диалогом из ViewModel
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val errorMessageResId: Int? = null,
)

class EditTaskViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository = FirebaseTaskRepositoryImpl(),
    private val projectRepository: ProjectRepository = FirebaseProjectRepositoryImpl(),
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    private val _state = MutableStateFlow(EditTaskUiState())
    val state: StateFlow<EditTaskUiState> = _state.asStateFlow()

    // Читаем taskId напрямую из аргументов навигации "edit_task/{taskId}"
    val taskId = savedStateHandle.get<String>("taskId").orEmpty()

    init {
        if (taskId.isNotBlank()) {
            loadTask()
        } else {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessageResId = R.string.error_task_id_missing,
                )
            }
        }
    }

    private fun loadTask() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val currentUid = authRepository.getCurrentUserUid().orEmpty()

            // 1. Сама задача
            val taskResult = taskRepository.getById(taskId)
            val task =
                taskResult.getOrElse { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Ошибка загрузки задачи",
                        )
                    }
                    return@launch
                }

            // 2. Название проекта
            val projectName =
                if (task.projectId.isNotBlank()) {
                    projectRepository
                        .getById(task.projectId)
                        .getOrNull()
                        ?.title
                        .orEmpty()
                } else {
                    ""
                }

            // 4. Имена исполнителей
            val assigneeNames = mutableListOf<String>()
            if (task.assigneeId.isNotBlank()) {
                val assigneeUser = userRepository.getById(task.assigneeId).getOrNull()
                if (assigneeUser != null) {
                    assigneeNames.add(
                        if (assigneeUser.uid == currentUid) "Ты" else assigneeUser.name,
                    )
                }
            }

            // 5. Формат даты
            val formattedDate =
                task.deadline.format(DateTimeFormatter.ofPattern("dd.MM.yy"))

            // 6. Публикуем в state
            _state.update {
                it.copy(
                    taskId = task.id,
                    title = task.title,
                    description = task.description,
                    assignees = assigneeNames.joinToString(", "),
                    project = projectName,
                    dueDate = formattedDate,
                    canSave = task.title.isNotBlank() && formattedDate.isNotBlank(),
                    isLoading = false,
                    errorMessage = null,
                )
            }
        }
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

    fun onSaveTask(onTaskSaved: (Task) -> Unit) {
        val current = _state.value
        if (current.isSaving) return

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val updateDto =
                TaskUpdateDto(
                    title = current.title.trim(),
                    description = current.description.trim(),
                    // нужно дополнить для исполнителей, проекта и даты
                )

            val result = taskRepository.updateTask(current.taskId, updateDto)

            result.fold(
                onSuccess = { updated ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            title = updated.title,
                            description = updated.description,
                            canSave = updated.title.isNotBlank(),
                        )
                    }
                    onTaskSaved(
                        Task(
                            id = updated.id,
                            title = updated.title,
                            description = updated.description,
                            assignees =
                                current.assignees
                                    .split(", ")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() },
                            projectName = current.project,
                            dueDate = current.dueDate,
                        ),
                    )
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message,
                        )
                    }
                },
            )
        }
    }

    fun showDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

//    fun confirmDelete(onTaskDeleted: (String) -> Unit) {
//        onTaskDeleted(_state.value.taskId)
//        hideDeleteDialog()
//    }

    fun confirmDelete(onTaskDeleted: (String) -> Unit) {
        val current = _state.value
        if (current.isDeleting) return
        if (current.taskId.isBlank()) {
            _state.update { it.copy(showDeleteDialog = false, errorMessage = "ID задачи не задан") }
            return
        }

        // Закрываем диалог и блокируем повторное нажатие
        _state.update { it.copy(showDeleteDialog = false, isDeleting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = taskRepository.deleteTask(current.taskId)

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isDeleting = false) }
                    onTaskDeleted(current.taskId)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = error.message ?: "Ошибка удаления задачи",
                        )
                    }
                },
            )
        }
    }
}

// Заглушка для получения задачи по ID (можно вынести в отдельный файл TestData)
private fun getMockTask(taskId: String): Task =
    when (taskId) {
        "1" -> {
            Task("1", "Обновить иконки в навигации", "", emptyList(), "Дизайн-система", "30.10.26")
        }

        "3" -> {
            Task(
                "3",
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
