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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.medi_agent.ui.MainViewModel
import com.project.medi_agent.ui.Screen
import com.project.medi_agent.ui.components.AppDrawer
import kotlinx.coroutines.launch

@Composable
fun AppContent(modifier: Modifier = Modifier) {
    val viewModel: MainViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentScreen by remember { mutableStateOf(Screen.Chat) }

    val sessions by viewModel.sessions.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val reminders by viewModel.reminders.collectAsState() // 获取提醒

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
                    currentSessionId = currentSession?.id,
                    sessions = sessions,
                    reminders = reminders, // 传入提醒列表
                    onDestinationClick = { dest ->
                        currentScreen = dest
                        scope.launch { drawerState.close() }
                    },
                    onSessionClick = { session ->
                        viewModel.selectSession(session)
                        currentScreen = Screen.Chat
                        scope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        viewModel.createNewSession()
                        currentScreen = Screen.Chat
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { sessionToDelete ->
                        viewModel.deleteSession(sessionToDelete)
                    },
                    onDeleteReminder = { reminder ->
                        viewModel.deleteReminder(reminder)
                    }
                )
            }
        }
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)).togetherWith(fadeOut(animationSpec = tween(300)))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            Box(modifier = modifier.fillMaxSize()) {
                when (targetScreen) {
                    Screen.Chat -> {
                        if (currentSession != null) {
                            ChatScreen(
                                modifier = Modifier.fillMaxSize(),
                                session = currentSession!!,
                                openDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                    }
                    Screen.Settings -> SettingsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}
