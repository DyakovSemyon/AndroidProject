package com.example.coflow.repositories.dto

/**
 * DTO для частичного обновления проекта.
 *
 * Поле, оставленное `null`, не меняется.
 * `updatedAt` репозиторий обновляет автоматически.
 */
data class ProjectUpdateDto(
    val title: String? = null,
    val description: String? = null,
)
