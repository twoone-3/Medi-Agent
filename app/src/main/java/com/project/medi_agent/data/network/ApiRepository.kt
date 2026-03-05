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
    data class Error(val message: String) : ChatStreamChunk()
}

class ApiRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("mediagent_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val systemPrompt = """
        你是一个专门为老年人设计的医疗陪诊助手，名字叫“银发医倚”。
        你的任务是解读医疗报告、分析药盒图片，并提供健康咨询。
        你的说话风格必须遵循：【极致温情】、【通俗易懂】、【视觉友好】、【安全第一】。
    """.trimIndent()

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

        val validMessages = messages.filter { it.text.isNotEmpty() || it.imageBase64 != null }

        val requestMessages = mutableListOf<ChatMessageRequest>()
        requestMessages.add(ChatMessageRequest(role = "system", content = systemPrompt))
        
        validMessages.forEach { msg ->
            val role = if (msg.isUser) "user" else "assistant"

            val content: Any = if (msg.imageBase64 != null) {
                if (isCurrentTurnVision) {
                    // 场景 A：当前是视觉模式 -> 发送完整多模态结构
                    listOf(
                        mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,${msg.imageBase64}")),
                        mapOf("type" to "text", "text" to msg.text.ifBlank { "分析图片" })
                    )
                } else {
                    // 场景 B：当前是文字模式 -> 将历史图片降级为文字描述，防止 API 报错
                    // 文本模型通过读取 assistant 之前的分析报告来“回忆”图片内容
                    "[用户发送了图片]: ${msg.text.ifBlank { "请分析这张图片" }}"
                }
            } else {
                msg.text
            }
            requestMessages.add(ChatMessageRequest(role = role, content = content))
        }

        val request = ChatCompletionRequest(
            model = modelName,
            messages = requestMessages,
            stream = true,
            maxTokens = 4096,
            temperature = 0.7,
            thinking = if (isCurrentTurnVision) null else ChatThinkingConfig(type = "enabled")
        )

        val responseBody: ResponseBody? = try {
            val call = service.chatCompletionStream(request)
            val response = call.execute()
            if (response.isSuccessful) response.body() else {
                val errorMsg = response.errorBody()?.string() ?: "未知错误"
                emit(ChatStreamChunk.Error("请求失败: $errorMsg"))
                null
            }
        } catch (e: Exception) {
            emit(ChatStreamChunk.Error("网络错误: ${e.message}"))
            null
        }

        responseBody?.use { body ->
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("data:") == true) {
                    val data = line.substringAfter("data:").trim()
                    if (data == "[DONE]") break
                    try {
                        val choiceResponse = gson.fromJson(data, ChatCompletionResponse::class.java)
                        val delta = choiceResponse.choices.firstOrNull()?.delta
                        val reasoning = delta?.reasoningContent ?: delta?.reasoning ?: delta?.thought ?: ""
                        if (reasoning.isNotEmpty()) emit(ChatStreamChunk.Thinking(reasoning))
                        val content = delta?.content ?: ""
                        if (content.isNotEmpty()) emit(ChatStreamChunk.Content(content))
                    } catch (e: Exception) {}
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
