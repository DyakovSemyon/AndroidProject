package com.example.coflow.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты авторизации на Firebase Auth/Firestore Emulator.
 *
 * Эмуляторы настраиваются в [FirebaseEmulatorConfig], а почта в каждом тесте
 * уникальна, чтобы прогоны не мешали друг другу.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseAuthRepositoryImplTest {
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        FirebaseEmulatorConfig.connect()
        repository = FirebaseAuthRepositoryImpl()
    }

    @Test
    fun registerAndLogin_returnsSameUser() {
        runBlocking {
            val email = "test_${System.currentTimeMillis()}@example.com"
            val password = "password123"
            val name = "Тестовый Пользователь"

            val registered = repository.register(email, password, name).getOrThrow()
            assertTrue("UID не должен быть пустым", registered.uid.isNotBlank())
            assertEquals(email, registered.email)
            assertEquals(name, registered.name)

            val loggedIn = repository.login(email, password).getOrThrow()
            assertEquals("UID после входа должен совпадать", registered.uid, loggedIn.uid)
            assertEquals(email, loggedIn.email)
            assertEquals(name, loggedIn.name)
            assertEquals(registered.uid, repository.getCurrentUserUid())
        }
    }

    @Test
    fun loginWithWrongPassword_returnsFailure() {
        runBlocking {
            val email = "wrong_password_${System.currentTimeMillis()}@example.com"
            val password = "password123"

            repository.register(email, password, "Test").getOrThrow()

            val result = repository.login(email, "wrong_password")

            assertTrue("Вход с неверным паролем должен вернуть ошибку", result.isFailure)
            assertNotNull(result.exceptionOrNull())
        }
    }
}
