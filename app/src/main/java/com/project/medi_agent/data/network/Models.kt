package com.project.medi_agent.data.network

import com.google.gson.annotations.SerializedName

// --- 通用聊天模型 (OpenAI 兼容) ---

data class ChatMessageRequest(
    val role: String,
    val content: Any // 纯文本传 String，多模态传 List<Map<String, Any>>
)

data class ChatThinkingConfig(
    val type: String = "enabled"
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageRequest>,
    val stream: Boolean = false,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val temperature: Double = 1.0,
    val thinking: ChatThinkingConfig? = null
)

data class ChatCompletionResponse(
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val message: ChatMessageResponse?,
    val delta: ChatDelta? = null,
    val finish_reason: String? = null
)

data class ChatMessageResponse(
    val role: String,
    val content: String
)

data class ChatDelta(
    val content: String? = null,
    @SerializedName("reasoning_content") val reasoningContent: String? = null,
    val reasoning: String? = null,
    val thought: String? = null
)

