package com.example.coflow.repositories.dto

import com.example.coflow.models.TaskStatus
import java.time.LocalDateTime

/**
 * DTO создания задачи.
 *
 * ID не передаётся: репозиторий создаёт документ и возвращает [com.example.coflow.models.TaskModel]
 * с уже заполненным `id`.
 */
data class TaskAddDto(
    val title: String = "",
    val description: String = "",
    val projectId: String = "",
    val authorId: String = "",
    val assigneeId: String = "",
    val deadline: LocalDateTime = LocalDateTime.now(),
    val status: TaskStatus = TaskStatus.TODO,
)
