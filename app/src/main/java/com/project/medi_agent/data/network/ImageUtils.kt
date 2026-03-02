package com.project.medi_agent.data.network

import android.graphics.Bitmap
import android.util.Base64
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream

/**
 * 图像处理工具类
 * 负责图片的缩放、压缩及 Base64 转换，优化 AI 接口传输效率
 */
object ImageUtils {

    /**
     * 将 Bitmap 缩放到指定长宽以内，并压缩转为 Base64 字符串
     * 针对 AI 识图（如 GLM-4V）优化的默认参数：1024px, 60% 质量
     */
    fun compressAndEncodeToBase64(
        bitmap: Bitmap,
        maxSize: Int = 1024,
        quality: Int = 60
    ): String {
        // 1. 计算目标尺寸
        var width = bitmap.width
        var height = bitmap.height
        
        if (width > maxSize || height > maxSize) {
            val ratio = width.toFloat() / height.toFloat()
            if (ratio > 1) {
                width = maxSize
                height = (maxSize / ratio).toInt()
            } else {
                height = maxSize
                width = (maxSize * ratio).toInt()
            }
        }
        
        // 2. 缩放图片 (使用 Android KTX 扩展函数)
        val scaledBitmap = bitmap.scale(width, height, true)
        
        // 3. 压缩并转码
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun bytesToBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
