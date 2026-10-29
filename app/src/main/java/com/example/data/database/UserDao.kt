package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user credentials, authentication, and session state.
 */
@Dao
interface UserDao {

    /**
     * Retrieves the primary user profile and credentials.
     */
    @Query("SELECT * FROM users ORDER BY is_logged_in DESC, login_timestamp DESC LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    /**
     * Reactive stream observing the current user profile and session state.
     */
    @Query("SELECT * FROM users ORDER BY is_logged_in DESC, login_timestamp DESC LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    /**
     * Retrieves the active logged-in user session, or null if no session is active.
     */
    @Query("SELECT * FROM users WHERE is_logged_in = 1 ORDER BY login_timestamp DESC LIMIT 1")
    suspend fun getActiveSession(): UserEntity?

    /**
     * Returns count of active logged-in user sessions.
     */
    @Query("SELECT COUNT(*) FROM users WHERE is_logged_in = 1")
    suspend fun getActiveSessionCount(): Int

    /**
     * Looks up a user by their username.
     */
    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :identifier COLLATE NOCASE OR email = :identifier COLLATE NOCASE LIMIT 1")
    suspend fun getUserByUsernameOrEmail(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE firebase_uid = :firebaseUid LIMIT 1")
    suspend fun getUserByFirebaseUid(firebaseUid: String): UserEntity?

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM users")
    suspend fun getNextUserId(): Long

    /**
     * Inserts or replaces a user record (e.g., on login or credential setup).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Updates an existing user record.
     */
    @Update
    suspend fun updateUser(user: UserEntity)

    /**
     * Marks the current user session as logged out and invalidates session token.
     */
    @Query("UPDATE users SET is_logged_in = 0, session_token = NULL")
    suspend fun logout()

    @Query("UPDATE users SET is_logged_in = 0, session_token = NULL WHERE id != :activeUserId")
    suspend fun logoutOtherUsers(activeUserId: Long)

    @Query("UPDATE users SET is_logged_in = 1, session_token = :sessionToken, login_timestamp = :loginTimestamp WHERE id = :userId")
    suspend fun activateUser(userId: Long, sessionToken: String, loginTimestamp: Long)

    /**
     * Completely wipes stored user credentials and session data.
     */
    @Query("DELETE FROM users")
    suspend fun clearUsers()
}
