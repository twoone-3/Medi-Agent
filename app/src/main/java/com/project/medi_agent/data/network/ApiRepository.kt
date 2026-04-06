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
    // 本地医学知识库缓存
    private var localKbCache: List<KnowledgeItem>? = null

    private val systemPrompt = """
        你是“银发医倚”，一位专为老年人设计的医疗健康助手。
        请始终以极其温情、通俗的口吻回答，称呼用户为“爷爷”或“奶奶”，用比喻和家常话让说明易懂。

        当需要给出用药或健康建议时：
        - 先调用工具 `get_health_profile` 查询用户健康档案（过敏史、慢性病等）。
        - 若需记录或更新健康信息，调用 `update_health_profile`。
        - 若需要为用户安排用药闹钟，调用 `add_medication_reminder`（并在返回中提供 medicine_name、dosage、time）。

        在你的 `reasoning_content`（思考过程）中，简短说明你检索了哪些信息（例如是否查询了健康档案或本地医学库），以及做出建议的关键依据；不要复杂分工，只需清晰说明推理要点。

        最终给用户的 `content` 必须温暖、简单、易懂；在建议前说明你已查看的关键事实（例如："查到您对青霉素过敏"），并给出可执行的下一步（例如：是否要我为您设闹钟）。
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
        toolOutputs: List<ChatMessageRequest> = emptyList()
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

        // 构建请求消息列表，并注入 system prompt
        val requestMessages = mutableListOf<ChatMessageRequest>()
        requestMessages.add(ChatMessageRequest(role = "system", content = systemPrompt))

        // 本地 RAG：如启用则检索本地 medical_knowledge.json，并将 top-N 注入为系统上下文
        val useLocalKb = prefs.getBoolean("use_local_kb", true)
        if (useLocalKb && !isCurrentTurnVision) {
            val query = lastUserMessage?.text ?: messages.joinToString(" ") { it.text }
            val hits = retrieveLocalKb(query, 3)
            if (hits.isNotEmpty()) {
                val kbText = hits.joinToString("\n\n") { "标题: ${it.title}\n标签: ${it.tags.joinToString()}\n内容: ${it.content}" }
                requestMessages.add(
                    ChatMessageRequest(role = "system", content = "本地医学知识库检索到如下条目：\n$kbText")
                )
            }
        }

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

    // 加载本地 medical_knowledge.json 到缓存
    private fun loadLocalKbIfNeeded() {
        if (localKbCache != null) return
        try {
            val stream = context.assets.open("medical_knowledge.json")
            val text = stream.bufferedReader().use { it.readText() }
            val arr = gson.fromJson(text, Array<KnowledgeItem>::class.java)
            localKbCache = arr?.toList() ?: emptyList()
        } catch (e: Exception) {
            localKbCache = emptyList()
        }
    }

    // 简单关键字检索：统计 query 中词在 title/content/tags 的出现次数，返回 topN
    private fun retrieveLocalKb(query: String?, topN: Int = 3): List<KnowledgeItem> {
        if (query.isNullOrBlank()) return emptyList()
        loadLocalKbIfNeeded()
        val kb = localKbCache ?: return emptyList()
        val qTokens = query.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }
        if (qTokens.isEmpty()) return emptyList()
        val scored = kb.map { item ->
            val hay = (item.title + " " + item.content + " " + item.tags.joinToString(" ")).lowercase()
            val score = qTokens.sumOf { token -> Regex(Regex.escape(token)).findAll(hay).count() }
            Pair(item, score)
        }.filter { it.second > 0 }
        return scored.sortedByDescending { it.second }.map { it.first }.take(topN)
    }
}
