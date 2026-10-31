package com.example.coflow.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.models.UserModel
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val username: String = "", // Имя пользователя
    val contact: String = "", // Почта или телефон
    val password: String = "", // Пароль
    val confirmPassword: String = "", // Подтверждение пароля
    val usernameError: String? = null, // Ошибка имени
    val contactError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: UserModel? = null,
    val isAuthorized: Boolean = false,
)

class AuthViewModel(
    private val repository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) {
        _state.value = _state.value.copy(username = value, usernameError = null, errorMessage = null)
    }

    fun onContactChange(value: String) {
        _state.value = _state.value.copy(contact = value, contactError = null, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, passwordError = null, errorMessage = null)
    }

    fun onConfirmChange(value: String) {
        _state.value = _state.value.copy(confirmPassword = value, confirmError = null, errorMessage = null)
    }

    fun onToggleMode() {
        _state.value =
            _state.value.copy(
                mode = if (_state.value.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN,
                username = "",
                contact = "",
                password = "",
                confirmPassword = "",
                usernameError = null,
                contactError = null,
                passwordError = null,
                confirmError = null,
                errorMessage = null,
            )
    }

    fun onSubmit() {
        if (_state.value.isSubmitting) return

        val current = _state.value

        // Валидация имени (только для регистрации)
        val usernameError =
            if (current.mode == AuthMode.REGISTER && current.username.isBlank()) {
                "Введите имя пользователя"
            } else if (current.mode == AuthMode.REGISTER && current.username.length < 2) {
                "Имя слишком короткое"
            } else {
                null
            }

        // Валидация Email
        val contactError =
            when {
                current.contact.isBlank() -> "Введите почту"
                !Patterns.EMAIL_ADDRESS.matcher(current.contact).matches() -> "Неверный формат email"
                else -> null
            }

        // Валидация пароля (минимум 8 символов, хотя бы одна буква и цифра)
        val passwordError =
            when {
                current.mode == AuthMode.REGISTER && current.password.length < 8 -> "Пароль должен быть не менее 8 символов"
                current.mode == AuthMode.REGISTER && !current.password.any { it.isDigit() } -> "Пароль должен содержать хотя бы одну цифру"
                current.mode == AuthMode.REGISTER && !current.password.any { it.isLetter() } -> "Пароль должен содержать хотя бы одну букву"
                else -> null
            }

        // Валидация подтверждения пароля
        val confirmError =
            if (current.mode == AuthMode.REGISTER && current.password != current.confirmPassword) {
                "Пароли не совпадают"
            } else {
                null
            }

        if (usernameError != null || contactError != null || passwordError != null || confirmError != null) {
            _state.value =
                _state.value.copy(
                    usernameError = usernameError,
                    contactError = contactError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                    isSubmitting = false,
                )
            return
        }

        _state.value =
            _state.value.copy(
                isSubmitting = true,
                usernameError = null,
                contactError = null,
                passwordError = null,
                confirmError = null,
                errorMessage = null,
            )

        // Вызов бэкенда
        viewModelScope.launch {
            val result =
                if (current.mode == AuthMode.REGISTER) {
                    repository.register(current.contact, current.password, current.username)
                } else {
                    repository.login(current.contact, current.password)
                }

            result.fold(
                onSuccess = { user ->
                    _state.value =
                        _state.value.copy(
                            isSubmitting = false,
                            currentUser = user,
                            isAuthorized = true,
                        )
                },
                onFailure = { error ->
                    _state.value =
                        _state.value.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "Неизвестная ошибка",
                        )
                },
            )
        }
    }
}
