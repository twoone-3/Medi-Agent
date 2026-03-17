package com.project.medi_agent.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.node.*
import org.commonmark.parser.Parser

@Composable
fun MarkdownText(
    modifier: Modifier = Modifier,
    content: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isThinking: Boolean = false
) {
    val parser = remember { Parser.builder().build() }
    val document = remember(content) { parser.parse(content) }

    val inlineCodeColor = MaterialTheme.colorScheme.secondary
    val linkColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        RenderNodes(document, color, inlineCodeColor, linkColor, isThinking)
    }
}

@Composable
private fun RenderNodes(
    parent: Node,
    color: Color,
    inlineCodeColor: Color,
    linkColor: Color,
    isThinking: Boolean
) {
    var node = parent.firstChild
    while (node != null) {
        when (val currentNode = node) {
            is Paragraph -> {
                Text(
                    text = buildMarkdownAnnotatedString(currentNode, color, inlineCodeColor, linkColor),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                )
                Spacer(modifier = Modifier.height(if (isThinking) 4.dp else 12.dp))
            }
            is Heading -> {
                val headingStyle = when (currentNode.level) {
                    1 -> MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.Black)
                    2 -> MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    else -> MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = buildMarkdownAnnotatedString(currentNode, color, inlineCodeColor, linkColor),
                    style = headingStyle,
                    modifier = Modifier.padding(top = if (isThinking) 4.dp else 16.dp, bottom = if (isThinking) 2.dp else 8.dp)
                )
            }
            is FencedCodeBlock -> {
                if (currentNode.info == "med_card") {
                    MedicationCard(currentNode.literal)
                } else {
                    CodeBlock(currentNode.literal, currentNode.info)
                }
                Spacer(modifier = Modifier.height(if (isThinking) 4.dp else 12.dp))
            }
            is BulletList -> RenderList(currentNode, color, inlineCodeColor, linkColor, isThinking)
            is OrderedList -> RenderList(currentNode, color, inlineCodeColor, linkColor, isThinking)
            is BlockQuote -> RenderBlockQuote(currentNode, color, inlineCodeColor, linkColor, isThinking)
            is ThematicBreak -> HorizontalDivider(modifier = Modifier.padding(vertical = if (isThinking) 8.dp else 16.dp))
        }
        node = node.next
    }
}

@Composable
private fun MedicationCard(content: String) {
    val context = LocalContext.current
    val isSeniorMode = MaterialTheme.typography.bodyLarge.fontSize > 20.sp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MedicalServices, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (isSeniorMode) 28.dp else 20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "用药确认卡",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            // 解析内容
            val lines = content.trim().split("\n")
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    val parts = line.split("：", ":", limit = 2)
                    if (parts.size == 2) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                parts[0].trim() + "：",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                parts[1].trim(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = if (isSeniorMode) 24.sp else 18.sp,
                                    lineHeight = if (isSeniorMode) 32.sp else 24.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // --- 核心：一键提醒按钮 ---
            Button(
                onClick = {
                    // 暂时只弹出提示，稍后实现具体闹钟逻辑
                    Toast.makeText(context, "正在为您同步到“用药闹钟”...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(if (isSeniorMode) 64.dp else 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "帮我定个闹钟",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "请严格遵照医嘱使用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderList(
    list: Node, 
    color: Color, 
    inlineCodeColor: Color, 
    linkColor: Color,
    isThinking: Boolean
) {
    var item = list.firstChild
    var index = 1
    while (item != null) {
        if (item is ListItem) {
            Row(modifier = Modifier.padding(start = 8.dp, bottom = if (isThinking) 2.dp else 6.dp)) {
                val bullet = if (list is BulletList) "• " else "$index. "
                Text(
                    bullet, 
                    color = color, 
                    fontWeight = FontWeight.Bold
                )
                Column { RenderNodes(item, color, inlineCodeColor, linkColor, isThinking) }
            }
            index++
        }
        item = item.next
    }
    Spacer(modifier = Modifier.height(if (isThinking) 4.dp else 8.dp))
}

@Composable
private fun RenderBlockQuote(
    quote: BlockQuote, 
    color: Color, 
    inlineCodeColor: Color, 
    linkColor: Color,
    isThinking: Boolean
) {
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    Column(
        modifier = Modifier
            .padding(start = 16.dp, top = 4.dp, bottom = if (isThinking) 4.dp else 12.dp)
            .drawBehind {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x = (-12).dp.toPx(), y = 0f),
                    size = Size(width = 4.dp.toPx(), height = size.height),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
    ) {
        RenderNodes(quote, color.copy(alpha = 0.8f), inlineCodeColor, linkColor, isThinking)
    }
}

@Composable
private fun CodeBlock(code: String, lang: String? = null) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (!lang.isNullOrBlank()) {
                Text(lang.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = code.trim(),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildMarkdownAnnotatedString(
    node: Node,
    color: Color,
    inlineCodeColor: Color,
    linkColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        // 统一设置基础颜色，避免内部 SpanStyle 重复设置 color 导致 Bold 失效
        withStyle(SpanStyle(color = color)) {
            fun appendNode(parent: Node) {
                var child = parent.firstChild
                while (child != null) {
                    when (child) {
                        is org.commonmark.node.Text -> append(child.literal)
                        is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendNode(child) }
                        is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendNode(child) }
                        is Code -> withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.LightGray.copy(alpha = 0.25f),
                            color = inlineCodeColor
                        )) { append(child.literal) }
                        is Link -> withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Bold)) { appendNode(child) }
                        is SoftLineBreak -> append(" ")
                        is HardLineBreak -> append("\n")
                        else -> appendNode(child) // 递归处理未知节点，确保内容不丢失
                    }
                    child = child.next
                }
            }
            appendNode(node)
        }
    }
}
