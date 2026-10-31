package com.example.coflow.repositories

import com.example.coflow.models.TaskModel
import com.example.coflow.repositories.dto.TaskAddDto
import com.example.coflow.repositories.dto.TaskUpdateDto

/**
 * Репозиторий задач (коллекция `tasks`).
 *
 * Единый стандарт изменения данных:
 * - [addTask] — создание нового документа;
 * - [updateTask] — частичное обновление по `id`;
 * - [deleteTask] — удаление.
 *
 * В `updateTask` поля [TaskUpdateDto], равные `null`, не меняются.
 *
 * ```kotlin
 * val task = repository.addTask(
 *     TaskAddDto(title = "Новая задача", projectId = projectId),
 * ).getOrThrow()
 *
 * repository.updateTask(
 *     id = task.id,
 *     update = TaskUpdateDto(status = TaskStatus.DONE),
 * )
 * ```
 */
interface TaskRepository {
    /** Возвращает задачу или [Result.failure], если документа нет. */
    suspend fun getById(id: String): Result<TaskModel>

    /** Возвращает список задач с указанным заголовком; пустой список — норма. */
    suspend fun getByTitle(title: String): Result<List<TaskModel>>

    /** Возвращает задачи проекта по строковому `projectId`; пустой список — норма. */
    suspend fun getByProjectId(id: String): Result<List<TaskModel>>

    /**
     * Создаёт задачу из [TaskAddDto].
     * ID генерирует репозиторий, поэтому в DTO его нет.
     * Возвращает сохранённую модель с заполненным `id`.
     */
    suspend fun addTask(task: TaskAddDto): Result<TaskModel>

    /**
     * Частично обновляет задачу [id].
     *
     * Меняются только поля [TaskUpdateDto], отличные от `null`.
     * Если не передано ни одного поля, вернёт [Result.failure].
     */
    suspend fun updateTask(
        id: String,
        update: TaskUpdateDto,
    ): Result<TaskModel>

    /** Удаляет задачу по ID. Возвращает [Result.failure], если удалить не удалось. */
    suspend fun deleteTask(id: String): Result<Unit>
}
