package com.project.medi_agent.ui

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// --- Chat History Models ---
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val sessionId: String, 
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val thinkingText: String = "",
    val imageBase64: String? = null
)

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val lastUpdateTime: Long = System.currentTimeMillis()
)

// --- Agent: Medication & Reminders ---
@Entity(tableName = "medication_reminders")
data class MedicationReminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val medicineName: String,
    val dosage: String,
    val time: String,
    val repeatType: String = "DAILY",
    val isActive: Boolean = true
)

// --- 【核心新增】：用户健康画像 (用于 RAG / 长期记忆) ---
@Entity(tableName = "health_profile")
data class HealthProfile(
    @PrimaryKey val key: String, // 例如: "allergies", "chronic_diseases"
    val content: String,        // 例如: "青霉素, 阿司匹林", "高血压, 糖尿病"
    val lastUpdateTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val medicineName: String,
    val takenTime: Long = System.currentTimeMillis(),
    val status: String = "TAKEN"
)

enum class Screen {
    Chat, Settings, HealthProfile
}
