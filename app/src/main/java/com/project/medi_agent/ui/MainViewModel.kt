package com.project.medi_agent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.project.medi_agent.data.AppDatabase
import com.project.medi_agent.data.network.ApiRepository
import com.project.medi_agent.data.network.ChatStreamChunk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatSessionDao = db.chatSessionDao()
    private val chatMessageDao = db.chatMessageDao()
    private val apiRepository = ApiRepository(application)
    private val gson = Gson()

    // 1. 会话列表
    val sessions: StateFlow<List<ChatSession>> = chatSessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 2. 当前会话指针
    private val _currentSessionId = MutableStateFlow<String?>(null)

    // 3. 当前会话对象
    val currentSession: StateFlow<ChatSession?> = _currentSessionId.combine(sessions) { id, list ->
        id?.let { selectedId -> list.find { it.id == selectedId } } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 4. 消息列表
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = currentSession.flatMapLatest { session ->
        if (session == null) flowOf(emptyList()) else chatMessageDao.getMessagesForSession(session.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 5. 提醒列表
    val reminders: StateFlow<List<MedicationReminder>> = db.medicationReminderDao().getAllReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _ttsEvent = MutableSharedFlow<String>()
    val ttsEvent = _ttsEvent.asSharedFlow()

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

    fun deleteReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            db.medicationReminderDao().deleteReminder(reminder)
        }
    }

    // --- 开发者工具：清除所有数据 ---
    fun debugClearAllData() {
        viewModelScope.launch {
            db.clearAllTables()
            _currentSessionId.value = null
        }
    }

    // --- 开发者工具：注入测试提醒 ---
    fun debugInjectTestReminder() {
        viewModelScope.launch {
            val testReminder = MedicationReminder(
                medicineName = "测试维他命 (Debug)",
                dosage = "1片",
                time = "09:00"
            )
            db.medicationReminderDao().insertReminder(testReminder)
        }
    }

    fun sendMessage(text: String, imageBase64: String?) {
        viewModelScope.launch {
            val session = currentSession.value ?: return@launch
            if (_isSending.value || (text.isBlank() && imageBase64 == null)) return@launch
            
            _isSending.value = true
            val currentHistory = messages.value

            val userMsg = ChatMessage(sessionId = session.id, text = text, isUser = true, imageBase64 = imageBase64)
            val userMsgId = chatMessageDao.insertMessage(userMsg)
            val userMsgWithId = userMsg.copy(messageId = userMsgId)

            val aiMsgPlaceholder = ChatMessage(sessionId = session.id, text = "", isUser = false)
            val aiMsgId = chatMessageDao.insertMessage(aiMsgPlaceholder)

            try {
                val historyForApi = currentHistory + userMsgWithId
                var currentAiMsg = aiMsgPlaceholder.copy(messageId = aiMsgId)
                var fullResponseForTTS = ""
                var pendingToolCall: Triple<String, String, String>? = null

                apiRepository.chatStream(historyForApi).collect { chunk ->
                    currentAiMsg = when (chunk) {
                        is ChatStreamChunk.Thinking -> currentAiMsg.copy(thinkingText = currentAiMsg.thinkingText + chunk.text)
                        is ChatStreamChunk.Content -> {
                            fullResponseForTTS += chunk.text
                            currentAiMsg.copy(text = currentAiMsg.text + chunk.text)
                        }
                        is ChatStreamChunk.ToolCall -> {
                            pendingToolCall = Triple(chunk.id, chunk.name, chunk.arguments)
                            currentAiMsg.copy(text = currentAiMsg.text + "\n*(正在为您安排提醒...)*")
                        }
                        is ChatStreamChunk.Error -> currentAiMsg.copy(text = "系统故障: ${chunk.message}")
                    }
                    chatMessageDao.insertMessage(currentAiMsg)
                }
                
                pendingToolCall?.let { (id, name, argsJson) ->
                    if (name == "add_medication_reminder") {
                        handleToolAddReminder(argsJson)
                    }
                }
                
                if (fullResponseForTTS.isNotBlank()) _ttsEvent.emit(fullResponseForTTS)
                updateSessionTitle(session, text)
            } catch (e: Exception) {
                chatMessageDao.insertMessage(aiMsgPlaceholder.copy(messageId = aiMsgId, text = "故障: ${e.message}"))
            } finally {
                _isSending.value = false
            }
        }
    }

    private fun handleToolAddReminder(argsJson: String) {
        viewModelScope.launch {
            try {
                val args = gson.fromJson(argsJson, Map::class.java)
                val name = args["medicine_name"]?.toString() ?: "未知药品"
                val dosage = args["dosage"]?.toString() ?: ""
                val time = args["time"]?.toString() ?: ""

                val reminder = MedicationReminder(medicineName = name, dosage = dosage, time = time)
                db.medicationReminderDao().insertReminder(reminder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
