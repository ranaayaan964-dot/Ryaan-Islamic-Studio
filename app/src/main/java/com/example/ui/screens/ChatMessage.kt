package com.example.ui.screens

/**
 * Lightweight chat message model used by the companion UI pipeline.
 *
 * This keeps the conversation state explicit and easy to render or transform
 * without depending on the underlying Room entity directly.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean
)
