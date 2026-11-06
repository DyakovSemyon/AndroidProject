package com.example.coflow.ui.tasks

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.models.TaskStatus
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseMembershipRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseTaskRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.MembershipRepository
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.TaskRepository
import com.example.coflow.repositories.UserRepository
import com.example.coflow.repositories.dto.TaskAddDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

// Модель элемента проекта (ID + название).
data class ProjectItem(
    val id: String,
    val title: String,
)

// Модель данных исполнителя задачи.
data class Assignee(
    val id: String,
    val name: String,
    val email: String,
    val initials: String,
    val avatarColor: Color,
)

// Модель задачи
data class CreateTaskFormData(
    val title: String,
    val description: String,
    val assignees: List<Assignee>,
    val project: String,
    val dueDate: String,
)

data class CreateTaskUiState(
    // --- Основные поля формы ---
    val title: String = "",
    val description: String = "",
    // --- Исполнители ---
    val assignees: List<Assignee> = emptyList(), // Уже подтверждённые исполнители
    val allAssignees: List<Assignee> = emptyList(), // Полный список доступных исполнителей
    val assigneeSearchQuery: String = "", // Текущий текст в поле поиска
    val pendingAssigneeIds: Set<String> = emptySet(), // Временно выбранные ID на экране выбора
    val isLoadingAssignees: Boolean = false,
    // --- Дата и время ---
    val selectedDueDate: LocalDate = LocalDate.now(),
    val calendarMonth: YearMonth = YearMonth.now(),
    val dueDate: String = "",
    // --- Проект ---
    val selectedProjectId: String = "",
    val project: String = "",
    val allProjects: List<String> = emptyList(),
    val allProjectItems: List<ProjectItem> = emptyList(),
    val projectSearchQuery: String = "",
    val pendingProjectId: String = "",
    val pendingProject: String = "",
    val isLoadingProjects: Boolean = false,
    // --- Статус и ошибки ---
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
) {
    /** Отфильтрованный список исполнителей на основе поискового запроса */
    val filteredAssignees: List<Assignee>
        get() =
            if (assigneeSearchQuery.isBlank()) {
                allAssignees
            } else {
                allAssignees.filter {
                    it.name.contains(assigneeSearchQuery, ignoreCase = true) ||
                        it.email.contains(assigneeSearchQuery, ignoreCase = true)
                }
            }

    /** Отфильтрованный список проектов на основе поискового запроса. */
    val filteredProjects: List<String>
        get() =
            if (projectSearchQuery.isBlank()) {
                allProjects
            } else {
                allProjects.filter { it.contains(projectSearchQuery, ignoreCase = true) }
            }

    /** Количество временно выбранных исполнителей (для отображения в UI). */
    val pendingSelectedCount: Int
        get() = pendingAssigneeIds.size

    /** Кнопка "Готово" на экране выбора исполнителей активна, если выбран хотя бы один. */
    val canConfirmAssignees: Boolean
        get() = pendingAssigneeIds.isNotEmpty()

    /** Кнопка "Готово" на экране выбора проекта активна, если проект выбран. */
    val canConfirmProject: Boolean get() = pendingProject.isNotBlank() || pendingProjectId.isNotBlank()

    /**
     * Главная кнопка "Создать задачу" активна только при заполнении обязательного поля (название)
     * и когда не идет процесс отправки.
     */
    val canCreate: Boolean
        get() = title.isNotBlank() && !isSubmitting
}

/**
 * ViewModel для управления состоянием создания задачи.
 * Координирует данные между основным экраном и экранами выбора (исполнитель, проект, дата).
 */
class CreateTaskViewModel(
    private val taskRepository: TaskRepository = FirebaseTaskRepositoryImpl(),
    private val projectRepository: ProjectRepository = FirebaseProjectRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val membershipRepository: MembershipRepository = FirebaseMembershipRepositoryImpl(),
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
) : ViewModel() {
    private val _state = MutableStateFlow(CreateTaskUiState())
    val state: StateFlow<CreateTaskUiState> = _state.asStateFlow()

    init {
        loadAssignees()
        loadProjects()
    }

    private fun loadAssignees(projectId: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingAssignees = true, errorMessage = null) }
            try {
                val currentUid = authRepository.getCurrentUserUid().orEmpty()
                val userIds = mutableSetOf<String>()

                if (currentUid.isNotBlank()) {
                    userIds.add(currentUid)

                    val targetProjectId = projectId ?: _state.value.selectedProjectId

                    val projectIds =
                        if (targetProjectId.isNotBlank()) {
                            listOf(targetProjectId)
                        } else {
                            val userMemberships = membershipRepository.getByUserId(currentUid).getOrDefault(emptyList())
                            val ownerProjects = projectRepository.getByOwnerId(currentUid).getOrDefault(emptyList())
                            val userProjects = projectRepository.getByUserId(currentUid).getOrDefault(emptyList())
                            (userMemberships.map { it.projectId } + ownerProjects.map { it.id } + userProjects.map { it.id })
                                .filter { it.isNotBlank() }
                                .distinct()
                        }

                    for (pId in projectIds) {
                        projectRepository.getById(pId).getOrNull()?.let { proj ->
                            if (proj.ownerId.isNotBlank()) {
                                userIds.add(proj.ownerId)
                            }
                        }
                        val membershipsResult = membershipRepository.getByProjectId(pId)
                        membershipsResult.getOrNull()?.forEach { membership ->
                            if (membership.userId.isNotBlank()) {
                                userIds.add(membership.userId)
                            }
                        }
                    }
                }

                val assignees = mutableListOf<Assignee>()
                for (uid in userIds) {
                    val userResult = userRepository.getById(uid)
                    val user = userResult.getOrNull()
                    val isDeleted = user?.isDeleted ?: false
                    if (!isDeleted) {
                        val displayName =
                            user?.name?.ifBlank { user.email }
                                ?: if (uid == currentUid) "Я" else "Пользователь (${uid.take(6)})"
                        val email = user?.email.orEmpty()
                        val initials = getInitials(displayName)
                        val avatarColor = getAvatarColor(uid)
                        assignees.add(
                            Assignee(
                                id = uid,
                                name = displayName,
                                email = email,
                                initials = initials,
                                avatarColor = avatarColor,
                            ),
                        )
                    }
                }

                _state.update {
                    it.copy(
                        allAssignees = assignees,
                        isLoadingAssignees = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingAssignees = false,
                        errorMessage = "Ошибка загрузки исполнителей: ${e.message ?: "неизвестная ошибка"}",
                    )
                }
            }
        }
    }

    private fun loadProjects() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingProjects = true, errorMessage = null) }
            try {
                val uid = authRepository.getCurrentUserUid().orEmpty()
                val projectItems = mutableListOf<ProjectItem>()

                if (uid.isNotBlank()) {
                    val userProjectsResult = projectRepository.getByUserId(uid)
                    val ownerProjectsResult = projectRepository.getByOwnerId(uid)

                    if (userProjectsResult.isFailure && ownerProjectsResult.isFailure) {
                        val error = userProjectsResult.exceptionOrNull() ?: ownerProjectsResult.exceptionOrNull()
                        _state.update {
                            it.copy(
                                isLoadingProjects = false,
                                errorMessage = "Ошибка загрузки проектов: ${error?.message ?: "неизвестная ошибка"}",
                            )
                        }
                        return@launch
                    }

                    val projects =
                        (
                            userProjectsResult.getOrDefault(emptyList()) +
                                ownerProjectsResult.getOrDefault(emptyList())
                        ).distinctBy { it.id }
                            .filterNot { it.isDeleted }
                            .map { ProjectItem(id = it.id, title = it.title) }

                    projectItems.addAll(projects)
                }

                _state.update {
                    it.copy(
                        allProjectItems = projectItems,
                        allProjects = projectItems.map { item -> item.title }.distinct(),
                        isLoadingProjects = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingProjects = false,
                        errorMessage = "Ошибка загрузки проектов: ${e.message ?: "неизвестная ошибка"}",
                    )
                }
            }
        }
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "??"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        }
    }

    private fun getAvatarColor(id: String): Color {
        val colors =
            listOf(
                Color(0xFF6B9AF3),
                Color(0xFFA3B8CC),
                Color(0xFF86D3B5),
                Color(0xFF8BB8F8),
                Color(0xFFAFA3F8),
                Color(0xFFF39C12),
                Color(0xFFE74C3C),
                Color(0xFF9B59B6),
            )
        val hash = abs(id.hashCode())
        return colors[hash % colors.size]
    }

    // --- Обработчики основных полей ---
    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value.take(250)) }
    }

    // --- Обработчики экрана выбора исполнителей ---
    fun onAssigneeSearchQueryChange(query: String) {
        _state.update { it.copy(assigneeSearchQuery = query) }
    }

    fun togglePendingAssignee(assigneeId: String) {
        _state.update { current ->
            val pending =
                if (assigneeId in current.pendingAssigneeIds) {
                    current.pendingAssigneeIds - assigneeId
                } else {
                    current.pendingAssigneeIds + assigneeId
                }
            current.copy(pendingAssigneeIds = pending)
        }
    }

    fun confirmAssigneeSelection(assignees: List<Assignee>) {
        _state.update { it.copy(assignees = assignees, pendingAssigneeIds = emptySet()) }
    }

    fun cancelAssigneeSelection() {
        _state.update {
            it.copy(
                pendingAssigneeIds = emptySet(),
                assigneeSearchQuery = "",
            )
        }
    }

    // --- Обработчики экрана выбора проекта ---
    fun onProjectSearchQueryChange(query: String) {
        _state.update { it.copy(projectSearchQuery = query) }
    }

    fun onPendingProjectChange(project: String) {
        val matchedItem = _state.value.allProjectItems.firstOrNull { it.id == project || it.title == project }
        _state.update {
            it.copy(
                pendingProject = matchedItem?.title ?: project,
                pendingProjectId = matchedItem?.id ?: "",
            )
        }
    }

    fun confirmProjectSelection(project: String) {
        val matchedItem = _state.value.allProjectItems.firstOrNull { it.id == project || it.title == project }
        val finalTitle = matchedItem?.title ?: project
        val finalId = matchedItem?.id ?: _state.value.pendingProjectId

        if (finalTitle.isNotBlank() || finalId.isNotBlank()) {
            _state.update {
                it.copy(
                    project = finalTitle,
                    selectedProjectId = finalId,
                )
            }
            loadAssignees(finalId)
        }
        _state.update { it.copy(pendingProject = "", pendingProjectId = "") }
    }

    fun cancelProjectSelection() {
        _state.update { it.copy(pendingProject = "", pendingProjectId = "") }
    }

    // --- Обработчики экрана выбора даты ---
    fun onCalendarMonthChange(month: YearMonth) {
        _state.update { it.copy(calendarMonth = month) }
    }

    fun onDateSelected(date: LocalDate) {
        _state.update { it.copy(selectedDueDate = date) }
    }

    fun confirmDateSelection(dateString: String) {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        _state.update { it.copy(selectedDueDate = date, dueDate = dateString) }
    }

    // --- Финальное действие ---
    fun onCreateTask(onTaskCreated: (CreateTaskFormData) -> Unit) {
        val current = _state.value
        if (current.title.isBlank()) {
            _state.update { it.copy(errorMessage = "Название задачи не может быть пустым") }
            return
        }

        _state.update {
            it.copy(
                isSubmitting = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            val authorId = authRepository.getCurrentUserUid().orEmpty()
            val assigneeId = current.assignees.joinToString(",") { it.id }
            val projectId = current.selectedProjectId

            val deadlineDateTime =
                try {
                    LocalDateTime.of(current.selectedDueDate, LocalTime.MIDNIGHT)
                } catch (_: Exception) {
                    LocalDateTime.now()
                }

            val taskDto =
                TaskAddDto(
                    title = current.title.trim(),
                    description = current.description.trim(),
                    projectId = projectId,
                    authorId = authorId,
                    assigneeId = assigneeId,
                    deadline = deadlineDateTime,
                    status = TaskStatus.TODO,
                )

            val result = taskRepository.addTask(taskDto)

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isSubmitting = false) }
                    onTaskCreated(
                        CreateTaskFormData(
                            title = current.title.trim(),
                            description = current.description.trim(),
                            assignees = current.assignees,
                            project = current.project.trim(),
                            dueDate = current.dueDate.trim(),
                        ),
                    )
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "Ошибка создания задачи",
                        )
                    }
                },
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
