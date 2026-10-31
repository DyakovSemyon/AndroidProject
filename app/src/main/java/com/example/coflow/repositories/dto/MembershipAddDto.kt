package com.example.coflow.repositories.dto

import com.example.coflow.models.Role

/**
 * DTO создания членства.
 *
 * [projectId] и [userId] обязательны. ID документа генерирует репозиторий
 * и возвращает в [com.example.coflow.models.MembershipModel.id].
 */
data class MembershipAddDto(
    val projectId: String,
    val userId: String,
    val role: Role = Role.MEMBER,
)
