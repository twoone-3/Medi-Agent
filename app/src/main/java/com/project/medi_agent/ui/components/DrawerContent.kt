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
    reminders: List<MedicationReminder> = emptyList(), // 新增提醒列表
    onDestinationClick: (Screen) -> Unit,
    onSessionClick: (ChatSession) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteSession: (ChatSession) -> Unit,
    onDeleteReminder: ((MedicationReminder) -> Unit)? = null
) {
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }

    // 删除确认弹窗
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除对话 \"${session.title}\" 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { onDeleteSession(session); sessionToDelete = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 16.dp)) {
            Text("银发医倚", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        NavigationDrawerItem(
            label = { Text("开启新对话") },
            selected = false,
            onClick = onNewChatClick,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- 核心新增：用药提醒展示区 ---
        if (reminders.isNotEmpty()) {
            Text(
                "当前的用药提醒",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
            )
            
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                reminders.take(3).forEach { reminder ->
                    ReminderSmallCard(reminder, onDeleteReminder)
                }
                if (reminders.size > 3) {
                    Text(
                        "查看更多 (${reminders.size})...",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp).clickable { /* 跳转到完整列表 */ },
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- 历史对话区 ---
        Text(
            "历史对话",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sessions) { session ->
                NavigationDrawerItem(
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(session.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { sessionToDelete = session }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    selected = selected == Screen.Chat && session.id == currentSessionId,
                    onClick = { onSessionClick(session) },
                    icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("设置") },
            selected = selected == Screen.Settings,
            onClick = { onDestinationClick(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )
    }
}

@Composable
fun ReminderSmallCard(reminder: MedicationReminder, onDelete: ((MedicationReminder) -> Unit)?) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.medicineName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("${reminder.time} | ${reminder.dosage}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onDelete != null) {
                IconButton(onClick = { onDelete(reminder) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
