package com.project.medi_agent.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val defaultTextBaseUrl = "https://open.bigmodel.cn/api/paas/v4/"
    val defaultTextModelName = "glm-4.7-flash"
    val defaultImageModelName = "glm-4.6v-flash"

    var textBaseUrl by remember { mutableStateOf("") }
    var textApiKey by remember { mutableStateOf("") }
    var textModelName by remember { mutableStateOf("") }
    var imageBaseModelName by remember { mutableStateOf("") }
    var imageApiKey by remember { mutableStateOf("") }
    var isSeniorMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        textBaseUrl = prefs.getString(textBaseUrlPref, defaultTextBaseUrl) ?: defaultTextBaseUrl
        textApiKey = prefs.getString(textApiKeyPref, "") ?: ""
        textModelName = prefs.getString(textModelNamePref, defaultTextModelName) ?: defaultTextModelName
        imageBaseModelName = prefs.getString(imageBaseUrlPref, defaultImageModelName) ?: defaultImageModelName
        imageApiKey = prefs.getString(imageApiKeyPref, "") ?: ""
        isSeniorMode = prefs.getBoolean(seniorModePref, false)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "设置", onMenuClick = onMenuClick)

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // --- Senior Mode Section ---
            Text(text = "个性化设置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "长辈模式",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        fontSize = if (isSeniorMode) 20.sp else 16.sp
                    )
                    Text(
                        text = "开启后字体将放大，界面更加清晰",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = if (isSeniorMode) 14.sp else 12.sp
                    )
                }
                Switch(
                    checked = isSeniorMode,
                    onCheckedChange = { isSeniorMode = it }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(text = "文字模型配置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = textBaseUrl,
                onValueChange = { textBaseUrl = it },
                label = { Text("模型 API 地址") },
                placeholder = { Text("https://open.bigmodel.cn/api/paas/v4/") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = textModelName,
                onValueChange = { textModelName = it },
                label = { Text("默认文字模型名称") },
                placeholder = { Text("glm-4.7-flash") },
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

            Text(text = "识图模型配置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = imageBaseModelName,
                onValueChange = { imageBaseModelName = it },
                label = { Text("默认识图模型名称") },
                placeholder = { Text("glm-4.6v-flash") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        prefs.edit {
                            putString(textBaseUrlPref, textBaseUrl)
                            putString(textApiKeyPref, textApiKey)
                            putString(textModelNamePref, textModelName)
                            putString(imageBaseUrlPref, imageBaseModelName)
                            putString(imageApiKeyPref, imageApiKey)
                            putBoolean(seniorModePref, isSeniorMode)
                        }
                        Toast.makeText(context, "设置已保存", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("保存设置")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
