package com.yidian.player.view.video.helper

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.annotation.WorkerThread
import com.yidian.player.YiDianApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/9
 */
object VideoScanHelper : MediaScannerConnection.OnScanCompletedListener {

    private val VIDEO_EXTENSIONS: List<String> = listOf(
        "mp4", "avi", "3gp", "3g2", "dl", "dif", "dv", "fli", "m4v",
        "mpg", "mpe", "mov", "mxu", "lsf", "lsx", "mng", "asf", "asx",
        "wm", "wmv", "wmx", "wvx", "movie", "webm", "ts", "rmvb", "rv",
        "flv", "mkv"
    )

    @Volatile
    private var isScanning: Boolean = false

    private val context: Context
        get() = YiDianApp.application.applicationContext

    var onScanCompleted: ((Int) -> Unit)? = null

    /**
     * 记录当前在列表中显示的视频, 全盘扫描时做过滤用
     */
    private val currentVideoPathList: MutableList<String> = mutableListOf()

    /**
     * 扫描到的视频文件个数
     */
    private var scanFileCount: Int = 0

    /**
     * onScanCompleted 回调计数
     */
    private var onScanCompletedCount: Int = 0

    suspend fun scanVideoFiles(currentVideoPathList: List<String>) {
        if (isScanning) return
        isScanning = true
        //记录当前在列表中显示的视频, 全盘扫描时做过滤用
        this.currentVideoPathList.clear()
        this.currentVideoPathList.addAll(currentVideoPathList)
        //待扫描的视频文件
        val videoPathList = mutableListOf<String>()
        withContext(Dispatchers.IO) {
            //获取手机根目录
            val rootFile = Environment.getExternalStorageDirectory()
            //根目录的所有文件夹
            val primaryFileDirs = mutableListOf<File>()
            if (rootFile.exists() && rootFile.isDirectory) {
                rootFile.listFiles()?.forEach {
                    if (shouldSkipDirectory(it)) {
                        return@forEach
                    }
                    val filePath = it.absolutePath
                    if (it.isDirectory) {
                        primaryFileDirs.add(it)
                    } else if (canAddVideoFile(it)) {
                        videoPathList.add(filePath)
                    }
                }
            }
            primaryFileDirs.map {
                //每一个根目录的文件夹对应一个 Deferred
                async { doScanVideoFiles(it, videoPathList) }
            }.forEach {
                //所有根目录同时开始扫描, 加快扫描速度
                it.await()
            }
        }
        Log.e("tag", "scanVideoFiles videoPathList: $videoPathList")
        //扫描文件结束, 准备开始通知媒体库更新
        scanFileCount = videoPathList.size
        onScanCompletedCount = scanFileCount
        if (videoPathList.isNotEmpty()) {
            MediaScannerConnection.scanFile(
                context,
                videoPathList.toTypedArray(),
                null,
                this@VideoScanHelper
            )
        }
        //如果扫描到的视频文件数为0则立即回调扫描完成
        if (scanFileCount == 0) {
            notifyScanCompleted(0)
        }
    }

    private fun notifyScanCompleted(scanVideoCount: Int) {
        Log.e("tag", "notifyScanCompleted")
        isScanning = false
        onScanCompleted?.invoke(scanVideoCount)
    }

    override fun onScanCompleted(path: String, uri: Uri?) {
        Log.d("tag", "onScanCompleted: $path, $uri")
        onScanCompletedCount--
        if (onScanCompletedCount == 0) {
            notifyScanCompleted(scanFileCount)
        }
    }

    @WorkerThread
    private fun doScanVideoFiles(file: File, videoPathList: MutableList<String>) {
        if (!file.exists() || !file.isDirectory) {
            return
        }
        val files = file.listFiles()
        if (files.isNullOrEmpty()) {
            return
        }
        files.forEach {
            if (shouldSkipDirectory(it)) {
                return@forEach
            }
            if (it.isDirectory) {
                doScanVideoFiles(it, videoPathList)
            } else if (canAddVideoFile(it)) {
                videoPathList.add(it.absolutePath)
            }
        }
    }

    private fun shouldSkipDirectory(file: File): Boolean {
        if (!file.exists()) {
            return true
        }
        //不扫描隐藏目录
        if (file.name.startsWith(".")) {
            return true
        }
        //不扫描 android/data 目录
        if (file.absolutePath.contains("android/data", true)) {
            return true
        }
        return false
    }

    /**
     * 判断是否是能添加指定的视频文件
     */
    private fun canAddVideoFile(file: File): Boolean {
        if (!file.exists() || !file.canRead() || !file.isFile) {
            return false
        }
        val filePath = file.absolutePath ?: ""
        val fileName = file.name ?: ""
        if (filePath.isEmpty() || fileName.isEmpty()) {
            return false
        }
        //获取后缀名
        val fileNameList = fileName.split(".")
        if (fileNameList.isEmpty()) {
            return false
        }
        //后缀名不是视频文件
        if (!VIDEO_EXTENSIONS.contains(fileNameList.last())) {
            return false
        }
        //没有在列表中显示
        return !currentVideoPathList.contains(filePath)
    }

}