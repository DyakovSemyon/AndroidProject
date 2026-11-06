package com.example.coflow.repositories.dto

/**
 * DTO создания проекта.
 *
 * ID не передаётся: репозиторий создаёт документ и возвращает
 * [com.example.coflow.models.ProjectModel] с заполненным `id`.
 * `updatedAt` и `isDeleted` выставляет репозиторий.
 */
data class ProjectAddDto(
    val ownerId: String,
    val title: String = "",
    val description: String = "",
)
