package com.project.medi_agent.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.project.medi_agent.data.HistoryManager
import com.project.medi_agent.ui.ChatSession
import com.project.medi_agent.ui.Screen
import com.project.medi_agent.ui.components.AppDrawer
import kotlinx.coroutines.launch

@Composable
fun AppContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val historyManager = remember { HistoryManager(context) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentScreen by remember { mutableStateOf(Screen.Chat) }
    
    val sessions = remember { 
        mutableStateListOf<ChatSession>().apply {
            val loaded = historyManager.loadSessions()
            if (loaded.isEmpty()) {
                add(ChatSession())
            } else {
                addAll(loaded)
            }
        }
    }
    
    var currentSessionId by remember { 
        mutableStateOf(sessions.firstOrNull()?.id ?: "") 
    }

    val currentSession = sessions.find { it.id == currentSessionId } ?: sessions.first()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = !drawerState.isOpen && currentScreen == Screen.Settings) {
        currentScreen = Screen.Chat
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                AppDrawer(
                    selected = currentScreen,
                    currentSessionId = currentSessionId,
                    sessions = sessions,
                    onDestinationClick = { dest ->
                        currentScreen = dest
                        scope.launch { drawerState.close() }
                    },
                    onSessionClick = { session ->
                        currentSessionId = session.id
                        currentScreen = Screen.Chat
                        scope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        val newSession = ChatSession()
                        sessions.add(0, newSession)
                        historyManager.saveSessions(sessions)
                        currentSessionId = newSession.id
                        currentScreen = Screen.Chat
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { sessionToDelete ->
                        if (sessions.size > 1) {
                            val index = sessions.indexOfFirst { it.id == sessionToDelete.id }
                            sessions.removeAt(index)
                            historyManager.deleteSession(sessionToDelete.id)
                            historyManager.saveSessions(sessions)
                            if (currentSessionId == sessionToDelete.id) {
                                currentSessionId = sessions.first().id
                            }
                        }
                    }
                )
            }
        }
    ) {
        // 使用 AnimatedContent 增加页面切换的平滑动画
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                // 定义渐入渐出动画，时长 300ms
                fadeIn(animationSpec = tween(300))
                    .togetherWith(fadeOut(animationSpec = tween(300)))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            Box(modifier = modifier.fillMaxSize()) {
                when (targetScreen) {
                    Screen.Chat -> ChatScreen(
                        modifier = Modifier.fillMaxSize(),
                        session = currentSession,
                        onSessionUpdated = { updated ->
                            val idx = sessions.indexOfFirst { it.id == updated.id }
                            if (idx != -1) {
                                sessions[idx] = updated
                                historyManager.saveSessions(sessions)
                            }
                        },
                        openDrawer = { scope.launch { drawerState.open() } }
                    )
                    Screen.Settings -> SettingsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}