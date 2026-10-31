package com.example.coflow.repositories

import com.example.coflow.models.MembershipModel
import com.example.coflow.models.Role
import com.example.coflow.repositories.dto.MembershipAddDto
import com.example.coflow.repositories.dto.MembershipUpdateDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firestore-реализация членства.
 *
 * Коллекция `memberships`; ID документа генерирует Firestore,
 * поля `projectId`, `userId`, `role` (Int). addMember() принимает MembershipAddDto,
 * запрещает дубли projectId + userId и возвращает модель с ID документа.
 * updateMember() меняет только переданные поля DTO.
 */
class FirebaseMembershipRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : MembershipRepository {
    private val membershipCollection: CollectionReference = db.collection("memberships")

    override suspend fun getByUserId(userId: String): Result<List<MembershipModel>> =
        try {
            val documents =
                membershipCollection
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                    .documents
            Result.success(documents.mapNotNull { it.toMembershipModel() })
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка загрузки членства"))
        }

    override suspend fun getByProjectId(projectId: String): Result<List<MembershipModel>> =
        try {
            val documents =
                membershipCollection
                    .whereEqualTo("projectId", projectId)
                    .get()
                    .await()
                    .documents
            Result.success(documents.mapNotNull { it.toMembershipModel() })
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка загрузки участников проекта"))
        }

    override suspend fun addMember(member: MembershipAddDto): Result<MembershipModel> =
        try {
            require(member.projectId.isNotBlank() && member.userId.isNotBlank()) {
                "projectId и userId обязательны"
            }

            // Один пользователь может быть в проекте только один раз.
            val alreadyExists =
                membershipCollection
                    .whereEqualTo("projectId", member.projectId)
                    .get()
                    .await()
                    .documents
                    .any { it.getString("userId") == member.userId }

            if (alreadyExists) {
                throw Exception("Участник уже существует")
            }

            val membershipDocument = membershipCollection.document()
            val saved =
                MembershipModel(
                    id = membershipDocument.id,
                    projectId = member.projectId,
                    userId = member.userId,
                    role = member.role,
                )
            membershipDocument.set(saved.toFirestoreMap()).await()
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка создания участника"))
        }

    override suspend fun updateMember(
        id: String,
        update: MembershipUpdateDto,
    ): Result<MembershipModel> =
        try {
            require(id.isNotBlank()) { "ID членства не задан" }

            val updates = update.toUpdateMap()
            require(updates.isNotEmpty()) { "Нет полей для обновления" }

            val membershipDocument = membershipCollection.document(id)
            membershipDocument.update(updates).await()

            val updated =
                membershipDocument.get().await().toMembershipModel()
                    ?: throw Exception("Членство не найдено")
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка обновления участника"))
        }

    override suspend fun deleteMember(id: String): Result<Unit> =
        try {
            membershipCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка удаления участника"))
        }
}

private fun MembershipModel.toFirestoreMap(): Map<String, Any> =
    mapOf(
        "projectId" to projectId,
        "userId" to userId,
        "role" to role.code,
    )

private fun MembershipUpdateDto.toUpdateMap(): Map<String, Any> =
    buildMap {
        role?.let { put("role", it.code) }
    }

private fun DocumentSnapshot.toMembershipModel(): MembershipModel? {
    if (!exists()) return null
    return MembershipModel(
        id = id,
        projectId = getString("projectId") ?: "",
        userId = getString("userId") ?: "",
        role = Role.fromInt((getLong("role") ?: Role.MEMBER.code.toLong()).toInt()),
    )
}
