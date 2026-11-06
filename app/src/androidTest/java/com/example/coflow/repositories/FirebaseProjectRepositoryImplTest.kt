package com.example.coflow.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coflow.models.MembershipModel
import com.example.coflow.models.Role
import com.example.coflow.repositories.dto.MembershipAddDto
import com.example.coflow.repositories.dto.ProjectAddDto
import com.example.coflow.repositories.dto.ProjectUpdateDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты проектов на Firestore Emulator.
 *
 * Проверяют создание, чтение по ID/владельцу/участнику, обновление
 * и мягкое удаление.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseProjectRepositoryImplTest {
    private lateinit var repository: ProjectRepository
    private lateinit var membershipRepository: MembershipRepository

    @Before
    fun setup() {
        FirebaseEmulatorConfig.connect()
        repository = FirebaseProjectRepositoryImpl()
        membershipRepository = FirebaseMembershipRepositoryImpl()
    }

    @Test
    fun addReadMembershipAndDeleteWorkflow() {
        runBlocking {
            val suffix = System.currentTimeMillis()
            val ownerId = "test_owner_$suffix"
            val userId = "test_user_$suffix"
            val project =
                ProjectAddDto(
                    ownerId = ownerId,
                    title = "Тестовый проект $suffix",
                    description = "Описание проекта для интеграционного теста",
                )

            val saved = repository.addProject(project).getOrThrow()
            var membership: MembershipModel? = null
            try {
                assertTrue("ID проекта должен быть заполнен", saved.id.isNotBlank())
                assertEquals(ownerId, saved.ownerId)
                assertEquals(project.title, saved.title)

                val fetchedById = repository.getById(saved.id).getOrThrow()
                assertEquals(project.title, fetchedById.title)

                val updated =
                    repository
                        .updateProject(
                            id = saved.id,
                            update =
                                ProjectUpdateDto(
                                    title = "Обновлённый проект",
                                    description = "Обновлённое описание",
                                ),
                        ).getOrThrow()
                assertEquals("Обновлённый проект", updated.title)
                assertEquals("Обновлённое описание", updated.description)

                val reloadedProject = repository.getById(saved.id).getOrThrow()
                assertEquals("Обновлённый проект", reloadedProject.title)

                assertTrue(
                    "Пустой DTO должен вернуть ошибку",
                    repository.updateProject(saved.id, ProjectUpdateDto()).isFailure,
                )

                val byOwner = repository.getByOwnerId(ownerId).getOrThrow()
                assertTrue(
                    "Проект должен находиться по владельцу",
                    byOwner.any { it.id == saved.id },
                )

                membership =
                    membershipRepository
                        .addMember(
                            MembershipAddDto(
                                projectId = saved.id,
                                userId = userId,
                                role = Role.MEMBER,
                            ),
                        ).getOrThrow()

                val byUser = repository.getByUserId(userId).getOrThrow()
                assertTrue(
                    "Проект должен находиться по участнику",
                    byUser.any { it.id == saved.id },
                )
            } finally {
                membership?.let { membershipRepository.deleteMember(it.id).getOrThrow() }
                repository.deleteProject(saved.id).getOrThrow()
            }

            val deleted = repository.getById(saved.id).getOrThrow()
            assertTrue("Проект должен быть помечен удалённым", deleted.isDeleted)

            val byOwnerAfterDelete = repository.getByOwnerId(ownerId).getOrThrow()
            assertTrue(
                "Удалённый проект не должен попадать в список владельца",
                byOwnerAfterDelete.none { it.id == saved.id },
            )
        }
    }
}
