package com.project.medi_agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
fun  MarkdownText(
    modifier: Modifier = Modifier,
    content: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val parser = remember { Parser.builder().build() }
    val document = remember(content) { parser.parse(content) }

    val inlineCodeColor = MaterialTheme.colorScheme.secondary
    val linkColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        RenderNodes(document, color, inlineCodeColor, linkColor)
    }
}

@Composable
private fun RenderNodes(parent: Node, color: Color, inlineCodeColor: Color, linkColor: Color) {
    var node = parent.firstChild
    while (node != null) {
        when (val currentNode = node) {
            is Paragraph -> {
                Text(
                    text = buildMarkdownAnnotatedString(currentNode, color, inlineCodeColor, linkColor),
                    color = color,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            is Heading -> {
                // 显著加大标题字号和加粗程度
                val headingStyle = when (currentNode.level) {
                    1 -> MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.Black)
                    2 -> MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    else -> MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = buildMarkdownAnnotatedString(currentNode, color, inlineCodeColor, linkColor),
                    style = headingStyle,
                    color = color,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            is FencedCodeBlock -> {
                CodeBlock(currentNode.literal, currentNode.info)
                Spacer(modifier = Modifier.height(12.dp))
            }
            is BulletList -> RenderList(currentNode, color, inlineCodeColor, linkColor)
            is OrderedList -> RenderList(currentNode, color, inlineCodeColor, linkColor)
            is BlockQuote -> RenderBlockQuote(currentNode, color, inlineCodeColor, linkColor)
            is ThematicBreak -> HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }
        node = node.next
    }
}

@Composable
private fun RenderList(list: Node, color: Color, inlineCodeColor: Color, linkColor: Color) {
    var item = list.firstChild
    var index = 1
    while (item != null) {
        if (item is ListItem) {
            Row(modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)) {
                val bullet = if (list is BulletList) "• " else "$index. "
                Text(bullet, color = color, fontWeight = FontWeight.Bold)
                Column { RenderNodes(item, color, inlineCodeColor, linkColor) }
            }
            index++
        }
        item = item.next
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun RenderBlockQuote(quote: BlockQuote, color: Color, inlineCodeColor: Color, linkColor: Color) {
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    // 使用 drawBehind 绘制垂直线，彻底规避 IntrinsicSize 编译错误
    Column(
        modifier = Modifier
            .padding(start = 16.dp, top = 4.dp, bottom = 12.dp)
            .drawBehind {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x = (-12).dp.toPx(), y = 0f),
                    size = Size(width = 4.dp.toPx(), height = size.height),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
    ) {
        RenderNodes(quote, color.copy(alpha = 0.8f), inlineCodeColor, linkColor)
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
        fun appendNode(parent: Node) {
            var child = parent.firstChild
            while (child != null) {
                when (child) {
                    is org.commonmark.node.Text -> append(child.literal)
                    is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendNode(child) }
                    // 强化加粗样式：使用 FontWeight.Black 确保在任何屏幕上都显眼
                    is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Black)) { appendNode(child) }
                    is Code -> withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.LightGray.copy(alpha = 0.25f),
                        color = inlineCodeColor
                    )) { append(child.literal) }
                    is Link -> withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Bold)) { appendNode(child) }
                    is SoftLineBreak -> append(" ")
                    is HardLineBreak -> append("\n")
                }
                child = child.next
            }
        }
        appendNode(node)
    }
}