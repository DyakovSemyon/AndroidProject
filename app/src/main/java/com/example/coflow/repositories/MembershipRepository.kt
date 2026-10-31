package com.example.coflow.repositories

import com.example.coflow.models.MembershipModel
import com.example.coflow.repositories.dto.MembershipAddDto
import com.example.coflow.repositories.dto.MembershipUpdateDto

/**
 * Членство пользователя в проекте (коллекция `memberships`).
 *
 * Единый стандарт изменения данных:
 * - [addMember] — создание членства;
 * - [updateMember] — изменение роли по ID;
 * - [deleteMember] — удаление.
 *
 * ID документа генерирует репозиторий. Проект и пользователь у существующего
 * членства не меняются — только роль.
 *
 * ```kotlin
 * val saved = repository.addMember(
 *     MembershipAddDto(
 *         projectId = projectId,
 *         userId = userId,
 *         role = Role.MEMBER,
 *     ),
 * ).getOrThrow()
 *
 * repository.updateMember(
 *     id = saved.id,
 *     update = MembershipUpdateDto(role = Role.OWNER),
 * )
 * ```
 */
interface MembershipRepository {
    /** Возвращает все членства пользователя (записи `projectId` + `role`). */
    suspend fun getByUserId(userId: String): Result<List<MembershipModel>>

    /** Возвращает все членства проекта. */
    suspend fun getByProjectId(projectId: String): Result<List<MembershipModel>>

    /**
     * Создаёт членство из [MembershipAddDto].
     * [MembershipAddDto.projectId] и [MembershipAddDto.userId] обязательны.
     * ID генерирует репозиторий и возвращает его в [MembershipModel.id].
     * Повторная пара projectId + userId не допускается.
     */
    suspend fun addMember(member: MembershipAddDto): Result<MembershipModel>

    /**
     * Частично обновляет членство по ID.
     *
     * Меняются только поля [MembershipUpdateDto], отличные от `null`.
     * Если не передано ни одного поля, вернёт [Result.failure].
     */
    suspend fun updateMember(
        id: String,
        update: MembershipUpdateDto,
    ): Result<MembershipModel>

    /** Удаляет членство по ID документа. */
    suspend fun deleteMember(id: String): Result<Unit>
}
