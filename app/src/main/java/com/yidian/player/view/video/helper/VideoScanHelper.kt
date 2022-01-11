package com.yidian.player.view.video.helper

import android.app.Application
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.blankj.utilcode.util.FileUtils
import com.yidian.player.YiDianApp
import com.yidian.player.utils.StorageUtils
import java.io.File
import java.util.concurrent.Executors

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/9
 */
object VideoScanHelper : Runnable {

    private val VIDEO_EXTENSIONS: List<String> = listOf(
        "mp4", "avi", "3gp", "3g2", "dl", "dif", "dv", "fli", "m4v",
        "mpg", "mpe", "mov", "mxu", "lsf", "lsx", "mng", "asf", "asx",
        "wm", "wmv", "wmx", "wvx", "movie", "webm", "ts", "rmvb", "rv",
        "flv", "mkv"
    )

    @Volatile
    private var isScanning: Boolean = false

    private val application: Application
        get() = YiDianApp.application

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "video_scan_thread")
    }

    private val listeners: MutableList<OnScanCompletedListener> = mutableListOf()

    fun scanLocalVideos() {
//        if (isScanning) return
//        synchronized(this) {
//            executor.submit(this)
//            isScanning = true
//        }
    }

    fun addOnScanCompletedListener(listener: OnScanCompletedListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeOnScanCompletedListener(listener: OnScanCompletedListener) {
        listeners.remove(listener)
    }

    private fun notifyScanCompleted() {
        listeners.forEach {
            it.onScanCompleted()
        }
    }

    override fun run() {
        try {
            val startTime = System.currentTimeMillis()
            val scanFiles: MutableSet<String> = mutableSetOf()
            val externalSdCardPath = StorageUtils.getExternalSdCardRoot(application)
            //扫描外置 SDCard
            if (!externalSdCardPath.isNullOrEmpty() && StorageUtils.isExternalSdcardMounted(application)) {
                scanSdCard(File(externalSdCardPath), scanFiles)
            }
            //扫描外置存储
            scanSdCard(Environment.getExternalStorageDirectory(), scanFiles)
            val useTime = System.currentTimeMillis() - startTime
            Log.e("tag", "useTime: $useTime, scanFiles: ${scanFiles.size}")
            if (scanFiles.isNotEmpty()) {
                MediaScannerConnection.scanFile(application, scanFiles.toTypedArray(), null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            notifyScanCompleted()
            isScanning = false
        }
    }

    private fun scanSdCard(file: File, scanFiles: MutableSet<String>) {
        if (!file.exists() || !file.isDirectory) {
            return
        }
        val files = file.listFiles()
        if (files.isNullOrEmpty()) {
            return
        }
        files.forEach {
            if (!it.exists() || it.name.startsWith(".")) {
                return@forEach
            }
            if (it.isDirectory && !shouldContinueLoop(it.absolutePath)) {
                scanSdCard(it, scanFiles)
            } else if (isVideoFile(it)) {
                scanFiles.add(it.absolutePath)
            }
        }
    }

    /**
     * 判断是否应该跳过循环
     */
    private fun shouldContinueLoop(path: String): Boolean {
        if (path.lowercase().contains("android/data")) {
            //不扫描 android/data 目录
            return true
        }
        return if (path.contains("emulated/0/")) {
            path.split(File.separator).size > 8
        } else {
            path.split(File.separator).size > 7
        }
    }

    /**
     * 判断是否是视频文件
     */
    private fun isVideoFile(file: File): Boolean {
        if (!file.exists() || !file.canRead()) {
            return false
        }
        val list = FileUtils.getFileName(file).split(".")
        if (list.isEmpty()) {
            return false
        }
        return VIDEO_EXTENSIONS.contains(list.last())
    }

    interface OnScanCompletedListener {

        fun onScanCompleted()

    }

}