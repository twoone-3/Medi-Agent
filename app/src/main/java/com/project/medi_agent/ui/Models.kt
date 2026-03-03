package com.project.medi_agent.ui

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// --- Chat History Models ---
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val sessionId: String, // Foreign key to link with ChatSession
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
    val medicineName: String,     // 药品名称
    val dosage: String,           // 剂量 (如: 1片, 10ml)
    val time: String,             // 时间 (如: 08:00)
    val repeatType: String = "DAILY", // 频率
    val isActive: Boolean = true
)

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val medicineName: String,
    val takenTime: Long = System.currentTimeMillis(),
    val status: String = "TAKEN" // TAKEN, SKIPPED
)

// --- Other UI Models ---
enum class Screen {
    Chat, Settings
}
