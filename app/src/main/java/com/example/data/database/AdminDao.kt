package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {
    @Query("SELECT * FROM admin_messages ORDER BY timestamp DESC")
    fun getAllAdminMessagesFlow(): Flow<List<AdminMessageEntity>>

    @Query("SELECT * FROM admin_messages ORDER BY timestamp DESC")
    suspend fun getAllAdminMessages(): List<AdminMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminMessage(message: AdminMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminMessages(messages: List<AdminMessageEntity>)

    @Query("DELETE FROM admin_messages WHERE id = :id")
    suspend fun deleteAdminMessageById(id: String)

    @Query("SELECT * FROM user_reports ORDER BY timestamp DESC")
    fun getAllUserReportsFlow(): Flow<List<UserReportEntity>>

    @Query("SELECT * FROM user_reports WHERE reporterUid = :uid ORDER BY timestamp DESC")
    fun getReportsByUserFlow(uid: String): Flow<List<UserReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserReport(report: UserReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserReports(reports: List<UserReportEntity>)

    @Query("UPDATE user_reports SET status = :status, adminReply = :adminReply, is_synced = 1 WHERE id = :reportId")
    suspend fun updateReportStatusAndReply(reportId: String, status: String, adminReply: String)

    @Query("SELECT * FROM user_reports WHERE reporterUid = :uid AND is_synced = 0")
    suspend fun getPendingReports(uid: String): List<UserReportEntity>

    @Query("UPDATE user_reports SET is_synced = 1 WHERE id = :reportId")
    suspend fun markReportSynced(reportId: String)

    @Query("DELETE FROM user_reports WHERE id = :reportId")
    suspend fun deleteUserReportById(reportId: String)
}
