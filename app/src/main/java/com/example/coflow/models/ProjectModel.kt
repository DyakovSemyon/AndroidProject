package com.example.coflow.models

import java.time.LocalDateTime

/**
 * Доменная модель проекта.
 *
 * [updatedAt] хранится в Firestore как Timestamp, преобразование выполняет
 * ProjectRepository.
 */
data class ProjectModel(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isDeleted: Boolean = false,
)
