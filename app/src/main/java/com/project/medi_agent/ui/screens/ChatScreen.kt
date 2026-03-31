package com.project.medi_agent.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.medi_agent.data.VoiceManager
import com.project.medi_agent.data.network.ImageUtils
import com.project.medi_agent.ui.ChatSession
import com.project.medi_agent.ui.MainViewModel
import com.project.medi_agent.ui.components.AppTopBar
import com.project.medi_agent.ui.components.ChatBubble
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    session: ChatSession,
    openDrawer: (() -> Unit)? = null,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val voiceManager = remember { VoiceManager(context) }
    val listState = rememberLazyListState()

    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val pendingReminder by viewModel.pendingReminder.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }

    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val isSeniorMode = MaterialTheme.typography.bodyLarge.fontSize > 20.sp

    // --- 核心：追踪当前正在播报的消息 ID ---
    var playingMessageId by remember { mutableLongStateOf(-1L) }

    // 监听自动 TTS 事件 (AI 回复结束时触发)
    LaunchedEffect(Unit) {
        viewModel.ttsEvent.collect { text ->
            val lastAiMsg = messages.lastOrNull { !it.isUser }
            playingMessageId = lastAiMsg?.messageId ?: -1L
            val cleanText = text.replace(Regex("\\[ACTION:.*?]"), "")
            voiceManager.speak(cleanText, onComplete = { playingMessageId = -1L })
        }
    }

    DisposableEffect(Unit) {
        onDispose { voiceManager.destroy() }
    }

    // --- 提醒确认弹窗 (人在回路) ---
    pendingReminder?.let { reminder ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingReminder() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("为您定个闹钟？", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("药名：${reminder.medicineName}", style = MaterialTheme.typography.bodyLarge)
                    Text("时间：${reminder.time}", style = MaterialTheme.typography.bodyLarge)
                    Text("剂量：${reminder.dosage}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("确认后，我会准时提醒您吃药。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmReminder(reminder) },
                    modifier = Modifier.height(if (isSeniorMode) 56.dp else 40.dp)
                ) {
                    Text("确认", fontSize = if (isSeniorMode) 20.sp else 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingReminder() }) {
                    Text("不用了")
                }
            }
        )
    }

    // --- Launchers 略 ---
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraImageUri?.let {
                    val inputStream = context.contentResolver.openInputStream(it)
                    selectedImageBitmap = BitmapFactory.decodeStream(inputStream)
                    showSheet = false
                }
            }
        }

    val recordAudioPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                isRecording = true
                voiceManager.startListening(
                    onResult = { result -> inputText = result },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() },
                    onFinished = { isRecording = false }
                )
            }
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera(
                context,
                { uri -> cameraImageUri = uri },
                { uri -> cameraLauncher.launch(uri) })
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                selectedImageBitmap = BitmapFactory.decodeStream(inputStream)
                showSheet = false
            }
        }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = session.title, onMenuClick = { openDrawer?.invoke() })

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(
                    message = msg,
                    isPlaying = playingMessageId == msg.messageId,
                    onPlayAudio = { text ->
                        playingMessageId = msg.messageId
                        val cleanText = text.replace(Regex("\\[ACTION:.*?]"), "")
                        voiceManager.speak(cleanText, onComplete = { playingMessageId = -1L })
                    },
                    onStopAudio = {
                        voiceManager.stopSpeaking()
                        playingMessageId = -1L
                    }
                )
            }
        }

        if (selectedImageBitmap != null) {
            Box(modifier = Modifier
                .padding(8.dp)
                .size(if (isSeniorMode) 150.dp else 100.dp)) {
                Image(
                    bitmap = selectedImageBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { selectedImageBitmap = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (isSeniorMode) 36.dp else 24.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(if (isSeniorMode) 18.dp else 12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(if (isSeniorMode) 24.dp else 16.dp)
                    )
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isSeniorMode) 12.dp else 8.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (!isRecording) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            isRecording = true
                            voiceManager.startListening(
                                onResult = { result -> inputText = result },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                },
                                onFinished = { isRecording = false }
                            )
                        }
                    } else {
                        isRecording = false
                        voiceManager.stopListening()
                    }
                },
                modifier = Modifier.size(if (isSeniorMode) 56.dp else 48.dp),
                enabled = !isSending
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(if (isSeniorMode) 36.dp else 24.dp)
                )
            }

            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        if (isRecording) "正在听您说话..." else "输入消息...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                maxLines = 4,
                enabled = !isSending,
                shape = RoundedCornerShape(if (isSeniorMode) 32.dp else 28.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.width(if (isSeniorMode) 8.dp else 4.dp))

            if (inputText.isNotBlank() || selectedImageBitmap != null) {
                IconButton(
                    onClick = {
                        voiceManager.stopSpeaking()
                        playingMessageId = -1L
                        viewModel.sendMessage(
                            inputText,
                            selectedImageBitmap?.let { ImageUtils.compressAndEncodeToBase64(it) })
                        inputText = ""
                        selectedImageBitmap = null
                    },
                    modifier = Modifier.size(if (isSeniorMode) 56.dp else 48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isSeniorMode) 36.dp else 24.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = { showSheet = true }, 
                    modifier = Modifier.size(if (isSeniorMode) 56.dp else 48.dp),
                    enabled = !isSending
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "More",
                        modifier = Modifier.size(if (isSeniorMode) 36.dp else 24.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isSeniorMode) 64.dp else 48.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentItem(Icons.Default.PhotoCamera, "拍照", isSeniorMode) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startCamera(
                            context,
                            { uri -> cameraImageUri = uri },
                            { uri -> cameraLauncher.launch(uri) })
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                AttachmentItem(Icons.Default.Image, "相册", isSeniorMode) {
                    galleryLauncher.launch("image/*")
                }
            }
        }
    }
}

@Composable
fun AttachmentItem(icon: ImageVector, label: String, isSeniorMode: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(if (isSeniorMode) 80.dp else 64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(if (isSeniorMode) 40.dp else 32.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
    }
}

private fun startCamera(context: Context, onUriReady: (Uri) -> Unit, onLaunch: (Uri) -> Unit) {
    val directory = File(context.cacheDir, "images")
    directory.mkdirs()
    val file = File(directory, "img_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    onUriReady(uri)
    onLaunch(uri)
}
