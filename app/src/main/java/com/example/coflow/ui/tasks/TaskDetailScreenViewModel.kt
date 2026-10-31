package com.example.coflow.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.models.TaskModel
import com.example.coflow.models.TaskStatus
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.TaskRepository
import com.example.coflow.repositories.UserRepository
import com.example.coflow.repositories.dto.TaskUpdateDto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TaskStatusUi {
    TODO,
    IN_PROGRESS,
    DONE,
}

data class TaskDetailUiState(
    val id: String,
    val title: String,
    val status: TaskStatusUi,
    val projectName: String,
    val description: String,
    val author: String,
    val assignees: List<String>,
    val dueDateFormatted: String,
    val isOverdue: Boolean,
    val canEdit: Boolean,
    val showBottomAction: Boolean,
    val bottomActionText: String,
    val actionError: String? = null,
)

class TaskDetailScreenViewModel(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    // События для UI (например, навигация или показ Toast)
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _uiState = MutableStateFlow<TaskDetailUiState?>(null)
    val uiState: StateFlow<TaskDetailUiState?> = _uiState.asStateFlow()

    private val taskId: String =
        requireNotNull(savedStateHandle.get<String>("taskId")) {
            "taskId is required for TaskDetailScreen"
        }

    init {
        loadTask()
    }

    // --- Обработчики действий ---
    fun onBottomActionClick() {
        val current = _uiState.value ?: return
        val nextStatus =
            when (current.status) {
                TaskStatusUi.TODO -> TaskStatusUi.IN_PROGRESS
                TaskStatusUi.IN_PROGRESS -> TaskStatusUi.DONE
                TaskStatusUi.DONE -> return
            }

        if (nextStatus == TaskStatusUi.DONE && current.assignees.isEmpty()) {
            _uiState.value = current.copy(actionError = "Нельзя завершить задачу без исполнителей")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(actionError = null)
            // Используем updateTask вместо updateStatus
            val updateDto = TaskUpdateDto(status = nextStatus.toDomain())
            taskRepository.updateTask(taskId, updateDto).onFailure { e ->
                _uiState.value = current.copy(actionError = e.message ?: "Не удалось изменить статус")
            }
        }
    }

    fun onEditClick() {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.NavigateToEdit(taskId))
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.NavigateBack)
        }
    }

    private fun loadTask() {
        viewModelScope.launch {
            taskRepository.getById(taskId).fold(
                onSuccess = { task ->
                    val projectName = if (task.projectId.isNotBlank()) {
                        projectRepository.getById(task.projectId).getOrNull()?.title ?: ""
                    } else ""

                    val authorName = if (task.authorId.isNotBlank()) {
                        userRepository.getById(task.authorId).getOrNull()?.name ?: ""
                    } else ""

                    val assigneeName = if (task.assigneeId.isNotBlank()) {
                        userRepository.getById(task.assigneeId).getOrNull()?.name ?: ""
                    } else ""

                    _uiState.value = task.toUiState(projectName, authorName, assigneeName)
                },
                onFailure = { e ->
                    _uiState.value = null
                    _uiEvent.send(UiEvent.ShowError(e.message ?: "Не удалось загрузить задачу"))
                },
            )
        }
    }

    // События, которые ViewModel отправляет в Route
    sealed class UiEvent {
        object NavigateBack : UiEvent()

        data class NavigateToEdit(
            val taskId: String,
        ) : UiEvent()

        data class ShowError(
            val message: String,
        ) : UiEvent()
    }
}

private fun TaskModel.toUiState(
    projectName: String,
    authorName: String,
    assigneeName: String,
): TaskDetailUiState {
    val dueDateFormatted = deadline.format(DateTimeFormatter.ofPattern("dd.MM.yy", Locale("ru")))
    val isOverdue = deadline.isBefore(LocalDateTime.now()) && status != TaskStatus.DONE

    return TaskDetailUiState(
        id = id,
        title = title,
        status = status.toUi(),
        projectName = projectName,
        description = description,
        author = authorName,
        assignees = listOfNotNull(assigneeName.ifBlank { null }),
        dueDateFormatted = dueDateFormatted,
        isOverdue = isOverdue,
        canEdit = status != TaskStatus.DONE,
        showBottomAction = status != TaskStatus.DONE,
        bottomActionText =
            when (status) {
                TaskStatus.TODO -> "Взять задачу"
                TaskStatus.IN_PROGRESS -> "Выполнить"
                TaskStatus.DONE -> ""
            },
        actionError = null,
    )
}

private fun TaskStatus.toUi(): TaskStatusUi =
    when (this) {
        TaskStatus.TODO -> TaskStatusUi.TODO
        TaskStatus.IN_PROGRESS -> TaskStatusUi.IN_PROGRESS
        TaskStatus.DONE -> TaskStatusUi.DONE
    }

private fun TaskStatusUi.toDomain(): TaskStatus =
    when (this) {
        TaskStatusUi.TODO -> TaskStatus.TODO
        TaskStatusUi.IN_PROGRESS -> TaskStatus.IN_PROGRESS
        TaskStatusUi.DONE -> TaskStatus.DONE
    }
