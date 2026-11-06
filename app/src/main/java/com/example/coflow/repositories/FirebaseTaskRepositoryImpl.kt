package com.example.coflow.repositories

import com.example.coflow.models.TaskModel
import com.example.coflow.models.TaskStatus
import com.example.coflow.repositories.dto.TaskAddDto
import com.example.coflow.repositories.dto.TaskUpdateDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Firestore-реализация задач.
 *
 * Коллекция `tasks`; `projectId`, `authorId` и `assigneeId` хранятся как
 * DocumentReference, `deadline` — Timestamp, `status` — Int.
 * addTask() принимает TaskAddDto и генерирует ID документа,
 * updateTask() принимает id и TaskUpdateDto.
 */
class FirebaseTaskRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : TaskRepository {
    private val taskCollection: CollectionReference = db.collection("tasks")
    private val projectCollection: CollectionReference = db.collection("projects")
    private val userCollection: CollectionReference = db.collection("users")

    override suspend fun getById(id: String): Result<TaskModel> =
        try {
            val task =
                fromDocument(taskCollection.document(id).get().await())
                    ?: throw Exception("Задача не найдена")
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка загрузки задачи"))
        }

    override suspend fun getByTitle(title: String): Result<List<TaskModel>> =
        try {
            val documents =
                taskCollection
                    .whereEqualTo("title", title)
                    .get()
                    .await()
                    .documents
            Result.success(documents.mapNotNull { fromDocument(it) })
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка поиска задач"))
        }

    override suspend fun getByProjectId(id: String): Result<List<TaskModel>> =
        try {
            val projectDocument = projectCollection.document(id)
            val documents =
                taskCollection
                    .whereEqualTo("projectId", projectDocument)
                    .get()
                    .await()
                    .documents
            Result.success(documents.mapNotNull { fromDocument(it) })
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка загрузки задач проекта"))
        }

    override fun observeByProjectId(id: String): Flow<Result<List<TaskModel>>> =
        callbackFlow {
            val projectDocument = projectCollection.document(id)
            val registration =
                taskCollection
                    .whereEqualTo("projectId", projectDocument)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(
                                Result.failure(
                                    Exception(error.message ?: "Ошибка наблюдения за задачами"),
                                ),
                            )
                            close()
                        } else {
                            trySend(
                                Result.success(
                                    snapshot
                                        ?.documents
                                        ?.mapNotNull { fromDocument(it) }
                                        .orEmpty(),
                                ),
                            )
                        }
                    }

            awaitClose { registration.remove() }
        }

    override fun observeTasksByProjectId(id: String): Flow<Result<List<TaskModel>>> =
        callbackFlow {
            val projectDocument = projectCollection.document(id)
            val registration =
                taskCollection
                    .whereEqualTo("projectId", projectDocument)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(
                                Result.failure(
                                    Exception(error.message ?: "Ошибка наблюдения за задачами"),
                                ),
                            )
                            close()
                        } else {
                            trySend(
                                Result.success(
                                    snapshot
                                        ?.documents
                                        ?.mapNotNull { fromDocument(it) }
                                        .orEmpty(),
                                ),
                            )
                        }
                    }

            awaitClose { registration.remove() }
        }

    /**
     * Реализация [TaskRepository.observeById] через [callbackFlow] и снапшот-слушатель
     *
     * При удалении документа отправляется [Result.failure], но поток не закрывается —
     * это позволяет продолжить наблюдение, если задачу пересоздадут с тем же ID.
     *
     * @see TaskRepository.observeById
     */
    override fun observeById(id: String): Flow<Result<TaskModel>> =
        callbackFlow {
            val registration = taskCollection
                .document(id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(
                            Result.failure(
                                Exception(error.message ?: "Ошибка наблюдения за задачей"),
                            ),
                        )
                        close()
                    } else {
                        val task = snapshot?.let { fromDocument(it) }
                        if (task == null) {
                            trySend(Result.failure(Exception("Задача не найдена")))
                        } else {
                            trySend(Result.success(task))
                        }
                    }
                }
            awaitClose { registration.remove() }
        }

    override suspend fun addTask(task: TaskAddDto): Result<TaskModel> =
        try {
            val taskDocument = taskCollection.document()
            val toWrite = task.toModel(taskDocument.id)
            taskDocument.set(toWrite.toCreateMap()).await()

            val created =
                fromDocument(taskDocument.get().await())
                    ?: throw Exception("Не удалось прочитать созданную задачу")
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка создания задачи"))
        }

    override suspend fun updateTask(
        id: String,
        update: TaskUpdateDto,
    ): Result<TaskModel> =
        try {
            require(id.isNotBlank()) { "ID задачи не задан" }

            val updates = update.toUpdateMap()
            require(updates.isNotEmpty()) { "Нет полей для обновления" }

            val taskDocument = taskCollection.document(id)
            // update не создаёт документ заново и не трогает поля вне DTO.
            taskDocument.update(updates).await()

            val updated =
                fromDocument(taskDocument.get().await())
                    ?: throw Exception("Задача не найдена")
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка обновления задачи"))
        }

    override suspend fun deleteTask(id: String): Result<Unit> =
        try {
            taskCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка удаления задачи"))
        }

    /** Собирает модель задачи из DTO и сгенерированного ID. */
    private fun TaskAddDto.toModel(id: String): TaskModel =
        TaskModel(
            id = id,
            title = title,
            description = description,
            projectId = projectId,
            authorId = authorId,
            assigneeId = assigneeId,
            deadline = deadline,
            status = status,
        )

    /** Поля, которые нужны при создании задачи. */
    private fun TaskModel.toCreateMap(): Map<String, Any> =
        buildMap {
            put("title", title)
            put("description", description)
            put("status", status.code.toLong())
            put("deadline", deadline.toTimestamp())
            if (projectId.isNotBlank()) put("projectId", projectCollection.document(projectId))
            if (authorId.isNotBlank()) put("authorId", userCollection.document(authorId))
            if (assigneeId.isNotBlank()) put("assigneeId", userCollection.document(assigneeId))
        }

    /** null в DTO означает «не менять», пустая строка ID — удалить ссылку. */
    private fun TaskUpdateDto.toUpdateMap(): Map<String, Any> =
        buildMap {
            title?.let { put("title", it) }
            description?.let { put("description", it) }
            status?.let { put("status", it.code.toLong()) }
            deadline?.let { put("deadline", it.toTimestamp()) }
            projectId?.let { put("projectId", it.toReference(projectCollection)) }
            authorId?.let { put("authorId", it.toReference(userCollection)) }
            assigneeId?.let { put("assigneeId", it.toReference(userCollection)) }
        }

    private fun String.toReference(collection: CollectionReference): Any =
        if (isBlank()) {
            FieldValue.delete()
        } else {
            collection.document(this)
        }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): TaskModel? {
            if (!doc.exists()) return null

            val deadline =
                doc.getTimestamp("deadline")?.let {
                    LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()),
                        ZoneOffset.UTC,
                    )
                } ?: LocalDateTime.now(ZoneOffset.UTC)

            return TaskModel(
                id = doc.id,
                projectId = doc.getDocumentReference("projectId")?.id.orEmpty(),
                authorId = doc.getDocumentReference("authorId")?.id.orEmpty(),
                assigneeId = doc.getDocumentReference("assigneeId")?.id.orEmpty(),
                title = doc.getString("title").orEmpty(),
                description = doc.getString("description").orEmpty(),
                deadline = deadline,
                status = TaskStatus.fromInt((doc.getLong("status") ?: 0L).toInt()),
            )
        }
    }
}

private fun LocalDateTime.toTimestamp(): Timestamp {
    val instant = toInstant(ZoneOffset.UTC)
    return Timestamp(instant.epochSecond, instant.nano)
}
