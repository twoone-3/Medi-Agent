package com.project.medi_agent.ui.components

import android.graphics.BitmapFactory
import android.text.format.DateFormat
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.project.medi_agent.ui.ChatMessage

@Composable
fun ChatBubble(
    message: ChatMessage, 
    isPlaying: Boolean = false,
    onPlayAudio: (String) -> Unit = {},
    onStopAudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    
    // 判断是否为长辈模式
    val isSeniorMode = MaterialTheme.typography.bodyLarge.fontSize > 20.sp
    
    var showFullScreen by remember { mutableStateOf(false) }

    val bitmap = remember(message.imageBase64) {
        message.imageBase64?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) { null }
        }
    }

    if (showFullScreen && bitmap != null) {
        FullScreenImageDialog(
            bitmap = bitmap,
            onDismiss = { showFullScreen = false }
        )
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = if (isSeniorMode) 340.dp else 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // --- 1. 思考过程 ---
            if (!isUser && message.thinkingText.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(if (isSeniorMode) 12.dp else 8.dp),
                    modifier = Modifier.padding(bottom = 4.dp).clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(if (isSeniorMode) 12.dp else 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(if (isSeniorMode) 20.dp else 16.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "思考过程", 
                                style = if (isSeniorMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        AnimatedVisibility(visible = expanded) {
                            MarkdownText(
                                content = message.thinkingText,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // --- 2. 图片展示 ---
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .fillMaxWidth()
                        .heightIn(max = if (isSeniorMode) 300.dp else 240.dp)
                        .clip(RoundedCornerShape(if (isSeniorMode) 20.dp else 16.dp))
                        .clickable { showFullScreen = true },
                    contentScale = ContentScale.Crop
                )
            }

            // --- 3. 文字内容气泡 ---
            if (message.text.isNotEmpty()) {
                Surface(
                    tonalElevation = if (isUser) 2.dp else 0.dp,
                    shape = RoundedCornerShape(if (isSeniorMode) 20.dp else 16.dp),
                    color = bubbleColor
                ) {
                    Column(modifier = Modifier.padding(horizontal = if (isSeniorMode) 16.dp else 12.dp, vertical = if (isSeniorMode) 12.dp else 8.dp)) {
                        if (isUser) {
                            Text(
                                text = message.text, 
                                style = MaterialTheme.typography.bodyLarge, 
                                color = textColor
                            )
                        } else {
                            // 修复点：移除非法的 style 参数
                            MarkdownText(
                                content = message.text, 
                                color = textColor
                            )
                        }
                    }
                }
            }

            // --- 4. 外部时间戳与语音控制 ---
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isUser && message.text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (isPlaying) onStopAudio() else onPlayAudio(message.text)
                        },
                        modifier = Modifier.size(if (isSeniorMode) 36.dp else 24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                            contentDescription = "Speak",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (isSeniorMode) 30.dp else 20.dp)
                        )
                    }
                    Spacer(Modifier.width(if (isSeniorMode) 12.dp else 8.dp))
                }
                
                Text(
                    text = DateFormat.format("HH:mm", message.timestamp).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (isSeniorMode) 14.sp else 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun FullScreenImageDialog(bitmap: android.graphics.Bitmap, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding()) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}
