package com.example.coflow.repositories.dto

/**
 * DTO создания пользователя.
 *
 * UID не передаётся: репозиторий создаёт документ и возвращает
 * [com.example.coflow.models.UserModel] с заполненным `uid`.
 * `createdAt` и `isDeleted` выставляет репозиторий.
 */
data class UserAddDto(
    val name: String,
    val email: String,
)
