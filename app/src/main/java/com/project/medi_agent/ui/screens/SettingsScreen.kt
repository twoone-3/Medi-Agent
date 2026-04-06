package com.project.medi_agent.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.medi_agent.ui.MainViewModel
import com.project.medi_agent.ui.components.AppTopBar

@Composable
fun SettingsScreen(
    onMenuClick: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    var currentSubScreen by remember { mutableStateOf("main") }

    BackHandler(enabled = currentSubScreen == "model") {
        currentSubScreen = "main"
    }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState == "model") {
                (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeOut(animationSpec = tween(300)))
            } else {
                (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it / 3 } + fadeOut(animationSpec = tween(300)))
            }.using(SizeTransform(clip = false))
        },
        label = "SettingsTransition"
    ) { subScreen ->
        if (subScreen == "model") {
            ModelConfigSubScreen(
                onBack = { currentSubScreen = "main" },
                viewModel = viewModel
            )
        } else if (subScreen == "health") {
            HealthProfileSubScreen(
                onBack = { currentSubScreen = "main" },
                viewModel = viewModel
            )
        } else {
            MainSettingsSubScreen(
                onMenuClick = onMenuClick,
                onNavigateToModel = { currentSubScreen = "model" },
                onNavigateToHealth = { currentSubScreen = "health" }
            )
        }
    }
}

@Composable
fun MainSettingsSubScreen(onMenuClick: () -> Unit, onNavigateToModel: () -> Unit, onNavigateToHealth: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mediagent_prefs", Context.MODE_PRIVATE) }
    
    val isSeniorMode = MaterialTheme.typography.bodyLarge.fontSize > 20.sp
    val themeOptions = listOf("跟随系统", "明亮模式", "暗黑模式")
    
    var seniorModeState by remember { mutableStateOf(prefs.getBoolean("senior_mode", false)) }
    var themeMode by remember { mutableStateOf(prefs.getInt("theme_mode", 0)) }
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            options = themeOptions,
            onDismiss = { showThemeDialog = false },
            onSelect = { index ->
                themeMode = index
                prefs.edit { putInt("theme_mode", index) }
                showThemeDialog = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "设置", onMenuClick = onMenuClick)

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "界面显示", 
                style = if (isSeniorMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsToggleItem(
                icon = Icons.Default.Visibility,
                title = "长辈模式",
                subtitle = "开启后字体将放大，界面更加清晰",
                checked = seniorModeState,
                isSeniorMode = isSeniorMode,
                onCheckedChange = {
                    seniorModeState = it
                    prefs.edit { putBoolean("senior_mode", it) }
                }
            )

            SettingsClickItem(
                icon = Icons.Default.Palette,
                title = "外观主题",
                subtitle = themeOptions[themeMode],
                isSeniorMode = isSeniorMode,
                onClick = { showThemeDialog = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Text(
                "高级选项", 
                style = if (isSeniorMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsClickItem(
                icon = Icons.Default.Settings,
                title = "大模型配置",
                subtitle = "配置 API Key、模型名称及地址",
                isSeniorMode = isSeniorMode,
                showArrow = true,
                onClick = onNavigateToModel
            )

            SettingsClickItem(
                icon = Icons.Default.MedicalServices,
                title = "我的健康档案",
                subtitle = "查看与编辑 Agent 记住的健康信息",
                isSeniorMode = isSeniorMode,
                showArrow = true,
                onClick = onNavigateToHealth
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigSubScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mediagent_prefs", Context.MODE_PRIVATE) }
    val isSeniorMode = MaterialTheme.typography.bodyLarge.fontSize > 20.sp

    var textBaseUrl by remember { mutableStateOf(prefs.getString("text_base_url", "https://open.bigmodel.cn/api/paas/v4/") ?: "") }
    var textApiKey by remember { mutableStateOf(prefs.getString("text_api_key", "") ?: "") }
    var textModelName by remember { mutableStateOf(prefs.getString("text_model_name", "glm-4.7-flash") ?: "") }
    var imageBaseModelName by remember { mutableStateOf(prefs.getString("image_base_url", "glm-4.6v-flash") ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("大模型配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text("API 设置", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            ModelTextField("文字模型接口地址", textBaseUrl) { textBaseUrl = it }
            ModelTextField("文字模型名称", textModelName) { textModelName = it }
            ModelTextField("识图模型名称", imageBaseModelName) { imageBaseModelName = it }
            ModelTextField("通用 API Key", textApiKey, isPassword = true) { textApiKey = it }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    prefs.edit {
                        putString("text_base_url", textBaseUrl)
                        putString("text_api_key", textApiKey)
                        putString("text_model_name", textModelName)
                        putString("image_base_url", imageBaseModelName)
                    }
                    Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(if (isSeniorMode) 60.dp else 50.dp)
            ) {
                Text("保存并返回", style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp))

            // --- 开发者调试工具 ---
            Text("开发者调试工具", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    viewModel.debugShowFullscreenReminder()
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("预览全屏提醒界面")
            }

            OutlinedButton(
                onClick = {
                    viewModel.debugInjectTestReminder()
                    Toast.makeText(context, "已注入测试提醒", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("注入 1 分钟后闹钟数据")
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ModelTextField(label: String, value: String, isPassword: Boolean = false, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Composable
fun SettingsToggleItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, isSeniorMode: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(if (isSeniorMode) 32.dp else 24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = if (isSeniorMode) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun SettingsClickItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, isSeniorMode: Boolean, showArrow: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(if (isSeniorMode) 32.dp else 24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = if (isSeniorMode) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showArrow) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(if (isSeniorMode) 20.dp else 16.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun ThemeSelectionDialog(currentMode: Int, options: List<String>, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEachIndexed { index, option ->
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).selectable(selected = (currentMode == index), onClick = { onSelect(index) }, role = Role.RadioButton).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (currentMode == index), onClick = null)
                        Text(option, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
