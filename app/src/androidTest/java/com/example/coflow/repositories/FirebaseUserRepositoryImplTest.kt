package com.example.coflow.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coflow.repositories.dto.UserAddDto
import com.example.coflow.repositories.dto.UserUpdateDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты репозитория пользователей на Firestore Emulator.
 *
 * Проверяют создание, чтение по ID/email, обновление и удаление.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseUserRepositoryImplTest {
    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        FirebaseEmulatorConfig.connect()
        repository = FirebaseUserRepositoryImpl()
    }

    @Test
    fun addUpdateReadAndDeleteWorkflow() {
        runBlocking {
            val email = "user_repo_${System.currentTimeMillis()}@example.com"
            val user =
                UserAddDto(
                    name = "Тестовый Пользователь",
                    email = email,
                )

            val added = repository.addUser(user).getOrThrow()
            try {
                assertTrue("UID созданного пользователя должен быть заполнен", added.uid.isNotBlank())
                assertEquals(user.name, added.name)
                assertEquals(email, added.email)
                assertFalse("Новый пользователь не должен быть удалён", added.isDeleted)

                val fetchedById = repository.getById(added.uid).getOrThrow()
                assertEquals(user.name, fetchedById.name)
                assertEquals(email, fetchedById.email)

                val updated =
                    repository
                        .updateUser(
                            id = added.uid,
                            update = UserUpdateDto(name = "Обновлённое имя"),
                        ).getOrThrow()
                assertEquals("Обновлённое имя", updated.name)
                assertEquals(email, updated.email)

                val reloaded = repository.getById(added.uid).getOrThrow()
                assertEquals("Обновлённое имя", reloaded.name)

                val fetchedByEmail = repository.getByEmail(email).getOrThrow()
                assertEquals(added.uid, fetchedByEmail.uid)
                assertEquals("Обновлённое имя", fetchedByEmail.name)

                assertTrue(
                    "Пустой DTO должен вернуть ошибку",
                    repository.updateUser(added.uid, UserUpdateDto()).isFailure,
                )
            } finally {
                repository.deleteUser(added.uid).getOrThrow()
            }

            assertTrue(
                "Пользователь должен быть удалён",
                repository.getById(added.uid).isFailure,
            )
        }
    }
}
