package com.example.coflow.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.models.Role
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseMembershipRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.MembershipRepository
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.UserRepository
import com.example.coflow.repositories.dto.MembershipAddDto
import com.example.coflow.repositories.dto.ProjectAddDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Участник, которого приглашают в проект.
 *
 * [initials] заполняются, только если пользователь с такой почтой уже
 * зарегистрирован. Для остальных аватарка остаётся дефолтной.
 */
data class ProjectParticipant(
    val email: String,
    val initials: String? = null,
)

/**
 * Результат создания проекта.
 *
 * [unresolvedEmails] — адреса, для которых не удалось создать членство
 * (пользователь ещё не зарегистрирован или произошла ошибка записи).
 */
data class CreateProjectResult(
    val projectId: String,
    val unresolvedEmails: List<String> = emptyList(),
)

data class CreateProjectUiState(
    val title: String = "",
    val description: String = "",
    val participants: List<ProjectParticipant> = emptyList(),
    // --- Диалог отмены приглашения ---
    val participantPendingRemoval: ProjectParticipant? = null,
    // --- Статус отправки ---
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Кнопка «Создать проект» активна, когда заполнено название. */
    val canCreate: Boolean
        get() = title.isNotBlank() && !isSubmitting
}

class CreateProjectViewModel(
    private val projectRepository: ProjectRepository = FirebaseProjectRepositoryImpl(),
    private val membershipRepository: MembershipRepository = FirebaseMembershipRepositoryImpl(),
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    private val _state = MutableStateFlow(CreateProjectUiState())
    val state: StateFlow<CreateProjectUiState> = _state.asStateFlow()

    // --- Основные поля формы ---

    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value, errorMessage = null) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value.take(MAX_DESCRIPTION_LENGTH), errorMessage = null) }
    }

    // --- Добавление участника ---

    /**
     * Заглушка: здесь позже будет открываться отдельное окно добавления участника.
     *
     * Окно передаст выбранную почту в [addParticipant], чтобы ViewModel
     * проверила пользователя и подготовила инициалы для аватарки.
     */
    fun onAddParticipantClick() = Unit

    /**
     * Добавляет участника по почте.
     *
     * Если пользователь уже зарегистрирован, для него вычисляются инициалы.
     * Если нет — участник остаётся с дефолтной аватаркой.
     */
    fun addParticipant(email: String) {
        val normalizedEmail = email.trim()

        if (!EMAIL_REGEX.matches(normalizedEmail)) return
        if (_state.value.participants.any { it.email.equals(normalizedEmail, ignoreCase = true) }) return

        viewModelScope.launch {
            val user = userRepository.getByEmail(normalizedEmail).getOrNull()
            val participant =
                if (user != null && !user.isDeleted && user.uid.isNotBlank()) {
                    ProjectParticipant(
                        email = normalizedEmail,
                        initials = getInitials(user.name.ifBlank { user.email.ifBlank { normalizedEmail } }),
                    )
                } else {
                    ProjectParticipant(email = normalizedEmail)
                }

            _state.update { current ->
                if (current.participants.any { it.email.equals(normalizedEmail, ignoreCase = true) }) {
                    current
                } else {
                    current.copy(participants = current.participants + participant)
                }
            }
        }
    }

    private fun getInitials(value: String): String {
        val parts = value.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        }
    }

    // --- Отмена приглашения ---

    fun onRemoveParticipantClick(participant: ProjectParticipant) {
        _state.update { it.copy(participantPendingRemoval = participant) }
    }

    fun onDismissRemoveParticipantDialog() {
        _state.update { it.copy(participantPendingRemoval = null) }
    }

    fun onConfirmRemoveParticipant() {
        val participant = _state.value.participantPendingRemoval ?: return
        _state.update {
            it.copy(
                participants =
                    it.participants.filterNot { p ->
                        p.email.equals(participant.email, ignoreCase = true)
                    },
                participantPendingRemoval = null,
            )
        }
    }

    // --- Создание проекта ---

    fun onCreateProject(onProjectCreated: (CreateProjectResult) -> Unit) {
        val current = _state.value
        val title = current.title.trim()

        if (title.isBlank()) {
            _state.update { it.copy(errorMessage = "Название проекта не может быть пустым") }
            return
        }

        if (current.isSubmitting) return

        _state.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val ownerId = authRepository.getCurrentUserUid()
            if (ownerId.isNullOrBlank()) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Пользователь не авторизован",
                    )
                }
                return@launch
            }

            projectRepository
                .addProject(
                    ProjectAddDto(
                        ownerId = ownerId,
                        title = title,
                        description = current.description.trim(),
                    ),
                ).fold(
                    onSuccess = { project ->
                        val unresolvedEmails = addParticipants(project.id, current.participants)
                        _state.update { it.copy(isSubmitting = false) }
                        onProjectCreated(
                            CreateProjectResult(
                                projectId = project.id,
                                unresolvedEmails = unresolvedEmails,
                            ),
                        )
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = error.message ?: "Ошибка создания проекта",
                            )
                        }
                    },
                )
        }
    }

    /**
     * Создаёт членство для каждого приглашённого.
     *
     * Коллекции приглашений пока нет, поэтому членство можно сохранить только
     * для уже зарегистрированных пользователей. Остальные адреса возвращаются,
     * чтобы экран мог сообщить, кому приглашение не отправлено.
     */
    private suspend fun addParticipants(
        projectId: String,
        participants: List<ProjectParticipant>,
    ): List<String> {
        val unresolved = mutableListOf<String>()

        participants.forEach { participant ->
            val user = userRepository.getByEmail(participant.email).getOrNull()

            if (user == null || user.isDeleted || user.uid.isBlank()) {
                unresolved += participant.email
                return@forEach
            }

            membershipRepository
                .addMember(
                    MembershipAddDto(
                        projectId = projectId,
                        userId = user.uid,
                        role = Role.MEMBER,
                    ),
                ).onFailure {
                    unresolved += participant.email
                }
        }

        return unresolved
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private companion object {
        const val MAX_DESCRIPTION_LENGTH = 250

        val EMAIL_REGEX =
            Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    }
}
