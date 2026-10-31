package com.example.coflow.ui.projects

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.coflow.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Статусы для UI
enum class ProjectStatus { IN_PROGRESS, ARCHIVE }

enum class ProjectRole { OWNER, EDITOR, READER }

// Модель проекта
data class ProjectUiItem(
    val id: String,
    val name: String,
    val status: ProjectStatus,
    val role: ProjectRole,
)

// Фильтры для выпадающего меню
enum class ProjectFilter(
    @StringRes val titleResId: Int,
) {
    ALL(R.string.project_filter_all),
    MANAGEMENT(R.string.project_filter_management),
    PARTICIPATION(R.string.project_filter_participation),
    ARCHIVE(R.string.project_filter_archive),
}

// Состояние экрана
data class ProjectsUiState(
    val searchQuery: String = "",
    val selectedFilter: ProjectFilter = ProjectFilter.ALL,
    val isFilterMenuExpanded: Boolean = false,
    val projects: List<ProjectUiItem> = emptyList(),
    val activeCount: Int = 0,
    val archivedCount: Int = 0,
)

class ProjectsViewModel : ViewModel() {
    private val stubProjects =
        listOf(
            ProjectUiItem("stub-1", "Mock Project", ProjectStatus.IN_PROGRESS, ProjectRole.OWNER),
            ProjectUiItem("stub-2", "Mock Archive", ProjectStatus.ARCHIVE, ProjectRole.READER),
        )

    private val _state =
        MutableStateFlow(
            ProjectsUiState(
                projects = stubProjects,
                activeCount = stubProjects.count { it.status == ProjectStatus.IN_PROGRESS },
                archivedCount = stubProjects.count { it.status == ProjectStatus.ARCHIVE },
            ),
        )
    val state: StateFlow<ProjectsUiState> = _state.asStateFlow()

    init {
        updateFilteredProjects()
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
        // ЗАГЛУШКА: Логика временно отключена
    }
}
