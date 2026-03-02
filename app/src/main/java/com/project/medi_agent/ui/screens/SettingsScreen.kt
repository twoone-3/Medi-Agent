package com.project.medi_agent.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.project.medi_agent.ui.components.AppTopBar

@Composable
fun SettingsScreen(onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val prefsName = "mediagent_prefs"
    
    val textBaseUrlPref = "text_base_url"
    val textApiKeyPref = "text_api_key"
    val textModelNamePref = "text_model_name"
    val imageBaseUrlPref = "image_base_url"
    val imageApiKeyPref = "image_api_key"
    val seniorModePref = "senior_mode"
    val themeModePref = "theme_mode" // 0: System, 1: Light, 2: Dark

    val defaultTextBaseUrl = "https://open.bigmodel.cn/api/paas/v4/"
    val defaultTextModelName = "glm-4.7-flash"
    val defaultImageModelName = "glm-4.6v-flash"

    var textBaseUrl by remember { mutableStateOf("") }
    var textApiKey by remember { mutableStateOf("") }
    var textModelName by remember { mutableStateOf("") }
    var imageBaseModelName by remember { mutableStateOf("") }
    var isSeniorMode by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(0) }
    
    // 对话框显示状态
    var showThemeDialog by remember { mutableStateOf(false) }
    val themeOptions = listOf("跟随系统", "明亮模式", "暗黑模式")

    val prefs = remember { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        textBaseUrl = prefs.getString(textBaseUrlPref, defaultTextBaseUrl) ?: defaultTextBaseUrl
        textApiKey = prefs.getString(textApiKeyPref, "") ?: ""
        textModelName = prefs.getString(textModelNamePref, defaultTextModelName) ?: defaultTextModelName
        imageBaseModelName = prefs.getString(imageBaseUrlPref, defaultImageModelName) ?: defaultImageModelName
        isSeniorMode = prefs.getBoolean(seniorModePref, false)
        themeMode = prefs.getInt(themeModePref, 0)
    }

    // 主题选择对话框
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题") },
            text = {
                Column(Modifier.selectableGroup()) {
                    themeOptions.forEachIndexed { index, option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (themeMode == index),
                                    onClick = {
                                        themeMode = index
                                        prefs.edit { putInt(themeModePref, index) }
                                        showThemeDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (themeMode == index),
                                onClick = null // 点击 Row 处理
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("取消")
                }
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
            // --- 界面显示部分 ---
            Text(
                text = "界面显示", 
                style = MaterialTheme.typography.titleMedium, // 升级为 titleMedium
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // 1. 长辈模式（全行可点击）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isSeniorMode,
                        onValueChange = { 
                            isSeniorMode = it
                            prefs.edit { putBoolean(seniorModePref, it) }
                        },
                        role = Role.Switch
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "长辈模式",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "开启后字体将放大，界面更加清晰",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isSeniorMode,
                    onCheckedChange = null // 处理在 Row 的 toggleable 中
                )
            }

            // 2. 外观选择
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "外观",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = themeOptions[themeMode],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            // --- 大模型配置部分 ---
            Text(
                text = "大模型配置", 
                style = MaterialTheme.typography.titleMedium, // 升级为 titleMedium
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            OutlinedTextField(
                value = textBaseUrl,
                onValueChange = { textBaseUrl = it },
                label = { Text("文字模型 API 地址") },
                placeholder = { Text(defaultTextBaseUrl) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = textModelName,
                onValueChange = { textModelName = it },
                label = { Text("文字模型名称") },
                placeholder = { Text(defaultTextModelName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = imageBaseModelName,
                onValueChange = { imageBaseModelName = it },
                label = { Text("识图模型名称") },
                placeholder = { Text(defaultImageModelName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = textApiKey,
                onValueChange = { textApiKey = it },
                label = { Text("通用 API Key") },
                placeholder = { Text("在此输入 API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        prefs.edit {
                            putString(textBaseUrlPref, textBaseUrl)
                            putString(textApiKeyPref, textApiKey)
                            putString(textModelNamePref, textModelName)
                            putString(imageBaseUrlPref, imageBaseModelName)
                        }
                        Toast.makeText(context, "模型配置已保存", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("保存模型配置")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
