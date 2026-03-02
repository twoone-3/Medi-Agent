package com.project.medi_agent.ui.screens

import android.Manifest
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.project.medi_agent.data.HistoryManager
import com.project.medi_agent.data.network.ApiRepository
import com.project.medi_agent.data.network.ChatStreamChunk
import com.project.medi_agent.data.network.ImageUtils
import com.project.medi_agent.ui.ChatMessage
import com.project.medi_agent.ui.ChatSession
import com.project.medi_agent.ui.components.AppTopBar
import com.project.medi_agent.ui.components.ChatBubble
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    session: ChatSession,
    onSessionUpdated: (ChatSession) -> Unit,
    openDrawer: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val historyManager = remember { HistoryManager(context) }
    val apiRepository = remember { ApiRepository(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    val messages = remember(session.id) { 
        mutableStateListOf<ChatMessage>().apply { addAll(historyManager.loadHistory(session.id)) }
    }
    
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isVoiceMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) } 
    
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- Launchers ---
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { cameraImageUri?.let { uri -> 
            val inputStream = context.contentResolver.openInputStream(uri)
            selectedImageBitmap = BitmapFactory.decodeStream(inputStream)
            showSheet = false
        } }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startCamera(context, { uri -> cameraImageUri = uri }, { uri -> cameraLauncher.launch(uri) })
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            val inputStream = context.contentResolver.openInputStream(it)
            selectedImageBitmap = BitmapFactory.decodeStream(inputStream)
            showSheet = false
        }
    }

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = session.title, onMenuClick = { openDrawer?.invoke() })

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.background),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
        ) {
            itemsIndexed(items = messages) { _, msg -> ChatBubble(msg) }
        }

        if (isRecording) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                    Text("正在听您说话...", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (selectedImageBitmap != null) {
            Box(modifier = Modifier.padding(8.dp).size(100.dp)) {
                Image(bitmap = selectedImageBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                IconButton(onClick = { selectedImageBitmap = null }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(12.dp))) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp).imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { isVoiceMode = !isVoiceMode }, enabled = !isSending) {
                Icon(if (isVoiceMode) Icons.Default.Keyboard else Icons.Default.Mic, contentDescription = null)
            }

            if (isVoiceMode) {
                Surface(
                    modifier = Modifier.weight(1f).height(56.dp).pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            isRecording = true
                            try { awaitRelease() } finally { isRecording = false }
                        })
                    },
                    shape = RoundedCornerShape(28.dp),
                    color = if (isRecording) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (isRecording) "松开 结束" else "按住 说话", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            } else {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("输入消息...") },
                    maxLines = 4,
                    enabled = !isSending,
                    shape = RoundedCornerShape(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            if (inputText.isNotBlank() || selectedImageBitmap != null) {
                IconButton(
                    onClick = {
                        if (!isSending) {
                            val userText = inputText.trim()
                            val imageBase64 = selectedImageBitmap?.let { ImageUtils.compressAndEncodeToBase64(it) }
                            inputText = ""
                            selectedImageBitmap = null
                            isSending = true
                            if (messages.isEmpty()) { onSessionUpdated(session.copy(title = if (userText.length > 10) userText.take(10) + "..." else "分析咨询")) }
                            val userMsgId = (messages.maxOfOrNull { it.id } ?: 0) + 1
                            messages.add(ChatMessage(userMsgId, userText, isUser = true, imageBase64 = imageBase64))
                            val aiMsgId = userMsgId + 1
                            var currentAiMsg = ChatMessage(aiMsgId, "", isUser = false)
                            messages.add(currentAiMsg)
                            scope.launch {
                                try {
                                    apiRepository.chatStream(messages.toList()).collect { chunk ->
                                        val index = messages.indexOfFirst { it.id == aiMsgId }
                                        if (index != -1) {
                                            when (chunk) {
                                                is ChatStreamChunk.Thinking -> {
                                                    currentAiMsg = currentAiMsg.copy(thinkingText = currentAiMsg.thinkingText + chunk.text)
                                                    messages[index] = currentAiMsg
                                                }
                                                is ChatStreamChunk.Content -> {
                                                    currentAiMsg = currentAiMsg.copy(text = currentAiMsg.text + chunk.text)
                                                    messages[index] = currentAiMsg
                                                }
                                                is ChatStreamChunk.Error -> messages[index] = currentAiMsg.copy(text = chunk.message)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    val index = messages.indexOfFirst { it.id == aiMsgId }
                                    if (index != -1) messages[index] = currentAiMsg.copy(text = "连接中断: ${e.localizedMessage}")
                                } finally {
                                    isSending = false
                                    historyManager.saveHistory(session.id, messages)
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                IconButton(onClick = { showSheet = true }, enabled = !isSending) {
                    Icon(Icons.Default.Add, contentDescription = "More")
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
                modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentItem(Icons.Default.PhotoCamera, "拍照") {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        startCamera(context, { uri -> cameraImageUri = uri }, { uri -> cameraLauncher.launch(uri) })
                    } else { permissionLauncher.launch(Manifest.permission.CAMERA) }
                }
                AttachmentItem(Icons.Default.Image, "相册") {
                    galleryLauncher.launch("image/*")
                }
            }
        }
    }
}

@Composable
fun AttachmentItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun startCamera(context: android.content.Context, onUriReady: (Uri) -> Unit, onLaunch: (Uri) -> Unit) {
    val directory = File(context.cacheDir, "images")
    directory.mkdirs()
    val file = File(directory, "img_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    onUriReady(uri)
    onLaunch(uri)
}
