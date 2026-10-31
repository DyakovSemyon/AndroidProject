package com.example.coflow.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.models.TaskModel
import com.example.coflow.models.TaskStatus
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseTaskRepositoryImpl
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class TaskGrouping { STATUS, DEADLINE }

data class TaskRowUi(
    val id: String,
    val title: String,
    val projectName: String,
    val authorId: String,
    val assigneeId: String,
    val dueDateFormatted: String,
    val status: TaskStatus,
    val isDone: Boolean,
    val isOverdue: Boolean,
)

data class TaskGroupUi(
    val id: String,
    val title: String,
    val count: Int,
    val isExpanded: Boolean,
    val tasks: List<TaskRowUi>,
)

data class TasksUiState(
    val activeCount: Int,
    val completedCount: Int,
    val groups: List<TaskGroupUi>,
    val currentGrouping: TaskGrouping,
    val selectedFilters: Set<String>,
    val filterButtonText: String,
    val availableFilters: List<String>,
    val isBottomBarVisible: Boolean,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allTaskRows: List<TaskRowUi> = emptyList(),
)

class TasksScreenViewModel(
    private val taskRepository: TaskRepository = FirebaseTaskRepositoryImpl(),
    private val projectRepository: ProjectRepository = FirebaseProjectRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            TasksUiState(
                activeCount = 0,
                completedCount = 0,
                groups = emptyList(),
                currentGrouping = TaskGrouping.STATUS,
                selectedFilters = emptySet(),
                filterButtonText = "Без фильтра",
                availableFilters = emptyList(),
                isBottomBarVisible = true,
                isLoading = true, // Сразу показываем крутилку
            ),
        )
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private var currentUid: String? = null

    init {
        loadTasks() // Запускаем загрузку при старте экрана
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            currentUid = authRepository.getCurrentUserUid()

            if (currentUid == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Пользователь не авторизован") }
                return@launch
            }

            // Загружаем проекты пользователя
            projectRepository.getByUserId(currentUid!!).fold(
                onSuccess = { projects ->
                    val allFetchedTasks = mutableListOf<TaskModel>()
                    // Создаем мапу ID -> Название, чтобы быстро подставлять имя проекта в UI
                    val projectMap = projects.associateBy { it.id }

                    // Загружаем задачи для каждого проекта
                    projects.forEach { project ->
                        taskRepository.getByProjectId(project.id).getOrNull()?.let { tasks ->
                            allFetchedTasks.addAll(tasks)
                        }
                    }

                    // Превращаем TaskModel в TaskRowUi
                    val taskRows =
                        allFetchedTasks.map { task ->
                            TaskRowUi(
                                id = task.id,
                                title = task.title,
                                projectName = projectMap[task.projectId]?.title ?: "Без проекта",
                                authorId = task.authorId,
                                assigneeId = task.assigneeId,
                                dueDateFormatted = task.deadline.format(DateTimeFormatter.ofPattern("dd.MM.yy")),
                                status = task.status,
                                isDone = task.status == TaskStatus.DONE,
                                // Firestore хранит даты в UTC, поэтому и текущее время берём в UTC.
                                // Иначе на устройствах с другим часовым поясом задача может
                                // считаться просроченной раньше или позже реального дедлайна.
                                isOverdue = task.deadline.isBefore(LocalDateTime.now(ZoneOffset.UTC)) && task.status != TaskStatus.DONE,
                            )
                        }

                    // Обновляем стейт: убираем загрузку, кидаем задачи и формируем фильтры
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            allTaskRows = taskRows,
                            availableFilters = listOf("Без фильтра", "Мои задачи") + projects.map { "Проект: ${it.title}" },
                        )
                    }

                    // Сразу применяем фильтры и группировку
                    applyFiltersAndGrouping()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Ошибка загрузки проектов") }
                },
            )
        }
    }

    // --- Обработчики событий от UI ---

    fun onGroupingChange(grouping: TaskGrouping) {
        _uiState.update { it.copy(currentGrouping = grouping) }
        applyFiltersAndGrouping()
    }

    fun onFilterToggle(filter: String) {
        _uiState.update { state ->
            val newFilters = state.selectedFilters.toMutableSet()

            if (filter == "Без фильтра") {
                newFilters.clear()
            } else {
                if (newFilters.contains(filter)) {
                    newFilters.remove(filter)
                } else {
                    newFilters.add(filter)
                }
            }

            val buttonText = if (newFilters.isEmpty()) "Без фильтра" else newFilters.joinToString(", ")

            state.copy(
                selectedFilters = newFilters,
                filterButtonText = buttonText,
            )
        }
        applyFiltersAndGrouping()
    }

    fun onToggleGroup(groupId: String) {
        _uiState.update { state ->
            val updatedGroups =
                state.groups.map { group ->
                    if (group.id == groupId) {
                        group.copy(isExpanded = !group.isExpanded)
                    } else {
                        group
                    }
                }
            state.copy(groups = updatedGroups)
        }
    }

    private fun applyFiltersAndGrouping() {
        _uiState.update { state ->
            var filtered = state.allTaskRows

            if (state.selectedFilters.isNotEmpty()) {
                if (state.selectedFilters.contains("Мои задачи") && currentUid != null) {
                    filtered = filtered.filter { it.authorId == currentUid || it.assigneeId == currentUid }
                }

                val selectedProjects = state.selectedFilters.filter { it.startsWith("Проект: ") }.map { it.removePrefix("Проект: ") }
                if (selectedProjects.isNotEmpty()) {
                    filtered = filtered.filter { selectedProjects.contains(it.projectName) }
                }
            }

            val newGroups =
                when (state.currentGrouping) {
                    TaskGrouping.STATUS -> groupTasksByStatus(filtered)
                    TaskGrouping.DEADLINE -> groupTasksByDeadline(filtered)
                }

            state.copy(
                groups = newGroups,
                activeCount = filtered.count { !it.isDone },
                completedCount = filtered.count { it.isDone },
            )
        }
    }

    fun onNotificationClick() {
        // Заглушка: здесь можно отправить событие или ничего не делать
    }

    // --- Вспомогательные функции для группировки ---

    private fun groupTasksByStatus(tasks: List<TaskRowUi>): List<TaskGroupUi> {
        val todo = tasks.filter { it.status == TaskStatus.TODO }
        val inProgress = tasks.filter { it.status == TaskStatus.IN_PROGRESS }
        val done = tasks.filter { it.status == TaskStatus.DONE }

        return listOf(
            TaskGroupUi("TODO", "К исполнению", todo.size, isExpanded = true, tasks = todo),
            TaskGroupUi("IN_PROGRESS", "В процессе", inProgress.size, isExpanded = false, tasks = inProgress),
            TaskGroupUi("DONE", "Сделано", done.size, isExpanded = false, tasks = done),
        )
    }

    private fun groupTasksByDeadline(tasks: List<TaskRowUi>): List<TaskGroupUi> {
        val overdue = tasks.filter { it.isOverdue && !it.isDone }
        val upcoming = tasks.filter { !it.isOverdue && !it.isDone }
        val done = tasks.filter { it.isDone }

        return listOf(
            TaskGroupUi("OVERDUE", "Просрочено", overdue.size, isExpanded = true, tasks = overdue),
            TaskGroupUi("UPCOMING", "Предстоящие", upcoming.size, isExpanded = false, tasks = upcoming),
            TaskGroupUi("COMPLETED", "Завершенные", done.size, isExpanded = false, tasks = done),
        )
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
