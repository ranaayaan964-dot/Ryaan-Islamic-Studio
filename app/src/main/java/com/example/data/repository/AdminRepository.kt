package com.example.data.repository

import android.content.Context
import com.example.data.database.AdminMessageEntity
import com.example.data.database.AppDatabase
import com.example.data.database.UserEntity
import com.example.data.database.UserReportEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AdminRepository(context: Context) {
    companion object {
        const val TAG = "AdminRepository"
        val AUTHORIZED_ADMIN_EMAILS = setOf(
            "ayianrana20133@gmail.com",
            "ranasalar964@gmail.com"
        )
    }

    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = AppDatabase.getInstance(appContext)
    private val userDao = database.userDao()
    private val adminDao = database.adminDao()

    fun observeAllUsers(): Flow<List<UserEntity>> = userDao.getAllActiveUsersFlow()
    fun observeAllMessages(): Flow<List<AdminMessageEntity>> = adminDao.getAllAdminMessagesFlow()
    fun observeAllReports(): Flow<List<UserReportEntity>> = adminDao.getAllUserReportsFlow()

    fun observeMessagesForUser(uid: String): Flow<List<AdminMessageEntity>> = callbackFlow {
        val lock = Any()
        var broadcasts = emptyList<AdminMessageEntity>()
        var directMessages = emptyList<AdminMessageEntity>()

        fun emitMessages(localMessages: List<AdminMessageEntity>) {
            val remoteMessages = synchronized(lock) { broadcasts + directMessages }
            val visible = (localMessages.filter {
                it.targetAll || uid.isBlank() || uid in it.targetUsersCsv.split(',')
            } + remoteMessages)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
            trySend(visible)
        }

        val localJob = launch {
            adminDao.getAllAdminMessagesFlow().collect(::emitMessages)
        }

        fun listen(
            query: com.google.firebase.firestore.Query,
            update: (List<AdminMessageEntity>) -> Unit
        ): ListenerRegistration? = runCatching {
            query.addSnapshotListener { snapshot, error ->
                if (error == null) {
                    val entities = snapshot?.documents?.mapNotNull { it.toMessageEntity() }.orEmpty()
                    synchronized(lock) { update(entities) }
                    launch(Dispatchers.IO) {
                        if (entities.isNotEmpty()) adminDao.insertAdminMessages(entities)
                        emitMessages(adminDao.getAllAdminMessages())
                    }
                }
            }
        }.getOrNull()

        val collection = firestore.collection("admin_messages")
        val broadcastListener = listen(collection.whereEqualTo("targetAll", true)) { broadcasts = it }
        val directListener = if (uid.isNotBlank()) {
            listen(collection.whereArrayContains("targetUsers", uid)) { directMessages = it }
        } else null

        awaitClose {
            localJob.cancel()
            broadcastListener?.remove()
            directListener?.remove()
        }
    }

    fun observeReportsForUser(uid: String): Flow<List<UserReportEntity>> = callbackFlow {
        val localJob = launch {
            if (uid.isNotBlank()) {
                adminDao.getReportsByUserFlow(uid).collect { trySend(it) }
            } else {
                adminDao.getAllUserReportsFlow().collect { trySend(it) }
            }
        }
        val listener = if (uid.isNotBlank()) {
            runCatching {
                firestore.collection("user_reports")
                    .whereEqualTo("reporterUid", uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null) {
                            val reports = snapshot?.documents?.mapNotNull { it.toReportEntity() }.orEmpty()
                            launch(Dispatchers.IO) {
                                if (reports.isNotEmpty()) adminDao.insertUserReports(reports)
                            }
                        }
                    }
            }.getOrNull()
        } else null

        awaitClose {
            localJob.cancel()
            listener?.remove()
        }
    }

    suspend fun isAdminSession(): Boolean = withContext(Dispatchers.IO) {
        val localUser = userDao.getActiveSession() ?: userDao.getCurrentUser()
        if (localUser != null && localUser.isLoggedIn && !localUser.isRemoved) {
            if (localUser.role.equals("ADMIN", ignoreCase = true) ||
                localUser.email.trim().lowercase() in AUTHORIZED_ADMIN_EMAILS ||
                localUser.username.equals("ayaan", ignoreCase = true)
            ) {
                return@withContext true
            }
        }
        val fbUser = auth.currentUser ?: return@withContext false
        val email = fbUser.email?.trim()?.lowercase()
        val claims = runCatching { fbUser.getIdToken(false).await().claims }.getOrDefault(emptyMap())
        email in AUTHORIZED_ADMIN_EMAILS || claims["admin"] == true
    }

    suspend fun syncUsersFromFirestore() = withContext(Dispatchers.IO) {
        requireAdmin()
        runCatching {
            val documents = firestore.collection("users").get().await().documents
            documents.forEach { document ->
                val uid = document.id
                if (document.getBoolean("isRemoved") == true || document.getString("status") == "REMOVED") {
                    userDao.markUserRemovedByFirebaseUid(uid)
                    return@forEach
                }
                val existing = userDao.getUserByFirebaseUid(uid)
                val localId = existing?.id ?: userDao.getNextUserId()
                val docEmail = document.getString("email").orEmpty()
                userDao.insertUser(
                    UserEntity(
                        id = localId,
                        username = document.getString("username").orEmpty().ifBlank { docEmail.substringBefore("@") },
                        passwordHash = existing?.passwordHash.orEmpty(),
                        fullName = document.getString("fullName").orEmpty().ifBlank { "Ayaan User" },
                        email = docEmail,
                        sessionToken = existing?.sessionToken,
                        isLoggedIn = existing?.isLoggedIn ?: false,
                        loginTimestamp = document.getLong("lastLoginAt") ?: 0L,
                        role = if (docEmail.lowercase() in AUTHORIZED_ADMIN_EMAILS) "ADMIN" else (document.getString("role") ?: "USER"),
                        firebaseUid = uid,
                        authProvider = document.getString("authProvider").orEmpty(),
                        createdAt = document.getLong("createdAt") ?: 0L,
                        isRemoved = false,
                        activitySummary = document.getString("activitySummary") ?: "Registered User",
                        subscriptionPlan = document.getString("subscriptionPlan") ?: existing?.subscriptionPlan ?: "FREE",
                        proExpiresAt = document.getLong("proExpiresAt") ?: existing?.proExpiresAt ?: 0L,
                        activatedProKey = document.getString("activatedProKey") ?: existing?.activatedProKey.orEmpty()
                    )
                )
            }
        }
    }

    suspend fun syncMessagesForCurrentUser() = withContext(Dispatchers.IO) {
        val uid = currentUserUid()
        runCatching {
            val collection = firestore.collection("admin_messages")
            val broadcasts = collection.whereEqualTo("targetAll", true).get().await().documents
            val targeted = if (uid.isNotBlank()) {
                collection.whereArrayContains("targetUsers", uid).get().await().documents
            } else emptyList()
            adminDao.insertAdminMessages((broadcasts + targeted).distinctBy { it.id }.mapNotNull { it.toMessageEntity() })
        }
    }

    suspend fun syncMessagesForAdmin() = withContext(Dispatchers.IO) {
        requireAdmin()
        runCatching {
            val documents = firestore.collection("admin_messages").get().await().documents
            adminDao.insertAdminMessages(documents.mapNotNull { it.toMessageEntity() })
        }
    }

    suspend fun syncReportsForAdmin() = withContext(Dispatchers.IO) {
        requireAdmin()
        runCatching {
            val documents = firestore.collection("user_reports").get().await().documents
            adminDao.insertUserReports(documents.mapNotNull { it.toReportEntity() })
        }
    }

    suspend fun syncReportsForCurrentUser() = withContext(Dispatchers.IO) {
        val uid = currentUserUid()
        if (uid.isBlank()) return@withContext
        runCatching {
            syncPendingReports(uid)
            val documents = firestore.collection("user_reports")
                .whereEqualTo("reporterUid", uid)
                .get()
                .await()
                .documents
            adminDao.insertUserReports(documents.mapNotNull { it.toReportEntity() })
        }
    }

    private suspend fun syncPendingReports(uid: String) {
        adminDao.getPendingReports(uid).forEach { report ->
            runCatching {
                val document = firestore.collection("user_reports").document(report.id)
                if (!document.get().await().exists()) document.set(report.toFirestore()).await()
                adminDao.markReportSynced(report.id)
            }
        }
    }

    suspend fun removeUser(user: UserEntity) = withContext(Dispatchers.IO) {
        val adminUid = requireAdmin()
        require(user.email.trim().lowercase() !in AUTHORIZED_ADMIN_EMAILS && user.firebaseUid != adminUid) {
            "You cannot remove the administrator account."
        }
        if (user.firebaseUid.isNotBlank()) {
            runCatching {
                firestore.collection("users").document(user.firebaseUid).set(
                    mapOf(
                        "isRemoved" to true,
                        "status" to "REMOVED",
                        "removedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            }
            userDao.markUserRemovedByFirebaseUid(user.firebaseUid)
        }
        userDao.markUserRemoved(user.username)
        userDao.deleteUserById(user.id)
    }

    suspend fun sendAdminMessage(
        title: String,
        body: String,
        messageType: String,
        targetAll: Boolean,
        targetUserUids: List<String>
    ) = withContext(Dispatchers.IO) {
        val senderUid = requireAdmin()
        require(title.isNotBlank() && body.isNotBlank()) { "Enter a title and message." }
        require(messageType in setOf("WARNING", "ANNOUNCEMENT", "DIRECT_MESSAGE")) { "Invalid message type." }
        val recipients = targetUserUids.filter(String::isNotBlank).distinct()
        require(targetAll || recipients.isNotEmpty()) { "Select at least one recipient." }
        val document = firestore.collection("admin_messages").document()
        val timestamp = System.currentTimeMillis()
        val entity = AdminMessageEntity(
            id = document.id,
            title = title.trim(),
            body = body.trim(),
            messageType = messageType,
            targetAll = targetAll,
            targetUsersCsv = if (targetAll) "ALL" else recipients.joinToString(","),
            senderUid = senderUid,
            timestamp = timestamp
        )
        adminDao.insertAdminMessage(entity)
        runCatching {
            document.set(entity.toFirestore(recipients)).await()
        }
    }

    suspend fun deleteAdminMessage(messageId: String) = withContext(Dispatchers.IO) {
        requireAdmin()
        adminDao.deleteAdminMessageById(messageId)
        runCatching {
            firestore.collection("admin_messages").document(messageId).delete().await()
        }
    }

    suspend fun submitUserReport(
        user: UserEntity,
        category: String,
        subject: String,
        description: String
    ) = withContext(Dispatchers.IO) {
        require(category.isNotBlank() && subject.isNotBlank() && description.isNotBlank()) {
            "Complete the category, subject, and description."
        }
        val uid = auth.currentUser?.uid ?: user.firebaseUid.ifBlank { "local_${user.username}" }
        val document = firestore.collection("user_reports").document()
        val report = UserReportEntity(
            id = document.id,
            reporterUid = uid,
            reporterUsername = user.username,
            reporterEmail = user.email,
            reporterFullName = user.fullName,
            category = category,
            subject = subject.trim(),
            description = description.trim()
        )
        adminDao.insertUserReport(report)
        runCatching {
            document.set(report.toFirestore()).await()
            adminDao.markReportSynced(report.id)
        }
    }

    suspend fun replyAndResolveReport(report: UserReportEntity, status: String, reply: String) = withContext(Dispatchers.IO) {
        val senderUid = requireAdmin()
        require(status == "RESOLVED" || status == "IN_PROGRESS") { "Invalid report status." }
        require(reply.isNotBlank()) { "Enter a reply." }
        val messageDocument = firestore.collection("admin_messages").document()
        val timestamp = System.currentTimeMillis()
        val message = AdminMessageEntity(
            id = messageDocument.id,
            title = "Reply to your report: ${report.subject}",
            body = reply.trim(),
            messageType = "DIRECT_MESSAGE",
            targetAll = false,
            targetUsersCsv = report.reporterUid,
            senderUid = senderUid,
            timestamp = timestamp
        )
        adminDao.updateReportStatusAndReply(report.id, status, reply.trim())
        adminDao.insertAdminMessage(message)
        runCatching {
            val batch = firestore.batch()
            batch.update(
                firestore.collection("user_reports").document(report.id),
                mapOf("status" to status, "adminReply" to reply.trim())
            )
            batch.set(messageDocument, message.toFirestore(listOf(report.reporterUid)))
            batch.commit().await()
        }
    }

    suspend fun deleteUserReport(reportId: String) = withContext(Dispatchers.IO) {
        requireAdmin()
        adminDao.deleteUserReportById(reportId)
        runCatching {
            firestore.collection("user_reports").document(reportId).delete().await()
        }
    }

    private suspend fun requireAdmin(): String {
        check(isAdminSession()) {
            "Administrator access requires signing in with ayianrana20133@gmail.com or ranasalar964@gmail.com."
        }
        return auth.currentUser?.uid
            ?: userDao.getActiveSession()?.firebaseUid?.takeIf { it.isNotBlank() }
            ?: "admin_ayaan_uid"
    }

    private suspend fun currentUserUid(): String =
        auth.currentUser?.uid
            ?: userDao.getActiveSession()?.firebaseUid.orEmpty()

    private fun AdminMessageEntity.toFirestore(targetUsers: List<String>) = mapOf(
        "title" to title,
        "body" to body,
        "messageType" to messageType,
        "targetAll" to targetAll,
        "targetUsers" to targetUsers,
        "senderName" to senderName,
        "senderUid" to senderUid,
        "timestamp" to timestamp
    )

    private fun UserReportEntity.toFirestore() = mapOf(
        "reporterUid" to reporterUid,
        "reporterUsername" to reporterUsername,
        "reporterEmail" to reporterEmail,
        "reporterFullName" to reporterFullName,
        "category" to category,
        "subject" to subject,
        "description" to description,
        "status" to status,
        "adminReply" to adminReply,
        "timestamp" to timestamp
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toMessageEntity(): AdminMessageEntity? {
        val id = id.takeIf(String::isNotBlank) ?: return null
        return AdminMessageEntity(
            id = id,
            title = getString("title").orEmpty(),
            body = getString("body").orEmpty(),
            messageType = getString("messageType") ?: "ANNOUNCEMENT",
            targetAll = getBoolean("targetAll") ?: false,
            targetUsersCsv = (get("targetUsers") as? List<*>)?.filterIsInstance<String>()?.joinToString(",").orEmpty(),
            senderName = getString("senderName") ?: "Admin Ayaan",
            senderUid = getString("senderUid").orEmpty(),
            timestamp = getLong("timestamp") ?: 0L
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toReportEntity(): UserReportEntity? {
        val id = id.takeIf(String::isNotBlank) ?: return null
        return UserReportEntity(
            id = id,
            reporterUid = getString("reporterUid").orEmpty(),
            reporterUsername = getString("reporterUsername").orEmpty(),
            reporterEmail = getString("reporterEmail").orEmpty(),
            reporterFullName = getString("reporterFullName").orEmpty(),
            category = getString("category").orEmpty(),
            subject = getString("subject").orEmpty(),
            description = getString("description").orEmpty(),
            status = getString("status") ?: "OPEN",
            adminReply = getString("adminReply").orEmpty(),
            timestamp = getLong("timestamp") ?: 0L,
            isSynced = true
        )
    }
}
