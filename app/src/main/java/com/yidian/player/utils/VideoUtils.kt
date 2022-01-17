package com.yidian.player.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.TextureView
import androidx.annotation.WorkerThread
import com.yidian.player.YiDianApp
import java.io.File

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/14
 */
object VideoUtils {

    private val context: Context
        get() = YiDianApp.application.applicationContext

    /**
     * 获取视频的时长, 单位毫秒
     */
    @WorkerThread
    fun getVideoDurationWithTimeMillis(videoUri: Uri): Long {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            if (!duration.isNullOrEmpty()) {
                return duration.toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * 获取视频指定位置的关键帧
     */
    @WorkerThread
    fun getVideoFrameByTimeMillis(videoUri: Uri, timeMillis: Long): Bitmap? {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val bitmap = retriever.getFrameAtTime(timeMillis * 1000)
            retriever.release()
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取相对路径
     */
    fun getRelativePath(path: String?): String {
        if (path.isNullOrEmpty()) {
            //路径不合法
            return ""
        }
        val lastSeparatorIndex = path.lastIndexOf(File.separator)
        if (lastSeparatorIndex == -1) {
            //路径不合法
            return ""
        }
        return path.substring(0, lastSeparatorIndex)
    }

    fun getVideoLength(filePath: String?): Long {
        if (filePath.isNullOrEmpty()) {
            return 0
        }
        val file = File(filePath)
        return if (file.exists()) {
            file.length()
        } else {
            0
        }
    }

    /**
     * 格式化大小
     */
    fun formatFileSize(sizeBytes: Long): String {
        return when {
            sizeBytes < 1024L * 1024L -> {
                val value = (sizeBytes * 1f) / (1024L)
                "${String.format("%.2f", value)}KB"
            }
            sizeBytes < 1024L * 1024L * 1024L -> {
                val value = (sizeBytes * 1f) / (1024L * 1024L)
                "${String.format("%.2f", value)}MB"
            }
            sizeBytes < 1024L * 1024L * 1024L * 1024L -> {
                val value = (sizeBytes * 1f) / (1024L * 1024L * 1024L)
                "${String.format("%.2f", value)}GB"
            }
            else -> {
                val value = (sizeBytes * 1f) / (1024L * 1024L * 1024L * 1024L)
                "${String.format("%.2f", value)}TB"
            }
        }
    }

    /**
     * 获取一个时间长度对应的 时分秒
     */
    fun formatTimeMillis(timeMillis: Long): String {
        val seconds = timeMillis / 1000L
        if (seconds < 1) {
            return "00:00"
        }
        if (seconds < 60) {
            return String.format("00:%02d", seconds)
        }
        val minutes = seconds / 60
        val extraSeconds = seconds % 60
        if (seconds < 60 * 60) {
            return String.format("%02d:%02d", minutes, extraSeconds)
        }
        val hours = seconds / 3600
        val extraMinutes = seconds % 3600 / 60
        val extraExtraSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, extraMinutes, extraExtraSeconds)
    }

    fun applyTextureViewRotation(textureView: TextureView, textureViewRotation: Int) {
        val transformMatrix = Matrix()
        val textureViewWidth = textureView.width.toFloat()
        val textureViewHeight = textureView.height.toFloat()
        if (textureViewWidth != 0f && textureViewHeight != 0f && textureViewRotation != 0) {
            val pivotX = textureViewWidth / 2
            val pivotY = textureViewHeight / 2
            transformMatrix.postRotate(textureViewRotation.toFloat(), pivotX, pivotY)

            // After rotation, scale the rotated texture to fit the TextureView size.
            val originalTextureRect = RectF(0f, 0f, textureViewWidth, textureViewHeight)
            val rotatedTextureRect = RectF()
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect)
            transformMatrix.postScale(
                textureViewWidth / rotatedTextureRect.width(),
                textureViewHeight / rotatedTextureRect.height(),
                pivotX,
                pivotY
            )
        }
        textureView.setTransform(transformMatrix)
    }

}