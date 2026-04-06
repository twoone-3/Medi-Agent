package com.project.medi_agent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.medi_agent.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthProfileSubScreen(onBack: () -> Unit, viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val isSeniorMode = MaterialTheme.typography.bodyLarge.fontSize > 20.sp
    val profiles by viewModel.healthProfiles.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editKey by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadHealthProfiles() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的健康档案") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)
            .fillMaxSize()) {

            Text("Agent 已记住的健康信息", fontWeight = FontWeight.Bold, style = if (isSeniorMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            Button(onClick = {
                editKey = ""
                editContent = ""
                showEditDialog = true
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("添加新条目")
            }

            Spacer(Modifier.height(12.dp))

            if (profiles.isEmpty()) {
                Text("暂无健康档案记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(profiles) { p ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.key, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(p.content, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    editKey = p.key
                                    editContent = p.content
                                    showEditDialog = true
                                }) { Icon(Icons.Default.Edit, null) }
                                IconButton(onClick = {
                                    viewModel.deleteHealthProfile(p.key)
                                    Toast.makeText(context, "已删除 ${p.key}", Toast.LENGTH_SHORT).show()
                                }) { Icon(Icons.Default.Delete, null) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editKey.isBlank()) "添加健康条目" else "编辑：${editKey}") },
            text = {
                Column {
                    OutlinedTextField(value = editKey, onValueChange = { editKey = it }, label = { Text("键 (例如: allergies)") }, enabled = editKey.isBlank())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = editContent, onValueChange = { editContent = it }, label = { Text("内容 (例如: 青霉素过敏)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editKey.isBlank()) {
                        Toast.makeText(context, "键名不能为空", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.upsertHealthProfile(editKey.trim(), editContent.trim())
                        showEditDialog = false
                        Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("取消") } }
        )
    }
}



