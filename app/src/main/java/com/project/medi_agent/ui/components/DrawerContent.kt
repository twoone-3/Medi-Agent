package com.project.medi_agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.medi_agent.ui.ChatSession
import com.project.medi_agent.ui.MedicationReminder
import com.project.medi_agent.ui.Screen

@Composable
fun AppDrawer(
    selected: Screen,
    currentSessionId: String?,
    sessions: List<ChatSession>,
    reminders: List<MedicationReminder> = emptyList(),
    onDestinationClick: (Screen) -> Unit,
    onSessionClick: (ChatSession) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteSession: (ChatSession) -> Unit,
    onDeleteReminder: ((MedicationReminder) -> Unit)? = null
) {
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
    
    // 判断是否为长辈模式
    val isSeniorMode = MaterialTheme.typography.bodyLarge.fontSize > 20.sp

    // 删除确认弹窗
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("确认删除", style = if (isSeniorMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium) },
            text = { Text("确定要删除对话 \"${session.title}\" 吗？", style = if (isSeniorMode) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { onDeleteSession(session); sessionToDelete = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error, style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("取消", style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
        // App Title
        Box(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 20.dp)) {
            Text(
                "银发医倚", 
                style = if (isSeniorMode) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // New Chat Button
        NavigationDrawerItem(
            label = { Text("开启新对话", style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge) },
            selected = false,
            onClick = onNewChatClick,
            icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(if (isSeniorMode) 32.dp else 24.dp)) },
            modifier = Modifier.padding(bottom = 8.dp),
            shape = RoundedCornerShape(if (isSeniorMode) 16.dp else 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // --- 用药提醒展示区 ---
        if (reminders.isNotEmpty()) {
            Text(
                "当前的用药提醒",
                style = if (isSeniorMode) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 12.dp),
                fontWeight = FontWeight.Bold
            )
            
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                reminders.take(3).forEach { reminder ->
                    ReminderSmallCard(reminder, isSeniorMode, onDeleteReminder)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- 历史对话区 ---
        Text(
            "历史对话",
            style = if (isSeniorMode) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sessions) { session ->
                NavigationDrawerItem(
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                session.title, 
                                modifier = Modifier.weight(1f), 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis,
                                style = if (isSeniorMode) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
                            )
                            IconButton(
                                onClick = { sessionToDelete = session }, 
                                modifier = Modifier.size(if (isSeniorMode) 40.dp else 32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.outline, 
                                    modifier = Modifier.size(if (isSeniorMode) 24.dp else 18.dp)
                                )
                            }
                        }
                    },
                    selected = selected == Screen.Chat && session.id == currentSessionId,
                    onClick = { onSessionClick(session) },
                    icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(if (isSeniorMode) 28.dp else 22.dp)) },
                    shape = RoundedCornerShape(if (isSeniorMode) 16.dp else 8.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Settings Item
        NavigationDrawerItem(
            label = { Text("设置", style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge) },
            selected = selected == Screen.Settings,
            onClick = { onDestinationClick(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(if (isSeniorMode) 32.dp else 24.dp)) },
            shape = RoundedCornerShape(if (isSeniorMode) 16.dp else 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Health Profile Item (use medical kit icon)
        NavigationDrawerItem(
            label = { Text("我的健康档案", style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge) },
            selected = selected == Screen.HealthProfile,
            onClick = { onDestinationClick(Screen.HealthProfile) },
            icon = { Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(if (isSeniorMode) 32.dp else 24.dp)) },
            shape = RoundedCornerShape(if (isSeniorMode) 16.dp else 8.dp)
        )
    }
}

@Composable
fun ReminderSmallCard(reminder: MedicationReminder, isSeniorMode: Boolean, onDelete: ((MedicationReminder) -> Unit)?) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(if (isSeniorMode) 16.dp else 12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(if (isSeniorMode) 16.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Alarm, 
                null, 
                tint = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.size(if (isSeniorMode) 28.dp else 20.dp)
            )
            Spacer(Modifier.width(if (isSeniorMode) 16.dp else 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminder.medicineName, 
                    style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${reminder.time} | ${reminder.dosage}", 
                    style = if (isSeniorMode) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDelete != null) {
                IconButton(
                    onClick = { onDelete(reminder) }, 
                    modifier = Modifier.size(if (isSeniorMode) 40.dp else 24.dp)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        null, 
                        modifier = Modifier.size(if (isSeniorMode) 24.dp else 16.dp)
                    )
                }
            }
        }
    }
}
