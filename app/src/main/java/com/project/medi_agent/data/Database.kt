package com.project.medi_agent.data

import android.content.Context
import androidx.room.*
import com.project.medi_agent.ui.ChatMessage
import com.project.medi_agent.ui.ChatSession
import com.project.medi_agent.ui.MedicationLog
import com.project.medi_agent.ui.MedicationReminder
import com.project.medi_agent.ui.HealthProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY lastUpdateTime DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long
    
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)
}

@Dao
interface MedicationReminderDao {
    @Query("SELECT * FROM medication_reminders ORDER BY time ASC")
    fun getAllReminders(): Flow<List<MedicationReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminder)

    @Delete
    suspend fun deleteReminder(reminder: MedicationReminder)
}

@Dao
interface HealthProfileDao {
    @Query("SELECT * FROM health_profile")
    suspend fun getAllProfiles(): List<HealthProfile>

    @Query("SELECT * FROM health_profile WHERE `key` = :key")
    suspend fun getProfileByKey(key: String): HealthProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: HealthProfile)
}

@Dao
interface MedicationLogDao {
    @Query("SELECT * FROM medication_logs ORDER BY takenTime DESC")
    fun getAllLogs(): Flow<List<MedicationLog>>

    // 用于 Dashboard：查询最近 X 天的服药统计
    @Query("SELECT * FROM medication_logs WHERE takenTime > :sinceTime ORDER BY takenTime ASC")
    suspend fun getRecentLogs(sinceTime: Long): List<MedicationLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLog)
}

@Database(entities = [ChatSession::class, ChatMessage::class, MedicationReminder::class, MedicationLog::class, HealthProfile::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun medicationLogDao(): MedicationLogDao
    abstract fun healthProfileDao(): HealthProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mediagent_database"
                )
                .fallbackToDestructiveMigration(true) // 显式传递参数修复 Deprecated 警告
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
