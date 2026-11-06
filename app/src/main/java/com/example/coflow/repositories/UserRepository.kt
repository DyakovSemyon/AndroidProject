package com.example.coflow.repositories

import com.example.coflow.models.UserModel
import com.example.coflow.repositories.dto.UserAddDto
import com.example.coflow.repositories.dto.UserUpdateDto

/**
 * Пользователи (коллекция `users`).
 *
 * Единый стандарт изменения данных:
 * - [addUser] — создание пользователя;
 * - [updateUser] — частичное обновление по UID;
 * - [deleteUser] — удаление.
 *
 * В `updateUser` поля [UserUpdateDto], равные `null`, не меняются.
 *
 * ```kotlin
 * val user = repository.addUser(
 *     UserAddDto(name = "Иван", email = "ivan@example.com"),
 * ).getOrThrow()
 *
 * repository.updateUser(
 *     id = user.uid,
 *     update = UserUpdateDto(name = "Иван Петров"),
 * )
 * ```
 *
 * [deleteUser] физически удаляет документ. Поле `isDeleted` читается и
 * записывается как `is_deleted`, но сейчас не используется для мягкого удаления.
 */
interface UserRepository {
    /** Возвращает пользователя по UID или [Result.failure], если его нет. */
    suspend fun getById(id: String): Result<UserModel>

    /** Ищет пользователя по email; возвращает [Result.failure], если не найден. */
    suspend fun getByEmail(email: String): Result<UserModel>

    /**
     * Создаёт пользователя из [UserAddDto].
     * UID генерирует репозиторий, поэтому в DTO его нет.
     * Возвращает сохранённую модель с заполненным `uid`.
     */
    suspend fun addUser(user: UserAddDto): Result<UserModel>

    /**
     * Частично обновляет пользователя [id].
     *
     * Меняются только поля [UserUpdateDto], отличные от `null`.
     * Если не передано ни одного поля, вернёт [Result.failure].
     */
    suspend fun updateUser(
        id: String,
        update: UserUpdateDto,
    ): Result<UserModel>

    /** Удаляет документ пользователя по UID. */
    suspend fun deleteUser(id: String): Result<Unit>
}
