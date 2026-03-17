package com.project.medi_agent.data.network

import com.google.gson.annotations.SerializedName

// --- 通用聊天模型 (OpenAI 兼容) ---

data class ChatMessageRequest(
    val role: String,
    val content: Any?, // 纯文本传 String，多模态传 List<Map<String, Any>>
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null
)

data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall
)

data class FunctionCall(
    val name: String,
    val arguments: String // JSON string
)

data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDefinition
)

data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: ParametersDefinition
)

data class ParametersDefinition(
    val type: String = "object",
    val properties: Map<String, PropertyDefinition>,
    val required: List<String> = emptyList()
)

data class PropertyDefinition(
    val type: String,
    val description: String,
    val enum: List<String>? = null
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
    val thinking: ChatThinkingConfig? = null,
    val tools: List<ToolDefinition>? = null,
    @SerializedName("tool_choice") val toolChoice: String? = null
)

data class ChatCompletionResponse(
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val message: ChatMessageResponse?,
    val delta: ChatDelta? = null,
    @SerializedName("finish_reason") val finishReason: String? = null
)

data class ChatMessageResponse(
    val role: String,
    val content: String?,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null
)

data class ChatDelta(
    val content: String? = null,
    @SerializedName("reasoning_content") val reasoningContent: String? = null,
    val reasoning: String? = null,
    val thought: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<ToolCallDelta>? = null
)

data class ToolCallDelta(
    val index: Int,
    val id: String?,
    val type: String?,
    val function: FunctionCallDelta?
)

data class FunctionCallDelta(
    val name: String?,
    val arguments: String?
)
