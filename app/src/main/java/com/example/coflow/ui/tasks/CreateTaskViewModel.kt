package com.example.coflow.ui.tasks

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.models.TaskStatus
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseTaskRepositoryImpl
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.TaskRepository
import com.example.coflow.repositories.dto.TaskAddDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

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
    // --- Дата и время ---
    val selectedDueDate: LocalDate = LocalDate.now(),
    val calendarMonth: YearMonth = YearMonth.now(),
    val dueDate: String = "",
    // --- Проект ---
    val project: String = "",
    val allProjects: List<String> = emptyList(),
    val projectSearchQuery: String = "",
    val pendingProject: String = "",
    // --- Статус и валидация ---
    val titleError: String? = null,
    val descriptionError: String? = null,
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
    val canConfirmProject: Boolean get() = pendingProject.isNotBlank()

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
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ViewModel() {
    private val _state = MutableStateFlow(CreateTaskUiState())
    val state: StateFlow<CreateTaskUiState> = _state.asStateFlow()

    init {
        loadAssignees()
        loadProjects()
    }

    private fun loadAssignees() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").get().await()
                val assignees =
                    snapshot.documents.mapNotNull { doc ->
                        val uid = doc.id
                        val name = doc.getString("name").orEmpty()
                        val email = doc.getString("email").orEmpty()
                        val isDeleted = doc.getBoolean("is_deleted") ?: doc.getBoolean("isDeleted") ?: false
                        if (isDeleted) return@mapNotNull null

                        val displayName = name.ifBlank { email }
                        val initials = getInitials(displayName)
                        val avatarColor = getAvatarColor(uid)

                        Assignee(
                            id = uid,
                            name = displayName,
                            email = email,
                            initials = initials,
                            avatarColor = avatarColor,
                        )
                    }
                _state.update { it.copy(allAssignees = assignees) }
            } catch (_: Exception) {
                // При ошибке сетевого запроса список остаётся пустым
            }
        }
    }

    private fun loadProjects() {
        viewModelScope.launch {
            try {
                val uid = authRepository.getCurrentUserUid().orEmpty()
                val projectList = mutableListOf<String>()

                if (uid.isNotBlank()) {
                    val userProjectsResult = projectRepository.getByUserId(uid)
                    val ownerProjectsResult = projectRepository.getByOwnerId(uid)

                    val projects =
                        (
                            userProjectsResult.getOrDefault(emptyList()) +
                                ownerProjectsResult.getOrDefault(emptyList())
                        ).distinctBy { it.id }
                            .filterNot { it.isDeleted }
                            .map { it.title }

                    projectList.addAll(projects)
                }

                _state.update { it.copy(allProjects = projectList.distinct()) }
            } catch (_: Exception) {
                // При ошибке список остаётся пустым
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
        _state.update { it.copy(title = value, titleError = null) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value.take(250), descriptionError = null) }
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
        _state.update { it.copy(pendingProject = project) }
    }

    fun confirmProjectSelection(project: String) {
        if (project.isNotBlank()) {
            _state.update { it.copy(project = project) }
        }
        _state.update { it.copy(pendingProject = "") }
    }

    fun cancelProjectSelection() {
        _state.update { it.copy(pendingProject = "") }
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

        var hasError = false
        var titleErr: String? = null
        var descErr: String? = null

        // Валидация: название — обязательное поле
        if (current.title.isBlank()) {
            titleErr = "Название задачи не может быть пустым"
            hasError = true
        }

        // Валидация: ограничение описания 250 символами
        if (current.description.length > 250) {
            descErr = "Описание не должно превышать 250 символов"
            hasError = true
        }

        if (hasError) {
            _state.update {
                it.copy(
                    titleError = titleErr,
                    descriptionError = descErr,
                )
            }
            return
        }

        _state.update {
            it.copy(
                isSubmitting = true,
                titleError = null,
                descriptionError = null,
            )
        }

        viewModelScope.launch {
            val authorId = authRepository.getCurrentUserUid().orEmpty()
            val assigneeId =
                current.assignees
                    .firstOrNull()
                    ?.id
                    .orEmpty()

            var projectId = ""
            if (current.project.isNotBlank() && authorId.isNotBlank()) {
                val userProjects = projectRepository.getByUserId(authorId).getOrDefault(emptyList())
                val ownerProjects = projectRepository.getByOwnerId(authorId).getOrDefault(emptyList())
                val allProjectsList = (userProjects + ownerProjects).distinctBy { it.id }
                projectId = allProjectsList.firstOrNull { it.title == current.project }?.id.orEmpty()
            }

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
                            titleError = error.message ?: "Ошибка создания задачи",
                        )
                    }
                },
            )
        }
    }
}
