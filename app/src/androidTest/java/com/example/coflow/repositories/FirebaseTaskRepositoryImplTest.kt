package com.example.coflow.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coflow.models.TaskStatus
import com.example.coflow.repositories.dto.TaskAddDto
import com.example.coflow.repositories.dto.TaskUpdateDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * Интеграционные тесты репозитория задач на Firestore Emulator.
 *
 * Проверяют полный сценарий: создание, чтение по ID/заголовку/проекту,
 * обновление и удаление.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseTaskRepositoryImplTest {
    private lateinit var repository: TaskRepository

    @Before
    fun setup() {
        FirebaseEmulatorConfig.connect()
        repository = FirebaseTaskRepositoryImpl()
    }

    @Test
    fun addReadUpdateQueryAndDeleteWorkflow() {
        runBlocking {
            val uniqueSuffix = System.currentTimeMillis()
            val title = "Тестовая задача $uniqueSuffix"
            val projectId = "test_project_$uniqueSuffix"
            val deadline = LocalDateTime.of(2026, 1, 1, 12, 0)
            val task =
                TaskAddDto(
                    title = title,
                    description = "Описание задачи для интеграционного теста",
                    projectId = projectId,
                    authorId = "test_author",
                    assigneeId = "test_assignee",
                    deadline = deadline,
                    status = TaskStatus.IN_PROGRESS,
                )

            val created = repository.addTask(task).getOrThrow()
            try {
                assertTrue("ID созданной задачи должен быть заполнен", created.id.isNotBlank())
                assertEquals(title, created.title)
                assertEquals(projectId, created.projectId)
                assertEquals("test_author", created.authorId)
                assertEquals("test_assignee", created.assigneeId)
                assertEquals(deadline, created.deadline)
                assertEquals(TaskStatus.IN_PROGRESS, created.status)

                val fetchedById = repository.getById(created.id).getOrThrow()
                assertEquals(title, fetchedById.title)
                assertEquals(TaskStatus.IN_PROGRESS, fetchedById.status)

                val byTitle = repository.getByTitle(title).getOrThrow()
                assertTrue(
                    "Задача должна находиться по заголовку",
                    byTitle.any { it.id == created.id },
                )

                val byProject = repository.getByProjectId(projectId).getOrThrow()
                assertTrue(
                    "Задача должна находиться по проекту",
                    byProject.any { it.id == created.id },
                )

                val newTitle = "$title — обновлено"
                val newDeadline = deadline.plusDays(1)
                val updated =
                    repository
                        .updateTask(
                            id = created.id,
                            update =
                                TaskUpdateDto(
                                    title = newTitle,
                                    description = "Обновлённое описание",
                                    deadline = newDeadline,
                                    status = TaskStatus.DONE,
                                ),
                        ).getOrThrow()

                assertEquals(created.id, updated.id)
                assertEquals(newTitle, updated.title)
                assertEquals(newDeadline, updated.deadline)
                assertEquals(TaskStatus.DONE, updated.status)
                assertEquals("test_assignee", updated.assigneeId)

                val reloaded = repository.getById(created.id).getOrThrow()
                assertEquals(newTitle, reloaded.title)
                assertEquals(TaskStatus.DONE, reloaded.status)

                assertTrue(
                    "Обновление без ID должно вернуть ошибку",
                    repository.updateTask("", TaskUpdateDto(title = "Без ID")).isFailure,
                )
                assertTrue(
                    "Пустой DTO должен вернуть ошибку",
                    repository.updateTask(created.id, TaskUpdateDto()).isFailure,
                )
            } finally {
                repository.deleteTask(created.id).getOrThrow()
            }

            assertTrue(
                "Задача должна быть удалена",
                repository.getById(created.id).isFailure,
            )
        }
    }
}
