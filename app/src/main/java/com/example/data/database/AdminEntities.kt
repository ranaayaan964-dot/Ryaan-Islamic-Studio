package com.example.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_messages")
data class AdminMessageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val messageType: String,
    val targetAll: Boolean,
    val targetUsersCsv: String,
    val senderName: String = "Admin Ayaan",
    val senderUid: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_reports")
data class UserReportEntity(
    @PrimaryKey val id: String,
    val reporterUid: String,
    val reporterUsername: String,
    val reporterEmail: String,
    val reporterFullName: String,
    val category: String,
    val subject: String,
    val description: String,
    val status: String = "OPEN",
    val adminReply: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false
)
