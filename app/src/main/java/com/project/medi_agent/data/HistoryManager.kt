package com.project.medi_agent.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.project.medi_agent.ui.ChatMessage
import com.project.medi_agent.ui.ChatSession
import java.io.File

class HistoryManager(context: Context) {
    private val gson = Gson()
    private val sessionsFile = File(context.filesDir, "sessions.json")
    private val historyDir = File(context.filesDir, "history").apply { mkdirs() }

    // --- Session Management ---

    fun saveSessions(sessions: List<ChatSession>) {
        try {
            sessionsFile.writeText(gson.toJson(sessions))
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun loadSessions(): List<ChatSession> {
        return try {
            if (sessionsFile.exists()) {
                val json = sessionsFile.readText()
                val type = object : TypeToken<List<ChatSession>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    // --- Message Management per Session ---

    fun saveHistory(sessionId: String, messages: List<ChatMessage>) {
        try {
            val file = File(historyDir, "chat_$sessionId.json")
            file.writeText(gson.toJson(messages))
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun loadHistory(sessionId: String): List<ChatMessage> {
        return try {
            val file = File(historyDir, "chat_$sessionId.json")
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<List<ChatMessage>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }
    
    fun deleteSession(sessionId: String) {
        File(historyDir, "chat_$sessionId.json").delete()
    }
}
