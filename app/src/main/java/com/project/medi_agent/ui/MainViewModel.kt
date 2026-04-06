package com.project.medi_agent.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.project.medi_agent.data.AppDatabase
import com.project.medi_agent.data.ReminderScheduler
import com.project.medi_agent.data.network.ApiRepository
import com.project.medi_agent.data.network.ChatMessageRequest
import com.project.medi_agent.data.network.ChatStreamChunk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatSessionDao = db.chatSessionDao()
    private val chatMessageDao = db.chatMessageDao()
    private val healthProfileDao = db.healthProfileDao()
    private val apiRepository = ApiRepository(application)
    private val reminderScheduler = ReminderScheduler(application)
    private val gson = Gson()
    private val context = application.applicationContext

    // 1. 会话与消息
    val sessions: StateFlow<List<ChatSession>> = chatSessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSessionId.combine(sessions) { id, list ->
        id?.let { selectedId -> list.find { it.id == selectedId } } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = currentSession.flatMapLatest { session ->
        if (session == null) flowOf(emptyList()) else chatMessageDao.getMessagesForSession(session.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 2. 提醒列表
    val reminders: StateFlow<List<MedicationReminder>> = db.medicationReminderDao().getAllReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 3. 待确认的提醒
    private val _pendingReminder = MutableStateFlow<MedicationReminder?>(null)
    val pendingReminder = _pendingReminder.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _ttsEvent = MutableSharedFlow<String>()
    val ttsEvent = _ttsEvent.asSharedFlow()

    // 网络/服务错误提示（用于在 UI 中显示大字号温情提示）
    private val _networkError = MutableStateFlow<String?>(null)
    val networkError = _networkError.asStateFlow()

    fun clearNetworkError() {
        _networkError.value = null
    }

    // Health profile state for UI
    private val _healthProfiles = MutableStateFlow<List<HealthProfile>>(emptyList())
    val healthProfiles: StateFlow<List<HealthProfile>> = _healthProfiles.asStateFlow()

    init {
        // load initial health profiles
        viewModelScope.launch {
            loadHealthProfiles()
        }
    }

    fun loadHealthProfiles() {
        viewModelScope.launch {
            try {
                val list = healthProfileDao.getAllProfiles()
                _healthProfiles.value = list
            } catch (e: Exception) {
                _healthProfiles.value = emptyList()
            }
        }
    }

    fun upsertHealthProfile(key: String, content: String) {
        viewModelScope.launch {
            try {
                healthProfileDao.insertProfile(HealthProfile(key = key, content = content))
                loadHealthProfiles()
            } catch (e: Exception) {
                // ignore or log
            }
        }
    }

    fun deleteHealthProfile(key: String) {
        viewModelScope.launch {
            try {
                healthProfileDao.deleteProfile(key)
                loadHealthProfiles()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun selectSession(session: ChatSession) {
        _currentSessionId.value = session.id
    }

    fun createNewSession() {
        viewModelScope.launch {
            val newSession = ChatSession(title = "新对话")
            chatSessionDao.insertSession(newSession)
            _currentSessionId.value = newSession.id
        }
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            chatSessionDao.deleteSessionById(session.id)
            chatMessageDao.deleteMessagesForSession(session.id)
            if (_currentSessionId.value == session.id) _currentSessionId.value = null
        }
    }

    // --- 提醒操作 ---
    fun confirmReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            db.medicationReminderDao().insertReminder(reminder)
            reminderScheduler.schedule(reminder)
            _pendingReminder.value = null
        }
    }

    fun dismissPendingReminder() {
        _pendingReminder.value = null
    }

    fun deleteReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            reminderScheduler.cancel(reminder)
            db.medicationReminderDao().deleteReminder(reminder)
        }
    }

    // --- 调试工具 ---
    fun debugClearAllData() {
        viewModelScope.launch {
            db.clearAllTables()
            _currentSessionId.value = null
        }
    }

    fun debugInjectTestReminder() {
        viewModelScope.launch {
            val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(System.currentTimeMillis() + 65000))
            val testReminder = MedicationReminder(medicineName = "测试维他命 (Debug)", dosage = "1片", time = time)
            db.medicationReminderDao().insertReminder(testReminder)
            reminderScheduler.schedule(testReminder)
        }
    }

    fun debugShowFullscreenReminder() {
        val intent = Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_MEDICINE_NAME", "调试药品 (Debug)")
            putExtra("EXTRA_DOSAGE", "99 粒")
        }
        context.startActivity(intent)
    }

    // 记录服药日志：当用户在提醒界面点击“我已吃完”时调用
    fun recordMedicationTaken(medicineName: String, status: String = "TAKEN") {
        viewModelScope.launch {
            try {
                val log = MedicationLog(medicineName = medicineName, status = status)
                db.medicationLogDao().insertLog(log)
            } catch (e: Exception) {
                // 忽略插入错误，避免影响主流程；可扩展为上报或显示错误
            }
        }
    }

    fun sendMessage(text: String, imageBase64: String?) {
        viewModelScope.launch {
            val session = currentSession.value ?: return@launch
            if (_isSending.value || (text.isBlank() && imageBase64 == null)) return@launch
            
            _isSending.value = true
            val history = messages.value

            val userMsg = ChatMessage(sessionId = session.id, text = text, isUser = true, imageBase64 = imageBase64)
            val userMsgId = chatMessageDao.insertMessage(userMsg)
            val userMsgWithId = userMsg.copy(messageId = userMsgId)

            runAgentLoop(session, history + userMsgWithId)
        }
    }

    private suspend fun runAgentLoop(session: ChatSession, history: List<ChatMessage>) {
        val toolOutputs = mutableListOf<ChatMessageRequest>()
        var aiMsgId: Long? = null
        var fullResponseForTTS = ""

        var streamFlow = apiRepository.chatStream(history, emptyList())
        
        var loopCount = 0
        while (loopCount < 3) {
            loopCount++
            var currentToolCall: Triple<String, String, String>? = null
            
            if (aiMsgId == null) {
                val placeholder = ChatMessage(sessionId = session.id, text = "", isUser = false)
                aiMsgId = chatMessageDao.insertMessage(placeholder)
            }

            var currentAiMsg = ChatMessage(messageId = aiMsgId, sessionId = session.id, text = "", isUser = false)

            streamFlow.collect { chunk ->
                currentAiMsg = when (chunk) {
                    is ChatStreamChunk.Thinking -> currentAiMsg.copy(thinkingText = currentAiMsg.thinkingText + chunk.text)
                    is ChatStreamChunk.Content -> {
                        fullResponseForTTS += chunk.text
                        currentAiMsg.copy(text = currentAiMsg.text + chunk.text)
                    }
                    is ChatStreamChunk.ToolCall -> {
                        currentToolCall = Triple(chunk.id, chunk.name, chunk.arguments)
                        currentAiMsg.copy(text = currentAiMsg.text + "\n*(正在查询/同步...)*")
                    }
                        is ChatStreamChunk.Error -> {
                            // 同时把错误信息提升为 UI 层的友好提示
                            _networkError.value = chunk.message
                            currentAiMsg.copy(text = "系统故障: ${chunk.message}")
                        }
                }
                chatMessageDao.insertMessage(currentAiMsg)
            }

            if (currentToolCall != null) {
                val (id, name, args) = currentToolCall!!
                val output = executeTool(name, args)
                toolOutputs.add(ChatMessageRequest(role = "tool", toolCallId = id, content = output))
                streamFlow = apiRepository.chatStream(history, toolOutputs)
            } else {
                break
            }
        }

        if (fullResponseForTTS.isNotBlank()) _ttsEvent.emit(fullResponseForTTS)
        updateSessionTitle(session, history.last().text)
        _isSending.value = false
    }

    private suspend fun executeTool(name: String, argsJson: String): String {
        return try {
            val args = gson.fromJson(argsJson, Map::class.java)
            when (name) {
                "add_medication_reminder" -> {
                    val reminder = MedicationReminder(
                        medicineName = args["medicine_name"]?.toString() ?: "未知",
                        dosage = args["dosage"]?.toString() ?: "",
                        time = args["time"]?.toString() ?: ""
                    )
                    _pendingReminder.value = reminder
                    "已触发用户确认弹窗"
                }
                "get_health_profile" -> {
                    val key = args["key"]?.toString() ?: "all"
                    if (key == "all") {
                        val all = healthProfileDao.getAllProfiles()
                        if (all.isEmpty()) "暂无健康档案记录" else all.joinToString { "${it.key}: ${it.content}" }
                    } else {
                        val profile = healthProfileDao.getProfileByKey(key)
                        profile?.content ?: "暂无此项记录"
                    }
                }
                "update_health_profile" -> {
                    val key = args["key"]?.toString() ?: ""
                    val content = args["content"]?.toString() ?: ""
                    if (key.isNotBlank()) {
                        healthProfileDao.insertProfile(HealthProfile(key, content))
                        // 同步更新 UI 状态
                        loadHealthProfiles()
                        "已成功更新健康档案: $key"
                    } else "更新失败: 键名为空"
                }
                else -> "未知工具: $name"
            }
        } catch (e: Exception) {
            "工具执行出错: ${e.message}"
        }
    }

    private suspend fun updateSessionTitle(session: ChatSession, text: String) {
        if (session.title == "新对话" || session.title == "分析咨询") {
            val newTitle = if (text.length > 12) text.take(12) + "..." else text
            chatSessionDao.insertSession(session.copy(title = newTitle, lastUpdateTime = System.currentTimeMillis()))
        } else {
            chatSessionDao.insertSession(session.copy(lastUpdateTime = System.currentTimeMillis()))
        }
    }
}
