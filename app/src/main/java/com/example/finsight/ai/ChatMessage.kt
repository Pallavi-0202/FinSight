package com.example.finsight.ai

/**
 * Data class representing a chat message in the conversation history.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean
)
