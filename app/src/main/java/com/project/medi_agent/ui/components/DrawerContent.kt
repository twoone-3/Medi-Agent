package com.project.medi_agent.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.medi_agent.ui.ChatSession
import com.project.medi_agent.ui.Screen

@Composable
fun AppDrawer(
    selected: Screen,
    currentSessionId: String?,
    sessions: List<ChatSession>,
    onDestinationClick: (Screen) -> Unit,
    onSessionClick: (ChatSession) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteSession: (ChatSession) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
        // App Title
        Box(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 16.dp)) {
            Text("MediAgent", style = MaterialTheme.typography.headlineSmall)
        }

        // New Chat Button
        NavigationDrawerItem(
            label = { Text("开启新对话") },
            selected = false,
            onClick = onNewChatClick,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // History Sessions Title
        Text(
            "历史对话",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
        )

        // Sessions List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sessions) { session ->
                NavigationDrawerItem(
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = session.title,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { onDeleteSession(session) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Session",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
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

        // Settings Item
        NavigationDrawerItem(
            label = { Text("设置") },
            selected = selected == Screen.Settings,
            onClick = { onDestinationClick(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )
    }
}
