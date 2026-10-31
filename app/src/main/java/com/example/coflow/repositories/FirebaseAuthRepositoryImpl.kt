package com.example.coflow.repositories

import com.example.coflow.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firebase Auth + профиль в `users/{uid}`.
 * В профиле хранятся `uid`, `name`, `email` и `createdAt` (Timestamp).
 * Если запись профиля не удалась, register() удаляет созданного Auth-пользователя.
 */
class FirebaseAuthRepositoryImpl : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    override suspend fun register(
        email: String,
        password: String,
        name: String,
    ): Result<UserModel> =
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Не удалось получить UID")
            val uid = firebaseUser.uid

            val user = UserModel(uid = uid, email = email, name = name)

            try {
                usersCollection
                    .document(uid)
                    .set(
                        mapOf(
                            "uid" to user.uid,
                            "email" to user.email,
                            "name" to user.name,
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
            } catch (e: Exception) {
                // откат: не оставляем пользователя без профиля
                runCatching { firebaseUser.delete().await() }
                throw e
            }

            Result.success(user)
        } catch (e: Exception) {
            val msg =
                when (e) {
                    is FirebaseAuthWeakPasswordException -> "Пароль слишком простой (минимум 6 символов)"
                    is FirebaseAuthUserCollisionException -> "Пользователь с такой почтой уже существует"
                    is FirebaseAuthInvalidCredentialsException -> "Некорректный email"
                    else -> e.message ?: "Ошибка регистрации"
                }
            Result.failure(Exception(msg))
        }

    override suspend fun login(
        email: String,
        password: String,
    ): Result<UserModel> =
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            val fbUser = auth.currentUser ?: throw Exception("Не удалось получить UID")
            val uid = fbUser.uid

            val doc = usersCollection.document(uid).get().await()
            val user =
                if (doc.exists()) {
                    doc
                        .toObject(UserModel::class.java)
                        ?.copy(uid = uid)
                        ?: throw Exception("Не удалось прочитать профиль")
                } else {
                    // Восстановление после сбоя register(): Auth есть, Firestore-до
                    // TODO: пересмотреть этот сомнительный функционал
                    val restored =
                        UserModel(
                            uid = uid,
                            email = fbUser.email.orEmpty(),
                            name = fbUser.displayName.orEmpty(),
                        )
                    usersCollection
                        .document(uid)
                        .set(
                            mapOf(
                                "uid" to restored.uid,
                                "email" to restored.email,
                                "name" to restored.name,
                                "createdAt" to FieldValue.serverTimestamp(),
                            ),
                        ).await()
                    restored
                }
            Result.success(user)
        } catch (e: Exception) {
            val msg =
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Неверная почта или пароль"
                    else -> e.message ?: "Ошибка входа"
                }
            Result.failure(Exception(msg))
        }

    override suspend fun signInWithGoogle(idToken: String): Result<UserModel> =
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val fbUser = authResult.user ?: throw Exception("Не удалось получить пользователя")
            val uid = fbUser.uid
            val email = fbUser.email ?: throw Exception("Google-аккаунт не содержит email")
            val name = fbUser.displayName?.takeIf { it.isNotBlank() } ?: "Пользователь Google"

            val doc = usersCollection.document(uid).get().await()
            val user =
                if (doc.exists()) {
                    doc
                        .toObject(UserModel::class.java)
                        ?.copy(uid = uid)
                        ?: throw Exception("Не удалось прочитать профиль")
                } else {
                    val newUser = UserModel(uid = uid, name = name, email = email)
                    usersCollection
                        .document(uid)
                        .set(
                            mapOf(
                                "uid" to newUser.uid,
                                "email" to newUser.email,
                                "name" to newUser.name,
                                "createdAt" to FieldValue.serverTimestamp(),
                            ),
                        ).await()
                    newUser
                }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка входа через Google"))
        }

    override fun logout() {
        auth.signOut()
    }

    override fun getCurrentUserUid(): String? = auth.currentUser?.uid
}
