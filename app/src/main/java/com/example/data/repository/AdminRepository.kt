package com.example.data.repository

import android.content.Context
import com.example.data.database.AdminMessageEntity
import com.example.data.database.AppDatabase
import com.example.data.database.UserEntity
import com.example.data.database.UserReportEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminRepository(context: Context) {
    private companion object {
        const val TAG = "AdminRepository"
    }
    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = AppDatabase.getInstance(appContext)
    private val userDao = database.userDao()
    private val adminDao = database.adminDao()

    fun observeAllUsers(): Flow<List<UserEntity>> =
        userDao.getAllActiveUsersFlow().map { users -> users.filter { it.firebaseUid.isNotBlank() } }
    fun observeAllMessages(): Flow<List<AdminMessageEntity>> = adminDao.getAllAdminMessagesFlow()
    fun observeAllReports(): Flow<List<UserReportEntity>> = adminDao.getAllUserReportsFlow()

    fun observeMessagesForUser(uid: String): Flow<List<AdminMessageEntity>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            awaitClose {}
        }

        val lock = Any()
        var broadcasts = emptyList<AdminMessageEntity>()
        var directMessages = emptyList<AdminMessageEntity>()
        fun emitMessages(localMessages: List<AdminMessageEntity>) {
            val remoteMessages = synchronized(lock) { broadcasts + directMessages }
            val visible = (localMessages.filter { it.targetAll || uid in it.targetUsersCsv.split(',') } + remoteMessages)
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
        ): ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.w(TAG, "User message listener failed", error)
            } else {
                val entities = snapshot?.documents?.mapNotNull { it.toMessageEntity() }.orEmpty()
                synchronized(lock) { update(entities) }
                launch(Dispatchers.IO) {
                    if (entities.isNotEmpty()) adminDao.insertAdminMessages(entities)
                    emitMessages(adminDao.getAllAdminMessages())
                }
            }
        }
        val collection = firestore.collection("admin_messages")
        val broadcastListener = listen(collection.whereEqualTo("targetAll", true)) { broadcasts = it }
        val directListener = listen(collection.whereArrayContains("targetUsers", uid)) { directMessages = it }
        awaitClose {
            localJob.cancel()
            broadcastListener.remove()
            directListener.remove()
        }
    }

    fun observeReportsForUser(uid: String): Flow<List<UserReportEntity>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            awaitClose {}
        }
        val localJob = launch {
            adminDao.getReportsByUserFlow(uid).collect { trySend(it) }
        }
        val listener = firestore.collection("user_reports")
            .whereEqualTo("reporterUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w(TAG, "User report listener failed", error)
                } else {
                    val reports = snapshot?.documents?.mapNotNull { it.toReportEntity() }.orEmpty()
                    launch(Dispatchers.IO) {
                        if (reports.isNotEmpty()) adminDao.insertUserReports(reports)
                    }
                }
            }
        awaitClose {
            localJob.cancel()
            listener.remove()
        }
    }

    suspend fun isAdminSession(): Boolean {
        val user = auth.currentUser ?: return false
        val claims = user.getIdToken(true).await().claims
        val firebaseClaims = claims["firebase"] as? Map<*, *>
        val secondFactor = firebaseClaims?.get("sign_in_second_factor") as? String
        if (
            claims["admin"] != true ||
            claims["email_verified"] != true ||
            secondFactor.isNullOrBlank()
        ) return false
        val profile = firestore.collection("users").document(user.uid).get().await()
        return profile.exists() && profile.getBoolean("isRemoved") != true &&
            profile.getString("status") != "REMOVED"
    }

    suspend fun syncUsersFromFirestore() {
        requireAdmin()
        val documents = firestore.collection("users").get().await().documents
        documents.forEach { document ->
            val uid = document.id
            if (document.getBoolean("isRemoved") == true || document.getString("status") == "REMOVED") {
                userDao.markUserRemovedByFirebaseUid(uid)
                return@forEach
            }
            val existing = userDao.getUserByFirebaseUid(uid)
            val localId = existing?.id ?: userDao.getNextUserId()
            userDao.insertUser(
                UserEntity(
                    id = localId,
                    username = document.getString("username").orEmpty(),
                    passwordHash = existing?.passwordHash.orEmpty(),
                    fullName = document.getString("fullName").orEmpty(),
                    email = document.getString("email").orEmpty(),
                    sessionToken = existing?.sessionToken,
                    isLoggedIn = existing?.isLoggedIn ?: false,
                    loginTimestamp = document.getLong("lastLoginAt") ?: 0L,
                    role = if (uid == auth.currentUser?.uid) "ADMIN" else document.getString("role") ?: "USER",
                    firebaseUid = uid,
                    authProvider = document.getString("authProvider").orEmpty(),
                    createdAt = document.getLong("createdAt") ?: 0L,
                    isRemoved = false,
                    activitySummary = document.getString("activitySummary") ?: "Registered User"
                )
            )
        }
    }

    suspend fun syncMessagesForCurrentUser() {
        val uid = requireSignedInUid()
        val collection = firestore.collection("admin_messages")
        val broadcasts = collection.whereEqualTo("targetAll", true).get().await().documents
        val targeted = collection.whereArrayContains("targetUsers", uid).get().await().documents
        adminDao.insertAdminMessages((broadcasts + targeted).distinctBy { it.id }.mapNotNull { it.toMessageEntity() })
    }

    suspend fun syncMessagesForAdmin() {
        requireAdmin()
        val documents = firestore.collection("admin_messages").get().await().documents
        adminDao.insertAdminMessages(documents.mapNotNull { it.toMessageEntity() })
    }

    suspend fun syncReportsForAdmin() {
        requireAdmin()
        val documents = firestore.collection("user_reports").get().await().documents
        adminDao.insertUserReports(documents.mapNotNull { it.toReportEntity() })
    }

    suspend fun syncReportsForCurrentUser() {
        val uid = requireSignedInUid()
        syncPendingReports(uid)
        val documents = firestore.collection("user_reports")
            .whereEqualTo("reporterUid", uid)
            .get()
            .await()
            .documents
        adminDao.insertUserReports(documents.mapNotNull { it.toReportEntity() })
    }

    private suspend fun syncPendingReports(uid: String) {
        adminDao.getPendingReports(uid).forEach { report ->
            val document = firestore.collection("user_reports").document(report.id)
            if (!document.get().await().exists()) document.set(report.toFirestore()).await()
            adminDao.markReportSynced(report.id)
        }
    }

    suspend fun removeUser(user: UserEntity) {
        val admin = requireAdmin()
        require(user.firebaseUid.isNotBlank()) { "This account has no Firebase UID and cannot be moderated remotely." }
        require(user.firebaseUid != admin.uid) { "You cannot remove your own administrator account." }
        firestore.collection("users").document(user.firebaseUid).set(
            mapOf(
                "isRemoved" to true,
                "status" to "REMOVED",
                "removedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
        userDao.markUserRemovedByFirebaseUid(user.firebaseUid)
    }

    suspend fun sendAdminMessage(
        title: String,
        body: String,
        messageType: String,
        targetAll: Boolean,
        targetUserUids: List<String>
    ) {
        val sender = requireAdmin()
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
            targetUsersCsv = recipients.joinToString(","),
            senderUid = sender.uid,
            timestamp = timestamp
        )
        document.set(entity.toFirestore(recipients)).await()
        adminDao.insertAdminMessage(entity)
    }

    suspend fun deleteAdminMessage(messageId: String) {
        requireAdmin()
        firestore.collection("admin_messages").document(messageId).delete().await()
        adminDao.deleteAdminMessageById(messageId)
    }

    suspend fun submitUserReport(
        user: UserEntity,
        category: String,
        subject: String,
        description: String
    ) {
        val uid = requireSignedInUid()
        require(user.firebaseUid == uid) { "The active Firebase profile could not be verified." }
        require(category.isNotBlank() && subject.isNotBlank() && description.isNotBlank()) {
            "Complete the category, subject, and description."
        }
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
        document.set(report.toFirestore()).await()
        adminDao.markReportSynced(report.id)
    }

    suspend fun replyAndResolveReport(report: UserReportEntity, status: String, reply: String) {
        val sender = requireAdmin()
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
            senderUid = sender.uid,
            timestamp = timestamp
        )
        val batch = firestore.batch()
        batch.update(
            firestore.collection("user_reports").document(report.id),
            mapOf("status" to status, "adminReply" to reply.trim())
        )
        batch.set(messageDocument, message.toFirestore(listOf(report.reporterUid)))
        batch.commit().await()
        adminDao.updateReportStatusAndReply(report.id, status, reply.trim())
        adminDao.insertAdminMessage(message)
    }

    suspend fun deleteUserReport(reportId: String) {
        requireAdmin()
        firestore.collection("user_reports").document(reportId).delete().await()
        adminDao.deleteUserReportById(reportId)
    }

    private suspend fun requireAdmin() = auth.currentUser?.also { user ->
        val claims = user.getIdToken(true).await().claims
        val firebaseClaims = claims["firebase"] as? Map<*, *>
        val secondFactor = firebaseClaims?.get("sign_in_second_factor") as? String
        check(
            claims["admin"] == true &&
                claims["email_verified"] == true &&
                !secondFactor.isNullOrBlank()
        ) {
            "Admin access requires a verified email, the server-issued admin claim, and a Firebase MFA sign-in."
        }
        val profile = firestore.collection("users").document(user.uid).get().await()
        check(profile.exists() && profile.getBoolean("isRemoved") != true &&
            profile.getString("status") != "REMOVED"
        ) { "This administrator account has been removed." }
    } ?: error("Sign in with an authorized Firebase account.")

    private fun requireSignedInUid(): String =
        auth.currentUser?.uid ?: error("Sign in to use admin messages and reports.")

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
