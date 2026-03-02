package com.project.medi_agent.ui

import java.util.UUID

// Shared UI models
data class ChatMessage(
    val id: Int,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val thinkingText: String = "",
    val imageBase64: String? = null // For vision support
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val lastUpdateTime: Long = System.currentTimeMillis()
)

enum class Screen {
    Chat, Settings
}
