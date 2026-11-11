package com.example.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing stored user credentials, authentication state,
 * and active session token.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Long = 1L,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String = "",

    @ColumnInfo(name = "full_name")
    val fullName: String = "Ayaan User",

    @ColumnInfo(name = "email")
    val email: String = "ranaayaan964@gmail.com",

    @ColumnInfo(name = "session_token")
    val sessionToken: String? = null,

    @ColumnInfo(name = "is_logged_in")
    val isLoggedIn: Boolean = true,

    @ColumnInfo(name = "login_timestamp")
    val loginTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "role")
    val role: String = "User",

    @ColumnInfo(name = "firebase_uid", defaultValue = "''")
    val firebaseUid: String = "",

    @ColumnInfo(name = "auth_provider", defaultValue = "''")
    val authProvider: String = "",

    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_removed", defaultValue = "0")
    val isRemoved: Boolean = false,

    @ColumnInfo(name = "activity_summary", defaultValue = "'Active in App'")
    val activitySummary: String = "Active in App"
)
