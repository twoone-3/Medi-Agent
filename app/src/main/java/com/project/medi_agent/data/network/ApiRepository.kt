package com.project.medi_agent.data.network

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import java.io.BufferedReader
import java.io.InputStreamReader

sealed class ChatStreamChunk {
    data class Thinking(val text: String) : ChatStreamChunk()
    data class Content(val text: String) : ChatStreamChunk()
    data class ToolCall(val id: String, val name: String, val arguments: String) : ChatStreamChunk()
    data class Error(val message: String) : ChatStreamChunk()
}

class ApiRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("mediagent_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val systemPrompt = """
        你是一个专门为老年人设计的医疗健康 Agent，名字叫“银发医倚”。
        
        你的核心能力：
        1. 解读医疗报告、分析药盒图片。
        2. 【主动行动】：你可以通过调用工具来帮用户设置用药提醒。
        
        工作流程：
        - 当用户提到需要提醒吃药、或者你分析出需要服药时，请直接调用 `add_medication_reminder` 工具。
        - 语气要极度温情，称呼用户为“爷爷”或“奶奶”。
        - 解释要通俗易懂，多用比喻。
    """.trimIndent()

    private val tools = listOf(
        ToolDefinition(
            function = FunctionDefinition(
                name = "add_medication_reminder",
                description = "为用户添加一个用药闹钟提醒",
                parameters = ParametersDefinition(
                    properties = mapOf(
                        "medicine_name" to PropertyDefinition("string", "药品名称，例如：感冒灵"),
                        "dosage" to PropertyDefinition("string", "剂量，例如：一袋 或 2粒"),
                        "time" to PropertyDefinition("string", "提醒时间，格式为 HH:mm，例如：08:30"),
                        "instruction" to PropertyDefinition("string", "特别医嘱，例如：饭后服用")
                    ),
                    required = listOf("medicine_name", "dosage", "time")
                )
            )
        )
    )

    private fun getChatService(): ChatService? {
        val baseUrl = prefs.getString("text_base_url", null) ?: "https://open.bigmodel.cn/api/paas/v4/"
        val apiKey = prefs.getString("text_api_key", null) ?: ""
        if (apiKey.isBlank()) return null
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return RetrofitClient.createService(formattedBaseUrl, ChatService::class.java, apiKey)
    }

    fun chatStream(
        messages: List<com.project.medi_agent.ui.ChatMessage>
    ): Flow<ChatStreamChunk> = flow {
        val service = getChatService() ?: run {
            emit(ChatStreamChunk.Error("错误: 请先在设置中配置 API Key"))
            return@flow
        }

        val lastUserMessage = messages.lastOrNull { it.isUser }
        val isCurrentTurnVision = lastUserMessage?.imageBase64 != null
        
        val modelName = if (isCurrentTurnVision) {
            prefs.getString("image_base_url", null) ?: "glm-4.6v-flash"
        } else {
            prefs.getString("text_model_name", null) ?: "glm-4.7-flash"
        }

        val requestMessages = mutableListOf<ChatMessageRequest>()
        requestMessages.add(ChatMessageRequest(role = "system", content = systemPrompt))
        
        messages.forEach { msg ->
            val role = if (msg.isUser) "user" else "assistant"
            val content: Any? = if (msg.imageBase64 != null) {
                if (isCurrentTurnVision) {
                    listOf(
                        mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,${msg.imageBase64}")),
                        mapOf("type" to "text", "text" to msg.text.ifBlank { "分析图片" })
                    )
                } else "[用户发送了图片]: ${msg.text}"
            } else msg.text
            requestMessages.add(ChatMessageRequest(role = role, content = content))
        }

        val request = ChatCompletionRequest(
            model = modelName,
            messages = requestMessages,
            stream = true,
            tools = if (isCurrentTurnVision) null else tools, // 视觉模型有时不支持 tools
            toolChoice = "auto"
        )

        val responseBody: ResponseBody? = try {
            val call = service.chatCompletionStream(request)
            val response = call.execute()
            if (response.isSuccessful) response.body() else {
                emit(ChatStreamChunk.Error("请求失败: ${response.code()}"))
                null
            }
        } catch (e: Exception) {
            emit(ChatStreamChunk.Error("网络错误: ${e.message}"))
            null
        }

        responseBody?.use { body ->
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String?
            val toolCallBuffers = mutableMapOf<Int, Triple<String, String, StringBuilder>>()

            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("data:") == true) {
                    val data = line.substringAfter("data:").trim()
                    if (data == "[DONE]") break
                    try {
                        val choiceResponse = gson.fromJson(data, ChatCompletionResponse::class.java)
                        val delta = choiceResponse.choices.firstOrNull()?.delta ?: continue

                        // 处理推理
                        val reasoning = delta.reasoningContent ?: delta.thought ?: ""
                        if (reasoning.isNotEmpty()) emit(ChatStreamChunk.Thinking(reasoning))

                        // 处理内容
                        val content = delta.content ?: ""
                        if (content.isNotEmpty()) emit(ChatStreamChunk.Content(content))

                        // 处理 Tool Calls (流式拼接)
                        delta.toolCalls?.forEach { toolDelta ->
                            val index = toolDelta.index
                            val buffer = toolCallBuffers.getOrPut(index) {
                                Triple(toolDelta.id ?: "", toolDelta.function?.name ?: "", StringBuilder())
                            }
                            toolDelta.function?.arguments?.let { buffer.third.append(it) }
                        }
                    } catch (e: Exception) {}
                }
            }
            // 发射拼接完成的 ToolCall
            toolCallBuffers.values.forEach { (id, name, args) ->
                emit(ChatStreamChunk.ToolCall(id, name, args.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
}
