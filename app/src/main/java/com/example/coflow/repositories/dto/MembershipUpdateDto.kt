package com.example.coflow.repositories.dto

import com.example.coflow.models.Role

/**
 * DTO для частичного обновления членства.
 *
 * `projectId` и `userId` не меняются: обновляется только роль.
 * Если [role] равен `null`, обновлять нечего.
 */
data class MembershipUpdateDto(
    val role: Role? = null,
)
