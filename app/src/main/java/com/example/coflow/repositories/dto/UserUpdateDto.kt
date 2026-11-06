package com.example.coflow.repositories.dto

/**
 * DTO для частичного обновления пользователя.
 *
 * Поле, оставленное `null`, не меняется. `uid` и `createdAt` не редактируются.
 */
data class UserUpdateDto(
    val name: String? = null,
    val email: String? = null,
)
