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
        3. 【记忆与检索】：你可以通过工具查询用户的健康档案（如过敏史、既往病史），并在给出建议前参考这些信息。
        
        工作流程：
        - 如果用户询问某个药是否能吃，或者你准备给出用药建议，请务必先调用 `get_health_profile` 查询其禁忌史。
        - 如果用户提到了自己的健康情况（如“我血糖高”或“我对海鲜过敏”），请调用 `update_health_profile` 记录下来。
        - 当用户提到需要提醒吃药时，请调用 `add_medication_reminder` 工具。
        
        语气要求：极度温情，称呼用户为“爷爷”或“奶奶”，解释通俗易懂。
    """.trimIndent()

    private val tools = listOf(
        ToolDefinition(
            function = FunctionDefinition(
                name = "add_medication_reminder",
                description = "为用户添加一个用药闹钟提醒",
                parameters = ParametersDefinition(
                    properties = mapOf(
                        "medicine_name" to PropertyDefinition("string", "药品名称"),
                        "dosage" to PropertyDefinition("string", "剂量"),
                        "time" to PropertyDefinition("string", "提醒时间 (HH:mm)"),
                        "instruction" to PropertyDefinition("string", "特别医嘱")
                    ),
                    required = listOf("medicine_name", "dosage", "time")
                )
            )
        ),
        ToolDefinition(
            function = FunctionDefinition(
                name = "get_health_profile",
                description = "查询用户的健康档案（过敏史、既往病史等）",
                parameters = ParametersDefinition(
                    properties = mapOf(
                        "key" to PropertyDefinition("string", "查询类别，可选值：'allergies'(过敏史), 'chronic_diseases'(慢性病), 'all'(全部)")
                    ),
                    required = listOf("key")
                )
            )
        ),
        ToolDefinition(
            function = FunctionDefinition(
                name = "update_health_profile",
                description = "更新用户的健康档案信息",
                parameters = ParametersDefinition(
                    properties = mapOf(
                        "key" to PropertyDefinition("string", "类别，如 'allergies' 或 'chronic_diseases'"),
                        "content" to PropertyDefinition("string", "具体内容，如 '青霉素过敏' 或 '高血压'")
                    ),
                    required = listOf("key", "content")
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
        messages: List<com.project.medi_agent.ui.ChatMessage>,
        toolOutputs: List<ChatMessageRequest> = emptyList() // 支持注入工具执行结果
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
        
        // 组装历史消息
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
        
        // 注入工具执行结果 (Observation)
        requestMessages.addAll(toolOutputs)

        val request = ChatCompletionRequest(
            model = modelName,
            messages = requestMessages,
            stream = true,
            tools = if (isCurrentTurnVision) null else tools,
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

                        val reasoning = delta.reasoningContent ?: delta.thought ?: ""
                        if (reasoning.isNotEmpty()) emit(ChatStreamChunk.Thinking(reasoning))

                        val content = delta.content ?: ""
                        if (content.isNotEmpty()) emit(ChatStreamChunk.Content(content))

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
            toolCallBuffers.values.forEach { (id, name, args) ->
                emit(ChatStreamChunk.ToolCall(id, name, args.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
}
