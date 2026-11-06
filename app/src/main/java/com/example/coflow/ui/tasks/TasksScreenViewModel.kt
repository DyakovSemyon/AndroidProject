package com.example.coflow.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.R
import com.example.coflow.models.TaskStatus
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseTaskRepositoryImpl
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class TaskGrouping { STATUS, DEADLINE }

enum class FilterType { NONE, MINE, PROJECT }

data class TaskFilterItem(
    val id: String,
    val title: String,
    val type: FilterType,
)

data class TaskRowUi(
    val id: String,
    val title: String,
    val projectId: String,
    val projectName: String,
    val authorId: String,
    val assigneeId: String,
    val dueDateFormatted: String,
    val status: TaskStatus,
    val isDone: Boolean,
    val isOverdue: Boolean,
    val isMine: Boolean = false,
)

data class TaskGroupUi(
    val id: String,
    val titleResId: Int,
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
    val availableFilters: List<TaskFilterItem>,
    val isBottomBarVisible: Boolean,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val errorMessageResId: Int? = null,
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
                availableFilters =
                    listOf(
                        TaskFilterItem("NONE", "", FilterType.NONE),
                        TaskFilterItem("MINE", "", FilterType.MINE),
                    ),
                isBottomBarVisible = true,
                isLoading = true,
            ),
        )
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private var currentUid: String? = null
    private var observeJob: Job? = null

    init {
        loadTasks()
    }

    private fun loadTasks() {
        // Отменяем предыдущую подписку, если она была
        observeJob?.cancel()

        observeJob =
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                try {
                    val uid = authRepository.getCurrentUserUid()
                    if (uid == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessageResId = R.string.error_not_authorized,
                            )
                        }
                        return@launch
                    }
                    currentUid = uid

                    // Загружаем проекты: где я участник И где я владелец
                    val userProjectsResult = projectRepository.getByUserId(uid)
                    val ownerProjectsResult = projectRepository.getByOwnerId(uid)

                    if (userProjectsResult.isFailure && ownerProjectsResult.isFailure) {
                        val error =
                            userProjectsResult.exceptionOrNull()
                                ?: ownerProjectsResult.exceptionOrNull()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error?.message,
                                errorMessageResId =
                                    if (error?.message == null) {
                                        R.string.error_load_projects
                                    } else {
                                        null
                                    },
                            )
                        }
                        return@launch
                    }

                    val projects =
                        (
                            userProjectsResult.getOrDefault(emptyList()) +
                                ownerProjectsResult.getOrDefault(emptyList())
                        ).distinctBy { it.id }.filterNot { it.isDeleted }

                    val projectMap = projects.associateBy { it.id }

                    // Real-time подписка на задачи каждого проекта
                    val taskFlows =
                        projects.map { project ->
                            taskRepository.observeTasksByProjectId(project.id)
                        }

                    if (taskFlows.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                allTaskRows = emptyList(),
                                availableFilters =
                                    listOf(
                                        TaskFilterItem("NONE", "", FilterType.NONE),
                                        TaskFilterItem("MINE", "", FilterType.MINE),
                                    ),
                            )
                        }
                        applyFiltersAndGrouping()
                        return@launch
                    }

                    combine(taskFlows) { resultsArray ->
                        resultsArray.flatMap { it.getOrNull() ?: emptyList() }
                    }.collect { allFetchedTasks ->
                        val taskRows =
                            allFetchedTasks.map { task ->
                                TaskRowUi(
                                    id = task.id,
                                    projectId = task.projectId,
                                    title = task.title,
                                    projectName = projectMap[task.projectId]?.title ?: "Без проекта",
                                    authorId = task.authorId,
                                    assigneeId = task.assigneeId,
                                    dueDateFormatted =
                                        task.deadline.format(
                                            DateTimeFormatter.ofPattern("dd.MM.yy"),
                                        ),
                                    status = task.status,
                                    isDone = task.status == TaskStatus.DONE,
                                    isOverdue =
                                        task.deadline.isBefore(
                                            LocalDateTime.now(ZoneOffset.UTC),
                                        ) && task.status != TaskStatus.DONE,
                                    isMine = task.authorId == uid || task.assigneeId == uid,
                                )
                            }

                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                allTaskRows = taskRows,
                                availableFilters =
                                    listOf(
                                        TaskFilterItem("NONE", "", FilterType.NONE),
                                        TaskFilterItem("MINE", "", FilterType.MINE),
                                    ) +
                                        projects.map {
                                            TaskFilterItem(it.id, it.title, FilterType.PROJECT)
                                        },
                            )
                        }
                        applyFiltersAndGrouping()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message,
                            errorMessageResId = R.string.error_system,
                        )
                    }
                }
            }
    }

    fun onGroupingChange(grouping: TaskGrouping) {
        _uiState.update { it.copy(currentGrouping = grouping) }
        applyFiltersAndGrouping()
    }

    fun onFilterToggle(filter: TaskFilterItem) {
        _uiState.update { state ->
            val newFilters = state.selectedFilters.toMutableSet()

            if (filter.type == FilterType.NONE) {
                newFilters.clear()
            } else {
                if (newFilters.contains(filter.id)) {
                    newFilters.remove(filter.id)
                } else {
                    newFilters.add(filter.id)
                }
            }

            state.copy(selectedFilters = newFilters)
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

    fun onTaskSwipe(task: TaskRowUi) {
        // TODO: Здесь позже будет реализована логика
        // взятия задачи или изменения её статуса, работайте друзья
    }

    private fun applyFiltersAndGrouping() {
        _uiState.update { state ->
            var filtered = state.allTaskRows
            val uid = currentUid

            if (state.selectedFilters.contains("MINE") && uid != null) {
                filtered = filtered.filter { it.authorId == uid || it.assigneeId == uid }
            }

            val selectedProjectIds =
                state.selectedFilters
                    .filter {
                        it != "MINE" && it != "NONE"
                    }.toSet()

            if (selectedProjectIds.isNotEmpty()) {
                filtered = filtered.filter { it.projectId in selectedProjectIds }
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

    fun refresh() {
        loadTasks()
    }

    private fun groupTasksByStatus(tasks: List<TaskRowUi>): List<TaskGroupUi> {
        val todo = tasks.filter { it.status == TaskStatus.TODO }
        val inProgress = tasks.filter { it.status == TaskStatus.IN_PROGRESS }
        val done = tasks.filter { it.status == TaskStatus.DONE }

        return listOf(
            TaskGroupUi("TODO", R.string.tasks_group_planned, todo.size, isExpanded = true, tasks = todo),
            TaskGroupUi("IN_PROGRESS", R.string.tasks_group_active, inProgress.size, isExpanded = false, tasks = inProgress),
            TaskGroupUi("DONE", R.string.tasks_group_completed, done.size, isExpanded = false, tasks = done),
        )
    }

    private fun groupTasksByDeadline(tasks: List<TaskRowUi>): List<TaskGroupUi> {
        val overdue = tasks.filter { it.isOverdue && !it.isDone }
        val upcoming = tasks.filter { !it.isOverdue && !it.isDone }
        val done = tasks.filter { it.isDone }

        return listOf(
            TaskGroupUi("OVERDUE", R.string.tasks_group_overdue, overdue.size, isExpanded = true, tasks = overdue),
            TaskGroupUi("UPCOMING", R.string.tasks_group_upcoming, upcoming.size, isExpanded = false, tasks = upcoming),
            TaskGroupUi("COMPLETED", R.string.tasks_group_completed_deadline, done.size, isExpanded = false, tasks = done),
        )
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
