package com.example.data.database

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "chat_history")
data class ChatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val messageText: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val scholarSource: String = "Gemini Ultra-Scholar",
    @ColumnInfo(defaultValue = "0") val isFatwa: Boolean = false,
    @ColumnInfo(defaultValue = "''") val referencesJson: String = ""
)

