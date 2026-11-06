package com.example.coflow.ui.projects

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.R
import com.example.coflow.models.ProjectModel
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.ProjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProjectRole { OWNER, READER }

// Модель проекта
data class ProjectUiItem(
    val id: String,
    val name: String,
    val role: ProjectRole,
)

// Фильтры для выпадающего меню
enum class ProjectFilter(
    @StringRes val titleResId: Int,
) {
    ALL(R.string.project_filter_all),
    MANAGEMENT(R.string.project_filter_management),
    PARTICIPATION(R.string.project_filter_participation),
}

// Состояние экрана
data class ProjectsUiState(
    val searchQuery: String = "",
    val selectedFilter: ProjectFilter = ProjectFilter.ALL,
    val isFilterMenuExpanded: Boolean = false,
    val projects: List<ProjectUiItem> = emptyList(),
    val activeCount: Int = 0,
)

class ProjectsViewModel(
    private val projectRepository: ProjectRepository = FirebaseProjectRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    // Полный список активных проектов пользователя; фильтрация и поиск применяются к копии.
    private var allProjects: List<ProjectUiItem> = emptyList()
    private var observationJob: Job? = null

    private val _state = MutableStateFlow(ProjectsUiState())
    val state: StateFlow<ProjectsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /**
     * Подписывается на активные проекты владельца и проекты, где пользователь состоит.
     * Удалённые (`isDeleted = true`) проекты в поток не попадают.
     */
    fun refresh() {
        observationJob?.cancel()
        observationJob =
            viewModelScope.launch {
                val uid = authRepository.getCurrentUserUid()

                if (uid.isNullOrBlank()) {
                    allProjects = emptyList()
                    updateFilteredProjects()
                    return@launch
                }

                projectRepository
                    .observeProjectsByUserId(userId = uid)
                    .collect { result ->
                        result.fold(
                            onSuccess = { projects ->
                                allProjects = projects.map { it.toUiItem(currentUid = uid) }
                                updateFilteredProjects()
                            },
                            onFailure = { _ ->
                                // В ProjectsUiState пока нет поля для ошибки.
                                // При сбое оставляем последний успешно загруженный список.
                            },
                        )
                    }
            }
    }

    // Ввод текста в поиск
    fun onSearchQueryChange(newQuery: String) {
        _state.update { it.copy(searchQuery = newQuery) }
        updateFilteredProjects()
    }

    // Открыть/закрыть меню фильтра
    fun onFilterMenuToggle() {
        _state.update { it.copy(isFilterMenuExpanded = !it.isFilterMenuExpanded) }
    }

    // Выбор фильтра
    fun onFilterSelect(filter: ProjectFilter) {
        _state.update { it.copy(selectedFilter = filter, isFilterMenuExpanded = false) }
        updateFilteredProjects()
    }

    // Логика фильтрации
    private fun updateFilteredProjects() {
        val current = _state.value
        val query = current.searchQuery.trim()

        val filteredByRole =
            when (current.selectedFilter) {
                ProjectFilter.ALL -> allProjects
                ProjectFilter.MANAGEMENT -> allProjects.filter { it.role == ProjectRole.OWNER }
                ProjectFilter.PARTICIPATION -> allProjects.filter { it.role != ProjectRole.OWNER }
            }

        val visibleProjects =
            if (query.isEmpty()) {
                filteredByRole
            } else {
                filteredByRole.filter { it.name.contains(query, ignoreCase = true) }
            }

        _state.update {
            it.copy(
                projects = visibleProjects,
                activeCount = allProjects.size,
            )
        }
    }
}

private fun ProjectModel.toUiItem(currentUid: String): ProjectUiItem =
    ProjectUiItem(
        id = id,
        name = title,
        // В доменной модели есть только OWNER/MEMBER, поэтому участник показывается как READER.
        role = if (ownerId == currentUid) ProjectRole.OWNER else ProjectRole.READER,
    )
