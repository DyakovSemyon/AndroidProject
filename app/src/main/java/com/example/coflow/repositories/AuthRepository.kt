package com.example.coflow.repositories

import com.example.coflow.models.UserModel

/**
 * Авторизация через Firebase Auth и профиль пользователя в Firestore.
 *
 * Все методы suspend. Вызывайте их из `viewModelScope` и обрабатывайте `Result`:
 * ```kotlin
 * viewModelScope.launch {
 *     repository.login(email, password)
 *         .onSuccess { user -> /* сохранить user в состоянии */ }
 *         .onFailure { error -> /* показать ошибку */ }
 * }
 * ```
 *
 * `register()` создаёт пользователя в Auth и документ `users/{uid}`.
 * `login()` и `signInWithGoogle()` возвращают профиль из `users`.
 * Для изменения имени или других данных профиля используйте
 * [UserRepository.updateUser] с существующим `uid`.
 */
interface AuthRepository {
    /**
     * Регистрирует пользователя в Firebase Auth и создаёт профиль в Firestore.
     *
     * @return [Result.success] с профилем или [Result.failure] с текстом ошибки.
     */
    suspend fun register(
        email: String,
        password: String,
        name: String,
    ): Result<UserModel>

    /**
     * Выполняет вход и читает профиль из Firestore.
     *
     * @return [Result.success] с профилем или [Result.failure] с текстом ошибки.
     */
    suspend fun login(
        email: String,
        password: String,
    ): Result<UserModel>

    /**
     * Авторизация через Google.
     *
     * @param idToken Токен, полученный от Google Sign-In API.
     * @return [Result.success] с профилем или [Result.failure] с ошибкой.
     */
    suspend fun signInWithGoogle(idToken: String): Result<UserModel>

    /** Выход из системы. Firestore-профиль при этом не удаляется. */
    fun logout()

    /** UID текущего пользователя Auth или null. */
    fun getCurrentUserUid(): String?
}
