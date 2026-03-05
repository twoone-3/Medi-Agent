package com.project.medi_agent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    // 1. 会话列表
    val sessions: StateFlow<List<ChatSession>> = chatSessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 2. 当前会话指针
    private val _currentSessionId = MutableStateFlow<String?>(null)

    // 3. 当前会话对象 (自动计算)
    val currentSession: StateFlow<ChatSession?> = _currentSessionId.combine(sessions) { id, list ->
        id?.let { selectedId -> list.find { it.id == selectedId } } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 4. 消息列表
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = currentSession.flatMapLatest { session ->
        if (session == null) {
            flowOf(emptyList())
        } else {
            chatMessageDao.getMessagesForSession(session.id)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // 5. TTS 播报事件流
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
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = null
            }
        }
    }

    fun sendMessage(text: String, imageBase64: String?) {
        viewModelScope.launch {
            val session = currentSession.value
            if (session == null) return@launch

            if (_isSending.value || (text.isBlank() && imageBase64 == null)) return@launch
            _isSending.value = true

            // 1. 保存用户消息
            val userMsg = ChatMessage(sessionId = session.id, text = text, isUser = true, imageBase64 = imageBase64)
            chatMessageDao.insertMessage(userMsg)

            // 2. 创建 AI 消息占位符
            val aiMsgPlaceholder = ChatMessage(sessionId = session.id, text = "", isUser = false)
            val aiMsgId = chatMessageDao.insertMessage(aiMsgPlaceholder)

            try {
                val history = messages.value
                var currentAiMsg = aiMsgPlaceholder.copy(messageId = aiMsgId)
                var fullResponseForTTS = ""

                apiRepository.chatStream(history).collect { chunk ->
                    currentAiMsg = when (chunk) {
                        is ChatStreamChunk.Thinking -> currentAiMsg.copy(thinkingText = currentAiMsg.thinkingText + chunk.text)
                        is ChatStreamChunk.Content -> {
                            fullResponseForTTS += chunk.text
                            currentAiMsg.copy(text = currentAiMsg.text + chunk.text)
                        }
                        is ChatStreamChunk.Error -> currentAiMsg.copy(text = "系统故障: ${chunk.message}")
                    }
                    chatMessageDao.insertMessage(currentAiMsg)
                }
                
                // --- 触发 TTS 播报 ---
                if (fullResponseForTTS.isNotBlank()) {
                    _ttsEvent.emit(fullResponseForTTS)
                }
                
                if (session.title == "新对话" || session.title == "分析咨询") {
                    val newTitle = if (text.length > 12) text.take(12) + "..." else text
                    chatSessionDao.insertSession(session.copy(title = newTitle, lastUpdateTime = System.currentTimeMillis()))
                } else {
                    chatSessionDao.insertSession(session.copy(lastUpdateTime = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                chatMessageDao.insertMessage(aiMsgPlaceholder.copy(messageId = aiMsgId, text = "故障: ${e.message}"))
            } finally {
                _isSending.value = false
            }
        }
    }
}
