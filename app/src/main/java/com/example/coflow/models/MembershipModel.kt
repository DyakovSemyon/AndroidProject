package com.example.coflow.models

/**
 * Доменная модель членства пользователя в проекте.
 *
 * [id] — идентификатор документа в Firestore. Для нового членства он пустой:
 * репозиторий сам сформирует ID из пары projectId + userId.
 */
data class MembershipModel(
    val id: String = "",
    val projectId: String = "",
    val userId: String = "",
    val role: Role = Role.MEMBER,
)
