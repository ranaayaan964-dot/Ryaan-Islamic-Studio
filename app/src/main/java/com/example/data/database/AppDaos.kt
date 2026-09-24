package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_history ORDER BY id ASC")
    fun getAllChatHistoryFlow(): Flow<List<ChatHistoryEntity>>

    @Query("SELECT * FROM chat_history ORDER BY id ASC")
    suspend fun getAllChatHistory(): List<ChatHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatHistoryEntity): Long

    @Query("DELETE FROM chat_history")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM chat_history")
    suspend fun getMessageCount(): Int
}

