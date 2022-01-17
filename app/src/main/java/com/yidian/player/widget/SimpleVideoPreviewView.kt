package com.yidian.player.widget

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import com.yidian.player.base.layoutInflater
import com.yidian.player.base.viewScope
import com.yidian.player.databinding.LayoutSimpleVideoPreviewViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/16
 */
class SimpleVideoPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewBinding = LayoutSimpleVideoPreviewViewBinding.inflate(layoutInflater, this)

    private val previewIv: ImageView
        get() = viewBinding.previewIv

    private var mediaMetadataRetriever: MediaMetadataRetriever? = null

    private val retrieveJobs: MutableList<Job> = mutableListOf()

    private var lastRetrievePosition: Long = 0
    private var lastRetrieveTimeMillis: Long = 0

    fun setDataSource(uri: Uri) {
        release()
        mediaMetadataRetriever = MediaMetadataRetriever().also {
            it.setDataSource(context, uri)
        }
    }

    fun seekTo(position: Long) {
        if (abs(position - lastRetrievePosition) < 1000) {
            return
        }
        val currentTimeMillis = SystemClock.uptimeMillis()
        if (currentTimeMillis - lastRetrieveTimeMillis <= 50) {
            return
        }
        lastRetrievePosition = position
        lastRetrieveTimeMillis = currentTimeMillis
        viewScope.launch {
            val bitmap = getVideoFrameByTimeMillis(position)
            if (bitmap != null) {
                previewIv.setImageBitmap(bitmap)
            }
        }.also { job ->
            retrieveJobs.add(job)
            job.invokeOnCompletion {
                retrieveJobs.remove(job)
            }
        }
    }

    private suspend fun getVideoFrameByTimeMillis(position: Long): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                mediaMetadataRetriever?.getFrameAtTime(position * 1000)
            } catch (e: Exception) {
                Log.e("tag", "getVideoFrameByTimeMillis fail: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    fun start() {
        previewIv.setImageBitmap(null)
    }

    fun stop() {
        retrieveJobs.forEach {
            if (it.isActive) {
                it.cancel()
            }
        }
        retrieveJobs.clear()
    }

    fun release() {
        stop()
        mediaMetadataRetriever?.apply {
            release()
            mediaMetadataRetriever = null
        }
    }

}